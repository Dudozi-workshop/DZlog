package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.policy.evaluateCounterEditConflict
import com.dudoziworkshop.dzlog.feature.table.policy.nextLowCounterWarningLatch
import com.dudoziworkshop.dzlog.feature.table.policy.openCounterConflictDialog
import com.dudoziworkshop.dzlog.feature.table.policy.parseNonNegativeInt
import com.dudoziworkshop.dzlog.ui.table.editor.InlineEditState
import com.dudoziworkshop.dzlog.ui.table.editor.clearInlineEditing

object TableEditorInlineEditActions {

    fun applyInlineValueChange(
        context: InlineEditSessionContext,
        nextValue: String,
    ): InlineEditActionResult {
        val editingCellId = context.inlineEdit.editingCellId ?: return noTemplateChange(
            context = context,
            actionLabel = "inline_value_change_ignored",
        )
        val currentCell = context.currentTemplate.cells.firstOrNull { it.cellId == editingCellId } ?: return noTemplateChange(
            context = context,
            actionLabel = "inline_value_change_missing_cell",
        )
        val shouldPushUndoSnapshot = context.inlineSessionState.activeCellId != editingCellId ||
            !context.inlineSessionState.hasPushedUndoSnapshot
        val updatedTemplate = context.updateCell(context.currentTemplate, editingCellId) { cell ->
            applyImmediateInlineValue(cell, nextValue)
        }
        return InlineEditActionResult(
            nextInlineEdit = context.inlineEdit.copy(editingValue = nextValue),
            nextTemplate = updatedTemplate,
            nextInlineSessionState = InlineEditSessionState(
                activeCellId = editingCellId,
                hasPushedUndoSnapshot = true,
            ),
            nextEditSessionOriginalCellState = resolveOriginalSnapshotForImmediateApply(
                context = context,
                editingCell = currentCell,
            ),
            nextEditSessionSnapshotCellId = editingCellId,
            nextSelectedCellId = context.selectedCellId,
            templateApplyMode = if (shouldPushUndoSnapshot) {
                InlineTemplateApplyMode.PUSH_UNDO_THEN_APPLY
            } else {
                InlineTemplateApplyMode.APPLY_DIRECTLY
            },
            actionLabel = if (shouldPushUndoSnapshot) "inline_value_change_start_session" else "inline_value_change_continue_session",
            wasBlocked = false,
            nextLowCounterWarningLatchedInSession = context.lowCounterWarningLatchedInSession,
        )
    }

    fun commitIfNeeded(context: InlineEditSessionContext): InlineEditActionResult {
        val id = context.inlineEdit.editingCellId ?: return noTemplateChange(
            context = context.copy(inlineSessionState = context.inlineSessionState.clearInlineSession()),
            actionLabel = "commit_inline_noop",
            nextInlineEdit = context.inlineEdit,
            nextInlineSessionState = context.inlineSessionState.clearInlineSession(),
        )

        val cell = context.currentTemplate.cells.firstOrNull { it.cellId == id }
        if (cell != null && cell.dataType == TableCellDataType.COUNTER) {
            val pendingValue = parseNonNegativeInt(context.inlineEdit.editingValue)
            if (pendingValue == null) {
                return noTemplateChange(
                    context = context,
                    actionLabel = "commit_inline_blocked_invalid_counter",
                    wasBlocked = true,
                )
            }
            val nextLowWarningLatch = nextLowCounterWarningLatch(
                pendingCounterCommitValue = pendingValue,
                streamNext = context.autoNextCounterValue,
            )
            val conflict = evaluateCounterEditConflict(
                newValueText = context.inlineEdit.editingValue,
                streamNext = context.autoNextCounterValue,
                lowCounterWarningLatchedInSession = context.lowCounterWarningLatchedInSession,
            )
            if (conflict != null) {
                return InlineEditActionResult(
                    nextInlineEdit = context.inlineEdit,
                    nextTemplate = null,
                    nextInlineSessionState = context.inlineSessionState,
                    nextEditSessionOriginalCellState = context.editSessionOriginalCellState,
                    nextEditSessionSnapshotCellId = context.editSessionSnapshotCellId,
                    nextSelectedCellId = context.selectedCellId,
                    templateApplyMode = InlineTemplateApplyMode.NONE,
                    actionLabel = "commit_inline_blocked_conflict",
                    wasBlocked = true,
                    nextLowCounterWarningLatchedInSession = nextLowWarningLatch,
                    openedCounterConflict = openCounterConflictDialog(
                        editingCellId = id,
                        conflict = conflict,
                    ),
                    committedCellId = id,
                )
            }
            return InlineEditActionResult(
                nextInlineEdit = clearInlineEditing(context.inlineEdit),
                nextTemplate = normalizeCounterValueIfNeeded(context.currentTemplate, id, pendingValue, context.updateCell),
                nextInlineSessionState = context.inlineSessionState.clearInlineSession(),
                nextEditSessionOriginalCellState = context.editSessionOriginalCellState,
                nextEditSessionSnapshotCellId = context.editSessionSnapshotCellId,
                nextSelectedCellId = context.selectedCellId,
                templateApplyMode = InlineTemplateApplyMode.APPLY_DIRECTLY,
                actionLabel = "commit_inline_counter",
                wasBlocked = false,
                nextLowCounterWarningLatchedInSession = nextLowWarningLatch,
                committedCounterSeed = pendingValue.coerceAtLeast(1),
                committedCellId = id,
            )
        }

        return InlineEditActionResult(
            nextInlineEdit = clearInlineEditing(context.inlineEdit),
            nextTemplate = null,
            nextInlineSessionState = context.inlineSessionState.clearInlineSession(),
            nextEditSessionOriginalCellState = context.editSessionOriginalCellState,
            nextEditSessionSnapshotCellId = context.editSessionSnapshotCellId,
            nextSelectedCellId = context.selectedCellId,
            templateApplyMode = InlineTemplateApplyMode.NONE,
            actionLabel = "commit_inline",
            wasBlocked = false,
            nextLowCounterWarningLatchedInSession = context.lowCounterWarningLatchedInSession,
            committedCellId = id,
        )
    }

    fun requestCellSelection(
        context: InlineEditSessionContext,
        requestedCellId: String?,
        shouldTrackSessionSnapshot: Boolean,
        allowReselectCurrentCell: Boolean,
    ): InlineEditActionResult {
        if (!allowReselectCurrentCell && context.selectedCellId == requestedCellId) {
            return noTemplateChange(
                context = context,
                sessionState = sessionStateForSelection(
                    template = context.currentTemplate,
                    selectedCellId = context.selectedCellId,
                    currentSnapshotCellId = context.editSessionSnapshotCellId,
                    currentOriginalCellState = context.editSessionOriginalCellState,
                    shouldTrackSessionSnapshot = shouldTrackSessionSnapshot,
                ),
                actionLabel = "select_cell_noop",
            )
        }

        val commitResult = commitIfNeeded(context)
        if (commitResult.wasBlocked) {
            return commitResult.copy(
                actionLabel = "select_cell_blocked",
                nextSelectedCellId = context.selectedCellId,
                nextInlineSessionState = context.inlineSessionState,
                nextEditSessionOriginalCellState = context.editSessionOriginalCellState,
                nextEditSessionSnapshotCellId = context.editSessionSnapshotCellId,
                wasBlocked = true,
            )
        }

        val templateAfterCommit = commitResult.nextTemplate ?: context.currentTemplate
        val sessionState = sessionStateForSelection(
            template = templateAfterCommit,
            selectedCellId = requestedCellId,
            currentSnapshotCellId = context.editSessionSnapshotCellId,
            currentOriginalCellState = context.editSessionOriginalCellState,
            shouldTrackSessionSnapshot = shouldTrackSessionSnapshot,
        )
        return commitResult.copy(
            nextSelectedCellId = requestedCellId,
            nextInlineSessionState = InlineEditSessionState(),
            nextEditSessionOriginalCellState = sessionState.originalCellState,
            nextEditSessionSnapshotCellId = sessionState.snapshotCellId,
            actionLabel = "select_cell",
            wasBlocked = false,
        )
    }

    fun requestSaveSelectedCell(
        context: InlineEditSessionContext,
        shouldTrackSessionSnapshot: Boolean,
    ): InlineEditActionResult {
        val commitResult = commitIfNeeded(context)
        if (commitResult.wasBlocked) {
            return commitResult.copy(
                actionLabel = "save_selected_cell_blocked",
                wasBlocked = true,
            )
        }
        val sessionState = sessionStateForSelection(
            template = commitResult.nextTemplate ?: context.currentTemplate,
            selectedCellId = context.selectedCellId,
            currentSnapshotCellId = null,
            currentOriginalCellState = null,
            shouldTrackSessionSnapshot = shouldTrackSessionSnapshot,
        )
        return commitResult.copy(
            nextInlineSessionState = InlineEditSessionState(),
            nextEditSessionOriginalCellState = sessionState.originalCellState,
            nextEditSessionSnapshotCellId = sessionState.snapshotCellId,
            nextSelectedCellId = context.selectedCellId,
            actionLabel = "save_selected_cell",
            wasBlocked = false,
        )
    }

    fun requestRevertSelectedCell(
        context: InlineEditSessionContext,
        shouldTrackSessionSnapshot: Boolean,
    ): InlineEditActionResult {
        val snapshot = context.editSessionOriginalCellState
        val selectedId = context.selectedCellId
        if (snapshot == null || selectedId == null || snapshot.cellId != selectedId) {
            return noTemplateChange(
                context = context,
                actionLabel = "revert_selected_cell_noop",
            )
        }
        val revertedTemplate = context.updateCell(context.currentTemplate, snapshot.cellId) { snapshot }
        val sessionState = sessionStateForSelection(
            template = revertedTemplate,
            selectedCellId = selectedId,
            currentSnapshotCellId = null,
            currentOriginalCellState = null,
            shouldTrackSessionSnapshot = shouldTrackSessionSnapshot,
        )
        return InlineEditActionResult(
            nextInlineEdit = clearInlineEditing(context.inlineEdit),
            nextTemplate = revertedTemplate,
            nextInlineSessionState = InlineEditSessionState(),
            nextEditSessionOriginalCellState = sessionState.originalCellState,
            nextEditSessionSnapshotCellId = sessionState.snapshotCellId,
            nextSelectedCellId = selectedId,
            templateApplyMode = InlineTemplateApplyMode.APPLY_DIRECTLY,
            actionLabel = "revert_selected_cell",
            wasBlocked = false,
            nextLowCounterWarningLatchedInSession = context.lowCounterWarningLatchedInSession,
        )
    }

    fun syncSessionSnapshot(
        context: InlineEditSessionContext,
        shouldTrackSessionSnapshot: Boolean,
    ): InlineEditActionResult {
        val sessionState = sessionStateForSelection(
            template = context.currentTemplate,
            selectedCellId = context.selectedCellId,
            currentSnapshotCellId = context.editSessionSnapshotCellId,
            currentOriginalCellState = context.editSessionOriginalCellState,
            shouldTrackSessionSnapshot = shouldTrackSessionSnapshot,
        )
        return noTemplateChange(
            context = context,
            sessionState = sessionState,
            actionLabel = if (shouldTrackSessionSnapshot) "sync_inline_session_snapshot" else "clear_inline_session_snapshot",
        )
    }

    private fun noTemplateChange(
        context: InlineEditSessionContext,
        sessionState: InlineEditSessionSnapshotState = InlineEditSessionSnapshotState(
            originalCellState = context.editSessionOriginalCellState,
            snapshotCellId = context.editSessionSnapshotCellId,
        ),
        actionLabel: String,
        nextInlineEdit: InlineEditState = context.inlineEdit,
        nextInlineSessionState: InlineEditSessionState = context.inlineSessionState,
        wasBlocked: Boolean = false,
    ): InlineEditActionResult {
        return InlineEditActionResult(
            nextInlineEdit = nextInlineEdit,
            nextTemplate = null,
            nextInlineSessionState = nextInlineSessionState,
            nextEditSessionOriginalCellState = sessionState.originalCellState,
            nextEditSessionSnapshotCellId = sessionState.snapshotCellId,
            nextSelectedCellId = context.selectedCellId,
            templateApplyMode = InlineTemplateApplyMode.NONE,
            actionLabel = actionLabel,
            wasBlocked = wasBlocked,
            nextLowCounterWarningLatchedInSession = context.lowCounterWarningLatchedInSession,
        )
    }

    private fun applyImmediateInlineValue(cell: TableCellState, nextValue: String): TableCellState {
        return when (cell.dataType) {
            TableCellDataType.TEXT -> cell.copy(
                rawText = nextValue,
                typedValue = CellValue.Text(nextValue),
            )
            TableCellDataType.NUMBER -> cell.copy(
                rawText = nextValue,
                typedValue = CellValue.Number(nextValue),
            )
            TableCellDataType.COUNTER -> {
                val parsed = parseNonNegativeInt(nextValue)
                if (parsed == null) {
                    cell.copy(rawText = nextValue)
                } else {
                    cell.copy(
                        rawText = nextValue,
                        typedValue = CellValue.CounterSeed(parsed.coerceAtLeast(0)),
                    )
                }
            }
            else -> cell
        }
    }

    private fun normalizeCounterValueIfNeeded(
        template: TableTemplateState,
        cellId: String,
        pendingValue: Int,
        updateCell: (TableTemplateState, String, (TableCellState) -> TableCellState) -> TableTemplateState,
    ): TableTemplateState {
        return updateCell(template, cellId) { cell ->
            cell.copy(
                rawText = pendingValue.toString(),
                typedValue = CellValue.CounterSeed(pendingValue.coerceAtLeast(0)),
            )
        }
    }

    private fun resolveOriginalSnapshotForImmediateApply(
        context: InlineEditSessionContext,
        editingCell: TableCellState,
    ): TableCellState {
        return if (context.editSessionSnapshotCellId == editingCell.cellId && context.editSessionOriginalCellState?.cellId == editingCell.cellId) {
            context.editSessionOriginalCellState
        } else {
            editingCell.copy()
        }
    }

    private fun sessionStateForSelection(
        template: TableTemplateState,
        selectedCellId: String?,
        currentSnapshotCellId: String?,
        currentOriginalCellState: TableCellState?,
        shouldTrackSessionSnapshot: Boolean,
    ): InlineEditSessionSnapshotState {
        if (!shouldTrackSessionSnapshot || selectedCellId == null) {
            return InlineEditSessionSnapshotState(originalCellState = null, snapshotCellId = null)
        }
        if (currentSnapshotCellId == selectedCellId && currentOriginalCellState?.cellId == selectedCellId) {
            return InlineEditSessionSnapshotState(
                originalCellState = currentOriginalCellState,
                snapshotCellId = currentSnapshotCellId,
            )
        }
        return InlineEditSessionSnapshotState(
            originalCellState = template.cells.firstOrNull { it.cellId == selectedCellId }?.copy(),
            snapshotCellId = selectedCellId,
        )
    }
}

private data class InlineEditSessionSnapshotState(
    val originalCellState: TableCellState?,
    val snapshotCellId: String?,
)
