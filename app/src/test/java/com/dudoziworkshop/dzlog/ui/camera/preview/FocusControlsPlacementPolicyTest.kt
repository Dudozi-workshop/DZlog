package com.dudoziworkshop.dzlog.ui.camera.preview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusControlsPlacementPolicyTest {
    private val ring = 22f
    private val lock = 36f
    private val exposure = 36f
    private val gap = 6f

    private fun place(y: Float, height: Float = 400f) = placeFocusControlsVertically(
        focusY = y,
        viewHeight = height,
        ringRadius = ring,
        lockHeight = lock,
        exposureHeight = exposure,
        gap = gap,
    )

    @Test fun centerKeepsLockOnTopArcAndSunSliderBelow() {
        val p = place(200f)
        assertFalse(p.lockAtBottom)
        assertFalse(p.exposureAbove)
        assertEquals(160f, p.lockTop, 0.001f)
        assertEquals(228f, p.exposureTop, 0.001f)
    }

    @Test fun nearTopPlacesUprightLockOnBottomArcAndSliderBelowIt() {
        val p = place(10f)
        assertTrue(p.lockAtBottom)
        assertFalse(p.exposureAbove)
        assertEquals(26f, p.lockTop, 0.001f)
        assertEquals(68f, p.exposureTop, 0.001f)
        assertTrue(p.exposureTop >= p.lockTop + lock + gap)
    }

    @Test fun nearBottomPlacesSliderAboveLockOnTopArc() {
        val p = place(390f)
        assertFalse(p.lockAtBottom)
        assertTrue(p.exposureAbove)
        assertEquals(338f, p.lockTop, 0.001f)
        assertEquals(296f, p.exposureTop, 0.001f)
        assertTrue(p.exposureTop + exposure + gap <= p.lockTop)
    }

    @Test fun aShortPreviewKeepsControlsWithinBounds() {
        val p = place(80f, height = 160f)
        assertTrue(p.lockTop >= 0f)
        assertTrue(p.lockTop + lock <= 160f)
        assertTrue(p.exposureTop >= 0f)
        assertTrue(p.exposureTop + exposure <= 160f)
    }

    @Test fun ringCenterDoesNotDisappearPastHorizontalEdges() {
        assertEquals(22f, visibleFocusCenter(1f, 300f, 22f), 0.001f)
        assertEquals(278f, visibleFocusCenter(299f, 300f, 22f), 0.001f)
        assertEquals(150f, visibleFocusCenter(150f, 300f, 22f), 0.001f)
    }
}
