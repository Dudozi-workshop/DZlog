package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaSavePlan
import com.dudoziworkshop.dzlog.domain.media.ConfirmedCaptureMediaSave

/**
 * iOS Photos adapter for the common capture/save contract.
 *
 * One save is allowed at a time per repository instance; the capture coordinator
 * must serialize calls and commit the counter/table patch only after the receipt.
 * App-private logical paths are kept in the index, not mapped to Photos folders.
 */
class PhotoKitCaptureMediaRepository : CaptureMediaRepository {
    private val photos = PhotoKitAssetAccess()
    private val index = PhotoKitCaptureMediaIndex()

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
                    ?: IllegalStateException("PhotoKit returned an incomplete write receipt."))
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
            SavedCaptureMedia(
                captureId = entry.captureId,
                asset = entry.asset,
                availability = when {
                    !readable -> CaptureMediaAvailability.INACCESSIBLE
                    photos.exists(entry.asset.assetId) -> CaptureMediaAvailability.PRESENT
                    else -> CaptureMediaAvailability.MISSING
                },
            )
        }
    }

    override suspend fun readAsset(assetId: String): ByteArray? {
        if (!photos.hasReadAccess()) return null
        reconcilePending()
        // Never read arbitrary Photos assets through the DZlog private gallery.
        if (index.load().none { it.asset.assetId == assetId }) return null
        return photos.read(assetId)
    }

    private suspend fun reconcilePending() {
        val pending = index.pending()
        if (pending.isEmpty()) return
        check(photos.hasReadAccess()) { "Photos read permission is needed for interrupted save recovery." }

        val surviving = pending.filter { photos.exists(it.asset.assetId) }
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
    ): Nothing {
        val removed = runCatching {
            photos.remove(entries.map { it.asset.assetId })
        }.getOrDefault(false)
        if (removed) runCatching { index.clearPending() }
        throw IllegalStateException(
            if (removed) "PhotoKit capture rolled back after metadata failure."
            else "PhotoKit metadata failed; automatic asset rollback also failed. Manual recovery is required.",
            cause,
        )
    }
}
