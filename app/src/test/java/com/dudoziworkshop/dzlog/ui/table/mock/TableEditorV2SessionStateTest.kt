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
    fun legacy_manual_text_color_becomes_automatic_in_draft_without_mutating_saved_input() {
        val initial = newBlankTableTemplateState(2, 2)
        val legacyStyle = TableStyleState(textColorMode = 1, manualTextColor = 0, valueScale = 120)
        val session = TableEditorV2SessionState(initial, legacyStyle, true, true)
        assertEquals(legacyStyle.copy(textColorMode = 0), session.draftStyleState)
        assertEquals(1, legacyStyle.textColorMode)
        assertTrue(session.isDirty)
        assertFalse(session.canUndo)
        session.commitStyleChange(session.draftStyleState.copy(bgStyle = 0))
        assertTrue(session.undo())
        assertEquals(0, session.draftStyleState.textColorMode)
        session.markSaved(session.finalTemplateForSave())
        assertFalse(session.isDirty)
    }


    @Test
    fun equalization_preserves_cells_other_axis_and_style_and_supports_save_undo_redo() {
        val blank = newBlankTableTemplateState(2, 3)
        val topRow = selectMockLayoutRange(blank, blank.cells[0].cellId, blank.cells[1].cellId)
        val merged = TableEditorV2StructureController.applyMergeDecision(blank,
            TableEditorV2StructureController.resolveMergeDecision(blank, topRow))
        val initial = merged.copy(
            rowWeights = listOf(0.5f, 1.5f),
            colWeights = listOf(0.5f, 1f, 1.5f),
            cells = merged.cells.map { it.copy(rawText = "retained") },
        )
        val style = TableStyleState(valueScale = 120)
        val session = TableEditorV2SessionState(initial, style, true, true)
        session.commitTemplateChange(TableEditorV2StructureController.equalizeColumns(session.draftTemplateState))
        assertEquals(initial.cells, session.draftTemplateState.cells)
        assertEquals(initial.rowWeights, session.draftTemplateState.rowWeights)
        assertEquals(style, session.draftStyleState)
        val widths = com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator.computeSizes(300f,
            com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator.resolveWeights(
                session.finalTemplateForSave().colWeights, initial.cols))
        assertEquals(listOf(100f, 100f, 100f), widths)
        assertEquals(300f, widths.sum(), 0.0001f)
        val equalColumns = session.draftTemplateState
        session.commitTemplateChange(TableEditorV2StructureController.equalizeRows(session.draftTemplateState))
        assertEquals(initial.cells, session.finalTemplateForSave().cells)
        val heights = com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator.computeSizes(200f,
            com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator.resolveWeights(
                session.finalTemplateForSave().rowWeights, initial.rows))
        assertEquals(listOf(100f, 100f), heights)
        assertEquals(style, session.draftStyleState)
        assertTrue(session.undo())
        assertEquals(equalColumns, session.draftTemplateState)
        assertTrue(session.undo())
        assertEquals(initial, session.draftTemplateState)
        assertFalse(session.isDirty)
        assertTrue(session.redo())
        assertEquals(equalColumns, session.draftTemplateState)
    }

    @Test
    fun equalizing_an_already_equal_table_does_not_create_a_change_or_undo_entry() {
        val initial = newBlankTableTemplateState(1, 3).copy(colWeights = listOf(2f, 2f, 2f))
        val session = TableEditorV2SessionState(initial, TableStyleState(), true, true)
        session.commitTemplateChange(TableEditorV2StructureController.equalizeRows(session.draftTemplateState))
        session.commitTemplateChange(TableEditorV2StructureController.equalizeColumns(session.draftTemplateState))
        assertEquals(initial, session.draftTemplateState)
        assertFalse(session.isDirty)
        assertFalse(session.canUndo)
    }

    @Test
    fun confirmed_populated_merge_updates_preview_save_and_undo_restores_values() {
        val blank = newBlankTableTemplateState(2, 2)
        val initial = blank.copy(cells = blank.cells.map {
            it.copy(rawText = "${it.rowIndex},${it.colIndex}")
        })
        val selection = selectMockLayoutRange(initial, initial.cells.first().cellId, initial.cells.last().cellId)
        val decision = TableEditorV2StructureController.resolveMergeDecision(initial, selection)
        assertEquals(com.dudoziworkshop.dzlog.feature.table.editor.TableMergeDecisionType.CONFIRM_MERGE, decision.type)
        val session = TableEditorV2SessionState(initial, TableStyleState(), true, true)
        session.commitTemplateChange(TableEditorV2StructureController.applyMergeDecision(session.draftTemplateState, decision))
        val visible = mockCellsFromTemplate(session.draftTemplateState).filterNot { it.isCovered }
        assertEquals(1, visible.size)
        assertEquals("0,0", visible.single().value)
        assertEquals(2, visible.single().rowSpan)
        assertEquals(2, visible.single().colSpan)
        assertEquals(2, session.finalTemplateForSave().cells.first().colSpan)
        assertTrue(session.isDirty)
        assertTrue(session.undo())
        assertEquals(initial, session.draftTemplateState)
        assertFalse(session.isDirty)
    }

    @Test
    fun complete_existing_merge_can_be_extended_after_confirmation() {
        val blank = newBlankTableTemplateState(2, 2)
        val populated = blank.copy(cells = blank.cells.map { it.copy(rawText = "value") })
        val topRow = selectMockLayoutRange(populated, populated.cells[0].cellId, populated.cells[1].cellId)
        val mergedRow = TableEditorV2StructureController.applyMergeDecision(populated,
            TableEditorV2StructureController.resolveMergeDecision(populated, topRow))
        val whole = selectMockLayoutRange(mergedRow, mergedRow.cells.first().cellId, mergedRow.cells.last().cellId)
        val decision = TableEditorV2StructureController.resolveMergeDecision(mergedRow, whole)
        assertEquals(com.dudoziworkshop.dzlog.feature.table.editor.TableMergeDecisionType.CONFIRM_MERGE, decision.type)
        val merged = TableEditorV2StructureController.applyMergeDecision(mergedRow, decision)
        assertEquals(1, mockCellsFromTemplate(merged).count { !it.isCovered })
        assertEquals(2, merged.cells.first().rowSpan)
        assertEquals(2, merged.cells.first().colSpan)
    }

    @Test
    fun a_populated_non_anchor_cell_requires_confirmation_even_if_anchor_is_blank() {
        val blank = newBlankTableTemplateState(1, 2)
        val initial = blank.copy(cells = blank.cells.mapIndexed { index, cell ->
            cell.copy(rawText = if (index == 1) "discarded" else "")
        })
        val selection = selectMockLayoutRange(initial, initial.cells.first().cellId, initial.cells.last().cellId)
        assertEquals(com.dudoziworkshop.dzlog.feature.table.editor.TableMergeDecisionType.CONFIRM_MERGE,
            TableEditorV2StructureController.resolveMergeDecision(initial, selection).type)
    }

    @Test
    fun boundary_drag_accumulates_keeps_total_and_undoes_as_one_action() {
        val initial = newBlankTableTemplateState(2, 2)
        val session = TableEditorV2SessionState(initial, TableStyleState(), true, true)
        session.beginContinuousTemplateChange()
        repeat(5) {
            session.replaceTemplateDraftWithoutHistory(adjustMockColumnBoundary(session.draftTemplateState, 0, 0.02f))
            session.replaceTemplateDraftWithoutHistory(adjustMockRowBoundary(session.draftTemplateState, 0, -0.02f))
        }
        assertEquals(1.2f, session.draftTemplateState.colWeights!![0], 0.0001f)
        assertEquals(0.8f, session.draftTemplateState.rowWeights!![0], 0.0001f)
        assertEquals(2f, session.draftTemplateState.colWeights!!.sum(), 0.0001f)
        assertEquals(session.draftTemplateState.colWeights, session.finalTemplateForSave().colWeights)
        assertTrue(session.isDirty)
        assertTrue(session.undo())
        assertEquals(initial, session.draftTemplateState)
        assertFalse(session.canUndo)
    }

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

    @Test
    fun edits_made_while_save_is_suspended_remain_dirty_and_are_not_overwritten() = runBlocking {
        val initial = newBlankTableTemplateState(1, 1)
        val session = TableEditorV2SessionState(initial, TableStyleState(), true, true)
        val coordinator = TableEditorV2SaveCoordinator()
        val saved = coordinator.save(session) { _, _, _, _, _, _, _, _ ->
            session.commitTemplateChange(initial.copy(cells = initial.cells.map { it.copy(rawText = "new draft") }))
            true
        }
        assertTrue(saved)
        assertEquals("new draft", session.draftTemplateState.cells.single().rawText)
        assertTrue(session.isDirty)
        assertTrue(coordinator.save(session) { _, _, _, _, _, _, _, _ -> true })
        assertFalse(session.isDirty)
    }

    @Test
    fun legacy_padding_normalizes_in_draft_and_save_preserves_manual_counter() = runBlocking {
        for (legacy in listOf(5, 6)) {
            val session = TableEditorV2SessionState(
                newBlankTableTemplateState(1, 1), TableStyleState(), true, true,
                initialCounterPadding = legacy,
            )
            assertEquals(4, session.draftCounterPadding)
            assertTrue(session.isDirty)
            session.initializeCounterState(12345, false)
            var savedPadding = 0
            var savedNext: Int? = null
            var savedAuto = true
            val saved = TableEditorV2SaveCoordinator().save(session) { _, _, _, _, _, padding, next, auto ->
                savedPadding = padding
                savedNext = next
                savedAuto = auto
                true
            }
            assertTrue(saved)
            assertEquals(4, savedPadding)
            assertEquals(12345, savedNext)
            assertFalse(savedAuto)
            assertFalse(session.isDirty)
        }
    }

    @Test
    fun padding_change_and_history_keep_counter_value_and_override_mode() {
        val session = TableEditorV2SessionState(
            newBlankTableTemplateState(1, 1), TableStyleState(), true, true,
            initialCounterPadding = 3,
        )
        session.initializeCounterState(12345, false)
        session.commitCounterPaddingChange(6)
        assertEquals(4, session.draftCounterPadding)
        assertEquals(12345, session.draftNextCounter)
        assertFalse(session.draftUsesAutoNext)
        assertTrue(session.undo())
        assertEquals(3, session.draftCounterPadding)
        assertEquals(12345, session.draftNextCounter)
        assertTrue(session.redo())
        assertEquals(4, session.draftCounterPadding)
        assertEquals(12345, session.draftNextCounter)
        assertFalse(session.draftUsesAutoNext)
    }

    @Test
    fun four_digit_format_never_truncates_large_counters() {
        assertEquals("0012", formatMockCounter(12, 6))
        assertEquals("12345", formatMockCounter(12345, 4))
        assertEquals("1자리", counterPaddingLabel(0))
        assertEquals("4자리", counterPaddingLabel(6))
    }

}

