package com.dudoziworkshop.dzlog.watermark

import org.junit.Assert.assertEquals
import org.junit.Test

class WatermarkTextSizingPolicyTest {

    @Test
    fun `base text size uses uniform table cell and not weights`() {
        val base = computeBaseTextSizeFromRenderedTable(
            tableWidth = 300f,
            tableHeight = 200f,
            rows = 2,
            cols = 3,
        )

        // baseCellWidth = 100, baseCellHeight = 100, shortSide = 100, ratio = 0.36
        assertEquals(36f, base, 0.0001f)
    }

    @Test
    fun `wmValueScale works as multiplicative factor`() {
        val scaled = applyValueScaleFactor(baseTextSize = 36f, valueScale = 125)
        assertEquals(45f, scaled, 0.0001f)
    }

    @Test
    fun `cell cap only limits upper bound`() {
        val capped = applyCellSafeTextCap(
            scaledTextSize = 45f,
            actualCellWidth = 40f,
            actualCellHeight = 30f,
        )
        // short side 30 * 0.8 = 24
        assertEquals(24f, capped, 0.0001f)

        val notCapped = applyCellSafeTextCap(
            scaledTextSize = 20f,
            actualCellWidth = 40f,
            actualCellHeight = 30f,
        )
        assertEquals(20f, notCapped, 0.0001f)
    }
}
