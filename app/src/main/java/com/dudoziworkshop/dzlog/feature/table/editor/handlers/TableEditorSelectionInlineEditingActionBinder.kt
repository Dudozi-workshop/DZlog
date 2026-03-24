package com.dudoziworkshop.dzlog.feature.table.editor.handlers

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.InlineEditActionResult
import com.dudoziworkshop.dzlog.feature.table.editor.InlineEditSessionContext
import com.dudoziworkshop.dzlog.feature.table.editor.TableEditorInlineEditActions
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionRange
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionResolver
import com.dudoziworkshop.dzlog.ui.table.editor.InlineEditState
import com.dudoziworkshop.dzlog.ui.table.editor.startInlineEditing
import com.dudoziworkshop.dzlog.ui.table.format.TableFormatDialogState
import com.dudoziworkshop.dzlog.ui.table.format.open
import com.dudoziworkshop.dzlog.ui.table.section.BottomEditorPanelMode

data class TableEditorSelectionActionBindings(
    val currentTemplate: () -> TableTemplateState,
    val currentBottomPanelMode: () -> BottomEditorPanelMode,
    val isStructureEditMode: () -> Boolean,
    val runInlineAction: ((InlineEditSessionContext) -> InlineEditActionResult) -> InlineEditActionResult,
    val setStructureSelectedCellIds: (Set<String>) -> Unit,
    val setStructureSelectionRange: (TableSelectionRange?) -> Unit,
    val setSelectedCellId: (String?) -> Unit,
)

data class TableEditorInlineEditingActionBindings(
    val currentInlineEdit: () -> InlineEditState,
    val setInlineEdit: (InlineEditState) -> Unit,
    val currentFormatDialog: () -> TableFormatDialogState,
    val setFormatDialog: (TableFormatDialogState) -> Unit,
    val runInlineAction: ((InlineEditSessionContext) -> InlineEditActionResult) -> InlineEditActionResult,
    val runInlineCommitAction: () -> InlineEditActionResult,
    val commitInlineEditIfNeeded: () -> Unit,
    val setShowCellSettingsPanel: (Boolean) -> Unit,
)

object TableEditorSelectionInlineEditingActionBinder {

    fun requestSelectCell(bindings: TableEditorSelectionActionBindings, cellId: String?) {
        val result = bindings.runInlineAction { context ->
            TableEditorInlineEditActions.requestCellSelection(
                context = context,
                requestedCellId = cellId,
                shouldTrackSessionSnapshot = bindings.currentBottomPanelMode() == BottomEditorPanelMode.CELL_EDIT,
                allowReselectCurrentCell = bindings.isStructureEditMode(),
            )
        }
        if (result.wasBlocked) return
        if (bindings.isStructureEditMode()) {
            val selected = cellId?.let { setOf(it) } ?: emptySet()
            bindings.setStructureSelectedCellIds(selected)
            bindings.setStructureSelectionRange(
                TableSelectionResolver.rangeFromSelection(bindings.currentTemplate().cells, selected)
            )
        }
    }

    fun selectStructureRange(bindings: TableEditorSelectionActionBindings, startId: String, endId: String) {
        if (!bindings.isStructureEditMode()) return
        val result = TableSelectionResolver.selectByDrag(bindings.currentTemplate().cells, startId, endId)
        if (result.range != null) {
            bindings.setStructureSelectedCellIds(result.selectedCellIds)
            bindings.setStructureSelectionRange(result.range)
            bindings.setSelectedCellId(result.lastSelectedCellId)
        }
    }

    fun startCellInlineEditing(bindings: TableEditorInlineEditingActionBindings, cellId: String, value: String) {
        bindings.setInlineEdit(startInlineEditing(bindings.currentInlineEdit(), cellId, value))
    }

    fun openCellFormatDialog(bindings: TableEditorInlineEditingActionBindings, cellId: String, type: TableCellDataType) {
        bindings.setFormatDialog(bindings.currentFormatDialog().open(cellId, type))
    }

    fun applyInlineEditingValue(bindings: TableEditorInlineEditingActionBindings, nextValue: String) {
        bindings.runInlineAction { context ->
            TableEditorInlineEditActions.applyInlineValueChange(
                context = context,
                nextValue = nextValue,
            )
        }
    }

    fun tryCommitInlineAndContinue(bindings: TableEditorInlineEditingActionBindings): Boolean {
        val result = bindings.runInlineCommitAction()
        return !result.wasBlocked
    }

    fun dismissCellSettingsPanel(bindings: TableEditorInlineEditingActionBindings) {
        bindings.commitInlineEditIfNeeded()
        bindings.setShowCellSettingsPanel(false)
    }
}
