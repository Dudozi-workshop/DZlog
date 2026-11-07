package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell
import com.dudoziworkshop.dzlog.feature.table.render.RenderRootCell

/**
 * Feed the active V2 editor's preview values to the same row-major renderer
 * used by the camera overlay and saved photograph.
 * Covered cells must not contribute a second visible label.
 */
internal fun editorRenderCells(
    cells: List<TableEditorCellUiModel>,
    rows: Int,
    cols: Int,
): List<WatermarkCell> {
    val safeRows = rows.coerceAtLeast(1)
    val safeCols = cols.coerceAtLeast(1)
    val byPosition = cells.associateBy { it.rowIndex to it.colIndex }
    return List(safeRows * safeCols) { index ->
        val cell = byPosition[index / safeCols to index % safeCols]
        WatermarkCell(
            if (cell == null || cell.isCovered) "" else cell.previewValue ?: cell.value
        )
    }
}

internal fun editorRenderRootCells(cells: List<TableEditorCellUiModel>): List<RenderRootCell> =
    cells.filterNot { it.isCovered }.mapNotNull { cell ->
        cell.domainCellId?.let { id ->
            RenderRootCell(
                cellId = id,
                rowIndex = cell.rowIndex,
                colIndex = cell.colIndex,
                rowSpan = cell.rowSpan,
                colSpan = cell.colSpan,
            )
        }
    }
