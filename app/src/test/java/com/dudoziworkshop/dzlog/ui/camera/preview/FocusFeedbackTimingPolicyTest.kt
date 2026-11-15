package com.dudoziworkshop.dzlog.ui.camera.preview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusFeedbackTimingPolicyTest {
    @Test fun pendingAfHasTwoSecondWatchdog() {
        assertEquals(2_000L, focusCollapseDelayMs(FocusRingPhase.FOCUSING))
    }

    @Test fun successWaitsTwelveHundredMillisecondsAfterFocusCompletes() {
        assertEquals(1_200L, focusCollapseDelayMs(FocusRingPhase.SUCCESS))
    }

    @Test fun failureAlsoWaitsTwelveHundredMillisecondsAndDoesNotBlockLocking() {
        assertEquals(1_200L, focusCollapseDelayMs(FocusRingPhase.FAILED))
        assertEquals(500L, FOCUS_FAILURE_FLASH_MS)
        assertTrue(FOCUS_FAILURE_FLASH_MS < FOCUS_RESULT_HOLD_MS)
    }

    @Test fun timedOutLockedRingNoLongerPulses() {
        assertEquals(1_200L, focusCollapseDelayMs(FocusRingPhase.TIMED_OUT))
        assertTrue(FocusRingPhase.TIMED_OUT != FocusRingPhase.FOCUSING)
    }
}
