package com.dudoziworkshop.dzlog.feature.capture.policy

/** Read-only capture info remains available; only mutations are locked. */
internal object CameraSelectionChangePolicy {
    fun canChange(
        readOnlyTour: Boolean,
        isCapturing: Boolean,
        pendingSaveCount: Int,
        pendingSaveError: Boolean,
        isRecoveringPendingCaptures: Boolean,
        pendingPreviewCaptureId: String?,
        undoPending: Boolean,
    ): Boolean = !readOnlyTour &&
        !isCapturing &&
        pendingSaveCount == 0 &&
        !pendingSaveError &&
        !isRecoveringPendingCaptures &&
        pendingPreviewCaptureId == null &&
        !undoPending
}
