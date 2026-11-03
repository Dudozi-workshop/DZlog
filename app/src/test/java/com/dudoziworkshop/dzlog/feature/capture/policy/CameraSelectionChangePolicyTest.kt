package com.dudoziworkshop.dzlog.feature.capture.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraSelectionChangePolicyTest {
    private fun canChange(
        readOnlyTour: Boolean = false,
        isCapturing: Boolean = false,
        pendingSaveCount: Int = 0,
        pendingSaveError: Boolean = false,
        isRecoveringPendingCaptures: Boolean = false,
        pendingPreviewCaptureId: String? = null,
        undoPending: Boolean = false,
    ): Boolean = CameraSelectionChangePolicy.canChange(
        readOnlyTour = readOnlyTour,
        isCapturing = isCapturing,
        pendingSaveCount = pendingSaveCount,
        pendingSaveError = pendingSaveError,
        isRecoveringPendingCaptures = isRecoveringPendingCaptures,
        pendingPreviewCaptureId = pendingPreviewCaptureId,
        undoPending = undoPending,
    )

    @Test fun idleCameraAllowsTargetChanges() {
        assertTrue(canChange())
    }

    @Test fun queuedOrCurrentCaptureBlocksProjectAndTemplateChanges() {
        assertFalse(canChange(pendingSaveCount = 1))
        assertFalse(canChange(pendingSaveCount = 3))
        assertFalse(canChange(isCapturing = true))
        assertFalse(canChange(pendingPreviewCaptureId = "capture-1"))
    }

    @Test fun failedOrUnresolvedSaveBlocksMutations() {
        assertFalse(canChange(pendingSaveError = true))
        assertFalse(canChange(isRecoveringPendingCaptures = true))
    }

    @Test fun undoAndReadOnlyTutorialCannotChangeRealTargets() {
        assertFalse(canChange(undoPending = true))
        assertFalse(canChange(readOnlyTour = true))
    }
}
