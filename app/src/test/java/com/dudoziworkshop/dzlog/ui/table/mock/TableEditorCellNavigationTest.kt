package com.dudoziworkshop.dzlog.ui.table.mock

import org.junit.Assert.assertEquals
import org.junit.Test

class TableEditorCellNavigationTest {
    @Test
    fun detail_navigation_visits_every_kind_in_row_order_and_does_not_wrap() {
        val cells = TableEditorCellType.entries.mapIndexed { index, type ->
            TableEditorCellUiModel(index, "", type, "cell$index", rowIndex = index / 3, colIndex = index % 3)
        }.reversed()
        assertEquals(EditorCellNeighbours(null, "cell1"), editorCellNeighbours(cells, "cell0"))
        assertEquals(EditorCellNeighbours("cell4", null), editorCellNeighbours(cells, "cell5"))
        assertEquals(EditorCellNeighbours("cell1", "cell3"), editorCellNeighbours(cells, "cell2"))
    }

    @Test
    fun merged_roots_are_one_stop_and_missing_selection_has_no_navigation() {
        val cells = listOf(
            TableEditorCellUiModel(0, "merged", domainCellId = "root", rowSpan = 2, colSpan = 2),
            TableEditorCellUiModel(1, "", domainCellId = "covered", colIndex = 1, isCovered = true),
            TableEditorCellUiModel(2, "next", domainCellId = "next", colIndex = 2),
        )
        assertEquals(EditorCellNeighbours(null, "next"), editorCellNeighbours(cells, "root"))
        assertEquals(EditorCellNeighbours("root", null), editorCellNeighbours(cells, "next"))
        assertEquals(EditorCellNeighbours(null, null), editorCellNeighbours(cells, "covered"))
        assertEquals(EditorCellNeighbours(null, null), editorCellNeighbours(emptyList(), null))
    }
}
