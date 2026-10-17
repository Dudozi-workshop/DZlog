package com.dudoziworkshop.dzlog.ui.table.template

import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableStructureRangeActions
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator

/** Geometry only; the read-only preview never changes template data or selection. */
internal data class TemplatePreviewCellBounds(
    val cell: TableCellState,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)

internal fun templatePreviewCellBounds(state: TableTemplateState): List<TemplatePreviewCellBounds> {
    if (state.rows <= 0 || state.cols <= 0) return emptyList()
    val rows = TableLayoutCalculator.resolveWeights(state.rowWeights, state.rows)
    val cols = TableLayoutCalculator.resolveWeights(state.colWeights, state.cols)
    val totalHeight = rows.sum()
    val totalWidth = cols.sum()

    return TableStructureRangeActions.rootCells(state.cells).mapNotNull { cell ->
        val row = cell.rowIndex
        val col = cell.colIndex
        if (row !in rows.indices || col !in cols.indices) return@mapNotNull null
        val endRow = (row + cell.rowSpan.coerceAtLeast(1)).coerceAtMost(rows.size)
        val endCol = (col + cell.colSpan.coerceAtLeast(1)).coerceAtMost(cols.size)
        TemplatePreviewCellBounds(
            cell = cell,
            left = cols.take(col).sum() / totalWidth,
            top = rows.take(row).sum() / totalHeight,
            width = cols.subList(col, endCol).sum() / totalWidth,
            height = rows.subList(row, endRow).sum() / totalHeight,
        )
    }
}
