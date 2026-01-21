package com.example.dzlog.watermark

import android.graphics.Bitmap
import com.example.dzlog.domain.model.CaptureRequest
import com.example.dzlog.domain.watermark.resolveCellsFromTemplate
import com.example.dzlog.domain.watermark.templateForPreset
import java.util.Date

fun renderWatermarkForRequest(
    renderer: WatermarkRenderer,
    originalBmp: Bitmap,
    request: CaptureRequest,
    capturedAt: Date
): Bitmap {
    val wm = request.watermark
    val counterText = request.counter.toString().padStart(request.counterDigits, '0')
    val template = templateForPreset(
        preset = wm.templatePreset,
        grid = wm.gridPreset,
        memo1 = wm.memo1,
        memo2 = wm.memo2,
        memo3 = wm.memo3
    )

    val cells = resolveCellsFromTemplate(
        template = template,
        treatment = wm.treatment,
        strain = wm.strain,
        folder2Text = wm.folder2Text,
        counterText = counterText,
        capturedAt = capturedAt,
        showDate = wm.showDate,
        showTime = wm.showTime,
        datePattern = wm.datePattern,
        timePattern = wm.timePattern,
        emptyPolicy = wm.emptyPolicy,
        emptyCustomText = wm.emptyCustomText
    )

    return renderer.renderTable(
        originalBmp = originalBmp,
        cells = cells,
        rows = wm.gridPreset.rows,
        cols = wm.gridPreset.cols,
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