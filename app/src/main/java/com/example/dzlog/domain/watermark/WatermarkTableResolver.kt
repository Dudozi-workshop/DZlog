package com.example.dzlog.domain.watermark

import com.example.dzlog.domain.model.ResolvedCell
import com.example.dzlog.domain.model.TableCellState

fun resolveCellsFromTableTemplate(
    cells: List<TableCellState>,
    rows: Int,
    cols: Int
): List<ResolvedCell> {
    return (0 until rows).flatMap { row ->
        (0 until cols).map { col ->
            val cell = cells.firstOrNull { it.rowIndex == row && it.colIndex == col }
            ResolvedCell(
                label = "", // ← 항상 비움
                valueText = cell?.valueText.orEmpty()
            )
        }
    }
}