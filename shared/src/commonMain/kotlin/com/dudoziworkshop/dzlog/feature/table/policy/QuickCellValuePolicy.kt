package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.table.resolveTableRootCells
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

fun quickRootCells(template: TableTemplateState): List<TableCellState> =
    resolveTableRootCells(template.cells)

/** Reading order used by the camera. A counter can be inspected, but never edited. */
fun quickNavigableCells(template: TableTemplateState): List<TableCellState> =
    quickRootCells(template)

fun quickEditableCells(template: TableTemplateState): List<TableCellState> =
    quickNavigableCells(template).filter {
        it.dataType == TableCellDataType.TEXT || it.dataType == TableCellDataType.NUMBER
    }

fun updateQuickCellValues(template: TableTemplateState, values: Map<String, String>): TableTemplateState =
    template.copy(cells = template.cells.map { cell ->
        val text = values[cell.cellId]
        when {
            text == null || text == cell.rawText -> cell
            cell.dataType == TableCellDataType.TEXT -> cell.copy(rawText = text, typedValue = CellValue.Text(text))
            cell.dataType == TableCellDataType.NUMBER -> cell.copy(rawText = text, typedValue = CellValue.Number(text))
            else -> cell
        }
    })

fun quickCellPosition(cell: TableCellState): String {
    fun range(start: Int, span: Int) = if (span <= 1) "${start + 1}" else "${start + 1}~${start + span}"
    return "${range(cell.rowIndex, cell.rowSpan)}행 ${range(cell.colIndex, cell.colSpan)}열"
}

/** Cycle through the supplied visible cells in both directions. */
fun adjacentQuickCellId(cells: List<TableCellState>, selectedId: String?, direction: Int): String? {
    if ((direction != -1 && direction != 1) || cells.size < 2) return null
    val index = cells.indexOfFirst { it.cellId == selectedId }
    if (index < 0) return null
    return cells[(index + direction + cells.size) % cells.size].cellId
}
