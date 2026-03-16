package com.dudoziworkshop.dzlog.feature.table.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePreviewRatioTest {

    @Test
    fun `common shape policy keeps template aspect for vertical case`() {
        val contentAspect = resolveContentAspectRatio(
            rows = 6,
            cols = 2,
            rowWeights = listOf(2f, 2f, 2f, 2f, 2f, 2f),
            colWeights = listOf(1f, 1f),
        )

        val ratio = computeShapeLockedRatios(
            contentAspectRatio = contentAspect,
            maxWidthRatio = 60,
            maxHeightRatio = 60,
        )

        val resolvedAspect = ratio.tableWidthRatio.toFloat() / ratio.tableHeightRatio.toFloat()
        assertTrue(resolvedAspect < 1f)
        assertTrue(kotlin.math.abs(resolvedAspect - contentAspect) < 0.2f)
    }

    @Test
    fun `common shape policy keeps template aspect for horizontal weighted case`() {
        val contentAspect = resolveContentAspectRatio(
            rows = 2,
            cols = 5,
            rowWeights = listOf(1f, 1f),
            colWeights = listOf(4f, 2f, 2f, 1f, 1f),
        )

        val ratio = computeShapeLockedRatios(
            contentAspectRatio = contentAspect,
            maxWidthRatio = 80,
            maxHeightRatio = 40,
        )

        val resolvedAspect = ratio.tableWidthRatio.toFloat() / ratio.tableHeightRatio.toFloat()
        assertTrue(resolvedAspect > 1f)
        assertTrue(kotlin.math.abs(resolvedAspect - contentAspect) < 0.25f)
    }

    @Test
    fun `home preview ignores absolute wm size and keeps same shape`() {
        val smallAbsoluteSize = computeHomePreviewRatio(
            contentAspectRatio = 0.5f,
            boundsWidth = 600f,
            boundsHeight = 260f,
            tableWidthRatio = 2,
            tableHeightRatio = 4,
        )
        val largeAbsoluteSize = computeHomePreviewRatio(
            contentAspectRatio = 0.5f,
            boundsWidth = 600f,
            boundsHeight = 260f,
            tableWidthRatio = 20,
            tableHeightRatio = 40,
        )

        assertEquals(smallAbsoluteSize.tableWidthRatio, largeAbsoluteSize.tableWidthRatio)
        assertEquals(smallAbsoluteSize.tableHeightRatio, largeAbsoluteSize.tableHeightRatio)
    }

    @Test
    fun `home safe clamp intervenes only for extreme aspect`() {
        val normal = computeHomePreviewRatio(
            contentAspectRatio = 1.2f,
            boundsWidth = 600f,
            boundsHeight = 260f,
            tableWidthRatio = 95,
            tableHeightRatio = 90,
        )
        val extreme = computeHomePreviewRatio(
            contentAspectRatio = 0.05f,
            boundsWidth = 600f,
            boundsHeight = 260f,
            tableWidthRatio = 40,
            tableHeightRatio = 20,
        )

        assertTrue(normal.tableWidthRatio > extreme.tableWidthRatio)
        assertTrue(normal.tableHeightRatio >= extreme.tableHeightRatio)
        assertTrue(extreme.tableWidthRatio >= 20)
        assertTrue(extreme.tableHeightRatio >= 20)
    }
}
