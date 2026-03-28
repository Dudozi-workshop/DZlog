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
private const val TEXT_BASE_RATIO = 0.36f
private const val TEXT_CELL_SAFE_RATIO = 0.80f

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
    val tableW = base * (tableWidthRatio.coerceIn(10, 100) / 100f)
    val tableH = base * (tableHeightRatio.coerceIn(10, 100) / 100f)

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
    val tableW = base * (tableWidthRatio.coerceIn(10, 100) / 100f)
    val tableH = base * (tableHeightRatio.coerceIn(10, 100) / 100f)

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


fun computeWatermarkTableLayoutPxClampedForRotation(
    bounds: RectF,
    anchor: WatermarkTableAnchor,
    offsetLeftPx: Float,
    offsetTopPx: Float,
    tableHeightRatio: Int,
    tableWidthRatio: Int,
    rotationCwDeg: Int
): WatermarkTableLayout {
    if (anchor != WatermarkTableAnchor.CUSTOM) {
        return computeWatermarkTableLayoutPx(
            bounds = bounds,
            anchor = anchor,
            offsetLeftPx = offsetLeftPx,
            offsetTopPx = offsetTopPx,
            tableHeightRatio = tableHeightRatio,
            tableWidthRatio = tableWidthRatio
        )
    }

    val baseW = bounds.width()
    val rawW = baseW * (tableWidthRatio.coerceIn(10, 100) / 100f)
    val rawH = baseW * (tableHeightRatio.coerceIn(10, 100) / 100f)

    val cx = bounds.left + offsetLeftPx + rawW / 2f
    val cy = bounds.top + offsetTopPx + rawH / 2f

    val normalized = ((rotationCwDeg % 360) + 360) % 360
    val halfW = if (normalized == 90) rawH / 2f else rawW / 2f
    val halfH = if (normalized == 90) rawW / 2f else rawH / 2f

    val minCx = bounds.left + halfW
    val maxCx = bounds.right - halfW
    val minCy = bounds.top + halfH
    val maxCy = bounds.bottom - halfH

    val cxClamped = if (minCx <= maxCx) cx.coerceIn(minCx, maxCx) else bounds.centerX()
    val cyClamped = if (minCy <= maxCy) cy.coerceIn(minCy, maxCy) else bounds.centerY()

    val left = cxClamped - rawW / 2f
    val top = cyClamped - rawH / 2f

    val effW = if (normalized == 90) rawH else rawW
    val effH = if (normalized == 90) rawW else rawH
    val maxX = (bounds.width() - effW).coerceAtLeast(0f)
    val maxY = (bounds.height() - effH).coerceAtLeast(0f)

    return WatermarkTableLayout(
        rect = RectF(left, top, left + rawW, top + rawH),
        maxX = maxX,
        maxY = maxY
    )
}

fun computeBoundsSize(rawW: Float, rawH: Float, rotationCwDeg: Int): Pair<Float, Float> {
    val normalized = ((rotationCwDeg % 360) + 360) % 360
    return if (normalized == 90) rawH to rawW else rawW to rawH
}

fun rawRectFromBounds(boundsRect: RectF, rawW: Float, rawH: Float): RectF {
    val cx = boundsRect.centerX()
    val cy = boundsRect.centerY()
    return RectF(cx - rawW / 2f, cy - rawH / 2f, cx + rawW / 2f, cy + rawH / 2f)
}

fun boundsRectFromOffset(
    captureRect: RectF,
    boundsW: Float,
    boundsH: Float,
    boundsLeftPx: Float,
    boundsTopPx: Float
): RectF {
    val maxX = (captureRect.width() - boundsW).coerceAtLeast(0f)
    val maxY = (captureRect.height() - boundsH).coerceAtLeast(0f)
    val left = boundsLeftPx.coerceIn(0f, maxX)
    val top = boundsTopPx.coerceIn(0f, maxY)
    return RectF(
        captureRect.left + left,
        captureRect.top + top,
        captureRect.left + left + boundsW,
        captureRect.top + top + boundsH
    )
}

fun computeWatermarkBoundsRect(rawRect: RectF, rotationCwDeg: Int): RectF {
    val normalized = ((rotationCwDeg % 360) + 360) % 360
    if (normalized != 90) return RectF(rawRect)

    val cx = rawRect.centerX()
    val cy = rawRect.centerY()
    val w = rawRect.width()
    val h = rawRect.height()
    val newW = h
    val newH = w
    return RectF(cx - newW / 2f, cy - newH / 2f, cx + newW / 2f, cy + newH / 2f)
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


/**
 * 표 전체 공통 텍스트 기준 크기 계산.
 * - 기준 셀: tableWidth/cols, tableHeight/rows의 균등 분할 셀
 * - row/col weights는 여기서 사용하지 않는다.
 */
internal fun computeBaseTextSizeFromRenderedTable(
    tableWidth: Float,
    tableHeight: Float,
    rows: Int,
    cols: Int,
): Float {
    val safeRows = rows.coerceAtLeast(1)
    val safeCols = cols.coerceAtLeast(1)
    val baseCellWidth = tableWidth / safeCols
    val baseCellHeight = tableHeight / safeRows
    val baseCellShortSide = minOf(baseCellWidth, baseCellHeight).coerceAtLeast(0f)
    return (baseCellShortSide * TEXT_BASE_RATIO).coerceAtLeast(1f)
}

internal fun applyValueScaleFactor(baseTextSize: Float, valueScale: Int): Float {
    val scaleFactor = valueScale / 100f
    return (baseTextSize * scaleFactor).coerceAtLeast(1f)
}

/**
 * 실제 셀 안전 상한(cap)만 적용한다.
 * - 셀별 기본 텍스트 크기 재계산은 하지 않는다.
 */
internal fun applyCellSafeTextCap(
    scaledTextSize: Float,
    actualCellWidth: Float,
    actualCellHeight: Float,
): Float {
    val actualCellShortSide = minOf(actualCellWidth, actualCellHeight).coerceAtLeast(0f)
    val cellMaxTextSize = (actualCellShortSide * TEXT_CELL_SAFE_RATIO).coerceAtLeast(1f)
    return minOf(scaledTextSize, cellMaxTextSize)
}


private fun resolveCellTextPadding(tableHeight: Float): Float =
    (tableHeight * 0.08f).coerceIn(8f, 20f)

private fun drawCellValueText(
    canvas: Canvas,
    paint: Paint,
    cellRect: RectF,
    cellText: String,
    textAlign: Int,
    commonScaledTextSize: Float,
    cellTextPadding: Float,
    isPlaceholder: Boolean = false,
    placeholderTextColorArgb: Int? = null,
) {
    paint.textSize = applyCellSafeTextCap(
        scaledTextSize = commonScaledTextSize,
        actualCellWidth = cellRect.width(),
        actualCellHeight = cellRect.height(),
    )

    val originalColor = paint.color
    if (isPlaceholder) {
        paint.color = placeholderTextColorArgb ?: Color.argb(160, Color.red(originalColor), Color.green(originalColor), Color.blue(originalColor))
    }

    val fm = paint.fontMetrics
    val centerY = cellRect.top + cellRect.height() / 2f - (fm.ascent + fm.descent) / 2
    val availableWidth = (cellRect.width() - (cellTextPadding * 2f)).coerceAtLeast(0f)
    val drawText = ellipsizeToWidth(cellText, paint, availableWidth)
    val drawX = resolveTextDrawX(
        cellLeft = cellRect.left,
        cellWidth = cellRect.width(),
        pad = cellTextPadding,
        text = drawText,
        paint = paint,
        textAlign = textAlign,
    )

    canvas.save()
    canvas.clipRect(cellRect)
    canvas.drawText(drawText, drawX, centerY, paint)
    canvas.restore()
    paint.color = originalColor
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
        val candidate = text.take(end) + ellipsis
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
    boundsOffsetX10000: Int = 0,
    boundsOffsetY10000: Int = 0,
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
    drawGrid: Boolean = true,
    rotationCwDeg: Int = 0,
    placeholderCellIndexes: Set<Int> = emptySet(),
    placeholderTextColorArgb: Int? = null,
): Bitmap {
    val out = src.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(out)

    val w = out.width.toFloat()
    val h = out.height.toFloat()
    val imageBounds = RectF(0f, 0f, w, h)
    val layout = computeWatermarkTableLayout(
        bounds = imageBounds,
        anchor = anchor,
        offsetXRatio = offsetXRatio,
        offsetYRatio = offsetYRatio,
        tableHeightRatio = tableHeightRatio,
        tableWidthRatio = tableWidthRatio
    )
    val rawRect = if (anchor == WatermarkTableAnchor.CUSTOM) {
        val baseW = imageBounds.width()
        val rawW = baseW * (tableWidthRatio.coerceIn(10, 100) / 100f)
        val rawH = baseW * (tableHeightRatio.coerceIn(10, 100) / 100f)
        val (boundsW, boundsH) = computeBoundsSize(rawW, rawH, rotationCwDeg)
        val boundsMaxX = (imageBounds.width() - boundsW).coerceAtLeast(0f)
        val boundsMaxY = (imageBounds.height() - boundsH).coerceAtLeast(0f)
        val boundsLeftPx = boundsMaxX * (boundsOffsetX10000.coerceIn(0, 10000) / 10000f)
        val boundsTopPx = boundsMaxY * (boundsOffsetY10000.coerceIn(0, 10000) / 10000f)
        // UI/저장 모두 boundsOffset10000 기반으로 boundsRect를 만든 뒤 rawRect로 역산하여 동일 위치를 보장한다.
        val boundsRect = boundsRectFromOffset(
            captureRect = imageBounds,
            boundsW = boundsW,
            boundsH = boundsH,
            boundsLeftPx = boundsLeftPx,
            boundsTopPx = boundsTopPx
        )
        rawRectFromBounds(boundsRect, rawW, rawH)
    } else {
        layout.rect
    }
    val tableW = rawRect.width()
    val tableH = rawRect.height()
    val left = rawRect.left
    val top = rawRect.top

    val shouldRotate = (rotationCwDeg % 360 + 360) % 360 == 90
    if (shouldRotate) {
        canvas.save()
        canvas.rotate(90f, left + tableW / 2f, top + tableH / 2f)
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

    val commonScaledTextSize = applyValueScaleFactor(
        baseTextSize = computeBaseTextSizeFromRenderedTable(
            tableWidth = tableW,
            tableHeight = tableH,
            rows = safeRows,
            cols = safeCols,
        ),
        valueScale = valueScale,
    )

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resolveValueTextColor(bgStyle, textColorMode, manualTextColor)
        typeface = Typeface.DEFAULT_BOLD
        textSize = commonScaledTextSize
    }

    val cellTextPadding = resolveCellTextPadding(tableH)

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

            drawCellValueText(
                canvas = canvas,
                paint = valuePaint,
                cellRect = cellRect,
                cellText = cell.valueText,
                textAlign = textAlign,
                commonScaledTextSize = commonScaledTextSize,
                cellTextPadding = cellTextPadding,
                isPlaceholder = idx in placeholderCellIndexes,
                placeholderTextColorArgb = placeholderTextColorArgb,
            )
        }
    }

    if (shouldRotate) canvas.restore()

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
    drawGrid: Boolean = true,
    rotationCwDeg: Int = 0,
    placeholderCellIndexes: Set<Int> = emptySet(),
    placeholderTextColorArgb: Int? = null,
) {
    val layout = if (
        anchor == WatermarkTableAnchor.CUSTOM &&
        overrideOffsetLeftPx != null &&
        overrideOffsetTopPx != null
    ) {
        computeWatermarkTableLayoutPxClampedForRotation(
            bounds = bounds,
            anchor = anchor,
            offsetLeftPx = overrideOffsetLeftPx,
            offsetTopPx = overrideOffsetTopPx,
            tableHeightRatio = tableHeightRatio,
            tableWidthRatio = tableWidthRatio,
            rotationCwDeg = rotationCwDeg
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

    val shouldRotate = (rotationCwDeg % 360 + 360) % 360 == 90
    if (shouldRotate) {
        canvas.save()
        canvas.rotate(90f, left + tableW / 2f, top + tableH / 2f)
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

    val commonScaledTextSize = applyValueScaleFactor(
        baseTextSize = computeBaseTextSizeFromRenderedTable(
            tableWidth = tableW,
            tableHeight = tableH,
            rows = safeRows,
            cols = safeCols,
        ),
        valueScale = valueScale,
    )

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = resolveValueTextColor(bgStyle, textColorMode, manualTextColor)
        typeface = Typeface.DEFAULT_BOLD
        textSize = commonScaledTextSize
    }

    val cellTextPadding = resolveCellTextPadding(tableH)

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

            drawCellValueText(
                canvas = canvas,
                paint = valuePaint,
                cellRect = cellRect,
                cellText = cell.valueText,
                textAlign = textAlign,
                commonScaledTextSize = commonScaledTextSize,
                cellTextPadding = cellTextPadding,
                isPlaceholder = idx in placeholderCellIndexes,
                placeholderTextColorArgb = placeholderTextColorArgb,
            )
        }
    }

    if (shouldRotate) canvas.restore()
}
