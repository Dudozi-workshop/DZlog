package com.dudoziworkshop.dzlog.feature.counter.camera

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraCounterSyncTest {
    @Test fun nextPhraseCounterIsReadDuringPreviousImageStaging() {
        assertTrue(shouldReadCameraCounter(
            isTemplateReady = true,
            isCapturing = true,
            pendingSaveCount = 3,
            isCurrentRequestSynced = false,
        ))
    }

    @Test fun pendingSameStreamDoesNotLaunchUnnecessaryRead() {
        assertFalse(shouldReadCameraCounter(
            isTemplateReady = true,
            isCapturing = false,
            pendingSaveCount = 2,
            isCurrentRequestSynced = true,
        ))
    }

    @Test fun unreadyTemplateNeverQueriesCounter() {
        assertFalse(shouldReadCameraCounter(
            isTemplateReady = false,
            isCapturing = false,
            pendingSaveCount = 0,
            isCurrentRequestSynced = false,
        ))
    }
}
