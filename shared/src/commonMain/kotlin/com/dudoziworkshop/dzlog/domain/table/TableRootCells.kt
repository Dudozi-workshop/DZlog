package com.dudoziworkshop.dzlog.domain.table

import com.dudoziworkshop.dzlog.domain.model.TableCellState

/** Existing editor ordering/coverage rule, also used by read-only previews. */
fun resolveTableRootCells(cells: List<TableCellState>): List<TableCellState> {
    val covered = mutableSetOf<Pair<Int, Int>>()
    val roots = mutableListOf<TableCellState>()
    cells.sortedWith(compareBy<TableCellState> { it.rowIndex }.thenBy { it.colIndex }).forEach { raw ->
        val cell = raw.copy(rowSpan = raw.rowSpan.coerceAtLeast(1), colSpan = raw.colSpan.coerceAtLeast(1))
        val origin = cell.rowIndex to cell.colIndex
        if (origin in covered) return@forEach
        roots += cell
        for (row in cell.rowIndex until cell.rowIndex + cell.rowSpan) {
            for (col in cell.colIndex until cell.colIndex + cell.colSpan) {
                if ((row to col) != origin) covered += row to col
            }
        }
    }
    return roots
}
