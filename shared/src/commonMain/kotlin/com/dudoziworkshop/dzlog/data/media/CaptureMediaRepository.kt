package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaSavePlan
import com.dudoziworkshop.dzlog.domain.media.ConfirmedCaptureMediaSave
import com.dudoziworkshop.dzlog.domain.media.PersistedCaptureAsset

/**
 * Platform-specific storage boundary.
 *
 * Implementations MUST either persist every requested role or roll back partial writes.
 * Return only a confirmed receipt with stable asset identifiers. The caller applies the
 * table/counter patch only AFTER this method succeeds. Never apply it on save failure.
 */
/**
 * Distinguish a deleted Photos asset from one hidden by limited/denied read access.
 * Missing assets keep their logical folder and capture relationship for recovery UI.
 */
enum class CaptureMediaAvailability { PRESENT, MISSING, INACCESSIBLE }

data class SavedCaptureMedia(
    val captureId: String,
    val asset: PersistedCaptureAsset,
    val availability: CaptureMediaAvailability,
)

interface CaptureMediaRepository {
    suspend fun saveAllOrRollback(
        plan: CaptureMediaSavePlan,
        jpegByRole: Map<CaptureMediaRole, ByteArray>,
    ): ConfirmedCaptureMediaSave

    /** Resolve the actual persisted media, not an in-memory camera preview. */
    suspend fun readAsset(assetId: String): ByteArray?

    /** Reload the persistent media index and reconcile with actual platform assets. */
    suspend fun listSavedAssets(): List<SavedCaptureMedia>
}
