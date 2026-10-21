package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.TableCellState

data class TableSelectionRange(
    val minRow: Int,
    val maxRow: Int,
    val minCol: Int,
    val maxCol: Int,
) {
    fun contains(row: Int, col: Int): Boolean = row in minRow..maxRow && col in minCol..maxCol

    val rowCount: Int get() = (maxRow - minRow + 1).coerceAtLeast(0)
    val colCount: Int get() = (maxCol - minCol + 1).coerceAtLeast(0)
}

data class TableSelectionResult(
    val selectedCellIds: Set<String>,
    val range: TableSelectionRange?,
    val lastSelectedCellId: String?,
)

object TableSelectionResolver {

    fun selectByTap(
        cells: List<TableCellState>,
        current: TableSelectionResult,
        tappedCellId: String,
        additive: Boolean,
    ): TableSelectionResult {
        val tappedCell = cells.firstOrNull { it.cellId == tappedCellId }
            ?: return TableSelectionResult(emptySet(), null, null)
        if (!additive || current.range == null) {
            return selectionFromRange(cells, TableSelectionRange(tappedCell.rowIndex, tappedCell.rowIndex, tappedCell.colIndex, tappedCell.colIndex), tappedCellId)
        }

        val expanded = expandRange(current.range, tappedCell.rowIndex, tappedCell.colIndex)
        val nextIds = cellsInRange(cells, expanded).map { it.cellId }.toSet()
        if (!isContinuousRectangle(cells, nextIds)) {
            return selectionFromRange(
                cells,
                TableSelectionRange(tappedCell.rowIndex, tappedCell.rowIndex, tappedCell.colIndex, tappedCell.colIndex),
                tappedCellId,
            )
        }
        return TableSelectionResult(nextIds, expanded, tappedCellId)
    }

    fun selectByDrag(
        cells: List<TableCellState>,
        startCellId: String,
        endCellId: String,
    ): TableSelectionResult {
        val start = cells.firstOrNull { it.cellId == startCellId }
            ?: return TableSelectionResult(emptySet(), null, null)
        val end = cells.firstOrNull { it.cellId == endCellId }
            ?: return TableSelectionResult(emptySet(), null, null)

        val range = TableSelectionRange(
            minRow = minOf(start.rowIndex, end.rowIndex),
            maxRow = maxOf(start.rowIndex, end.rowIndex),
            minCol = minOf(start.colIndex, end.colIndex),
            maxCol = maxOf(start.colIndex, end.colIndex),
        )
        return selectionFromRange(cells, range, endCellId)
    }

    fun isContinuousRectangle(cells: List<TableCellState>, selectedCellIds: Set<String>): Boolean {
        if (selectedCellIds.isEmpty()) return true
        val selected = cells.filter { it.cellId in selectedCellIds }
        if (selected.size != selectedCellIds.size) return false
        val rows = selected.map { it.rowIndex }
        val cols = selected.map { it.colIndex }
        val range = TableSelectionRange(rows.min(), rows.max(), cols.min(), cols.max())
        val expectedCount = range.rowCount * range.colCount
        return selected.size == expectedCount
    }

    fun rangeFromSelection(cells: List<TableCellState>, selectedCellIds: Set<String>): TableSelectionRange? {
        if (selectedCellIds.isEmpty()) return null
        val selected = cells.filter { it.cellId in selectedCellIds }
        if (selected.isEmpty()) return null
        return TableSelectionRange(
            minRow = selected.minOf { it.rowIndex },
            maxRow = selected.maxOf { it.rowIndex },
            minCol = selected.minOf { it.colIndex },
            maxCol = selected.maxOf { it.colIndex },
        )
    }

    private fun selectionFromRange(
        cells: List<TableCellState>,
        range: TableSelectionRange,
        lastSelectedCellId: String,
    ): TableSelectionResult {
        val ids = cellsInRange(cells, range).map { it.cellId }.toSet()
        return TableSelectionResult(ids, range, lastSelectedCellId)
    }

    private fun cellsInRange(cells: List<TableCellState>, range: TableSelectionRange): List<TableCellState> {
        return cells.filter { cell -> range.contains(cell.rowIndex, cell.colIndex) }
    }

    private fun expandRange(range: TableSelectionRange, row: Int, col: Int): TableSelectionRange {
        return TableSelectionRange(
            minRow = minOf(range.minRow, row),
            maxRow = maxOf(range.maxRow, row),
            minCol = minOf(range.minCol, col),
            maxCol = maxOf(range.maxCol, col),
        )
    }
}
