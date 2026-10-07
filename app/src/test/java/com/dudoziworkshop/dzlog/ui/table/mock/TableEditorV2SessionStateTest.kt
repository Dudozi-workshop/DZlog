package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.data.template.newBlankTableTemplateState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TableEditorV2SessionStateTest {

    @Test
    fun commit_undo_and_save_baseline_are_owned_by_session() {
        val initial = newBlankTableTemplateState(rows = 1, cols = 1)
        val session = TableEditorV2SessionState(
            initialTemplateState = initial,
            initialStyleState = TableStyleState(),
            includePathInCounterScope = true,
            includeFilenameInCounterScope = true,
        )

        assertFalse(session.isDirty)
        assertFalse(session.canUndo)

        val changed = initial.copy(
            cells = initial.cells.map { cell ->
                cell.copy(rawText = "changed")
            }
        )
        session.commitTemplateChange(changed)

        assertTrue(session.isDirty)
        assertTrue(session.canUndo)
        assertEquals("changed", session.draftTemplateState.cells.single().rawText)

        assertTrue(session.undo())
        assertEquals(initial, session.draftTemplateState)
        assertFalse(session.isDirty)

        assertTrue(session.redo())
        assertEquals(changed, session.draftTemplateState)
        assertTrue(session.isDirty)

        session.markSaved(session.finalTemplateForSave())
        assertFalse(session.isDirty)
    }

    @Test
    fun save_rules_change_participates_in_dirty_and_history() {
        val initial = newBlankTableTemplateState(rows = 1, cols = 1)
        val session = TableEditorV2SessionState(
            initialTemplateState = initial,
            initialStyleState = TableStyleState(),
            includePathInCounterScope = true,
            includeFilenameInCounterScope = true,
        )

        val changedRules = session.saveRulesDraft.copy(
            includePathInScope = false,
        )
        session.commitSaveRulesChange(changedRules)

        assertTrue(session.isDirty)
        assertFalse(session.saveRulesDraft.includePathInScope)

        assertTrue(session.undo())
        assertTrue(session.saveRulesDraft.includePathInScope)
        assertFalse(session.isDirty)
    }
}
