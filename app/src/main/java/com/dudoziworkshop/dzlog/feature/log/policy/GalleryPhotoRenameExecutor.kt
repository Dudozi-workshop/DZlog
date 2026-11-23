package com.dudoziworkshop.dzlog.feature.log.policy

import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.domain.naming.splitFileName
import java.util.UUID

internal data class GalleryStoredPhotoName(val name: String, val path: String)

internal interface GalleryPhotoRenameStorage {
    fun read(photo: MediaImageItem): GalleryStoredPhotoName?
    fun folderFiles(path: String): List<Pair<Long, String>>
    fun update(photo: MediaImageItem, name: String): Boolean
}

internal enum class GalleryRenameOutcome { Success, Failure, Excluded, Unchanged, RecoveryNeeded }

internal data class GalleryPhotoRenameReportRow(
    val preview: GalleryPhotoRenameRow,
    val outcome: GalleryRenameOutcome,
    val issue: String? = null,
    val recoveryNames: Map<Long, String> = emptyMap(),
)

internal data class GalleryPhotoRenameResult(val rows: List<GalleryPhotoRenameReportRow>) {
    val renamedIds get() = rows.filter { it.outcome == GalleryRenameOutcome.Success }
        .flatMap { it.preview.group.photos }.mapTo(mutableSetOf()) { it.id }
}

/** Runs the exact confirmed plan. A failed group never releases its old names to the next group. */
internal class GalleryPhotoRenameExecutor(private val storage: GalleryPhotoRenameStorage) {
    fun execute(
        plan: GalleryPhotoRenamePlan,
        unavailable: Map<Long, String> = emptyMap(),
    ): GalleryPhotoRenameResult {
        val outcomes = mutableMapOf<Long, GalleryPhotoRenameReportRow>()
        val pending = linkedMapOf<Long, GalleryPhotoRenameRow>()
        for (row in plan.rows) {
            val id = row.group.id
            outcomes[id] = when (row.status) {
                GalleryRenameStatus.Ready -> {
                    val issue = unavailable[id] ?: if (row.group.photos.all(::isOriginal)) null
                        else "파일 이름 또는 위치가 바뀌었습니다."
                    if (issue == null) {
                        pending[id] = row
                        continue
                    }
                    GalleryPhotoRenameReportRow(row, GalleryRenameOutcome.Excluded, issue)
                }
                GalleryRenameStatus.Unchanged -> GalleryPhotoRenameReportRow(row, GalleryRenameOutcome.Unchanged)
                else -> GalleryPhotoRenameReportRow(row, GalleryRenameOutcome.Excluded, row.issue)
            }
        }

        val sourceGroups = pending.values.flatMap { row ->
            row.group.photos.map { it.id to row.group.id }
        }.toMap()
        val paths = pending.values.flatMap { row -> row.group.photos.map { it.relativePath } }.distinct()
        val inventory = paths.associateWith { path -> runCatching { storage.folderFiles(path) } }
        val blockers = mutableMapOf<Long, Set<Long>>()
        for ((id, row) in pending) {
            val occupied = runCatching {
                row.group.photos.flatMap { photo ->
                    owners(inventory.getValue(photo.relativePath).getOrThrow(), row.finalName) - photo.id
                }.toSet()
            }.getOrNull()
            if (occupied == null || occupied.any { it !in sourceGroups }) {
                outcomes[id] = lateExclusion(row, unreadable = occupied == null)
            } else {
                blockers[id] = occupied.mapNotNull(sourceGroups::get).toSet()
            }
        }
        pending.keys.removeAll(outcomes.keys)

        while (pending.isNotEmpty()) {
            for ((id, row) in pending) {
                if (blockers.getValue(id).any { dependency ->
                        outcomes[dependency]?.outcome?.let { it != GalleryRenameOutcome.Success } == true
                    }) {
                    outcomes[id] = lateExclusion(row)
                }
            }
            if (pending.keys.removeAll(outcomes.keys)) continue

            val dependencies = pending.keys.associateWith { id -> blockers.getValue(id).intersect(pending.keys) }
            val independent = dependencies.entries.firstOrNull { it.value.isEmpty() }?.key
            val ids = if (independent != null) setOf(independent) else terminalCycle(dependencies)
            val rows = ids.map { pending.getValue(it) }
            // Staging is only needed when final names are genuinely interdependent (e.g. a swap).
            val results = executeGroup(rows, stage = independent == null)
            results.forEach { outcomes[it.preview.group.id] = it }
            pending.keys.removeAll(ids)
        }
        return GalleryPhotoRenameResult(plan.rows.map { outcomes.getValue(it.group.id) })
    }

    private fun lateExclusion(row: GalleryPhotoRenameRow, unreadable: Boolean = false) =
        GalleryPhotoRenameReportRow(
            row, GalleryRenameOutcome.Excluded,
            if (unreadable) "대상 폴더의 파일명을 확인하지 못했습니다."
            else "변경할 이름을 다른 파일에서 사용 중입니다.",
        )

    private fun owners(files: List<Pair<Long, String>>, name: String): Set<Long> =
        files.filter { it.second.equals(name, ignoreCase = true) }.mapTo(mutableSetOf()) { it.first }

    private fun destinationIsFree(photo: MediaImageItem, name: String): Boolean =
        (owners(storage.folderFiles(photo.relativePath), name) - photo.id).isEmpty()

    private fun terminalCycle(dependencies: Map<Long, Set<Long>>): Set<Long> {
        fun reachable(start: Long): Set<Long> {
            val stack = ArrayDeque(listOf(start))
            val visited = linkedSetOf<Long>()
            while (stack.isNotEmpty()) {
                val current = stack.removeLast()
                if (visited.add(current)) stack.addAll(dependencies.getValue(current))
            }
            return visited
        }
        for (id in dependencies.keys) {
            val reachable = reachable(id)
            if (reachable.all { id in reachable(it) }) return reachable
        }
        error("A finite dependency graph has a terminal component")
    }

    private fun executeGroup(
        rows: List<GalleryPhotoRenameRow>,
        stage: Boolean,
    ): List<GalleryPhotoRenameReportRow> {
        val journal = mutableListOf<RenameStep>()
        val unknown = mutableSetOf<Long>()
        val operation = runCatching {
            for (row in rows) check(row.group.photos.all(::isOriginal)) { "파일 이름 또는 위치가 바뀌었습니다." }
            val staged = if (stage) {
                rows.associate { row ->
                    val (_, extension) = splitFileName(row.group.oldName)
                    val temporaryName = "DZlog-rename-${UUID.randomUUID()}$extension"
                    for (photo in row.group.photos) {
                        change(photo, photo.displayName, temporaryName, journal, unknown)
                    }
                    row.group.id to temporaryName
                }
            } else emptyMap()
            for (row in rows) {
                for (photo in row.group.photos) {
                    change(photo, staged[row.group.id] ?: photo.displayName, row.finalName, journal, unknown)
                }
            }
            for (row in rows) {
                check(row.group.photos.all { storage.read(it) == metadata(it, row.finalName) }) {
                    "변경 결과를 확인하지 못했습니다."
                }
            }
        }
        if (operation.isSuccess) return rows.map { GalleryPhotoRenameReportRow(it, GalleryRenameOutcome.Success) }

        for (step in journal.asReversed()) {
            runCatching {
                if (storage.read(step.photo) == metadata(step.photo, step.after) &&
                    destinationIsFree(step.photo, step.before)) {
                    storage.update(step.photo, step.before)
                }
            }
        }
        val cause = operation.exceptionOrNull()
        val message = if (cause is SecurityException) "사진 수정 권한이 없습니다."
            else cause?.message ?: "사진 이름을 변경하지 못했습니다."
        return rows.map { row ->
            val recovery = row.group.photos
                .filter { it.id in unknown || journal.any { step -> step.photo.id == it.id } }
                .mapNotNull { photo ->
                    val actual = runCatching { storage.read(photo) }.getOrNull()
                    if (actual == metadata(photo, photo.displayName)) null
                    else photo.id to (actual?.name ?: "현재 이름 확인 필요")
                }.toMap()
            GalleryPhotoRenameReportRow(
                row,
                if (recovery.isEmpty()) GalleryRenameOutcome.Failure else GalleryRenameOutcome.RecoveryNeeded,
                if (journal.any { step -> step.photo.id in row.group.photos.map { it.id } } && recovery.isEmpty())
                    "$message 변경된 파일은 기존 이름으로 복구했습니다." else message,
                recovery,
            )
        }
    }

    private fun change(
        photo: MediaImageItem,
        before: String,
        target: String,
        journal: MutableList<RenameStep>,
        unknown: MutableSet<Long>,
    ) {
        check(storage.read(photo) == metadata(photo, before)) { "파일 이름 또는 위치가 바뀌었습니다." }
        check(destinationIsFree(photo, target)) {
            "변경 직전에 같은 이름의 파일이 생겼습니다."
        }
        val updated = runCatching { storage.update(photo, target) }
        val actual = runCatching { storage.read(photo) }.getOrNull()
        if (actual?.path == renamePath(photo.relativePath) && actual.name != before &&
            (updated.getOrNull() == true || actual.name == target)) {
            journal += RenameStep(photo, before, actual.name)
        } else if (actual != metadata(photo, before)) {
            unknown += photo.id
        }
        updated.getOrThrow()
        check(actual == metadata(photo, target) &&
            destinationIsFree(photo, target)) {
            "변경 결과를 확인하지 못했습니다."
        }
    }

    private fun isOriginal(photo: MediaImageItem): Boolean =
        runCatching { storage.read(photo) == metadata(photo, photo.displayName) }.getOrDefault(false)

    private fun metadata(photo: MediaImageItem, name: String) =
        GalleryStoredPhotoName(name, renamePath(photo.relativePath))

    private data class RenameStep(val photo: MediaImageItem, val before: String, val after: String)
}
