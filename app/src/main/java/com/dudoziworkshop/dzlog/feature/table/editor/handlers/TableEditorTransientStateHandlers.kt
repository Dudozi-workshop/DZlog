package com.dudoziworkshop.dzlog.feature.table.editor.handlers

object TableEditorTransientStateHandlers {
    fun clearFileNameEditorTransientState(
        clearDraft: Boolean,
        setIsFileNameCellPickMode: (Boolean) -> Unit,
        setShowManualInputEditor: (Boolean) -> Unit,
        setManualInputDraft: (String) -> Unit,
    ) {
        setIsFileNameCellPickMode(false)
        setShowManualInputEditor(false)
        if (clearDraft) {
            setManualInputDraft("")
        }
    }

    fun clearPathEditorTransientState(
        clearDraft: Boolean,
        setIsPathCellPickMode: (Boolean) -> Unit,
        setShowPathManualInputEditor: (Boolean) -> Unit,
        setPathManualInputDraft: (String) -> Unit,
    ) {
        setIsPathCellPickMode(false)
        setShowPathManualInputEditor(false)
        if (clearDraft) {
            setPathManualInputDraft("")
        }
    }
}

