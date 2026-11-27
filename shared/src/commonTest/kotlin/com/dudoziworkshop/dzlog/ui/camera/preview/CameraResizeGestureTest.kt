package com.dudoziworkshop.dzlog.ui.camera.preview

import kotlin.test.Test
import kotlin.test.assertEquals

class CameraResizeGestureTest {
    @Test
    fun draggingAwayFromTheOppositeCornerGrowsInEveryQuadrant() {
        for (right in listOf(false, true)) for (bottom in listOf(false, true)) {
            val left = if (right) 0f else 140f
            val top = if (bottom) 0f else 100f
            val corner = chooseResizeHandleCorner(left, top, left + 60, top + 40, 100f, 70f)
            assertEquals(right, corner.right)
            assertEquals(bottom, corner.bottom)
            val pivotX = if (right) left else left + 60
            val pivotY = if (bottom) top else top + 40
            // Start slightly off the mathematical corner, as a finger on the handle does.
            val dx = if (right) 54f else -54f
            val dy = if (bottom) 37f else -37f
            val gesture = CameraResizeGesture(pivotX, pivotY, pivotX + dx, pivotY + dy)
            assertEquals(1f, gesture.scaleAt(pivotX + dx, pivotY + dy))
            assertEquals(0.5f, gesture.scaleAt(pivotX + dx / 2, pivotY + dy / 2), 0.0001f)
            assertEquals(1.5f, gesture.scaleAt(pivotX + dx * 1.5f, pivotY + dy * 1.5f), 0.0001f)
        }
    }
}
