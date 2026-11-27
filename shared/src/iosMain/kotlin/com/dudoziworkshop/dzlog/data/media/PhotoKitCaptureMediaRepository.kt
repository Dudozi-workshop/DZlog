package com.dudoziworkshop.dzlog.data.media

import platform.Foundation.NSUserDefaults
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaSavePlan
import com.dudoziworkshop.dzlog.domain.media.ConfirmedCaptureMediaSave
import com.dudoziworkshop.dzlog.domain.counter.CounterMediaReadbackPolicy
import com.dudoziworkshop.dzlog.domain.counter.CounterMediaRecord

class PhotoKitProjectDeletion internal constructor(
    internal val captureIds: Set<String>,
    internal val entries: Set<IndexedCaptureMedia>,
    internal val present: Set<String>,
) {
    val resultCount = entries.count { it.asset.assetId in present && it.asset.target.role == CaptureMediaRole.RESULT }
    val originalCount = present.size - resultCount
    val alreadyMissingCount = entries.size - present.size
}

class PhotoKitFolderDeletion internal constructor(
    internal val contents: IndexedFolderContents,
    internal val presentIds: Set<String>,
) {
    val resultCount = contents.entries.count { it.asset.assetId in presentIds && it.asset.target.role == CaptureMediaRole.RESULT }
    val originalCount = presentIds.size - resultCount
    val missingCount = contents.entries.size - presentIds.size
    val folderCount = contents.folders.count { folder ->
        folder != contents.source && folder.removePrefix(contents.source).split('/').none { it.equals("original", true) }
    }
}

/**
 * iOS Photos adapter for the common capture/save contract.
 *
 * One save is allowed at a time per repository instance; the capture coordinator
 * must serialize calls and commit the counter/table patch only after the receipt.
 * App-private logical paths are kept in the index, not mapped to Photos folders.
 */
class PhotoKitCaptureMediaRepository internal constructor(
    defaults: NSUserDefaults,
    private val photos: PhotoKitAssetAccess,
) : CaptureMediaRepository, IosCaptureDeletionStore {
    constructor(defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults) : this(defaults, PhotoKitAssetAccess())

    private val index = PhotoKitCaptureMediaIndex(defaults)

    /** Ask for Photos access before staging the irreversible workspace journal. */
    suspend fun ensureWritePermission() {
        photos.ensureReadWriteAccess()
    }
    private var saving = false

    override suspend fun saveAllOrRollback(
        plan: CaptureMediaSavePlan,
        jpegByRole: Map<CaptureMediaRole, ByteArray>,
    ): ConfirmedCaptureMediaSave {
        plan.validatePayloads(jpegByRole)
        check(!saving) { "A previous iOS capture is still being saved." }
        saving = true
        try {
            photos.ensureReadWriteAccess()
            reconcilePending()
            index.ensureCaptureIsNew(plan.captureId)
            val batch = photos.create(plan, jpegByRole, index)
            if (batch.metadataError != null || batch.entries.size != plan.targets.size) {
                rollbackAndFail(batch.entries, batch.metadataError
                    ?: IllegalStateException("PhotoKit returned an incomplete write receipt."),
                    allCreatedAssetsTracked = batch.entries.size == plan.targets.size)
            }

            try {
                index.append(batch.entries)
            } catch (failure: Throwable) {
                rollbackAndFail(batch.entries, failure)
            }

            // If clearing the journal fails, the indexed receipt is already durable.
            // The next access reconciles it idempotently.
            runCatching { index.clearPending() }
            return plan.confirm(batch.entries.map { it.asset })
        } finally {
            saving = false
        }
    }

    override suspend fun listSavedAssets(): List<SavedCaptureMedia> {
        if (photos.hasReadAccess()) reconcilePending()
        val readable = photos.hasReadAccess()
        return index.load().map { entry ->
            val metadata = if (readable) photos.metadata(entry.asset.assetId) else null
            SavedCaptureMedia(
                captureId = entry.captureId,
                asset = entry.asset,
                availability = when {
                    !readable -> CaptureMediaAvailability.INACCESSIBLE
                    metadata != null -> CaptureMediaAvailability.PRESENT
                    !photos.hasFullReadAccess() -> CaptureMediaAvailability.INACCESSIBLE
                    else -> CaptureMediaAvailability.MISSING
                },
                createdAtMillis = metadata?.createdAtMillis,
            )
        }
    }

    /** Automatic next is based only on DZlog assets still present in Photos. */
    suspend fun resolveStableAutomaticNext(
        relativePathKey: String,
        scanPrefix: String,
        fnDelim: String,
        currentNext: Int,
    ): Int = CounterMediaReadbackPolicy.resolveStableNext(
        records = listSavedAssets()
            .filter { it.availability == CaptureMediaAvailability.PRESENT }
            .map { CounterMediaRecord(it.asset.target.relativePath, it.asset.target.displayName) },
        relativePathKey = relativePathKey,
        scanPrefix = scanPrefix,
        fnDelim = fnDelim,
        currentNext = currentNext,
    )

    override suspend fun readAsset(assetId: String): ByteArray? {
        if (!photos.hasReadAccess()) return null
        reconcilePending()
        // Never read arbitrary Photos assets through the DZlog private gallery.
        if (index.load().none { it.asset.assetId == assetId }) return null
        return photos.read(assetId)
    }

    suspend fun readThumbnail(assetId: String): ByteArray? {
        if (!photos.hasReadAccess() || index.load().none { it.asset.assetId == assetId }) return null
        return photos.thumbnail(assetId)
    }

    /** Deletes only explicitly selected assets, never implicit original/result companions. */
    suspend fun deleteGalleryAsset(assetId: String): Boolean = deleteGalleryAssets(setOf(assetId))

    suspend fun deleteGalleryAssets(assetIds: Set<String>): Boolean {
        require(assetIds.isNotEmpty() && assetIds.all { it.isNotBlank() }) { "Select at least one photo." }
        val selectedIds = assetIds.toSet()
        check(!saving) { "Wait until the current photo operation finishes." }
        saving = true
        try {
            photos.ensureReadWriteAccess()
            reconcilePending()
            val indexed = index.load()
            check(selectedIds.all { id -> indexed.count { it.asset.assetId == id } == 1 }) {
                "A selected photo is no longer uniquely indexed."
            }
            // One Photos transaction/prompt; cancellation preserves the entire index.
            val removed = photos.remove(selectedIds.sorted())
            if (removed) index.forgetDeleted(selectedIds)
            return removed
        } finally { saving = false }
    }

    /** Deletes original/result companions together for camera-session Undo. */
    override suspend fun deleteCaptureAssets(captureId: String): Boolean {
        check(!saving) { "Wait until the current photo operation finishes." }
        saving = true
        try {
            photos.ensureReadWriteAccess()
            reconcilePending()
            val entries = index.load().filter { it.captureId == captureId }
            check(entries.isNotEmpty()) { "Capture is no longer indexed." }
            val removed = photos.remove(entries.map { it.asset.assetId })
            if (removed) index.forgetCapture(captureId)
            return removed
        } finally { saving = false }
    }

    /** Changes DZlog's logical folders only; Photos resource names stay unchanged. */
    suspend fun moveGalleryAsset(assetId: String, relativePath: String) =
        moveGalleryAssets(setOf(assetId), relativePath)

    suspend fun moveGalleryAssets(
        assetIds: Set<String>, relativePath: String, includeOriginals: Boolean = false,
        confirmedPaths: Map<String, String>? = null, confirmedSources: Map<String, String>? = null,
    ): Map<String, String> {
        check(!saving) { "Wait until the current photo operation finishes." }
        saving = true
        try {
            check(photos.hasReadAccess()) { "사진 접근 권한을 확인해 주세요." }
            val paths = SavedGalleryMovePolicy.destinations(listSavedAssets(), assetIds, relativePath, includeOriginals)
            check(confirmedPaths == null || paths == confirmedPaths) { "이동할 사진이 변경되었습니다. 다시 선택해 주세요." }
            check(relativePath in index.folders()) { "대상 폴더가 더 이상 존재하지 않습니다." }
            index.move(paths, confirmedSources)
            return paths
        } finally { saving = false }
    }

    fun listGalleryFolders(): List<String> = index.folders()

    fun favoriteGalleryAssetIds(): Set<String> = index.favoriteAssetIds()

    suspend fun setGalleryFavorite(assetId: String, favorite: Boolean) = manageFolder {
        index.setFavorite(assetId, favorite)
    }

    suspend fun createGalleryFolder(parent: String, name: String): String = manageFolder {
        index.createFolder(parent, name)
    }

    suspend fun renameGalleryFolder(path: String, name: String): String = manageFolder {
        index.renameFolder(path, name)
    }

    suspend fun moveGalleryFolder(path: String, destination: String): String = manageFolder {
        index.moveFolder(path, destination)
    }

    private fun inspectProjectDeletion(captureIds: Set<String>): PhotoKitProjectDeletion {
        val entries = index.projectEntries(captureIds)
        val present = photos.presentIds(entries.map { it.asset.assetId })
        check(entries.size == present.size || photos.hasFullReadAccess()) {
            "접근할 수 없는 사진이 있습니다. 사진 접근 권한을 확인해 주세요."
        }
        return PhotoKitProjectDeletion(captureIds.toSet(), entries, present)
    }

    suspend fun prepareProjectDeletion(captureIds: Set<String>): PhotoKitProjectDeletion = manageFolder {
        if (captureIds.isNotEmpty()) photos.ensureReadWriteAccess()
        inspectProjectDeletion(captureIds)
    }

    suspend fun deleteProjectPhotos(
        confirmed: PhotoKitProjectDeletion,
        currentCaptureIds: () -> Set<String>,
        deleteVerifiedProject: () -> Boolean,
    ) = manageFolder {
        val current = inspectProjectDeletion(currentCaptureIds())
        check(current.captureIds == confirmed.captureIds && current.entries == confirmed.entries &&
            current.present == confirmed.present) { "삭제 범위가 변경됐습니다. 다시 확인해 주세요." }
        if (current.present.isNotEmpty()) {
            check(photos.remove(current.present.toList())) { "사진 삭제가 취소되었거나 실패했습니다. 프로젝트를 유지합니다." }
        }
        val ids = current.entries.mapTo(mutableSetOf()) { it.asset.assetId }
        check(ids.isEmpty() || (photos.hasReadAccess() &&
            (current.alreadyMissingCount == 0 || photos.hasFullReadAccess()) && photos.presentIds(ids).isEmpty())) {
            "연결 사진의 삭제를 확인하지 못했습니다. 프로젝트를 유지합니다."
        }
        check(currentCaptureIds() == confirmed.captureIds && index.projectEntries(confirmed.captureIds) == current.entries) {
            "촬영 기록이 변경됐습니다. 프로젝트를 유지합니다."
        }
        // Keep the exact IDs available for a retry if the registry write fails.
        check(deleteVerifiedProject()) { "프로젝트 기록을 저장하지 못했습니다. 다시 확인해 주세요." }
        if (ids.isNotEmpty()) index.forgetDeleted(ids)
    }

    suspend fun prepareFolderDeletion(path: String): PhotoKitFolderDeletion = manageFolder {
        inspectFolderDeletion(path)
    }

    private fun inspectFolderDeletion(path: String): PhotoKitFolderDeletion {
        val contents = index.folderContents(path)
        val present = photos.presentIds(contents.entries.map { it.asset.assetId })
        check(present.size == contents.entries.size || photos.hasFullReadAccess()) {
            "접근할 수 없는 사진이 있습니다. iPhone 설정에서 폴더의 모든 사진에 접근을 허용한 뒤 다시 확인해 주세요."
        }
        return PhotoKitFolderDeletion(contents, present)
    }

    suspend fun deleteGalleryFolder(confirmed: PhotoKitFolderDeletion) = manageFolder {
        if (confirmed.contents.entries.isNotEmpty()) photos.ensureReadWriteAccess()
        val current = inspectFolderDeletion(confirmed.contents.source)
        check(current.contents == confirmed.contents && current.presentIds == confirmed.presentIds) {
            "폴더 내용이 변경됐습니다. 삭제 범위를 다시 확인해 주세요."
        }
        if (current.presentIds.isNotEmpty()) {
            check(photos.remove(current.presentIds.toList())) { "사진 삭제가 취소되었거나 권한을 확인할 수 없습니다." }
        }
        index.finishFolderDeletion(current.contents)
    }

    private suspend fun <T> manageFolder(operation: suspend () -> T): T {
        check(!saving) { "Wait until the current photo operation finishes." }
        saving = true
        try {
            reconcilePending()
            return operation()
        } finally { saving = false }
    }

    private suspend fun reconcilePending() {
        val pending = index.pending()
        if (pending.isEmpty()) return
        check(photos.hasReadAccess()) { "Photos read permission is needed for interrupted save recovery." }

        val surviving = pending.filter { photos.exists(it.asset.assetId) }
        check(surviving.size == pending.size || photos.hasFullReadAccess()) {
            "Full Photos access is needed to reconcile unavailable pending assets."
        }
        when (surviving.size) {
            pending.size -> {
                index.append(pending)
                index.clearPending()
            }
            0 -> index.clearPending()
            else -> {
                // A PhotoKit transaction should be atomic. If the user deleted one
                // role before recovery, remove its orphaned companion.
                check(photos.remove(surviving.map { it.asset.assetId })) {
                    "Could not roll back a partially available PhotoKit capture."
                }
                index.clearPending()
            }
        }
    }

    private suspend fun rollbackAndFail(
        entries: List<IndexedCaptureMedia>,
        cause: Throwable,
        allCreatedAssetsTracked: Boolean = true,
    ): Nothing {
        val removed = runCatching {
            photos.remove(entries.map { it.asset.assetId })
        }.getOrDefault(false)
        if (removed) runCatching { index.clearPending() }
        // A missing placeholder may hide an untracked creation. Deleting only
        // known IDs is not proof that the whole transaction was rolled back.
        if (removed && allCreatedAssetsTracked) throw PhotoKitSaveRolledBackException(
            "PhotoKit capture rolled back after metadata failure.", cause)
        throw IllegalStateException(
            "PhotoKit metadata failed; automatic asset rollback also failed. Manual recovery is required.",
            cause,
        )
    }
}

