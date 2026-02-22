package com.example.dzlog.watermark

import android.graphics.Bitmap
import com.example.dzlog.domain.model.CaptureRequest

fun renderWatermarkForRequest(
    renderer: WatermarkRenderer,
    originalBmp: Bitmap,
    request: CaptureRequest
): Bitmap {
    val wm = request.watermark
    val cells = request.watermarkCells

    return renderer.renderTable(
        originalBmp = originalBmp,
        cells = cells,
        rows = request.tableTemplate.rows,
        cols = request.tableTemplate.cols,
        anchor = wm.anchor,
        offsetXRatio = wm.offsetXRatio,
        offsetYRatio = wm.offsetYRatio,
        tableHeightRatio = wm.tableHeightRatio,
        tableWidthRatio = wm.tableWidthRatio,
        bgAlpha = wm.tableBgAlpha,
        valueScale = wm.valueScale,
        textColorMode = wm.textColorMode,
        manualTextColor = wm.manualTextColor,
        textAlign = wm.textAlign,
        rowWeights = request.tableTemplate.rowWeights,
        colWeights = request.tableTemplate.colWeights,
        bgStyle = wm.bgStyle,
        drawGrid = wm.gridEnabled
    )
}
