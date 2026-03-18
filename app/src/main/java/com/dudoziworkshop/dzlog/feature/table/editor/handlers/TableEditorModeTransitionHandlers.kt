package com.dudoziworkshop.dzlog.feature.table.editor.handlers

import com.dudoziworkshop.dzlog.ui.table.section.BottomEditorPanelMode

data class TableEditorModeTransitionEffects(
    val clearFileNameTransientState: Boolean,
    val clearPathTransientState: Boolean,
    val shouldResetStructureSelection: Boolean,
)

object TableEditorModeTransitionHandlers {
    fun resolveEffects(
        nextMode: BottomEditorPanelMode,
        wasStructureMode: Boolean,
    ): TableEditorModeTransitionEffects {
        return TableEditorModeTransitionEffects(
            clearFileNameTransientState = nextMode != BottomEditorPanelMode.FILENAME_EDIT,
            clearPathTransientState = nextMode != BottomEditorPanelMode.PATH_EDIT,
            shouldResetStructureSelection = nextMode == BottomEditorPanelMode.STRUCTURE_EDIT || wasStructureMode,
        )
    }
}

