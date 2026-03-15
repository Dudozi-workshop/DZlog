package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.feature.table.model.TableEditMode

object TableEditOverlay {
    fun resolveTapAction(mode: TableEditMode, tappedCell: TableCellState?): TableEditTapAction {
        if (tappedCell == null) return TableEditTapAction.None
        return when (mode) {
            TableEditMode.Normal -> TableEditTapAction.OpenCellEditor(tappedCell.cellId)
            TableEditMode.Structure -> TableEditTapAction.SelectCell(tappedCell.cellId)
        }
    }
}

sealed interface TableEditTapAction {
    data class OpenCellEditor(val cellId: String) : TableEditTapAction
    data class SelectCell(val cellId: String) : TableEditTapAction
    data object None : TableEditTapAction
}
