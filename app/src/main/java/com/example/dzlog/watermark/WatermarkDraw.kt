package com.example.dzlog.watermark

import android.graphics.*
import com.example.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell
import com.example.dzlog.domain.model.WatermarkTableAnchor
import kotlin.math.abs

private fun resolveWeightsOrOnes(weights: List<Float>?, n: Int): List<Float> {
    if (n <= 0) return emptyList()
    if (weights == null || weights.size != n) return List(n) { 1f }
    return weights.map { it.coerceAtLeast(0f) }
}

private fun computeSizes(total: Float, weights: List<Float>): List<Float> {
    val n = weights.size.coerceAtLeast(1)
    val sum = weights.sum()
    if (abs(sum) < 1e-6f) {
        val each = total / n
        val sizes = MutableList(n) { each }
        val diff = total - sizes.sum()
        sizes[n - 1] = sizes[n - 1] + diff
        return sizes
    }
    val sizes = MutableList(n) { idx -> total * (weights[idx] / sum) }
    val diff = total - sizes.sum()
    sizes[n - 1] = sizes[n - 1] + diff
    return sizes
}

private fun computeOffsets(sizes: List<Float>): List<Float> {
    val offsets = ArrayList<Float>(sizes.size + 1)
    var acc = 0f
    offsets.add(0f)
    for (s in sizes) {
        acc += s
        offsets.add(acc)
    }
    return offsets
}

fun drawWatermarkTableFromResolvedCells(
    src: Bitmap,
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
    valueScale: Int,
    rowWeights: List<Float>? = null,
    colWeights: List<Float>? = null
): Bitmap {
    // ✅ 너가 준 함수 본문 그대로 붙여넣기
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(out)

    val w = out.width.toFloat()
    val h = out.height.toFloat()

    // ✅ 비율(3:4/9:16/1:1)에 상관없이 "표 크기"가 동일해야 하므로
    // 표 크기 계산 기준을 width(가로)로 통일한다.
    val base = w
    val tableW = base * (tableWidthRatio.coerceIn(40, 100) / 100f)
    val tableH = base * (tableHeightRatio.coerceIn(10, 35) / 100f)

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
        color = Color.argb(bgAlpha.coerceIn(0, 255), 0, 0, 0)
    }

    canvas.drawRect(left, top, left + tableW, top + tableH, bgPaint)

    val safeRows = rows.coerceAtLeast(1)
    val safeCols = cols.coerceAtLeast(1)

    val rW = resolveWeightsOrOnes(rowWeights, safeRows)
    val cW = resolveWeightsOrOnes(colWeights, safeCols)
    val rowHeights = computeSizes(tableH, rW)
    val colWidths = computeSizes(tableW, cW)
    val rowOffsets = computeOffsets(rowHeights)
    val colOffsets = computeOffsets(colWidths)

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = (tableH * 0.12f * (labelScale / 100f)).coerceAtLeast(14f)
    }

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textSize = (tableH * 0.16f * (valueScale / 100f)).coerceAtLeast(18f)
    }

    val pad = (tableH * 0.08f).coerceIn(8f, 20f)

    for (r in 0 until safeRows) {
        for (c in 0 until safeCols) {
            val idx = r * safeCols + c
            if (idx !in cells.indices) continue

            val cell = cells[idx]
            val cellH = rowHeights[r]
            val x = left + colOffsets[c]
            val y = top + rowOffsets[r]

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
    valueScale: Int,
    rowWeights: List<Float>? = null,
    colWeights: List<Float>? = null
) {
    val w = bounds.width()
    val h = bounds.height()

    // ✅ 비율에 따른 높이 변화에 영향을 받지 않도록
    // 표 크기 계산 기준을 width(가로)로 통일한다.
    val base = w
    val tableW = base * (tableWidthRatio.coerceIn(40, 100) / 100f)
    val tableH = base * (tableHeightRatio.coerceIn(10, 35) / 100f)

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
        color = Color.argb(bgAlpha.coerceIn(0, 255), 0, 0, 0)
    }

    canvas.drawRect(left, top, left + tableW, top + tableH, bgPaint)

    val safeRows = rows.coerceAtLeast(1)
    val safeCols = cols.coerceAtLeast(1)

    val rW = resolveWeightsOrOnes(rowWeights, safeRows)
    val cW = resolveWeightsOrOnes(colWeights, safeCols)
    val rowHeights = computeSizes(tableH, rW)
    val colWidths = computeSizes(tableW, cW)
    val rowOffsets = computeOffsets(rowHeights)
    val colOffsets = computeOffsets(colWidths)

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = (tableH * 0.12f * (labelScale / 100f)).coerceAtLeast(14f)
    }

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
        textSize = (tableH * 0.16f * (valueScale / 100f)).coerceAtLeast(18f)
    }

    val pad = (tableH * 0.08f).coerceIn(8f, 20f)

    for (r in 0 until safeRows) {
        for (c in 0 until safeCols) {
            val idx = r * safeCols + c
            if (idx !in cells.indices) continue

            val cell = cells[idx]
            val cellH = rowHeights[r]
            val x = left + colOffsets[c]
            val y = top + rowOffsets[r]

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