package com.dudoziworkshop.dzlog.feature.table.editor.coordinator

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.counter.core.CounterFacade
import com.dudoziworkshop.dzlog.feature.counter.table.TableCounterUiState
import com.dudoziworkshop.dzlog.feature.counter.table.restoreCounterCellToAutoNext
import com.dudoziworkshop.dzlog.ui.table.editor.InlineEditState
import kotlinx.coroutines.CoroutineScope

internal data class TableEditorSelectedCounterResetInput(
    val selectedCell: TableCellState?,
    val templateState: TableTemplateState,
    val counterRequest: com.dudoziworkshop.dzlog.feature.counter.core.CounterRequest,
    val counterFacade: CounterFacade,
    val counterUi: TableCounterUiState,
    val inlineEdit: InlineEditState,
    val onTemplateChange: (TableTemplateState) -> Unit,
    val setCounterUi: (TableCounterUiState) -> Unit,
    val updateCell: (TableTemplateState, String, (TableCellState) -> TableCellState) -> TableTemplateState,
    val scope: CoroutineScope,
)

internal data class TableEditorSelectedCounterResetResult(
    val nextInlineEdit: InlineEditState,
    val toastMessage: String?,
)

internal object TableEditorSelectedCounterResetCoordinator {

    internal fun resetToAutoNext(input: TableEditorSelectedCounterResetInput): TableEditorSelectedCounterResetResult {
        val cell = input.selectedCell
            ?: return TableEditorSelectedCounterResetResult(
                nextInlineEdit = input.inlineEdit,
                toastMessage = null,
            )
        if (cell.dataType != TableCellDataType.COUNTER) {
            return TableEditorSelectedCounterResetResult(
                nextInlineEdit = input.inlineEdit,
                toastMessage = null,
            )
        }

        val syncedCounterText = input.counterUi.autoNextCounterValue.toString()
        restoreCounterCellToAutoNext(
            templateState = input.templateState,
            cellId = cell.cellId,
            counterRequest = input.counterRequest,
            counterFacade = input.counterFacade,
            counterUi = input.counterUi,
            onTemplateChange = input.onTemplateChange,
            setCounterUi = input.setCounterUi,
            updateCell = input.updateCell,
            scope = input.scope,
        )
        val nextInlineEdit = if (input.inlineEdit.editingCellId == cell.cellId) {
            input.inlineEdit.copy(editingValue = syncedCounterText)
        } else {
            input.inlineEdit
        }
        return TableEditorSelectedCounterResetResult(
            nextInlineEdit = nextInlineEdit,
            toastMessage = "카운터를 자동 기준으로 초기화했습니다.",
        )
    }
}
