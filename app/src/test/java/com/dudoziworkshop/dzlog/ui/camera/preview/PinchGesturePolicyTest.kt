package com.dudoziworkshop.dzlog.ui.camera.preview

import android.graphics.RectF
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinchGesturePolicyTest {

    @Test
    fun allows_pinch_when_centroid_in_preview_and_not_over_watermark() {
        val allowed = shouldHandlePinch(
            centroidX = 100f,
            centroidY = 200f,
            previewWidth = 1080f,
            previewHeight = 1920f,
            watermarkRect = RectF(20f, 20f, 80f, 80f),
            watermarkDragActive = false
        )

        assertTrue(allowed)
    }

    @Test
    fun blocks_pinch_when_watermark_is_being_dragged() {
        val allowed = shouldHandlePinch(
            centroidX = 300f,
            centroidY = 300f,
            previewWidth = 1080f,
            previewHeight = 1920f,
            watermarkRect = null,
            watermarkDragActive = true
        )

        assertFalse(allowed)
    }

    @Test
    fun blocks_pinch_when_centroid_is_outside_preview() {
        val allowed = shouldHandlePinch(
            centroidX = -1f,
            centroidY = 300f,
            previewWidth = 1080f,
            previewHeight = 1920f,
            watermarkRect = null,
            watermarkDragActive = false
        )

        assertFalse(allowed)
    }
}
