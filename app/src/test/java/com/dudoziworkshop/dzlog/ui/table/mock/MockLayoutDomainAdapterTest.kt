package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.data.template.newBlankTableTemplateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockLayoutDomainAdapterTest {

    @Test
    fun `add row and column mutate real template structure`() {
        val base = newBlankTableTemplateState(rows = 2, cols = 2)
        val first = base.cells.first()
        val selection = selectMockLayoutCell(
            templateState = base,
            current = MockLayoutSelection(),
            tappedDomainCellId = first.cellId,
        )

        val rowAdded = addMockLayoutRow(base, selection)
        assertEquals(3, rowAdded.rows)

        val colAdded = addMockLayoutColumn(rowAdded, selection)
        assertEquals(3, colAdded.cols)
    }

    @Test
    fun `merge and unmerge selection use production range actions`() {
        val base = newBlankTableTemplateState(rows = 2, cols = 2)
        val topLeft = base.cells.first { it.rowIndex == 0 && it.colIndex == 0 }
        val bottomRight = base.cells.first { it.rowIndex == 1 && it.colIndex == 1 }

        var selection = selectMockLayoutCell(
            templateState = base,
            current = MockLayoutSelection(),
            tappedDomainCellId = topLeft.cellId,
        )
        selection = selectMockLayoutCell(
            templateState = base,
            current = selection,
            tappedDomainCellId = bottomRight.cellId,
        )

        val merged = mergeOrUnmergeMockLayoutSelection(base, selection)
        val root = merged.cells.first { it.cellId == topLeft.cellId }
        assertEquals(2, root.rowSpan)
        assertEquals(2, root.colSpan)

        val unmerged = mergeOrUnmergeMockLayoutSelection(merged, selection)
        val unmergedRoot = unmerged.cells.first { it.cellId == topLeft.cellId }
        assertEquals(1, unmergedRoot.rowSpan)
        assertEquals(1, unmergedRoot.colSpan)
    }

    @Test
    fun `row deletion clears selection targets through structure mutation`() {
        val base = newBlankTableTemplateState(rows = 3, cols = 2)
        val middle = base.cells.first { it.rowIndex == 1 && it.colIndex == 0 }
        val selection = selectMockLayoutCell(
            templateState = base,
            current = MockLayoutSelection(),
            tappedDomainCellId = middle.cellId,
        )

        val next = removeMockLayoutRows(base, selection)

        assertEquals(2, next.rows)
        assertTrue(next.cells.none { it.cellId == middle.cellId })
    }
    @Test
    fun `boundary drag preserves total weight and changes neighbors`() {
        val base = newBlankTableTemplateState(rows = 2, cols = 2)

        val rowAdjusted = adjustMockRowBoundary(
            templateState = base,
            boundaryIndex = 0,
            deltaFraction = 0.10f,
        )
        val rowWeights = rowAdjusted.rowWeights ?: error("row weights missing")
        assertEquals(2f, rowWeights.sum(), 0.0001f)
        assertTrue(rowWeights[0] > rowWeights[1])

        val colAdjusted = adjustMockColumnBoundary(
            templateState = base,
            boundaryIndex = 0,
            deltaFraction = -0.10f,
        )
        val colWeights = colAdjusted.colWeights ?: error("column weights missing")
        assertEquals(2f, colWeights.sum(), 0.0001f)
        assertTrue(colWeights[0] < colWeights[1])
    }


}
