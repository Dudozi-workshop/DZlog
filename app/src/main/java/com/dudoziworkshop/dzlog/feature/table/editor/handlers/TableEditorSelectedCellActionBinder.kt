package com.dudoziworkshop.dzlog.feature.table.editor.handlers

import com.dudoziworkshop.dzlog.domain.model.CounterScopeMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.withDataType
import com.dudoziworkshop.dzlog.feature.table.editor.updateCell
import com.dudoziworkshop.dzlog.ui.table.PathGroupAction
import com.dudoziworkshop.dzlog.ui.table.applyPathGroupAction

data class TableEditorSelectedCellActionBindings(
    val currentTemplate: () -> TableTemplateState,
    val currentSelectedCell: () -> TableCellState?,
    val updateTemplateDraft: (TableTemplateState) -> Unit,
    val showCellSettingsPanel: (Boolean) -> Unit,
    val openRotatingPhraseTemplateDialog: (String) -> Unit,
)

object TableEditorSelectedCellActionBinder {

    fun setDataTypeForSelected(bindings: TableEditorSelectedCellActionBindings, type: TableCellDataType) {
        updateSelectedCellTemplate(bindings) { current ->
            current.withDataType(type)
        }
    }

    fun applyPathGroupActionForSelected(
        bindings: TableEditorSelectedCellActionBindings,
        action: PathGroupAction,
    ) {
        val cell = bindings.currentSelectedCell() ?: return
        val updated = applyPathGroupAction(
            state = bindings.currentTemplate(),
            targetCellId = cell.cellId,
            action = action,
        )
        bindings.updateTemplateDraft(updated)
    }

    fun setCounterScopeModeForSelected(
        bindings: TableEditorSelectedCellActionBindings,
        mode: CounterScopeMode,
    ) {
        val cell = bindings.currentSelectedCell() ?: return
        if (cell.dataType != TableCellDataType.DATE && cell.dataType != TableCellDataType.TIME) return
        updateSelectedCellTemplate(bindings) { current ->
            current.copy(counterScopeMode = mode)
        }
    }

    fun openRotatingTemplateDialogForSelected(
        bindings: TableEditorSelectedCellActionBindings,
        cellId: String,
    ) {
        val cell = bindings.currentTemplate().cells.firstOrNull { it.cellId == cellId }
        if (cell != null && cell.dataType == TableCellDataType.ROTATING_TEXT) {
            bindings.openRotatingPhraseTemplateDialog(cell.cellId)
        } else {
            bindings.showCellSettingsPanel(true)
        }
    }

    private fun updateSelectedCellTemplate(
        bindings: TableEditorSelectedCellActionBindings,
        transform: (TableCellState) -> TableCellState,
    ) {
        val cell = bindings.currentSelectedCell() ?: return
        bindings.updateTemplateDraft(
            updateCell(bindings.currentTemplate(), cell.cellId, transform)
        )
    }
}
