package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.data.template.defaultTableTemplateState
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.ui.table.editor.InlineEditState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TableEditorInlineEditActionsTest {

    @Test
    fun first_value_change_updates_template_immediately_and_requests_undo_snapshot() {
        val template = defaultTableTemplateState()
        val cell = template.cells.first()
        val context = context(
            currentTemplate = template,
            selectedCellId = cell.cellId,
            inlineEdit = InlineEditState(
                editingCellId = cell.cellId,
                editingValue = cell.toEditableText()
            ),
        )

        val result = TableEditorInlineEditActions.applyInlineValueChange(
            context = context,
            nextValue = "a",
        )

        assertEquals("a", result.nextTemplate?.cells?.first { it.cellId == cell.cellId }?.rawText)
        assertEquals("a", result.nextInlineEdit.editingValue)
        assertTrue(result.shouldApplyTemplateWithUndo)
        assertFalse(result.shouldApplyTemplateDirectly)
        assertEquals(cell.cellId, result.nextInlineSessionState.activeCellId)
        assertTrue(result.nextInlineSessionState.hasPushedUndoSnapshot)
    }

    @Test
    fun repeated_value_changes_in_same_session_do_not_request_additional_undo_snapshot() {
        val template = defaultTableTemplateState()
        val cell = template.cells.first()
        val startedSession = context(
            currentTemplate = updateCell(template, cell.cellId) {
                it.copy(rawText = "a", typedValue = CellValue.Text("a"))
            },
            selectedCellId = cell.cellId,
            inlineEdit = InlineEditState(
                editingCellId = cell.cellId,
                editingValue = "a"
            ),
            inlineSessionState = InlineEditSessionState(
                activeCellId = cell.cellId,
                hasPushedUndoSnapshot = true,
            ),
            editSessionOriginalCellState = cell.copy(),
        )

        val result = TableEditorInlineEditActions.applyInlineValueChange(
            context = startedSession,
            nextValue = "abc",
        )

        assertEquals("abc", result.nextTemplate?.cells?.first { it.cellId == cell.cellId }?.rawText)
        assertFalse(result.shouldApplyTemplateWithUndo)
        assertTrue(result.shouldApplyTemplateDirectly)
        assertEquals(cell.cellId, result.nextInlineSessionState.activeCellId)
        assertTrue(result.nextInlineSessionState.hasPushedUndoSnapshot)
    }

    @Test
    fun selecting_other_cell_after_edit_ends_inline_session() {
        val template = defaultTableTemplateState()
        val sourceCell = template.cells.first()
        val targetCell = template.cells.last()
        val editedTemplate = updateCell(template, sourceCell.cellId) {
            it.copy(rawText = "edited", typedValue = CellValue.Text("edited"))
        }
        val context = context(
            currentTemplate = editedTemplate,
            selectedCellId = sourceCell.cellId,
            inlineEdit = InlineEditState(
                editingCellId = sourceCell.cellId,
                editingValue = "edited"
            ),
            inlineSessionState = InlineEditSessionState(
                activeCellId = sourceCell.cellId,
                hasPushedUndoSnapshot = true,
            ),
            editSessionOriginalCellState = sourceCell.copy(),
        )

        val result = TableEditorInlineEditActions.requestCellSelection(
            context = context,
            requestedCellId = targetCell.cellId,
            shouldTrackSessionSnapshot = true,
            allowReselectCurrentCell = false,
        )

        assertFalse(result.wasBlocked)
        assertNull(result.nextInlineEdit.editingCellId)
        assertEquals(targetCell.cellId, result.nextSelectedCellId)
        assertNull(result.nextInlineSessionState.activeCellId)
        assertFalse(result.nextInlineSessionState.hasPushedUndoSnapshot)
    }

    @Test
    fun commit_if_needed_clears_inline_session_after_panel_exit_or_save_path() {
        val template = defaultTableTemplateState()
        val cell = template.cells.first()
        val editedTemplate = updateCell(template, cell.cellId) {
            it.copy(rawText = "edited", typedValue = CellValue.Text("edited"))
        }
        val context = context(
            currentTemplate = editedTemplate,
            selectedCellId = cell.cellId,
            inlineEdit = InlineEditState(
                editingCellId = cell.cellId,
                editingValue = "edited"
            ),
            inlineSessionState = InlineEditSessionState(
                activeCellId = cell.cellId,
                hasPushedUndoSnapshot = true,
            ),
        )

        val result = TableEditorInlineEditActions.commitIfNeeded(context)

        assertNull(result.nextInlineEdit.editingCellId)
        assertNull(result.nextInlineSessionState.activeCellId)
        assertFalse(result.nextInlineSessionState.hasPushedUndoSnapshot)
        assertFalse(result.wasBlocked)
    }

    @Test
    fun editing_other_cell_selection_stays_when_counter_commit_is_blocked() {
        val template = counterTemplate()
        val sourceCell = template.cells.first()
        val targetCell = template.cells.last()
        val context = context(
            currentTemplate = template,
            selectedCellId = sourceCell.cellId,
            inlineEdit = InlineEditState(
                editingCellId = sourceCell.cellId,
                editingValue = "not-a-number"
            ),
            inlineSessionState = InlineEditSessionState(
                activeCellId = sourceCell.cellId,
                hasPushedUndoSnapshot = true,
            ),
            editSessionOriginalCellState = sourceCell.copy(),
        )

        val result = TableEditorInlineEditActions.requestCellSelection(
            context = context,
            requestedCellId = targetCell.cellId,
            shouldTrackSessionSnapshot = true,
            allowReselectCurrentCell = false,
        )

        assertTrue(result.wasBlocked)
        assertEquals(sourceCell.cellId, result.nextSelectedCellId)
        assertEquals(sourceCell.cellId, result.nextInlineEdit.editingCellId)
        assertEquals(sourceCell.cellId, result.nextInlineSessionState.activeCellId)
        assertNull(result.nextTemplate)
    }



    @Test
    fun sync_session_snapshot_keeps_same_cell_snapshot_and_clears_when_tracking_is_disabled() {
        val template = defaultTableTemplateState()
        val firstCell = template.cells.first()
        val context = context(
            currentTemplate = template,
            selectedCellId = firstCell.cellId,
            editSessionOriginalCellState = firstCell.copy(rawText = "origin"),
            inlineSessionState = InlineEditSessionState(
                activeCellId = firstCell.cellId,
                hasPushedUndoSnapshot = true,
            ),
        )

        val tracked = TableEditorInlineEditActions.syncSessionSnapshot(
            context = context,
            shouldTrackSessionSnapshot = true,
        )
        val cleared = TableEditorInlineEditActions.syncSessionSnapshot(
            context = context,
            shouldTrackSessionSnapshot = false,
        )

        assertEquals("origin", tracked.nextEditSessionOriginalCellState?.rawText)
        assertEquals(firstCell.cellId, tracked.nextEditSessionOriginalCellState?.cellId)
        assertNull(cleared.nextEditSessionOriginalCellState)
    }

    private fun context(
        currentTemplate: TableTemplateState,
        inlineEdit: InlineEditState = InlineEditState(),
        selectedCellId: String? = null,
        inlineSessionState: InlineEditSessionState = InlineEditSessionState(),
        editSessionOriginalCellState: TableCellState? = null,
        autoNextCounterValue: Int = 10,
        lowCounterWarningLatchedInSession: Boolean = false,
    ): InlineEditSessionContext {
        return InlineEditSessionContext(
            currentTemplate = currentTemplate,
            inlineEdit = inlineEdit,
            selectedCellId = selectedCellId,
            inlineSessionState = inlineSessionState,
            editSessionOriginalCellState = editSessionOriginalCellState,
            autoNextCounterValue = autoNextCounterValue,
            lowCounterWarningLatchedInSession = lowCounterWarningLatchedInSession,
            updateCell = ::updateCell,
        )
    }

    private fun counterTemplate(): TableTemplateState {
        val seed = 3
        val template = defaultTableTemplateState()
        val cell = template.cells.first()
        return updateCell(template, cell.cellId) {
            it.copy(
                dataType = TableCellDataType.COUNTER,
                rawText = seed.toString(),
                typedValue = CellValue.CounterSeed(seed),
            )
        }
    }
}
