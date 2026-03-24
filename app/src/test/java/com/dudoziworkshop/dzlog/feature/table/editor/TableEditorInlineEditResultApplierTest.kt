package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.data.template.defaultTableTemplateState
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.feature.counter.table.TableCounterUiState
import com.dudoziworkshop.dzlog.feature.table.policy.TableCounterConflictDialogState
import com.dudoziworkshop.dzlog.ui.table.editor.InlineEditState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TableEditorInlineEditResultApplierTest {

    @Test
    fun apply_directly_uses_direct_template_path_and_updates_latched_counter_ui() {
        val template = defaultTableTemplateState()
        val cell = template.cells.first()
        val nextTemplate = updateCell(template, cell.cellId) {
            it.copy(rawText = "edited", typedValue = CellValue.Text("edited"))
        }
        var directApplied: TableTemplateState? = null
        var undoApplied = false

        val applied = TableEditorInlineEditResultApplier.apply(
            TableEditorInlineEditApplyInput(
                result = InlineEditActionResult(
                    nextInlineEdit = InlineEditState(editingCellId = cell.cellId, editingValue = "edited"),
                    nextTemplate = nextTemplate,
                    nextInlineSessionState = InlineEditSessionState(activeCellId = cell.cellId, hasPushedUndoSnapshot = true),
                    nextSelectedCellId = cell.cellId,
                    templateApplyMode = InlineTemplateApplyMode.APPLY_DIRECTLY,
                    nextLowCounterWarningLatchedInSession = true,
                ),
                currentTemplate = template,
                currentCounterUi = TableCounterUiState(autoNextCounterValue = 3),
                applyTemplateWithUndo = { undoApplied = true },
                applyTemplateDirectly = { directApplied = it },
                updateCounterConflictUi = { counterUi, _ -> counterUi },
                applyCommittedCounter = { _, _, _, _, counterUi -> counterUi },
            )
        )

        assertSame(nextTemplate, directApplied)
        assertFalse(undoApplied)
        assertTrue(applied.nextCounterUi.lowCounterWarningLatchedInSession)
        assertEquals("edited", applied.nextInlineEdit.editingValue)
        assertEquals(cell.cellId, applied.nextSelectedCellId)
    }

    @Test
    fun committed_counter_uses_bridge_and_skips_template_apply_callbacks() {
        val template = defaultTableTemplateState()
        val cell = template.cells.first()
        var directApplied = false
        var undoApplied = false
        var committedCellId: String? = null
        var committedSeed: Int? = null

        val applied = TableEditorInlineEditResultApplier.apply(
            TableEditorInlineEditApplyInput(
                result = InlineEditActionResult(
                    nextInlineEdit = InlineEditState(),
                    nextTemplate = template,
                    nextInlineSessionState = InlineEditSessionState(),
                    templateApplyMode = InlineTemplateApplyMode.APPLY_DIRECTLY,
                    nextLowCounterWarningLatchedInSession = false,
                    committedCounterSeed = 7,
                    committedCellId = cell.cellId,
                ),
                currentTemplate = template,
                currentCounterUi = TableCounterUiState(autoNextCounterValue = 5),
                applyTemplateWithUndo = { undoApplied = true },
                applyTemplateDirectly = { directApplied = true },
                updateCounterConflictUi = { counterUi, _ -> counterUi },
                applyCommittedCounter = { _, cellId, seed, _, counterUi ->
                    committedCellId = cellId
                    committedSeed = seed
                    counterUi.copy(scopeNextCounter = seed)
                },
            )
        )

        assertFalse(directApplied)
        assertFalse(undoApplied)
        assertEquals(cell.cellId, committedCellId)
        assertEquals(7, committedSeed)
        assertEquals(7, applied.nextCounterUi.scopeNextCounter)
        assertNull(applied.nextSelectedCellId)
    }

    @Test
    fun opened_counter_conflict_updates_counter_ui_via_callback() {
        val template = defaultTableTemplateState()
        val conflict = TableCounterConflictDialogState(
            isVisible = true,
            editingCellId = "cell-1",
            pendingCounterCommitValue = 9,
            pendingCounterStreamNextValue = 10,
        )

        val applied = TableEditorInlineEditResultApplier.apply(
            TableEditorInlineEditApplyInput(
                result = InlineEditActionResult(
                    nextInlineEdit = InlineEditState(),
                    nextInlineSessionState = InlineEditSessionState(),
                    nextLowCounterWarningLatchedInSession = false,
                    openedCounterConflict = conflict,
                ),
                currentTemplate = template,
                currentCounterUi = TableCounterUiState(),
                applyTemplateWithUndo = { error("should not be called") },
                applyTemplateDirectly = { error("should not be called") },
                updateCounterConflictUi = { counterUi, dialogState ->
                    counterUi.copy(counterConflictDialogState = dialogState)
                },
                applyCommittedCounter = { _, _, _, _, counterUi -> counterUi },
            )
        )

        assertTrue(applied.nextCounterUi.counterConflictDialogState.isVisible)
        assertEquals(9, applied.nextCounterUi.counterConflictDialogState.pendingCounterCommitValue)
    }
}
