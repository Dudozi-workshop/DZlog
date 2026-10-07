package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TableStructureRangeActionsTest {

    @Test
    fun rootCells_excludesCoveredCellsAfterMerge() {
        val merged = TableStructureRangeActions.mergeSelection(
            templateState = template3x3(),
            selectionRange = TableSelectionRange(minRow = 0, maxRow = 1, minCol = 0, maxCol = 1),
        )

        val roots = TableStructureRangeActions.rootCells(merged.cells)
        assertFalse(roots.any { it.rowIndex == 0 && it.colIndex == 1 })
        assertFalse(roots.any { it.rowIndex == 1 && it.colIndex == 1 })
        assertTrue(roots.any { it.rowIndex == 0 && it.colIndex == 0 })
    }

    @Test
    fun expandRangeToMergedBlocks_expandsWhenSelectingCoveredCell() {
        val merged = TableStructureRangeActions.mergeSelection(
            templateState = template3x3(),
            selectionRange = TableSelectionRange(minRow = 0, maxRow = 1, minCol = 0, maxCol = 1),
        )

        val expanded = TableStructureRangeActions.expandRangeToMergedBlocks(
            cells = merged.cells,
            base = TableSelectionRange(minRow = 1, maxRow = 1, minCol = 1, maxCol = 1),
        )

        assertEquals(TableSelectionRange(0, 1, 0, 1), expanded)
    }

    @Test
    fun mergeThenDelete_keepsCoveredCellsNonInteractive() {
        val merged = TableStructureRangeActions.mergeSelection(
            templateState = template3x3(),
            selectionRange = TableSelectionRange(minRow = 0, maxRow = 1, minCol = 0, maxCol = 1),
        )
        val deleted = TableStructureRangeActions.deleteSelectionWithAbsorb(
            templateState = merged,
            selectionRange = TableSelectionRange(minRow = 0, maxRow = 1, minCol = 0, maxCol = 1),
        )

        val absorber = TableStructureRangeActions.resolveRootCell(
            deleted.cells,
            cellAt(deleted.cells, 0, 0),
        )
        val resolvedCovered = TableStructureRangeActions.resolveRootCell(
            deleted.cells,
            cellAt(deleted.cells, 1, 1),
        )

        assertEquals(absorber.cellId, resolvedCovered.cellId)
        assertEquals(
            deleted.cells.size,
            deleted.cells.map { it.rowIndex to it.colIndex }.toSet().size,
        )
        assertFalse(
            TableStructureRangeActions.rootCells(deleted.cells)
                .any { it.rowIndex == 1 && it.colIndex == 1 }
        )
    }

    @Test
    fun deleteThenMerge_coveredCellsDoNotResurrectAsRoots() {
        val deleted = TableStructureRangeActions.deleteSelectionWithAbsorb(
            templateState = template3x3(),
            selectionRange = TableSelectionRange(minRow = 1, maxRow = 1, minCol = 1, maxCol = 1),
        )
        val merged = TableStructureRangeActions.mergeSelection(
            templateState = deleted,
            selectionRange = TableSelectionRange(minRow = 0, maxRow = 2, minCol = 2, maxCol = 2),
        )

        val mergedRoot = cellAt(merged.cells, 0, 2)
        assertEquals(3, mergedRoot.rowSpan)
        assertEquals(1, mergedRoot.colSpan)
        assertTrue(TableStructureRangeActions.isCoveredCell(merged.cells, 1, 2))
        assertFalse(
            TableStructureRangeActions.rootCells(merged.cells)
                .any { it.rowIndex == 1 && it.colIndex == 2 }
        )
    }

    @Test
    fun deleteThenDelete_usesSameRootCoveredRule() {
        val firstDelete = TableStructureRangeActions.deleteSelectionWithAbsorb(
            templateState = template3x3(),
            selectionRange = TableSelectionRange(minRow = 1, maxRow = 1, minCol = 1, maxCol = 1),
        )
        val secondDelete = TableStructureRangeActions.deleteSelectionWithAbsorb(
            templateState = firstDelete,
            selectionRange = TableSelectionRange(minRow = 1, maxRow = 1, minCol = 1, maxCol = 1),
        )

        val resolved = TableStructureRangeActions.resolveRootCell(
            secondDelete.cells,
            cellAt(secondDelete.cells, 1, 1),
        )
        assertEquals(cellAt(secondDelete.cells, 0, 0).cellId, resolved.cellId)
        assertFalse(
            TableStructureRangeActions.rootCells(secondDelete.cells)
                .any { it.rowIndex == 1 && it.colIndex == 1 }
        )
    }

    @Test
    fun interactiveRootCellsInRange_returnsMergedRootForPartialSelection() {
        val merged = TableStructureRangeActions.mergeSelection(
            templateState = template3x3(),
            selectionRange = TableSelectionRange(minRow = 0, maxRow = 1, minCol = 0, maxCol = 1),
        )

        val interactiveRoots = TableStructureRangeActions.interactiveRootCellsInRange(
            cells = merged.cells,
            range = TableSelectionRange(minRow = 1, maxRow = 1, minCol = 1, maxCol = 1),
        )

        assertEquals(1, interactiveRoots.size)
        assertEquals(cellAt(merged.cells, 0, 0).cellId, interactiveRoots.first().cellId)
    }

    @Test
    fun resolveStructureCells_resolvesRootAndCoveredInSinglePassResult() {
        val merged = TableStructureRangeActions.mergeSelection(
            templateState = template3x3(),
            selectionRange = TableSelectionRange(minRow = 0, maxRow = 1, minCol = 0, maxCol = 1),
        )

        val resolvedById = TableStructureRangeActions.resolveStructureCells(merged.cells)
            .associateBy { it.cellId }

        val covered = resolvedById.getValue("r1c1")
        assertTrue(covered.isCovered)
        assertEquals(0, covered.rootRowIndex)
        assertEquals(0, covered.rootColIndex)
    }

    private fun template3x3(): TableTemplateState {
        return template(rows = 3, cols = 3)
    }

    private fun template(rows: Int, cols: Int): TableTemplateState {
        val cells = buildList {
            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    add(
                        TableCellState(
                            rowIndex = row,
                            colIndex = col,
                            cellId = "r${row}c${col}",
                        )
                    )
                }
            }
        }
        return TableTemplateState(rows = rows, cols = cols, cells = cells)
    }

    private fun cellAt(cells: List<TableCellState>, row: Int, col: Int): TableCellState {
        return cells.first { it.rowIndex == row && it.colIndex == col }
    }
}
