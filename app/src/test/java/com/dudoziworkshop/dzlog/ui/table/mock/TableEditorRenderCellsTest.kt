package com.dudoziworkshop.dzlog.ui.table.mock

import org.junit.Assert.assertEquals
import org.junit.Test

class TableEditorRenderCellsTest {
    @Test
    fun render_values_are_row_major_and_prefer_resolved_preview_values() {
        val cells = listOf(
            TableEditorCellUiModel(id = 2, rowIndex = 1, colIndex = 0, value = "raw", previewValue = "카메라"),
            TableEditorCellUiModel(id = 1, rowIndex = 0, colIndex = 1, value = "수동"),
            TableEditorCellUiModel(id = 0, rowIndex = 0, colIndex = 0, value = "원본", previewValue = "미리보기"),
            TableEditorCellUiModel(id = 3, rowIndex = 1, colIndex = 1, value = "숨김", isCovered = true),
        )

        assertEquals(
            listOf("미리보기", "수동", "카메라", ""),
            editorRenderCells(cells, rows = 2, cols = 2).map { it.valueText },
        )
    }

    @Test
    fun merged_roots_are_kept_and_covered_cells_not_rendered_twice() {
        val cells = listOf(
            TableEditorCellUiModel(id = 0, domainCellId = "root", rowIndex = 0, colIndex = 0,
                rowSpan = 1, colSpan = 2, value = "병합"),
            TableEditorCellUiModel(id = 1, domainCellId = "covered", rowIndex = 0, colIndex = 1,
                value = "중복", isCovered = true),
        )
        assertEquals(listOf("병합", ""), editorRenderCells(cells, 1, 2).map { it.valueText })
        val roots = editorRenderRootCells(cells)
        assertEquals(1, roots.size)
        assertEquals("root", roots.single().cellId)
        assertEquals(2, roots.single().colSpan)
    }

    @Test
    fun missing_cells_keep_stable_row_major_slots() {
        val cells = listOf(
            TableEditorCellUiModel(id = 0, rowIndex = 0, colIndex = 0, value = "A"),
        )
        assertEquals(listOf("A", "", "", ""), editorRenderCells(cells, 2, 2).map { it.valueText })
    }
}
