package com.dudoziworkshop.dzlog.ui.camera.preview

import org.junit.Assert.assertEquals
import org.junit.Test

class LockedFocusTapPolicyTest {
    @Test fun firstTapOnCollapsedLockedPreviewOnlyOpensBrightness() {
        assertEquals(
            LockedFocusTapAction.SHOW_EXPOSURE,
            lockedFocusTapAction(controlsExpanded = false, awaitingSecondTap = false),
        )
    }

    @Test fun firstTapOnAlreadyExpandedLockedPreviewDoesNotRefocus() {
        assertEquals(
            LockedFocusTapAction.SHOW_EXPOSURE,
            lockedFocusTapAction(controlsExpanded = true, awaitingSecondTap = false),
        )
    }

    @Test fun secondTapWhileBrightnessIsShownMovesLockedFocus() {
        assertEquals(
            LockedFocusTapAction.REFOCUS,
            lockedFocusTapAction(controlsExpanded = true, awaitingSecondTap = true),
        )
    }

    @Test fun collapsingControlsDisarmsTheSecondTap() {
        assertEquals(
            LockedFocusTapAction.SHOW_EXPOSURE,
            lockedFocusTapAction(controlsExpanded = false, awaitingSecondTap = true),
        )
    }

    @Test fun sunSliderIsApproximatelyTwoThirdsOfPreviousWidth() {
        assertEquals(78, FOCUS_EV_WIDTH_DP)
    }
}
