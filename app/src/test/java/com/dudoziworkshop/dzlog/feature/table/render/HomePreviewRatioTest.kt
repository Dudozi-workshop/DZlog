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
    fun `home safe clamp intervenes only for extreme aspect`() {
        val normal = computeHomePreviewRatio(
            contentAspectRatio = 1.2f,
            boundsWidth = 600f,
            boundsHeight = 260f,
        )
        val extreme = computeHomePreviewRatio(
            contentAspectRatio = 0.05f,
            boundsWidth = 600f,
            boundsHeight = 260f,
        )

        assertEquals(95, normal.tableWidthRatio)
        assertTrue(normal.tableHeightRatio in 70..90)
        assertTrue(extreme.tableWidthRatio >= 30)
        assertTrue(extreme.tableHeightRatio >= 30)
    }
}
