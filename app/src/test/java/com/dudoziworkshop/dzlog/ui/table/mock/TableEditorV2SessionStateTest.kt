package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.data.template.newBlankTableTemplateState
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.RotatingCounterProgressMode
import kotlinx.coroutines.runBlocking
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
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
    fun changing_counter_stream_invalidates_loaded_next_and_undo_restores_it() {
        val initial = newBlankTableTemplateState(rows = 1, cols = 1)
        val session = TableEditorV2SessionState(
            initialTemplateState = initial,
            initialStyleState = TableStyleState(),
            includePathInCounterScope = true,
            includeFilenameInCounterScope = true,
        )
        session.initializeCounterState(next = 6, usesAutoNext = true)

        session.commitSaveModeChange(SaveMode.ORIGINAL_ONLY)

        assertEquals(null, session.draftNextCounter)
        assertTrue(session.isDirty)

        assertTrue(session.undo())
        assertEquals(6, session.draftNextCounter)
        assertEquals(SaveMode.BOTH, session.draftSaveMode)
        assertFalse(session.isDirty)
    }

    @Test
    fun template_cell_change_refreshes_save_rule_cell_preview() {
        val blank = newBlankTableTemplateState(rows = 1, cols = 1)
        val cellId = blank.cells.single().cellId
        val initial = blank.copy(
            cells = blank.cells.map { it.copy(rawText = "before") },
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(
                    kind = "CELL",
                    label = "셀",
                    cellId = cellId,
                ),
                null,
                null,
            ),
        )
        val session = TableEditorV2SessionState(
            initialTemplateState = initial,
            initialStyleState = TableStyleState(),
            includePathInCounterScope = true,
            includeFilenameInCounterScope = true,
        )
        session.initializeCounterState(next = 3, usesAutoNext = true)

        session.commitTemplateChange(
            initial.copy(cells = initial.cells.map { it.copy(rawText = "after") })
        )

        assertEquals("after", session.saveRulesDraft.fileNameItems.first()?.value)
        assertEquals(null, session.draftNextCounter)
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
    @Test
    fun mode_change_invalidates_manual_draft_and_failed_save_keeps_draft_unsaved() = runBlocking {
        val initial = newBlankTableTemplateState(1, 1).copy(
            phraseSets = listOf(RotatingPhraseSet("set", "처리구", listOf("A", "B", "C"))),
        )
        val session = TableEditorV2SessionState(initial, TableStyleState(), true, true)
        session.initializeCounterState(9, false)
        session.commitTemplateChange(initial.copy(phraseSets = initial.phraseSets.map {
            it.copy(counterProgressMode = RotatingCounterProgressMode.CONTINUOUS)
        }))
        assertEquals(null, session.draftNextCounter)
        assertTrue(session.draftUsesAutoNext)
        val coordinator = TableEditorV2SaveCoordinator()
        var persisted = initial
        val failed = coordinator.save(session) { _, _, _, _, _, _, _, _ -> false }
        assertFalse(failed)
        assertEquals(RotatingCounterProgressMode.PER_PHRASE, persisted.phraseSets.single().counterProgressMode)
        assertTrue(session.isDirty)
        assertFalse(coordinator.isSaving)
        val saved = coordinator.save(session) { template, _, _, _, _, _, _, _ -> persisted = template; true }
        assertTrue(saved)
        assertEquals(RotatingCounterProgressMode.CONTINUOUS, persisted.phraseSets.single().counterProgressMode)
        assertFalse(session.isDirty)
    }

}

