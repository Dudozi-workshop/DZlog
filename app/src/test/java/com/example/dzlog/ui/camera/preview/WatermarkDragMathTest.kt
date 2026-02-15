package com.example.dzlog.ui.camera.preview

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class WatermarkDragMathTest {

    @Test
    fun clampOffset_moves_only_y_when_x_axis_has_no_room() {
        val offset = computeClampedDragOffsetPx(
            dragStartLeftPx = 0f,
            dragStartTopPx = 10f,
            dragAccumDx = 120f,
            dragAccumDy = 25f,
            maxX = 0f,
            maxY = 80f
        )

        assertEquals(0f, offset.x, 0.0001f)
        assertEquals(35f, offset.y, 0.0001f)
    }

    @Test
    fun clampOffset_moves_only_x_when_y_axis_has_no_room() {
        val offset = computeClampedDragOffsetPx(
            dragStartLeftPx = 20f,
            dragStartTopPx = 0f,
            dragAccumDx = -8f,
            dragAccumDy = 32f,
            maxX = 70f,
            maxY = 0f
        )

        assertEquals(12f, offset.x, 0.0001f)
        assertEquals(0f, offset.y, 0.0001f)
    }

    @Test
    fun ratioFromPx_returns_zero_for_axis_without_room() {
        val ratios = computeOffsetRatioFromPx(
            committedOffsetPx = Offset(25f, 50f),
            maxX = 0f,
            maxY = 200f
        )

        assertEquals(0, ratios.first)
        assertEquals(25, ratios.second)
    }

    @Test
    fun ratioFromPx_rounds_and_clamps_between_zero_and_hundred() {
        val ratios = computeOffsetRatioFromPx(
            committedOffsetPx = Offset(74.8f, 109.9f),
            maxX = 75f,
            maxY = 110f
        )

        assertEquals(100, ratios.first)
        assertEquals(100, ratios.second)
    }
}
