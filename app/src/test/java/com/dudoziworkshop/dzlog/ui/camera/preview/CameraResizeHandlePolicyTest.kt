package com.dudoziworkshop.dzlog.ui.camera.preview

import android.graphics.RectF
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraResizeHandlePolicyTest {
    private val photo = RectF(0f, 0f, 300f, 400f)

    @Test fun topLeftTablePointsToBottomRightHandle() {
        assertEquals(ResizeHandleCorner.BOTTOM_RIGHT, chooseResizeHandleCorner(RectF(0f, 0f, 90f, 70f), photo))
    }

    @Test fun topRightTablePointsToBottomLeftHandle() {
        assertEquals(ResizeHandleCorner.BOTTOM_LEFT, chooseResizeHandleCorner(RectF(210f, 0f, 300f, 70f), photo))
    }

    @Test fun bottomLeftTablePointsToTopRightHandle() {
        assertEquals(ResizeHandleCorner.TOP_RIGHT, chooseResizeHandleCorner(RectF(0f, 330f, 90f, 400f), photo))
    }

    @Test fun bottomRightTablePointsToTopLeftHandle() {
        assertEquals(ResizeHandleCorner.TOP_LEFT, chooseResizeHandleCorner(RectF(210f, 330f, 300f, 400f), photo))
    }
}
