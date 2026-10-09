package com.dudoziworkshop.dzlog.ui.camera.preview

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraResizeHandlePolicyTest {

    @Test fun topLeftTablePointsToBottomRightHandle() {
        assertEquals(ResizeHandleCorner.BOTTOM_RIGHT, chooseResizeHandleCorner(0f, 0f, 90f, 70f, 150f, 200f))
    }

    @Test fun topRightTablePointsToBottomLeftHandle() {
        assertEquals(ResizeHandleCorner.BOTTOM_LEFT, chooseResizeHandleCorner(210f, 0f, 300f, 70f, 150f, 200f))
    }

    @Test fun bottomLeftTablePointsToTopRightHandle() {
        assertEquals(ResizeHandleCorner.TOP_RIGHT, chooseResizeHandleCorner(0f, 330f, 90f, 400f, 150f, 200f))
    }

    @Test fun bottomRightTablePointsToTopLeftHandle() {
        assertEquals(ResizeHandleCorner.TOP_LEFT, chooseResizeHandleCorner(210f, 330f, 300f, 400f, 150f, 200f))
    }
}
