package com.dudoziworkshop.dzlog.watermark

import android.graphics.Bitmap
import com.dudoziworkshop.dzlog.domain.model.CaptureRequest

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
        templateCells = request.tableTemplate.cells,
        rows = request.tableTemplate.rows,
        cols = request.tableTemplate.cols,
        anchor = wm.anchor,
        offsetXRatio = wm.offsetXRatio,
        offsetYRatio = wm.offsetYRatio,
        boundsOffsetX10000 = wm.boundsOffsetX10000,
        boundsOffsetY10000 = wm.boundsOffsetY10000,
        tableHeightRatio = wm.tableHeightRatio,
        tableWidthRatio = wm.tableWidthRatio,
        bgAlpha = wm.tableBgAlpha,
        valueScale = wm.valueScale,
        textColorMode = wm.textColorMode,
        manualTextColor = wm.manualTextColor,
        textAlign = wm.textAlign,
        bgStyle = wm.bgStyle,
        drawGrid = wm.gridEnabled,
        rotationCwDeg = wm.rotationCwDeg
    )
}
