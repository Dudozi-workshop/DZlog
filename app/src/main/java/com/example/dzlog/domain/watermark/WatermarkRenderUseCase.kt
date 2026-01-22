package com.example.dzlog.watermark

import android.graphics.Bitmap
import com.example.dzlog.domain.model.CaptureRequest
import com.example.dzlog.domain.watermark.resolveCellsFromTableTemplate
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