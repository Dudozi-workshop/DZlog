package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import org.junit.Assert.assertEquals
import org.junit.Test

class TableStructureMutationTest {

    @Test
    fun remove_row_by_selection_removes_selected_row_span() {
        val base = addRow(addRow(TableTemplateState.default))
        val selected = TableSelectionRange(minRow = 1, maxRow = 2, minCol = 0, maxCol = 1)

        val next = removeRowBySelection(base, selected)
        assertEquals(1, next.rows)
    }

    @Test
    fun add_column_by_selection_inserts_after_selected_right_edge() {
        val base = TableTemplateState.default
        val selected = TableSelectionRange(minRow = 0, maxRow = 0, minCol = 0, maxCol = 0)

        val next = addColumnBySelection(base, selected)
        assertEquals(base.cols + 1, next.cols)
        assertEquals(base.rows * next.cols, next.cells.size)
    }
}
