package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.*

internal fun quickRootCells(template: TableTemplateState): List<TableCellState> = template.cells.filter { cell ->
    template.cells.none { other -> other.cellId != cell.cellId &&
        (other.rowSpan > 1 || other.colSpan > 1) &&
        cell.rowIndex in other.rowIndex until other.rowIndex + other.rowSpan &&
        cell.colIndex in other.colIndex until other.colIndex + other.colSpan }
}

internal fun quickEditableCells(template: TableTemplateState): List<TableCellState> =
    quickRootCells(template)
        .filter { it.dataType == TableCellDataType.TEXT || it.dataType == TableCellDataType.NUMBER }
        .sortedWith(compareBy({ it.rowIndex }, { it.colIndex }))

internal fun updateQuickCellValues(template: TableTemplateState, values: Map<String, String>): TableTemplateState =
    template.copy(cells = template.cells.map { cell ->
        val text = values[cell.cellId]
        when {
            text == null || text == cell.rawText -> cell
            cell.dataType == TableCellDataType.TEXT -> cell.copy(rawText = text, typedValue = CellValue.Text(text))
            cell.dataType == TableCellDataType.NUMBER -> cell.copy(rawText = text, typedValue = CellValue.Number(text))
            else -> cell
        }
    })

internal fun quickCellPosition(cell: TableCellState): String {
    fun range(start: Int, span: Int) = if (span <= 1) "${start + 1}" else "${start + 1}~${start + span}"
    return "${range(cell.rowIndex, cell.rowSpan)}행 ${range(cell.colIndex, cell.colSpan)}열"
}

/** Row-major navigation across selectable root cells, wrapping at both ends. */
internal fun adjacentQuickCellId(cells: List<TableCellState>, selectedId: String?, direction: Int): String? {
    if (direction != -1 && direction != 1 || cells.size < 2) return null
    val index = cells.indexOfFirst { it.cellId == selectedId }
    if (index < 0) return null
    val next = (index + direction + cells.size) % cells.size
    return cells[next].cellId
}
