package com.dudoziworkshop.dzlog.ui.table.mock

import org.junit.Assert.assertEquals
import androidx.compose.ui.unit.dp
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
    @Test
    fun editor_state_follows_domain_identity_when_display_indices_change() {
        val original = TableEditorCellUiModel(0, "alpha", domainCellId = "persist-A")
        val moved = original.copy(id = 7, rowIndex = 3, colIndex = 1)
        val replacement = original.copy(domainCellId = "persist-B")
        assertEquals("persist-A", editorCellStateKey(original))
        assertEquals(editorCellStateKey(original), editorCellStateKey(moved))
        assertEquals(false, editorCellStateKey(original) == editorCellStateKey(replacement))

        val unlinked = TableEditorCellUiModel(4, "", domainCellId = null)
        assertEquals("ui:4", editorCellStateKey(unlinked))
    }

    @Test
    fun landscape_panel_reserves_both_fixed_controls() {
        // Before the fix, a 200dp viewport could cap the entire panel at 60dp.
        // It now reserves input (56), navigation (44), and vertical chrome (24).
        assertEquals(124.dp, editorContentPanelMaxHeight(200.dp, 48.dp))
        assertEquals(124.dp, editorContentPanelMaxHeight(300.dp, 48.dp))
    }

    @Test
    fun tall_and_tiny_viewports_keep_panel_within_available_height() {
        val tall = editorContentPanelMaxHeight(600.dp, 48.dp)
        assertEquals((648f * 0.35f - 48f), tall.value, 0.001f)
        assertEquals(100.dp, editorContentPanelMaxHeight(100.dp, 48.dp))
    }

    @Test
    fun keyboard_handoff_is_allowed_only_for_text_and_number_cells() {
        assertEquals(true, editorCanKeepKeyboard(TableEditorCellType.TEXT))
        assertEquals(true, editorCanKeepKeyboard(TableEditorCellType.NUMBER))
        TableEditorCellType.entries
            .filterNot { it == TableEditorCellType.TEXT || it == TableEditorCellType.NUMBER }
            .forEach { assertEquals(false, editorCanKeepKeyboard(it)) }
        assertEquals(false, editorCanKeepKeyboard(null))
    }

}
