package com.dudoziworkshop.dzlog.watermark

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.dudoziworkshop.dzlog.domain.model.WatermarkManualTextColor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextAlign
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextColorMode
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell
import kotlin.math.abs

private const val BG_STYLE_BLACK = 0
private const val BG_STYLE_WHITE = 1
private const val BG_STYLE_TRANSPARENT = 2

data class WatermarkTableLayout(
    val rect: RectF,
    val maxX: Float,
    val maxY: Float
)

fun computeWatermarkTableLayout(
    bounds: RectF,
    anchor: WatermarkTableAnchor,
    offsetXRatio: Int,
    offsetYRatio: Int,
    tableHeightRatio: Int,
    tableWidthRatio: Int
): WatermarkTableLayout {
    val w = bounds.width()
    val h = bounds.height()
    val base = w
    val tableW = base * (tableWidthRatio.coerceIn(40, 100) / 100f)
    val tableH = base * (tableHeightRatio.coerceIn(10, 200) / 100f)

    val maxX = (w - tableW).coerceAtLeast(0f)
    val maxY = (h - tableH).coerceAtLeast(0f)

    val left = bounds.left + when (anchor) {
        WatermarkTableAnchor.TOP_LEFT,
        WatermarkTableAnchor.BOTTOM_LEFT -> 0f
        WatermarkTableAnchor.TOP_RIGHT,
        WatermarkTableAnchor.BOTTOM_RIGHT -> maxX
        WatermarkTableAnchor.CUSTOM -> maxX * (offsetXRatio.coerceIn(0, 100) / 100f)
    }

    val top = bounds.top + when (anchor) {
        WatermarkTableAnchor.TOP_LEFT,
        WatermarkTableAnchor.TOP_RIGHT -> 0f
        WatermarkTableAnchor.BOTTOM_LEFT,
        WatermarkTableAnchor.BOTTOM_RIGHT -> maxY
        WatermarkTableAnchor.CUSTOM -> maxY * (offsetYRatio.coerceIn(0, 100) / 100f)
    }

    return WatermarkTableLayout(RectF(left, top, left + tableW, top + tableH), maxX, maxY)
}

fun computeWatermarkTableRect(
    bounds: RectF,
    anchor: WatermarkTableAnchor,
    offsetXRatio: Int,
    offsetYRatio: Int,
    tableHeightRatio: Int,
    tableWidthRatio: Int
): RectF {
    val layout = computeWatermarkTableLayout(
        bounds = bounds,
        anchor = anchor,
        offsetXRatio = offsetXRatio,
        offsetYRatio = offsetYRatio,
        tableHeightRatio = tableHeightRatio,
        tableWidthRatio = tableWidthRatio
    )
    return RectF(layout.rect)
}

fun computeWatermarkTableLayoutPx(
    bounds: RectF,
    anchor: WatermarkTableAnchor,
    offsetLeftPx: Float,
    offsetTopPx: Float,
    tableHeightRatio: Int,
    tableWidthRatio: Int
): WatermarkTableLayout {
    val w = bounds.width()
    val h = bounds.height()
    val base = w
    val tableW = base * (tableWidthRatio.coerceIn(40, 100) / 100f)
    val tableH = base * (tableHeightRatio.coerceIn(10, 200) / 100f)

    val maxX = (w - tableW).coerceAtLeast(0f)
    val maxY = (h - tableH).coerceAtLeast(0f)

    val left = bounds.left + when (anchor) {
        WatermarkTableAnchor.TOP_LEFT,
        WatermarkTableAnchor.BOTTOM_LEFT -> 0f
        WatermarkTableAnchor.TOP_RIGHT,
        WatermarkTableAnchor.BOTTOM_RIGHT -> maxX
        WatermarkTableAnchor.CUSTOM -> offsetLeftPx.coerceIn(0f, maxX)
    }

    val top = bounds.top + when (anchor) {
        WatermarkTableAnchor.TOP_LEFT,
        WatermarkTableAnchor.TOP_RIGHT -> 0f
        WatermarkTableAnchor.BOTTOM_LEFT,
        WatermarkTableAnchor.BOTTOM_RIGHT -> maxY
        WatermarkTableAnchor.CUSTOM -> offsetTopPx.coerceIn(0f, maxY)
    }

    return WatermarkTableLayout(RectF(left, top, left + tableW, top + tableH), maxX, maxY)
}

private fun drawBackgroundRect(
    canvas: Canvas,
    left: Float,
    top: Float,
    tableW: Float,
    tableH: Float,
    bgAlpha: Int,
    bgStyle: Int
) {
    if (bgStyle == BG_STYLE_TRANSPARENT) return
    val a = bgAlpha.coerceIn(0, 255)
    if (a <= 0) return

    val (r, g, b) = if (bgStyle == BG_STYLE_WHITE) Triple(255, 255, 255) else Triple(0, 0, 0)
    val bgPaint = Paint().apply { color = Color.argb(a, r, g, b) }
    canvas.drawRect(left, top, left + tableW, top + tableH, bgPaint)
}


private fun resolveGridColor(bgStyle: Int): Int = when (bgStyle) {
    BG_STYLE_WHITE, BG_STYLE_TRANSPARENT -> Color.argb(110, 0, 0, 0)
    else -> Color.argb(110, 255, 255, 255)
}

private fun drawGridLines(
    canvas: Canvas,
    left: Float,
    top: Float,
    tableW: Float,
    tableH: Float,
    rowOffsets: List<Float>,
    colOffsets: List<Float>,
    bgStyle: Int
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resolveGridColor(bgStyle)
        strokeWidth = 1.2f
    }

    for (xOffset in colOffsets) {
        val x = left + xOffset
        canvas.drawLine(x, top, x, top + tableH, paint)
    }
    for (yOffset in rowOffsets) {
        val y = top + yOffset
        canvas.drawLine(left, y, left + tableW, y, paint)
    }
}

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


private fun resolveValueTextColor(bgStyle: Int, textColorMode: Int, manualTextColor: Int): Int {
    return if (textColorMode == WatermarkTextColorMode.MANUAL) {
        if (manualTextColor == WatermarkManualTextColor.WHITE) Color.WHITE else Color.BLACK
    } else {
        when (bgStyle) {
            BG_STYLE_WHITE -> Color.BLACK
            BG_STYLE_BLACK -> Color.WHITE
            BG_STYLE_TRANSPARENT -> Color.BLACK
            else -> Color.BLACK
        }
    }
}

private fun resolveTextDrawX(
    cellLeft: Float,
    cellWidth: Float,
    pad: Float,
    text: String,
    paint: Paint,
    textAlign: Int
): Float {
    val textWidth = paint.measureText(text)
    val leftTextX = cellLeft + pad
    val rightTextX = cellLeft + cellWidth - pad
    val centerTextX = cellLeft + (cellWidth / 2f)
    return when (textAlign) {
        WatermarkTextAlign.CENTER -> centerTextX - (textWidth / 2f)
        WatermarkTextAlign.RIGHT -> rightTextX - textWidth
        else -> leftTextX
    }
}


private fun ellipsizeToWidth(text: String, paint: Paint, maxWidthPx: Float): String {
    if (text.isEmpty()) return text
    if (maxWidthPx <= 0f) return ""
    if (paint.measureText(text) <= maxWidthPx) return text

    val ellipsis = "…"
    val ellipsisWidth = paint.measureText(ellipsis)
    if (ellipsisWidth > maxWidthPx) return ellipsis

    var end = text.length
    while (end > 0) {
        val candidate = text.substring(0, end) + ellipsis
        if (paint.measureText(candidate) <= maxWidthPx) return candidate
        end--
    }
    return ellipsis
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
    anchor: WatermarkTableAnchor,
    offsetXRatio: Int,
    offsetYRatio: Int,
    tableHeightRatio: Int,
    tableWidthRatio: Int,
    bgAlpha: Int,
    valueScale: Int,
    textColorMode: Int = WatermarkTextColorMode.AUTO,
    manualTextColor: Int = WatermarkManualTextColor.BLACK,
    textAlign: Int = WatermarkTextAlign.LEFT,
    rowWeights: List<Float>? = null,
    colWeights: List<Float>? = null,
    bgStyle: Int = BG_STYLE_BLACK,
    drawGrid: Boolean = true
): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(out)

    val w = out.width.toFloat()
    val h = out.height.toFloat()
    val base = w
    val tableW = base * (tableWidthRatio.coerceIn(40, 100) / 100f)
    val tableH = base * (tableHeightRatio.coerceIn(10, 200) / 100f)

    val maxX = (w - tableW).coerceAtLeast(0f)
    val maxY = (h - tableH).coerceAtLeast(0f)

    val left = when (anchor) {
        WatermarkTableAnchor.TOP_LEFT,
        WatermarkTableAnchor.BOTTOM_LEFT -> 0f
        WatermarkTableAnchor.TOP_RIGHT,
        WatermarkTableAnchor.BOTTOM_RIGHT -> maxX
        WatermarkTableAnchor.CUSTOM -> maxX * (offsetXRatio.coerceIn(0, 100) / 100f)
    }

    val top = when (anchor) {
        WatermarkTableAnchor.TOP_LEFT,
        WatermarkTableAnchor.TOP_RIGHT -> 0f
        WatermarkTableAnchor.BOTTOM_LEFT,
        WatermarkTableAnchor.BOTTOM_RIGHT -> maxY
        WatermarkTableAnchor.CUSTOM -> maxY * (offsetYRatio.coerceIn(0, 100) / 100f)
    }

    drawBackgroundRect(canvas, left, top, tableW, tableH, bgAlpha, bgStyle)

    val safeRows = rows.coerceAtLeast(1)
    val safeCols = cols.coerceAtLeast(1)

    val rowHeights = computeSizes(tableH, resolveWeightsOrOnes(rowWeights, safeRows))
    val colWidths = computeSizes(tableW, resolveWeightsOrOnes(colWeights, safeCols))
    val rowOffsets = computeOffsets(rowHeights)
    val colOffsets = computeOffsets(colWidths)

    if (drawGrid) {
        drawGridLines(canvas, left, top, tableW, tableH, rowOffsets, colOffsets, bgStyle)
    }

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resolveValueTextColor(bgStyle, textColorMode, manualTextColor)
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
            val cellW = colWidths[c]
            val x = left + colOffsets[c]
            val y = top + rowOffsets[r]
            val cellRect = RectF(x, y, x + cellW, y + cellH)

            val fm = valuePaint.fontMetrics
            val centerY = y + cellH / 2f - (fm.ascent + fm.descent) / 2
            val availableWidth = (cellRect.width() - (pad * 2f)).coerceAtLeast(0f)
            val drawText = ellipsizeToWidth(cell.valueText, valuePaint, availableWidth)
            val drawX = resolveTextDrawX(x, cellW, pad, drawText, valuePaint, textAlign)

            canvas.save()
            canvas.clipRect(cellRect)
            canvas.drawText(drawText, drawX, centerY, valuePaint)
            canvas.restore()
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
    anchor: WatermarkTableAnchor,
    offsetXRatio: Int,
    offsetYRatio: Int,
    tableHeightRatio: Int,
    tableWidthRatio: Int,
    bgAlpha: Int,
    valueScale: Int,
    textColorMode: Int = WatermarkTextColorMode.AUTO,
    manualTextColor: Int = WatermarkManualTextColor.BLACK,
    textAlign: Int = WatermarkTextAlign.LEFT,
    rowWeights: List<Float>? = null,
    colWeights: List<Float>? = null,
    bgStyle: Int = BG_STYLE_BLACK,
    overrideOffsetLeftPx: Float? = null,
    overrideOffsetTopPx: Float? = null,
    drawGrid: Boolean = true
) {
    val layout = if (
        anchor == WatermarkTableAnchor.CUSTOM &&
        overrideOffsetLeftPx != null &&
        overrideOffsetTopPx != null
    ) {
        computeWatermarkTableLayoutPx(
            bounds = bounds,
            anchor = anchor,
            offsetLeftPx = overrideOffsetLeftPx,
            offsetTopPx = overrideOffsetTopPx,
            tableHeightRatio = tableHeightRatio,
            tableWidthRatio = tableWidthRatio
        )
    } else {
        computeWatermarkTableLayout(
            bounds = bounds,
            anchor = anchor,
            offsetXRatio = offsetXRatio,
            offsetYRatio = offsetYRatio,
            tableHeightRatio = tableHeightRatio,
            tableWidthRatio = tableWidthRatio
        )
    }

    val tableW = layout.rect.width()
    val tableH = layout.rect.height()
    val left = layout.rect.left
    val top = layout.rect.top

    drawBackgroundRect(canvas, left, top, tableW, tableH, bgAlpha, bgStyle)

    val safeRows = rows.coerceAtLeast(1)
    val safeCols = cols.coerceAtLeast(1)

    val rowHeights = computeSizes(tableH, resolveWeightsOrOnes(rowWeights, safeRows))
    val colWidths = computeSizes(tableW, resolveWeightsOrOnes(colWeights, safeCols))
    val rowOffsets = computeOffsets(rowHeights)
    val colOffsets = computeOffsets(colWidths)

    if (drawGrid) {
        drawGridLines(canvas, left, top, tableW, tableH, rowOffsets, colOffsets, bgStyle)
    }

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resolveValueTextColor(bgStyle, textColorMode, manualTextColor)
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
            val cellW = colWidths[c]
            val x = left + colOffsets[c]
            val y = top + rowOffsets[r]
            val cellRect = RectF(x, y, x + cellW, y + cellH)

            val fm = valuePaint.fontMetrics
            val centerY = y + cellH / 2f - (fm.ascent + fm.descent) / 2
            val availableWidth = (cellRect.width() - (pad * 2f)).coerceAtLeast(0f)
            val drawText = ellipsizeToWidth(cell.valueText, valuePaint, availableWidth)
            val drawX = resolveTextDrawX(x, cellW, pad, drawText, valuePaint, textAlign)

            canvas.save()
            canvas.clipRect(cellRect)
            canvas.drawText(drawText, drawX, centerY, valuePaint)
            canvas.restore()
        }
    }
}
