package com.dudoziworkshop.dzlog.ui.table

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionRange
import com.dudoziworkshop.dzlog.feature.table.editor.TableUndoManager
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState

internal data class TableEditorUndoSnapshot(
    val templateState: TableTemplateState,
    val styleState: TableStyleState,
    val selectedCellId: String?,
    val structureSelectedCellIds: Set<String>,
    val structureSelectionRange: TableSelectionRange?,
)

internal fun buildTableEditorUndoSnapshot(
    templateState: TableTemplateState,
    styleState: TableStyleState,
    selectedCellId: String?,
    structureSelectedCellIds: Set<String>,
    structureSelectionRange: TableSelectionRange?,
): TableEditorUndoSnapshot = TableEditorUndoSnapshot(
    templateState = templateState,
    styleState = styleState,
    selectedCellId = selectedCellId,
    structureSelectedCellIds = structureSelectedCellIds,
    structureSelectionRange = structureSelectionRange,
)

internal fun pushUndoSnapshotBeforeChange(
    undoManager: TableUndoManager<TableEditorUndoSnapshot>,
    currentSnapshot: TableEditorUndoSnapshot,
    nextTemplate: TableTemplateState = currentSnapshot.templateState,
    nextStyle: TableStyleState = currentSnapshot.styleState,
): Boolean {
    val nextSnapshot = currentSnapshot.copy(
        templateState = nextTemplate,
        styleState = nextStyle,
    )
    if (currentSnapshot == nextSnapshot) return false
    undoManager.pushSnapshotBeforeAction(currentSnapshot)
    return true
}

internal fun undoTableEditorSnapshot(
    undoManager: TableUndoManager<TableEditorUndoSnapshot>,
    currentSnapshot: TableEditorUndoSnapshot,
): TableEditorUndoSnapshot? {
    val restored = undoManager.undo(currentSnapshot)
    return restored.takeUnless { it == currentSnapshot }
}
