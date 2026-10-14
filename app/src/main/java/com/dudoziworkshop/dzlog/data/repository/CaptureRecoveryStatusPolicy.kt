package com.dudoziworkshop.dzlog.data.repository

/**
 * Classifies durable jobs when a camera screen is recreated.
 *
 * READY, PROCESSING and RESERVED can be normal while the process-owned
 * camera/staging/queue worker still owns the ID. A job without an active owner
 * must be reviewed (or explicitly resumed by a separate verified-READY flow).
 * Never silently resume PROCESSING or SAVED_UNCOMMITTED.
 */
internal object CaptureRecoveryStatusPolicy {
    fun needsManualReview(
        report: CapturePendingStore.RecoveryInventory,
        activeIds: Set<String>,
    ): Boolean =
        report.ready.any { it.reservation.id !in activeIds } ||
            report.needsReview.any { record ->
                record.status == CapturePendingStore.Status.NEEDS_REVIEW ||
                    record.reservation.id !in activeIds
            } ||
            report.corruptJournals.isNotEmpty() ||
            report.orphanFiles.isNotEmpty()

    /** The worker is allowed to resume only independently verified READY images. */
    fun resumableReadyIds(
        report: CapturePendingStore.RecoveryInventory,
        activeIds: Set<String>,
    ): List<String> = report.ready.mapNotNull { record ->
        record.reservation.id.takeUnless(activeIds::contains)
    }
}
