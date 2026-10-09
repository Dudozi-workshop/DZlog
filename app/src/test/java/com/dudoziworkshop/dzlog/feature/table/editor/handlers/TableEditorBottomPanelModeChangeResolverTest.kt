package com.dudoziworkshop.dzlog.feature.table.editor.handlers

import com.dudoziworkshop.dzlog.ui.table.section.BottomEditorPanelMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TableEditorBottomPanelModeChangeResolverTest {

    @Test
    fun blocked_inline_commit_returns_noop_state() {
        val changed = TableEditorBottomPanelModeChangeResolver.resolve(
            TableEditorBottomPanelModeChangeInput(
                currentMode = BottomEditorPanelMode.CELL_EDIT,
                nextMode = BottomEditorPanelMode.NONE,
                inlineCommitWasBlocked = true,
                wasStructureMode = false,
                currentSelectedCellId = "cell-1",
            )
        )

        assertFalse(changed.shouldApply)
        assertEquals(BottomEditorPanelMode.CELL_EDIT, changed.nextBottomPanelMode)
        assertFalse(changed.shouldClearFileNameTransientState)
        assertFalse(changed.shouldClearPathTransientState)
        assertEquals(null, changed.nextSelectedCellId)
    }

    @Test
    fun leaving_structure_mode_resets_selection_and_clears_transient_state() {
        val changed = TableEditorBottomPanelModeChangeResolver.resolve(
            TableEditorBottomPanelModeChangeInput(
                currentMode = BottomEditorPanelMode.STRUCTURE_EDIT,
                nextMode = BottomEditorPanelMode.NONE,
                inlineCommitWasBlocked = false,
                wasStructureMode = true,
                currentSelectedCellId = "cell-42",
            )
        )

        assertTrue(changed.shouldApply)
        assertTrue(changed.shouldClearFileNameTransientState)
        assertTrue(changed.shouldClearPathTransientState)
        assertEquals(emptySet<String>(), changed.nextStructureSelectedCellIds)
        assertTrue(changed.nextStructureSelectionRangeCleared)
        assertEquals("cell-42", changed.nextSelectedCellId)
        assertEquals(BottomEditorPanelMode.NONE, changed.nextBottomPanelMode)
    }
}
