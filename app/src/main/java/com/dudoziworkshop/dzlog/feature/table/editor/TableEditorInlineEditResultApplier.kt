package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.counter.table.TableCounterUiState
import com.dudoziworkshop.dzlog.feature.table.policy.TableCounterConflictDialogState
import com.dudoziworkshop.dzlog.ui.table.editor.InlineEditState

data class TableEditorInlineEditApplyInput(
    val result: InlineEditActionResult,
    val currentTemplate: TableTemplateState,
    val currentCounterUi: TableCounterUiState,
    val applyTemplateWithUndo: (TableTemplateState) -> Unit,
    val applyTemplateDirectly: (TableTemplateState) -> Unit,
    val updateCounterConflictUi: (TableCounterUiState, TableCounterConflictDialogState) -> TableCounterUiState,
    val applyCommittedCounter: (templateState: TableTemplateState, cellId: String, seed: Int, lowCounterWarningLatchedInSession: Boolean, counterUi: TableCounterUiState) -> TableCounterUiState,
)

data class TableEditorInlineEditAppliedState(
    val nextInlineEdit: InlineEditState,
    val nextInlineSessionState: InlineEditSessionState,
    val nextEditSessionOriginalCellState: TableCellState?,
    val nextSelectedCellId: String?,
    val nextCounterUi: TableCounterUiState,
)

object TableEditorInlineEditResultApplier {

    fun apply(input: TableEditorInlineEditApplyInput): TableEditorInlineEditAppliedState {
        val result = input.result
        val nextCounterUi = applyCounterSideEffects(input)
        applyTemplateSideEffects(input)
        return TableEditorInlineEditAppliedState(
            nextInlineEdit = result.nextInlineEdit,
            nextInlineSessionState = result.nextInlineSessionState,
            nextEditSessionOriginalCellState = result.nextEditSessionOriginalCellState,
            nextSelectedCellId = result.nextSelectedCellId,
            nextCounterUi = nextCounterUi,
        )
    }

    private fun applyCounterSideEffects(input: TableEditorInlineEditApplyInput): TableCounterUiState {
        val result = input.result
        var nextCounterUi = input.currentCounterUi.copy(
            lowCounterWarningLatchedInSession = result.nextLowCounterWarningLatchedInSession,
        )
        result.openedCounterConflict?.let {
            nextCounterUi = input.updateCounterConflictUi(nextCounterUi, it)
        }
        result.committedCounterSeed?.let { seed ->
            val cellId = result.committedCellId ?: return@let
            nextCounterUi = input.applyCommittedCounter(
                result.nextTemplate ?: input.currentTemplate,
                cellId,
                seed,
                result.nextLowCounterWarningLatchedInSession,
                nextCounterUi,
            )
        }
        return nextCounterUi
    }

    private fun applyTemplateSideEffects(input: TableEditorInlineEditApplyInput) {
        val result = input.result
        if (result.committedCounterSeed != null) return
        val nextTemplate = result.nextTemplate ?: return
        when (result.templateApplyMode) {
            InlineTemplateApplyMode.NONE -> Unit
            InlineTemplateApplyMode.PUSH_UNDO_THEN_APPLY -> input.applyTemplateWithUndo(nextTemplate)
            InlineTemplateApplyMode.APPLY_DIRECTLY -> input.applyTemplateDirectly(nextTemplate)
        }
    }
}
