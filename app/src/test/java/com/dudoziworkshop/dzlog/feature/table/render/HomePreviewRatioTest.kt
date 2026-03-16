package com.dudoziworkshop.dzlog.feature.table.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePreviewRatioTest {

    @Test
    fun `ratio only shape keeps wm aspect for vertical case`() {
        val ratio = computeRatioOnlyTableShape(
            tableWidthRatio = 20,
            tableHeightRatio = 40,
            maxWidthRatio = 60,
            maxHeightRatio = 60,
        )

        val resolvedAspect = ratio.tableWidthRatio.toFloat() / ratio.tableHeightRatio.toFloat()
        assertTrue(resolvedAspect < 1f)
        assertTrue(kotlin.math.abs(resolvedAspect - 0.5f) < 0.1f)
    }

    @Test
    fun `ratio only shape keeps wm aspect for horizontal case`() {
        val ratio = computeRatioOnlyTableShape(
            tableWidthRatio = 40,
            tableHeightRatio = 20,
            maxWidthRatio = 80,
            maxHeightRatio = 40,
        )

        val resolvedAspect = ratio.tableWidthRatio.toFloat() / ratio.tableHeightRatio.toFloat()
        assertTrue(resolvedAspect > 1f)
        assertTrue(kotlin.math.abs(resolvedAspect - 2f) < 0.1f)
    }

    @Test
    fun `design preview ignores absolute wm size and keeps same shape`() {
        val smallAbsoluteSize = computeDesignPreviewFitShape(
            boundsWidth = 600f,
            boundsHeight = 260f,
            tableWidthRatio = 2,
            tableHeightRatio = 4,
            cellCount = 9,
        )
        val largeAbsoluteSize = computeDesignPreviewFitShape(
            boundsWidth = 600f,
            boundsHeight = 260f,
            tableWidthRatio = 20,
            tableHeightRatio = 40,
            cellCount = 9,
        )

        assertEquals(smallAbsoluteSize.tableWidthRatio, largeAbsoluteSize.tableWidthRatio)
        assertEquals(smallAbsoluteSize.tableHeightRatio, largeAbsoluteSize.tableHeightRatio)
    }

    @Test
    fun `design preview safe clamp keeps ratio-only shape and enforces minimum size`() {
        val normal = computeDesignPreviewFitShape(
            boundsWidth = 600f,
            boundsHeight = 260f,
            tableWidthRatio = 95,
            tableHeightRatio = 90,
            cellCount = 9,
        )
        val extreme = computeDesignPreviewFitShape(
            boundsWidth = 600f,
            boundsHeight = 260f,
            tableWidthRatio = 40,
            tableHeightRatio = 20,
            cellCount = 9,
        )

        val normalAspect = normal.tableWidthRatio.toFloat() / normal.tableHeightRatio.toFloat()
        val extremeAspect = extreme.tableWidthRatio.toFloat() / extreme.tableHeightRatio.toFloat()
        assertTrue(kotlin.math.abs(normalAspect - (95f / 90f)) < 0.1f)
        assertTrue(kotlin.math.abs(extremeAspect - 2f) < 0.1f)
        assertTrue(extreme.tableWidthRatio >= 20)
        assertTrue(extreme.tableHeightRatio >= 20)
    }

    @Test
    fun `design preview scale is shared between home and detail`() {
        assertEquals(0.72f, resolveDesignPreviewScale(cellCount = 4), 0.0001f)
        assertEquals(0.82f, resolveDesignPreviewScale(cellCount = 6), 0.0001f)
        assertEquals(0.90f, resolveDesignPreviewScale(cellCount = 8), 0.0001f)
        assertEquals(1.0f, resolveDesignPreviewScale(cellCount = 9), 0.0001f)
    }
}
