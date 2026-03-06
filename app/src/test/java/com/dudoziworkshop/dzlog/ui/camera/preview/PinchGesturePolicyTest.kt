package com.dudoziworkshop.dzlog.ui.camera.preview

import android.graphics.RectF
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinchGesturePolicyTest {

    @Test
    fun allows_pinch_when_centroid_in_capture_rect() {
        val allowed = shouldHandlePinch(
            centroidX = 100f,
            centroidY = 200f,
            captureRect = RectF(0f, 0f, 1080f, 1920f),
            watermarkDragActive = false
        )

        assertTrue(allowed)
    }

    @Test
    fun blocks_pinch_when_watermark_is_being_dragged() {
        val allowed = shouldHandlePinch(
            centroidX = 300f,
            centroidY = 300f,
            captureRect = RectF(0f, 0f, 1080f, 1920f),
            watermarkDragActive = true
        )

        assertFalse(allowed)
    }

    @Test
    fun blocks_pinch_when_centroid_is_outside_preview() {
        val allowed = shouldHandlePinch(
            centroidX = -1f,
            centroidY = 300f,
            captureRect = RectF(0f, 0f, 1080f, 1920f),
            watermarkDragActive = false
        )

        assertFalse(allowed)
    }
}
