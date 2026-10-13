package com.dudoziworkshop.dzlog.ui.camera.preview

import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraPreviewLayoutTest {
    @Test
    fun `portrait preview keeps existing full width`() {
        for (aspect in CaptureAspect.entries) {
            val layout = calculatePreviewBoxLayout(
                parentWidthPx = 1080f, parentHeightPx = 2400f, captureAspect = aspect,
            )
            assertEquals(1f, layout.widthFraction, 0.0001f)
        }
    }

    @Test
    fun `landscape preview fits both axes without cropping the view`() {
        for (aspect in CaptureAspect.entries) {
            val layout = calculatePreviewBoxLayout(
                parentWidthPx = 2560f, parentHeightPx = 1600f, captureAspect = aspect,
            )
            val measuredWidth = 2560f * layout.widthFraction
            val measuredHeight = measuredWidth / layout.safeAspect
            assertTrue("preview must not exceed available width: $aspect", measuredWidth <= 2560.01f)
            assertTrue("preview must not exceed available height: $aspect", measuredHeight <= 1600.01f)
            assertTrue("preview top offset must remain on screen: $aspect",
                layout.topOffsetPx >= 0f && layout.topOffsetPx + measuredHeight <= 1600.01f)
        }
    }

    @Test
    fun `short split screen height also limits preview width`() {
        val layout = calculatePreviewBoxLayout(
            parentWidthPx = 1080f, parentHeightPx = 650f,
            captureAspect = CaptureAspect.R3_4,
        )
        assertTrue(layout.widthFraction < 1f)
        assertTrue(1080f * layout.widthFraction / layout.safeAspect <= 650.01f)
    }
}
