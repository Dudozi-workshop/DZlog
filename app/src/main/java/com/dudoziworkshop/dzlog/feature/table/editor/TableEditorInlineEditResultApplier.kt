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
        val nextCounterUi = applyCounterSideEffects(input, result)
        applyTemplateSideEffects(input, result)
        return TableEditorInlineEditAppliedState(
            nextInlineEdit = result.nextInlineEdit,
            nextInlineSessionState = result.nextInlineSessionState,
            nextEditSessionOriginalCellState = result.nextEditSessionOriginalCellState,
            nextSelectedCellId = result.nextSelectedCellId,
            nextCounterUi = nextCounterUi,
        )
    }

    private fun applyCounterSideEffects(
        input: TableEditorInlineEditApplyInput,
        result: InlineEditActionResult,
    ): TableCounterUiState {
        var nextCounterUi = input.currentCounterUi.copy(
            lowCounterWarningLatchedInSession = result.nextLowCounterWarningLatchedInSession,
        )
        nextCounterUi = applyOpenedCounterConflict(input, result, nextCounterUi)
        nextCounterUi = applyCommittedCounter(input, result, nextCounterUi)
        return nextCounterUi
    }

    private fun applyOpenedCounterConflict(
        input: TableEditorInlineEditApplyInput,
        result: InlineEditActionResult,
        counterUi: TableCounterUiState,
    ): TableCounterUiState {
        val openedCounterConflict = result.openedCounterConflict ?: return counterUi
        return input.updateCounterConflictUi(counterUi, openedCounterConflict)
    }

    private fun applyCommittedCounter(
        input: TableEditorInlineEditApplyInput,
        result: InlineEditActionResult,
        counterUi: TableCounterUiState,
    ): TableCounterUiState {
        val seed = result.committedCounterSeed ?: return counterUi
        val cellId = result.committedCellId ?: return counterUi
        return input.applyCommittedCounter(
            result.nextTemplate ?: input.currentTemplate,
            cellId,
            seed,
            result.nextLowCounterWarningLatchedInSession,
            counterUi,
        )
    }

    private fun applyTemplateSideEffects(
        input: TableEditorInlineEditApplyInput,
        result: InlineEditActionResult,
    ) {
        if (result.committedCounterSeed != null) return
        val nextTemplate = result.nextTemplate ?: return
        when (result.templateApplyMode) {
            InlineTemplateApplyMode.NONE -> Unit
            InlineTemplateApplyMode.PUSH_UNDO_THEN_APPLY -> input.applyTemplateWithUndo(nextTemplate)
            InlineTemplateApplyMode.APPLY_DIRECTLY -> input.applyTemplateDirectly(nextTemplate)
        }
    }
}
