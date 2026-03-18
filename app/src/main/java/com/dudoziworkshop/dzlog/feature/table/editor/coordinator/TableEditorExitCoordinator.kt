package com.dudoziworkshop.dzlog.feature.table.editor.coordinator

object TableEditorExitCoordinator {
    sealed interface Effect {
        data object ExitNow : Effect
        data object OpenUnsavedChangesDialog : Effect
    }

    fun onBackPressed(hasUnsavedChanges: Boolean): Effect {
        return if (hasUnsavedChanges) Effect.OpenUnsavedChangesDialog else Effect.ExitNow
    }
}

