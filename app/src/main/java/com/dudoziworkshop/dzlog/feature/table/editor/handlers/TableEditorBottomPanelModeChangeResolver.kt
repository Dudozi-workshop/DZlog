package com.dudoziworkshop.dzlog.feature.table.editor.handlers

import com.dudoziworkshop.dzlog.ui.table.section.BottomEditorPanelMode

data class TableEditorBottomPanelModeChangeInput(
    val currentMode: BottomEditorPanelMode,
    val nextMode: BottomEditorPanelMode,
    val inlineCommitWasBlocked: Boolean,
    val wasStructureMode: Boolean,
    val currentSelectedCellId: String?,
)

data class TableEditorBottomPanelModeChangedState(
    val shouldApply: Boolean,
    val shouldClearFileNameTransientState: Boolean,
    val shouldClearPathTransientState: Boolean,
    val nextBottomPanelMode: BottomEditorPanelMode,
    val nextStructureSelectedCellIds: Set<String>?,
    val nextStructureSelectionRangeCleared: Boolean,
    val nextSelectedCellId: String?,
)

object TableEditorBottomPanelModeChangeResolver {

    fun resolve(input: TableEditorBottomPanelModeChangeInput): TableEditorBottomPanelModeChangedState {
        if (input.currentMode == input.nextMode || input.inlineCommitWasBlocked) {
            return TableEditorBottomPanelModeChangedState(
                shouldApply = false,
                shouldClearFileNameTransientState = false,
                shouldClearPathTransientState = false,
                nextBottomPanelMode = input.currentMode,
                nextStructureSelectedCellIds = null,
                nextStructureSelectionRangeCleared = false,
                nextSelectedCellId = null,
            )
        }

        val effects = TableEditorModeTransitionHandlers.resolveEffects(
            nextMode = input.nextMode,
            wasStructureMode = input.wasStructureMode,
        )
        return TableEditorBottomPanelModeChangedState(
            shouldApply = true,
            shouldClearFileNameTransientState = effects.clearFileNameTransientState,
            shouldClearPathTransientState = effects.clearPathTransientState,
            nextBottomPanelMode = input.nextMode,
            nextStructureSelectedCellIds = if (effects.shouldResetStructureSelection) emptySet() else null,
            nextStructureSelectionRangeCleared = effects.shouldResetStructureSelection,
            nextSelectedCellId = if (effects.shouldResetStructureSelection) input.currentSelectedCellId else null,
        )
    }
}
