package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.data.template.defaultTableTemplateState
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.policy.TableEditorPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableStructureMutationTest {

    @Test
    fun remove_row_by_selection_removes_selected_row_span() {
        val base = addRow(addRow(defaultTableTemplateState()))
        val selected = TableSelectionRange(minRow = 1, maxRow = 2, minCol = 0, maxCol = 1)

        val next = removeRowBySelection(base, selected)
        assertEquals(2, next.rows)
        assertEquals(base.cols, next.cols)
        assertRectangularInvariant(next)
    }

    @Test
    fun add_column_by_selection_inserts_after_selected_right_edge() {
        val base = removeColumn(defaultTableTemplateState())
        val selected = TableSelectionRange(minRow = 0, maxRow = 1, minCol = 1, maxCol = 1)
        val unchangedCellId = cellAtFirstRow(base, col = 1).cellId
        val shiftedCellId = cellAtFirstRow(base, col = 2).cellId

        val next = addColumnBySelection(base, selected)
        val expectedCols = if (base.cols < TableEditorPolicy.MAX_COLS) base.cols + 1 else base.cols
        assertEquals(expectedCols, next.cols)
        assertRectangularInvariant(next)
        if (expectedCols == base.cols) {
            assertEquals(base, next)
        } else {
            assertEquals(unchangedCellId, cellAtFirstRow(next, col = 1).cellId)
            assertEquals("", cellAtFirstRow(next, col = 2).rawText)
            assertEquals(shiftedCellId, cellAtFirstRow(next, col = 3).cellId)
            assertEquals(base.cells.map { it.cellId }.toSet().size + base.rows, next.cells.map { it.cellId }.toSet().size)
            assertTrue(base.cells.all { old -> next.cells.count { it.cellId == old.cellId } == 1 })
        }
    }

    @Test
    fun structural_mutation_is_blocked_when_merged_cells_exist() {
        val base = defaultTableTemplateState()
        val mergedTemplate = base.copy(
            cells = base.cells.mapIndexed { index, cell ->
                if (index == 0) cell.copy(colSpan = 2) else cell
            }
        )

        val addNext = addColumnBySelection(
            templateState = mergedTemplate,
            selectionRange = TableSelectionRange(minRow = 0, maxRow = 1, minCol = 1, maxCol = 2),
        )
        val removeNext = removeColumnBySelection(
            templateState = mergedTemplate,
            selectionRange = TableSelectionRange(minRow = 0, maxRow = 1, minCol = 1, maxCol = 2),
        )

        assertEquals(mergedTemplate, addNext)
        assertEquals(mergedTemplate, removeNext)
        assertTrue(addNext.cells.all { it.rowSpan >= 1 && it.colSpan >= 1 })
        assertRectangularInvariant(addNext)
    }

    @Test
    fun row_mutation_is_blocked_when_merged_cells_exist() {
        val base = defaultTableTemplateState()
        val mergedTemplate = base.copy(
            cells = base.cells.mapIndexed { index, cell ->
                if (index == 1) cell.copy(rowSpan = 2) else cell
            }
        )

        val addNext = addRowBySelection(
            templateState = mergedTemplate,
            selectionRange = TableSelectionRange(minRow = 0, maxRow = 1, minCol = 0, maxCol = 3),
        )
        val removeNext = removeRowBySelection(
            templateState = mergedTemplate,
            selectionRange = TableSelectionRange(minRow = 0, maxRow = 1, minCol = 0, maxCol = 3),
        )

        assertEquals(mergedTemplate, addNext)
        assertEquals(mergedTemplate, removeNext)
        assertRectangularInvariant(addNext)
    }

    private fun cellAtFirstRow(template: TableTemplateState, col: Int): TableCellState {
        return template.cells.first { it.rowIndex == 0 && it.colIndex == col }
    }

    private fun assertRectangularInvariant(template: TableTemplateState) {
        assertEquals(template.rows * template.cols, template.cells.size)
        val uniqueCoordinates = template.cells.map { it.rowIndex to it.colIndex }.toSet()
        assertEquals(template.rows * template.cols, uniqueCoordinates.size)
        repeat(template.rows) { row ->
            assertEquals(template.cols, template.cells.count { it.rowIndex == row })
        }
        repeat(template.cols) { col ->
            assertEquals(template.rows, template.cells.count { it.colIndex == col })
        }
    }
}
