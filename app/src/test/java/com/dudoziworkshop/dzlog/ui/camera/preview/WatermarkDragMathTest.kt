package com.dudoziworkshop.dzlog.ui.camera.preview

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class WatermarkDragMathTest {

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

    @Test
    fun resolveZoomBounds_clamps_to_at_least_one_and_orders_range() {
        val bounds = resolveZoomBounds(minSupported = 0.2f, maxSupported = 0.5f)

        assertEquals(1f, bounds.first, 0.0001f)
        assertEquals(1f, bounds.second, 0.0001f)
    }
}
