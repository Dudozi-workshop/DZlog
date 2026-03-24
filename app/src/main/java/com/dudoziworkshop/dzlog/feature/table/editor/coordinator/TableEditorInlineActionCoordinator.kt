package com.dudoziworkshop.dzlog.feature.table.editor.coordinator

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.counter.table.TableCounterUiState
import com.dudoziworkshop.dzlog.feature.table.editor.InlineEditActionResult
import com.dudoziworkshop.dzlog.feature.table.editor.InlineEditSessionContext
import com.dudoziworkshop.dzlog.feature.table.editor.TableEditorInlineEditActions
import com.dudoziworkshop.dzlog.feature.table.editor.TableEditorInlineEditApplyInput
import com.dudoziworkshop.dzlog.feature.table.editor.TableEditorInlineEditAppliedState
import com.dudoziworkshop.dzlog.feature.table.editor.TableEditorInlineEditResultApplier
import com.dudoziworkshop.dzlog.feature.table.policy.TableCounterConflictDialogState

data class TableEditorInlineActionBindings(
    val currentSessionContext: () -> InlineEditSessionContext,
    val currentTemplate: () -> TableTemplateState,
    val currentCounterUi: () -> TableCounterUiState,
    val applyTemplateWithUndo: (TableTemplateState) -> Unit,
    val applyTemplateDirectly: (TableTemplateState) -> Unit,
    val updateCounterConflictUi: (TableCounterUiState, TableCounterConflictDialogState) -> TableCounterUiState,
    val applyCommittedCounter: (templateState: TableTemplateState, cellId: String, seed: Int, lowCounterWarningLatchedInSession: Boolean, counterUi: TableCounterUiState) -> TableCounterUiState,
    val reflectAppliedState: (TableEditorInlineEditAppliedState) -> Unit,
)

object TableEditorInlineActionCoordinator {

    fun runAction(
        bindings: TableEditorInlineActionBindings,
        action: (InlineEditSessionContext) -> InlineEditActionResult,
    ): InlineEditActionResult {
        val result = action(bindings.currentSessionContext())
        applyResult(bindings, result)
        return result
    }

    fun runCommitAction(bindings: TableEditorInlineActionBindings): InlineEditActionResult {
        return runAction(bindings, TableEditorInlineEditActions::commitIfNeeded)
    }

    private fun applyResult(
        bindings: TableEditorInlineActionBindings,
        result: InlineEditActionResult,
    ) {
        val appliedState = TableEditorInlineEditResultApplier.apply(
            TableEditorInlineEditApplyInput(
                result = result,
                currentTemplate = bindings.currentTemplate(),
                currentCounterUi = bindings.currentCounterUi(),
                applyTemplateWithUndo = bindings.applyTemplateWithUndo,
                applyTemplateDirectly = bindings.applyTemplateDirectly,
                updateCounterConflictUi = bindings.updateCounterConflictUi,
                applyCommittedCounter = bindings.applyCommittedCounter,
            )
        )
        bindings.reflectAppliedState(appliedState)
    }
}
