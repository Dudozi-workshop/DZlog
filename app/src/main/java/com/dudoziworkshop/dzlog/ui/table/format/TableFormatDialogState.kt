package com.dudoziworkshop.dzlog.ui.table.format

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType

data class TableFormatDialogState(
    val isVisible: Boolean = false,
    val targetCellId: String? = null,
    val targetType: TableCellDataType? = null
)

fun TableFormatDialogState.open(cellId: String, type: TableCellDataType) =
    copy(isVisible = true, targetCellId = cellId, targetType = type)

fun closeTableFormatDialog() =
    TableFormatDialogState()
