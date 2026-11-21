package com.dudoziworkshop.dzlog.feature.log.policy

import com.dudoziworkshop.dzlog.domain.model.MediaImageItem
import com.dudoziworkshop.dzlog.domain.naming.duplicateFileName
import java.util.Locale

internal const val GALLERY_PHOTO_RENAME_LIMIT = 100

internal fun renamePath(path: String) = path.trimEnd('/') + "/"

internal data class GalleryRenameLocation(val path: String, val name: String) {
    companion object {
        fun of(path: String, name: String) = GalleryRenameLocation(
            renamePath(path).lowercase(Locale.ROOT), name.lowercase(Locale.ROOT),
        )
    }
}

internal data class GalleryPhotoRenameGroup(
    val photos: List<MediaImageItem>,
    val pairingIssue: String? = null,
) {
    val id get() = photos.first().id
    val oldName get() = photos.first().displayName
    fun locations(name: String = oldName) = photos.map { GalleryRenameLocation.of(it.relativePath, name) }
}

internal sealed interface GalleryRenameResolution {
    data class Manual(val prefix: String) : GalleryRenameResolution
    data object Automatic : GalleryRenameResolution
    data object Exclude : GalleryRenameResolution
}

internal enum class GalleryRenameStatus { Ready, Conflict, Check, Excluded, Unchanged }

internal data class GalleryPhotoRenameRow(
    val group: GalleryPhotoRenameGroup,
    val proposedName: String,
    val finalName: String = proposedName,
    val status: GalleryRenameStatus,
    val issue: String? = null,
    val resolution: GalleryRenameResolution? = null,
)

internal data class GalleryPhotoRenamePlan(
    val rows: List<GalleryPhotoRenameRow>,
    val related: Map<Long, Set<Long>>,
) {
    val ready get() = rows.filter { it.status == GalleryRenameStatus.Ready }
    val writablePhotos get() = ready.flatMap { it.group.photos }

    fun conflictGroup(id: Long): Set<Long> {
        val queue = ArrayDeque(listOf(id))
        val members = mutableSetOf<Long>()
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (members.add(next)) queue.addAll(related[next].orEmpty())
        }
        return members
    }
}

internal object GalleryPhotoRenamePolicy {
    fun counterpartPath(path: String): String? = when {
        !renamePath(path).startsWith(GalleryFolderIndexPolicy.ROOT) -> null
        GalleryPhotoMovePolicy.isOriginalPath(path) -> GalleryFolderIndexPolicy.parentOf(path)
        else -> renamePath(path) + "original/"
    }

    /** Preserve selection order. Exact legacy name/path matches are the fallback for unindexed photos. */
    fun groups(
        selected: List<MediaImageItem>,
        allPhotos: List<MediaImageItem>,
        indexedPairs: Map<Long, Set<Long>> = emptyMap(),
    ): List<GalleryPhotoRenameGroup> {
        require(selected.isNotEmpty()) { "이름을 변경할 사진을 선택해 주세요." }
        val byLocation = allPhotos.groupBy { GalleryRenameLocation.of(it.relativePath, it.displayName) }
        val byId = allPhotos.associateBy { it.id }
        val included = mutableSetOf<Long>()
        return selected.mapNotNull { photo ->
            if (!included.add(photo.id)) return@mapNotNull null
            val same = byLocation[GalleryRenameLocation.of(photo.relativePath, photo.displayName)].orEmpty()
            val counterpart = counterpartPath(photo.relativePath)?.let {
                byLocation[GalleryRenameLocation.of(it, photo.displayName)]
            }.orEmpty()
            val recordedIds = indexedPairs[photo.id]
            val recorded = recordedIds?.mapNotNull(byId::get)?.filter { it.id != photo.id }
            val matches = recorded ?: counterpart
            val pair = matches.singleOrNull()
            val issue = when {
                !renamePath(photo.relativePath).startsWith(GalleryFolderIndexPolicy.ROOT) ->
                    "DZlog 외부 사진은 변경할 수 없습니다."
                same.size != 1 || matches.size > 1 ||
                    (recordedIds != null && recordedIds.any { it !in byId }) ||
                    (pair != null && (pair.displayName != photo.displayName ||
                        renamePath(pair.relativePath) != counterpartPath(photo.relativePath) ||
                        byLocation[GalleryRenameLocation.of(pair.relativePath, pair.displayName)]?.size != 1)) ||
                    (recorded == null && pair != null && indexedPairs[pair.id]?.contains(photo.id) == false) ->
                    "연결된 원본 사진을 확인해 주세요."
                else -> null
            }
            val members = if (issue == null) listOfNotNull(photo, pair) else listOf(photo)
            included.addAll(members.map { it.id })
            GalleryPhotoRenameGroup(members, issue)
        }
    }

    fun plan(
        groups: List<GalleryPhotoRenameGroup>,
        allPhotos: List<MediaImageItem>,
        rule: GalleryPhotoRenameRule,
        resolutions: Map<Long, GalleryRenameResolution> = emptyMap(),
        otherFiles: Set<GalleryRenameLocation> = emptySet(),
    ): GalleryPhotoRenamePlan {
        rule.validate()
        val occupants = allPhotos.groupBy { GalleryRenameLocation.of(it.relativePath, it.displayName) }
        val rows = groups.map { group -> proposedRow(group, rule, resolutions[group.id]) }
        val byId = rows.associateBy { it.group.id }
        val active = rows.filter { it.status == GalleryRenameStatus.Ready }
            .associateByTo(linkedMapOf()) { it.group.id }
        val excluded = mutableMapOf<Long, String>()
        val related = mutableMapOf<Long, MutableSet<Long>>()
        val sourceOwners = groups.flatMap { group -> group.photos.map { it.id to group.id } }.toMap()

        fun link(left: GalleryPhotoRenameRow, rightId: Long) {
            if (rightId == left.group.id) return
            related.getOrPut(left.group.id) { mutableSetOf() }.add(rightId)
            related.getOrPut(rightId) { mutableSetOf() }.add(left.group.id)
        }

        val allProposed = rows.filter { it.proposedName != it.group.oldName }
            .flatMap { row -> row.group.locations(row.proposedName).map { it to row } }.groupBy({ it.first }, { it.second })
        for ((location, contenders) in allProposed) {
            val sourceIds = occupants[location].orEmpty().mapNotNull { sourceOwners[it.id] }
            for (row in contenders) {
                contenders.forEach { link(row, it.group.id) }
                sourceIds.filter { byId[it]?.proposedName != byId[it]?.group?.oldName }
                    .forEach { link(row, it) }
            }
        }

        fun blocked(row: GalleryPhotoRenameRow, target: GalleryRenameLocation): Boolean =
            target in otherFiles || occupants[target].orEmpty().any { occupant ->
                sourceOwners[occupant.id] != row.group.id && sourceOwners[occupant.id] !in active
            }

        val allocated = mutableMapOf<Long, String>()
        while (true) {
            val fixed = active.values.filter { it.resolution != GalleryRenameResolution.Automatic }
            val claims = fixed.flatMap { row -> row.group.locations(row.finalName).map { it to row } }
                .groupBy({ it.first }, { it.second })
            val conflicts = mutableMapOf<Long, String>()
            for ((location, contenders) in claims) {
                val manual = contenders.filter { it.resolution is GalleryRenameResolution.Manual }.singleOrNull()
                for (row in contenders) {
                    if (contenders.size > 1) contenders.forEach { link(row, it.group.id) }
                    if (contenders.size > 1 && row != manual) {
                        conflicts[row.group.id] = "선택한 사진의 변경 후 이름이 서로 겹칩니다."
                    } else if (blocked(row, location)) {
                        conflicts[row.group.id] = if (location.path != renamePath(row.group.photos.first().relativePath)
                                .lowercase(Locale.ROOT) && row.group.photos.size > 1) {
                            "연결된 사진 폴더에서 이 이름을 사용 중입니다."
                        } else "이 폴더의 다른 파일이 이 이름을 사용 중입니다."
                    }
                }
            }
            if (conflicts.isNotEmpty()) {
                excluded.putAll(conflicts)
                active.keys.removeAll(conflicts.keys)
                continue
            }

            allocated.clear()
            val reserved = claims.keys.toMutableSet()
            for (row in active.values.filter { it.resolution == GalleryRenameResolution.Automatic }) {
                val available = availableName(row) { target -> target !in reserved && !blocked(row, target) }
                if (available == null) {
                    conflicts[row.group.id] = "사용할 수 있는 중복 구분번호가 있는 이름을 만들지 못했습니다."
                } else {
                    allocated[row.group.id] = available
                    reserved += row.group.locations(available)
                }
            }
            if (conflicts.isEmpty()) break
            excluded.putAll(conflicts)
            active.keys.removeAll(conflicts.keys)
        }

        return GalleryPhotoRenamePlan(rows.map { row ->
            val reason = excluded[row.group.id]
            val allocatedName = allocated[row.group.id]
            when {
                reason != null -> row.copy(status = GalleryRenameStatus.Conflict, issue = reason)
                allocatedName == row.group.oldName ->
                    row.copy(status = GalleryRenameStatus.Unchanged, finalName = allocatedName)
                allocatedName != null -> row.copy(finalName = allocatedName)
                else -> row
            }
        }, related)
    }

    private fun proposedRow(
        group: GalleryPhotoRenameGroup,
        rule: GalleryPhotoRenameRule,
        resolution: GalleryRenameResolution?,
    ): GalleryPhotoRenameRow {
        val old = group.oldName
        val original = GalleryPhotoName.parse(old)
        val proposed = runCatching { original.apply(rule) }
        val issue = group.pairingIssue ?: proposed.exceptionOrNull()?.message
        if (issue != null) return GalleryPhotoRenameRow(group, old, status = GalleryRenameStatus.Check, issue = issue)
        val target = proposed.getOrThrow()
        if (resolution == GalleryRenameResolution.Exclude) {
            return GalleryPhotoRenameRow(group, target, status = GalleryRenameStatus.Excluded, resolution = resolution)
        }
        val finalName = runCatching {
            if (resolution is GalleryRenameResolution.Manual) original.withPrefix(resolution.prefix) else target
        }
        return finalName.fold(
            onSuccess = { name ->
                GalleryPhotoRenameRow(group, target, name,
                    if (name == old) GalleryRenameStatus.Unchanged else GalleryRenameStatus.Ready,
                    resolution = resolution)
            },
            onFailure = {
                GalleryPhotoRenameRow(group, target, status = GalleryRenameStatus.Conflict, issue = it.message,
                    resolution = resolution)
            },
        )
    }

    private fun availableName(
        row: GalleryPhotoRenameRow,
        isAvailable: (GalleryRenameLocation) -> Boolean,
    ): String? {
        val name = row.proposedName
        if (row.group.locations(name).all(isAvailable)) return name
        val parsed = GalleryPhotoName.parse(name)
        val base = parsed.prefix + parsed.counter + parsed.extension
        var number = 2
        while (true) {
            val candidate = duplicateFileName(base, number++)
            if (!isValidGalleryPhotoNameLength(candidate)) return null
            if (row.group.locations(candidate).all(isAvailable)) return candidate
        }
    }
}
