package com.example.dzlog.watermark

import android.graphics.*
import com.example.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell
import com.example.dzlog.domain.model.WatermarkTableAnchor


fun drawWatermarkTableFromResolvedCells(
    src: Bitmap,
    cells: List<WatermarkCell>,
    rows: Int,
    cols: Int,
    showLabel: Boolean,
    anchor: com.example.dzlog.domain.model.WatermarkTableAnchor,
    offsetXRatio: Int,
    offsetYRatio: Int,
    tableHeightRatio: Int,
    tableWidthRatio: Int,
    bgAlpha: Int,
    labelScale: Int,
    valueScale: Int
): Bitmap {
    // ✅ 너가 준 함수 본문 그대로 붙여넣기
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(out)

    val w = out.width.toFloat()
    val h = out.height.toFloat()

    val tableW = w * (tableWidthRatio.coerceIn(40, 100) / 100f)
    val tableH = h * (tableHeightRatio.coerceIn(10, 35) / 100f)

    val maxX = (w - tableW).coerceAtLeast(0f)
    val maxY = (h - tableH).coerceAtLeast(0f)

    val left = when (anchor) {
        WatermarkTableAnchor.TOP_LEFT,
        WatermarkTableAnchor.BOTTOM_LEFT -> 0f

        WatermarkTableAnchor.TOP_RIGHT,
        WatermarkTableAnchor.BOTTOM_RIGHT -> maxX

        WatermarkTableAnchor.CUSTOM ->
            maxX * (offsetXRatio.coerceIn(0, 100) / 100f)
    }

    val top = when (anchor) {
        WatermarkTableAnchor.TOP_LEFT,
        WatermarkTableAnchor.TOP_RIGHT -> 0f

        WatermarkTableAnchor.BOTTOM_LEFT,
        WatermarkTableAnchor.BOTTOM_RIGHT -> maxY

        WatermarkTableAnchor.CUSTOM ->
            maxY * (offsetYRatio.coerceIn(0, 100) / 100f)
    }

    val bgPaint = Paint().apply {
        color = android.graphics.Color.argb(bgAlpha.coerceIn(0, 255), 0, 0, 0)
    }

    canvas.drawRect(left, top, left + tableW, top + tableH, bgPaint)

    val cellW = tableW / cols.coerceAtLeast(1)
    val cellH = tableH / rows.coerceAtLeast(1)

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.LTGRAY
        textSize = (tableH * 0.12f * (labelScale / 100f)).coerceAtLeast(14f)
    }

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textSize = (tableH * 0.16f * (valueScale / 100f)).coerceAtLeast(18f)
    }

    val pad = (tableH * 0.08f).coerceIn(8f, 20f)

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val idx = r * cols + c
            if (idx !in cells.indices) continue

            val cell = cells[idx]
            val x = left + c * cellW
            val y = top + r * cellH

            if (showLabel) {
                canvas.drawText(
                    cell.label,
                    x + pad,
                    y + pad + labelPaint.textSize,
                    labelPaint
                )
                canvas.drawText(
                    cell.valueText,
                    x + pad,
                    y + cellH - pad,
                    valuePaint
                )
            } else {
                val fm = valuePaint.fontMetrics
                val centerY = y + cellH / 2f - (fm.ascent + fm.descent) / 2
                canvas.drawText(cell.valueText, x + pad, centerY, valuePaint)
            }
        }
    }

    return out
}

fun drawWatermarkTableOnCanvas(
    canvas: Canvas,
    bounds: RectF,
    cells: List<WatermarkCell>,
    rows: Int,
    cols: Int,
    showLabel: Boolean,
    anchor: WatermarkTableAnchor,
    offsetXRatio: Int,
    offsetYRatio: Int,
    tableHeightRatio: Int,
    tableWidthRatio: Int,
    bgAlpha: Int,
    labelScale: Int,
    valueScale: Int
) {
    val w = bounds.width()
    val h = bounds.height()

    val tableW = w * (tableWidthRatio.coerceIn(40, 100) / 100f)
    val tableH = h * (tableHeightRatio.coerceIn(10, 35) / 100f)

    val maxX = (w - tableW).coerceAtLeast(0f)
    val maxY = (h - tableH).coerceAtLeast(0f)

    val left = bounds.left + when (anchor) {
        WatermarkTableAnchor.TOP_LEFT,
        WatermarkTableAnchor.BOTTOM_LEFT -> 0f
        WatermarkTableAnchor.TOP_RIGHT,
        WatermarkTableAnchor.BOTTOM_RIGHT -> maxX
        WatermarkTableAnchor.CUSTOM ->
            maxX * (offsetXRatio.coerceIn(0, 100) / 100f)
    }

    val top = bounds.top + when (anchor) {
        WatermarkTableAnchor.TOP_LEFT,
        WatermarkTableAnchor.TOP_RIGHT -> 0f
        WatermarkTableAnchor.BOTTOM_LEFT,
        WatermarkTableAnchor.BOTTOM_RIGHT -> maxY
        WatermarkTableAnchor.CUSTOM ->
            maxY * (offsetYRatio.coerceIn(0, 100) / 100f)
    }

    val bgPaint = Paint().apply {
        color = android.graphics.Color.argb(bgAlpha.coerceIn(0, 255), 0, 0, 0)
    }

    canvas.drawRect(left, top, left + tableW, top + tableH, bgPaint)

    val cellW = tableW / cols.coerceAtLeast(1)
    val cellH = tableH / rows.coerceAtLeast(1)

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.LTGRAY
        textSize = (tableH * 0.12f * (labelScale / 100f)).coerceAtLeast(14f)
    }

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textSize = (tableH * 0.16f * (valueScale / 100f)).coerceAtLeast(18f)
    }

    val pad = (tableH * 0.08f).coerceIn(8f, 20f)

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val idx = r * cols + c
            if (idx !in cells.indices) continue

            val cell = cells[idx]
            val x = left + c * cellW
            val y = top + r * cellH

            if (showLabel) {
                canvas.drawText(
                    cell.label,
                    x + pad,
                    y + pad + labelPaint.textSize,
                    labelPaint
                )
                canvas.drawText(
                    cell.valueText,
                    x + pad,
                    y + cellH - pad,
                    valuePaint
                )
            } else {
                val fm = valuePaint.fontMetrics
                val centerY = y + cellH / 2f - (fm.ascent + fm.descent) / 2
                canvas.drawText(cell.valueText, x + pad, centerY, valuePaint)
            }
        }
    }
}