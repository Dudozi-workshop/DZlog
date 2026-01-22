package com.example.dzlog.watermark

import android.graphics.Bitmap
import com.example.dzlog.domain.model.CaptureRequest
import com.example.dzlog.domain.model.ResolvedCell
import com.example.dzlog.domain.model.TableCellState

fun renderWatermarkForRequest(
    renderer: WatermarkRenderer,
    originalBmp: Bitmap,
    request: CaptureRequest
): Bitmap {
    val wm = request.watermark
    val cells = resolveCellsFromTableTemplate(
        request.tableTemplate.cells,
        request.tableTemplate.rows,
        request.tableTemplate.cols
    )

    return renderer.renderTable(
        originalBmp = originalBmp,
        cells = cells,
        rows = request.tableTemplate.rows,
        cols = request.tableTemplate.cols,
        showLabel = wm.showLabel,
        anchor = wm.anchor,
        offsetXRatio = wm.offsetXRatio,
        offsetYRatio = wm.offsetYRatio,
        tableHeightRatio = wm.tableHeightRatio,
        tableWidthRatio = wm.tableWidthRatio,
        bgAlpha = wm.tableBgAlpha,
        labelScale = wm.labelScale,
        valueScale = wm.valueScale
    )
}

private fun resolveCellsFromTableTemplate(
    cells: List<TableCellState>,
    rows: Int,
    cols: Int
): List<ResolvedCell> {
    return (0 until rows).flatMap { row ->
        (0 until cols).map { col ->
            val cell = cells.firstOrNull { it.rowIndex == row && it.colIndex == col }
            ResolvedCell(
                label = cell?.label.orEmpty(),
                valueText = cell?.valueText.orEmpty()
            )
        }
    }
}