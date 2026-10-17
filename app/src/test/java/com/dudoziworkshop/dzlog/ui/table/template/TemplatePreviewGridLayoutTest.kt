package com.dudoziworkshop.dzlog.ui.table.template

import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TemplatePreviewGridLayoutTest {
    private fun cell(
        id: String,
        row: Int,
        col: Int,
        rowSpan: Int = 1,
        colSpan: Int = 1,
    ) = TableCellState(
        rowIndex = row,
        colIndex = col,
        rowSpan = rowSpan,
        colSpan = colSpan,
        cellId = id,
    )

    @Test fun mergedCellOccupiesBothRowsAndWeightedColumns() {
        val state = TableTemplateState(
            rows = 2,
            cols = 3,
            rowWeights = listOf(1f, 3f),
            colWeights = listOf(1f, 2f, 1f),
            cells = listOf(
                cell("merged", 0, 0, rowSpan = 2, colSpan = 2),
                cell("right-top", 0, 2),
                cell("right-bottom", 1, 2),
            ),
        )

        val bounds = templatePreviewCellBounds(state).associateBy { it.cell.cellId }
        assertEquals(3, bounds.size)
        val merged = requireNotNull(bounds["merged"])
        assertEquals(0f, merged.left, 0.0001f)
        assertEquals(0f, merged.top, 0.0001f)
        assertEquals(0.75f, merged.width, 0.0001f)
        assertEquals(1f, merged.height, 0.0001f)
        val bottom = requireNotNull(bounds["right-bottom"])
        assertEquals(0.75f, bottom.left, 0.0001f)
        assertEquals(0.25f, bottom.top, 0.0001f)
        assertEquals(0.25f, bottom.width, 0.0001f)
        assertEquals(0.75f, bottom.height, 0.0001f)
    }

    @Test fun invalidDimensionsCannotCreateOffGridBounds() {
        val state = TableTemplateState(
            rows = 1,
            cols = 1,
            cells = listOf(
                cell("valid", 0, 0),
                cell("stale", 2, 1),
            ),
        )
        val bounds = templatePreviewCellBounds(state)
        assertEquals(1, bounds.size)
        assertEquals("valid", bounds.single().cell.cellId)
        assertTrue(bounds.all { it.width > 0f && it.height > 0f })
    }
}
