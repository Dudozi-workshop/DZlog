package com.dudoziworkshop.dzlog.ui.camera.preview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockedFocusTapPolicyTest {
    @Test fun unlockedPreviewTapResetsExposure() {
        assertTrue(shouldResetExposureOnPreviewTap(locked = false))
    }

    @Test fun lockedPreviewTapPreservesExposure() {
        // The first tap on the preview immediately moves AF and keeps EV.
        assertFalse(shouldResetExposureOnPreviewTap(locked = true))
    }

    @Test fun compactSliderKeepsVisualLengthButIncreasesHitWidth() {
        assertEquals(78, FOCUS_EV_WIDTH_DP)
        assertEquals(108, FOCUS_EV_TOUCH_WIDTH_DP)
        assertEquals(48, FOCUS_EV_TOUCH_HEIGHT_DP)
        assertEquals(48, FOCUS_LOCK_TOUCH_DP)
    }
}
