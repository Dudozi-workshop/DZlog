package com.dudoziworkshop.dzlog.feature.table.preview

import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell
import com.dudoziworkshop.dzlog.feature.table.render.computeResolvedRenderLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TablePreviewPlaceholderPolicyTest {
    @Test
    fun placeholder_labels_do_not_expand_column_geometry() {
        val cameraCells = listOf(WatermarkCell(""), WatermarkCell("A"), WatermarkCell(""), WatermarkCell("B"))
        val displayCells = cameraCells.mapIndexed { i, cell ->
            if (i == 0 || i == 2) WatermarkCell("빈 셀") else cell
        }
        val layoutCells = cellsForPreviewLayout(displayCells, setOf(0, 2))

        assertEquals(cameraCells, layoutCells)
        assertEquals("빈 셀", displayCells[0].valueText)
        assertEquals("빈 셀", displayCells[2].valueText)

        val fromEditor = computeResolvedRenderLayout(
            cells = layoutCells,
            rows = 2,
            cols = 2,
            valueScale = 100,
        )
        val fromCamera = computeResolvedRenderLayout(
            cells = cameraCells,
            rows = 2,
            cols = 2,
            valueScale = 100,
        )
        val withLabel = computeResolvedRenderLayout(
            cells = displayCells,
            rows = 2,
            cols = 2,
            valueScale = 100,
        )
        assertTrue(withLabel.intrinsicColWidthsPx[0] > fromCamera.intrinsicColWidthsPx[0])
        assertEquals(fromCamera.intrinsicColWidthsPx, fromEditor.intrinsicColWidthsPx)
        assertEquals(fromCamera.intrinsicRowHeightsPx, fromEditor.intrinsicRowHeightsPx)
        assertEquals(fromCamera.finalColWeights, fromEditor.finalColWeights)
        assertEquals(fromCamera.finalRowWeights, fromEditor.finalRowWeights)
    }

    @Test
    fun non_placeholder_cells_are_unchanged() {
        val displayCells = listOf(WatermarkCell("촬영"), WatermarkCell("26.11.07"))
        assertEquals(displayCells, cellsForPreviewLayout(displayCells, emptySet()))
        assertEquals(displayCells, cellsForPreviewLayout(displayCells, setOf(9)))
    }
}
