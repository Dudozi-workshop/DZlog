package com.dudoziworkshop.dzlog.ui.camera.preview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusControlsPlacementPolicyTest {
    private val ring = 28f
    private val lock = 44f
    private val exposure = 52f
    private val gap = 8f

    private fun place(y: Float, height: Float = 400f) = placeFocusControlsVertically(
        focusY = y,
        viewHeight = height,
        ringRadius = ring,
        lockHeight = lock,
        exposureHeight = exposure,
        gap = gap,
    )

    @Test fun centerFocusKeepsLockAboveAndExposureBelow() {
        val placement = place(200f)
        assertEquals(120f, placement.lockTop, 0.001f)
        assertEquals(236f, placement.exposureTop, 0.001f)
    }

    @Test fun topEdgeMovesBothControlsBelowTheRing() {
        val placement = place(10f)
        assertEquals(46f, placement.lockTop, 0.001f)
        assertEquals(98f, placement.exposureTop, 0.001f)
        assertTrue(placement.lockTop >= 10f + ring)
    }

    @Test fun bottomEdgeMovesBothControlsAboveTheRing() {
        val placement = place(390f)
        assertEquals(250f, placement.lockTop, 0.001f)
        assertEquals(302f, placement.exposureTop, 0.001f)
        assertTrue(placement.exposureTop + exposure <= 390f - ring)
    }

    @Test fun shortPreviewStillKeepsControlsSeparateAndInBounds() {
        val placement = place(80f, height = 160f)
        assertTrue(placement.lockTop >= 0f)
        assertTrue(placement.exposureTop >= placement.lockTop + lock + gap)
        assertTrue(placement.exposureTop + exposure <= 160f)
    }
}
