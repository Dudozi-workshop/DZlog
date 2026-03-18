package com.dudoziworkshop.dzlog.ui.table

import com.dudoziworkshop.dzlog.ui.table.editor.InlineEditState
import com.dudoziworkshop.dzlog.ui.table.editor.isEditing
import com.dudoziworkshop.dzlog.ui.table.editor.shouldBlockTabSwitchAfterCommit
import com.dudoziworkshop.dzlog.ui.table.section.BottomEditorPanelMode

internal fun handleTableEditorBackNavigation(
    hasUnsavedChanges: Boolean,
    onShowUnsavedDialog: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    if (hasUnsavedChanges) {
        onShowUnsavedDialog()
    } else {
        onNavigateBack()
    }
}

internal fun requestCloseBottomPanel(
    inlineEdit: InlineEditState,
    onCommitInlineEdit: () -> Unit,
    onClosed: () -> Unit,
) {
    if (inlineEdit.isEditing()) {
        onCommitInlineEdit()
        if (shouldBlockTabSwitchAfterCommit(inlineEdit)) return
    }
    onClosed()
}

internal fun closeBottomPanelUiState(
    clearFileNameEditorTransientState: (Boolean) -> Unit,
    clearPathEditorTransientState: (Boolean) -> Unit,
    setBottomPanelMode: (BottomEditorPanelMode) -> Unit,
    setShowCellSettingsPanel: (Boolean) -> Unit,
    clearSelectedFileNameSlot: () -> Unit,
    clearSelectedPathSlot: () -> Unit,
) {
    setBottomPanelMode(BottomEditorPanelMode.NONE)
    setShowCellSettingsPanel(false)
    clearSelectedFileNameSlot()
    clearSelectedPathSlot()
    clearFileNameEditorTransientState(true)
    clearPathEditorTransientState(true)
}
