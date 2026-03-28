package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.ui.table.editor.InlineEditState
import com.dudoziworkshop.dzlog.ui.table.editor.clearInlineEditing

object TableEditorInlineEditActions {

    // 주요 정책: 첫 onValueChange 시점에만 undo snapshot을 1회 push하고, 값은 template에 즉시 반영한다.
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

    // 주요 정책: commitIfNeeded는 값 반영 본체가 아니라 세션 종료 / validation / conflict 처리를 담당한다.
    fun commitIfNeeded(context: InlineEditSessionContext): InlineEditActionResult {
        val id = context.inlineEdit.editingCellId ?: return noTemplateChange(
            context = context.copy(inlineSessionState = clearInlineSession()),
            actionLabel = "commit_inline_noop",
            nextInlineEdit = context.inlineEdit,
            nextInlineSessionState = clearInlineSession(),
        )

        val cell = context.currentTemplate.cells.firstOrNull { it.cellId == id }
        if (cell != null && cell.dataType == TableCellDataType.COUNTER) {
            val counterCommit = TableEditorCounterInlineCommitResolver.resolve(
                TableEditorCounterInlineCommitInput(
                    cellId = id,
                    editingValue = context.inlineEdit.editingValue,
                    currentTemplate = context.currentTemplate,
                    autoNextCounterValue = context.autoNextCounterValue,
                    lowCounterWarningLatchedInSession = context.lowCounterWarningLatchedInSession,
                    updateCell = context.updateCell,
                )
            )
            return when (counterCommit.outcome) {
                TableEditorCounterInlineCommitOutcome.BLOCKED_INVALID -> noTemplateChange(
                    context = context,
                    actionLabel = "commit_inline_blocked_invalid_counter",
                    wasBlocked = true,
                )
                TableEditorCounterInlineCommitOutcome.BLOCKED_CONFLICT -> InlineEditActionResult(
                    nextInlineEdit = context.inlineEdit,
                    nextTemplate = null,
                    nextInlineSessionState = context.inlineSessionState,
                    nextEditSessionOriginalCellState = context.editSessionOriginalCellState,
                    nextSelectedCellId = context.selectedCellId,
                    templateApplyMode = InlineTemplateApplyMode.NONE,
                    actionLabel = "commit_inline_blocked_conflict",
                    wasBlocked = true,
                    nextLowCounterWarningLatchedInSession = counterCommit.nextLowCounterWarningLatchedInSession,
                    openedCounterConflict = counterCommit.openedCounterConflict,
                    committedCellId = id,
                )
                TableEditorCounterInlineCommitOutcome.COMMITTED -> InlineEditActionResult(
                    nextInlineEdit = clearInlineEditing(context.inlineEdit),
                    nextTemplate = counterCommit.nextTemplate,
                    nextInlineSessionState = clearInlineSession(),
                    nextEditSessionOriginalCellState = context.editSessionOriginalCellState,
                    nextSelectedCellId = context.selectedCellId,
                    templateApplyMode = InlineTemplateApplyMode.APPLY_DIRECTLY,
                    actionLabel = "commit_inline_counter",
                    wasBlocked = false,
                    nextLowCounterWarningLatchedInSession = counterCommit.nextLowCounterWarningLatchedInSession,
                    committedCounterSeed = counterCommit.committedCounterSeed,
                    committedCellId = id,
                )
            }
        }

        return InlineEditActionResult(
            nextInlineEdit = clearInlineEditing(context.inlineEdit),
            nextTemplate = null,
            nextInlineSessionState = clearInlineSession(),
            nextEditSessionOriginalCellState = context.editSessionOriginalCellState,
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
                wasBlocked = true,
            )
        }

        val templateAfterCommit = commitResult.nextTemplate ?: context.currentTemplate
        val sessionState = sessionStateForSelection(
            template = templateAfterCommit,
            selectedCellId = requestedCellId,
            currentOriginalCellState = context.editSessionOriginalCellState,
            shouldTrackSessionSnapshot = shouldTrackSessionSnapshot,
        )
        return commitResult.copy(
            nextSelectedCellId = requestedCellId,
            nextInlineSessionState = InlineEditSessionState(),
            nextEditSessionOriginalCellState = sessionState.originalCellState,
            actionLabel = "select_cell",
            wasBlocked = false,
        )
    }

    // 선택 셀 기준 원본 스냅샷은 CELL_EDIT 세션 동안만 추적하고, 다른 패널에서는 정리한다.
    fun syncSessionSnapshot(
        context: InlineEditSessionContext,
        shouldTrackSessionSnapshot: Boolean,
    ): InlineEditActionResult {
        val sessionState = sessionStateForSelection(
            template = context.currentTemplate,
            selectedCellId = context.selectedCellId,
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
                val parsed = parseInlineCounterValue(nextValue)
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


    private fun resolveOriginalSnapshotForImmediateApply(
        context: InlineEditSessionContext,
        editingCell: TableCellState,
    ): TableCellState {
        return if (context.editSessionOriginalCellState?.cellId == editingCell.cellId) {
            context.editSessionOriginalCellState
        } else {
            editingCell.copy()
        }
    }

    private fun sessionStateForSelection(
        template: TableTemplateState,
        selectedCellId: String?,
        currentOriginalCellState: TableCellState?,
        shouldTrackSessionSnapshot: Boolean,
    ): InlineEditSessionSnapshotState {
        if (!shouldTrackSessionSnapshot || selectedCellId == null) {
            return InlineEditSessionSnapshotState(originalCellState = null)
        }
        if (currentOriginalCellState?.cellId == selectedCellId) {
            return InlineEditSessionSnapshotState(
                originalCellState = currentOriginalCellState,
            )
        }
        return InlineEditSessionSnapshotState(
            originalCellState = template.cells.firstOrNull { it.cellId == selectedCellId }?.copy(),
        )
    }
}

private data class InlineEditSessionSnapshotState(
    val originalCellState: TableCellState?,
)
