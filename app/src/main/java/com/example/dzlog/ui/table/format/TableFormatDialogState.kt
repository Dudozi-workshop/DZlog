package com.example.dzlog.ui.table.format

import com.example.dzlog.domain.model.TableCellDataType

data class TableFormatDialogState(
    val isVisible: Boolean = false,
    val targetCellId: String? = null,
    val targetType: TableCellDataType? = null
)

fun TableFormatDialogState.open(cellId: String, type: TableCellDataType) =
    copy(isVisible = true, targetCellId = cellId, targetType = type)

fun TableFormatDialogState.close() =
    TableFormatDialogState()
