@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.dudoziworkshop.dzlog.poc

import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.useContents
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Native overlay renderer using Android's table geometry and text-layout rules.
 * Caller resolves dynamic values once before both preview and final rendering.
 */
/** iOS-local mirror of Android's five anchor options; no Android source dependency. */
internal enum class IosTableAnchor {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CUSTOM,
}

internal object IosTableOverlayRenderer {
    fun renderPng(
        imageWidth: Int,
        imageHeight: Int,
        rows: Int,
        cols: Int,
        resolvedTexts: List<String>,
        style: TableStyleState,
        widthRatio: Int = 60,
        heightRatio: Int = 20,
        rowWeights: List<Float>? = null,
        colWeights: List<Float>? = null,
        offsetXRatio: Float = 0f,
        offsetYRatio: Float = 0f,
        rotationDegrees: Float = 0f,
        cellRects: List<IosCaptureCellRect>? = null,
        placement: IosCaptureTablePlacement? = null,
        onContentAspect: ((Float) -> Unit)? = null,
    ): ByteArray {
        require(imageWidth > 0 && imageHeight > 0)
        require(rows > 0 && cols > 0 && rows.toLong() * cols <= 10000)
        require(resolvedTexts.size == rows * cols) { "표 셀 개수가 일치하지 않습니다." }
        val rects = cellRects ?: List(rows * cols) { IosCaptureCellRect(it / cols, it % cols, 1, 1) }
        val owners = IntArray(rows * cols) { -1 }
        rects.forEachIndexed { index, cell ->
            require(cell.row in 0 until rows && cell.col in 0 until cols &&
                cell.rows > 0 && cell.rows <= rows - cell.row &&
                cell.cols > 0 && cell.cols <= cols - cell.col)
            for (r in cell.row until cell.row + cell.rows) for (c in cell.col until cell.col + cell.cols) {
                require(owners[r * cols + c] == -1) { "병합 영역이 서로 겹칩니다." }
                owners[r * cols + c] = index
            }
        }
        require(owners.all { it >= 0 }) { "표 셀 좌표가 누락됐습니다." }
        require(widthRatio in 10..100 && heightRatio in 10..100)
        require(offsetXRatio.isFinite() && offsetYRatio.isFinite())
        val maxW = imageWidth * (widthRatio / 100f)
        val maxH = imageWidth * (heightRatio / 100f)
        val layout = placement?.let {
            fitCameraTable(resolvedTexts, rects, rows, cols, style.valueScale, rowWeights, colWeights, maxW, maxH)
        }
        val xEdges = edges(cols, layout?.widths ?: colWeights)
        val yEdges = edges(rows, layout?.heights ?: rowWeights)
        val tableW = layout?.width ?: maxW
        val tableH = layout?.height ?: maxH
        val fittedBounds = placement?.let {
            val aspect = tableW / tableH
            onContentAspect?.invoke(aspect)
            iosCaptureTableBounds(imageWidth, imageHeight, it, aspect)
        }
        val originX = fittedBounds?.rawX ?: (imageWidth * offsetXRatio)
        val originY = fittedBounds?.rawY ?: (imageHeight * offsetYRatio)
        require(rotationDegrees.isFinite()) { "표 회전값이 올바르지 않습니다." }
        val angle = rotationDegrees * (kotlin.math.PI / 180.0)
        val rotatedW = abs(tableW * cos(angle)) + abs(tableH * sin(angle))
        val rotatedH = abs(tableW * sin(angle)) + abs(tableH * cos(angle))
        val centerX = originX + tableW / 2f
        val centerY = originY + tableH / 2f
        // Pixel-space arithmetic can round a flush right/bottom anchor a few
        // thousandths beyond the edge; do not reject that valid placement.
        val edgeTolerance = 0.01
        require(centerX - rotatedW / 2 >= -edgeTolerance &&
            centerX + rotatedW / 2 <= imageWidth + edgeTolerance &&
            centerY - rotatedH / 2 >= -edgeTolerance &&
            centerY + rotatedH / 2 <= imageHeight + edgeTolerance) {
            "회전된 표가 이미지 영역을 벗어납니다."
        }
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(imageWidth.toDouble(), imageHeight.toDouble()), false, 1.0)
        try {
            val context = checkNotNull(UIGraphicsGetCurrentContext())
            CGContextTranslateCTM(context, centerX.toDouble(), centerY.toDouble())
            CGContextRotateCTM(context, angle)
            CGContextTranslateCTM(context, -centerX.toDouble(), -centerY.toDouble())
            if (style.bgStyle != 2 && style.bgAlpha > 0) {
                val gray = if (style.bgStyle == 1) 1.0 else 0.0
                CGContextSetGrayFillColor(context, gray, style.bgAlpha.coerceIn(0, 255) / 255.0)
                CGContextFillRect(context, CGRectMake(originX.toDouble(), originY.toDouble(), tableW.toDouble(), tableH.toDouble()))
            }
            fun x(col: Int) = (originX + xEdges[col] * tableW).toDouble()
            fun y(row: Int) = (originY + yEdges[row] * tableH).toDouble()
            if (style.gridEnabled) {
                CGContextSetGrayStrokeColor(context, if (style.bgStyle == 0) 1.0 else 0.0, 110 / 255.0)
                CGContextSetLineWidth(context, 1.2)
                for (r in 0 until rows) for (c in 0..cols) {
                    if (c == 0 || c == cols || owners[r * cols + c - 1] != owners[r * cols + c]) {
                        CGContextMoveToPoint(context, x(c), y(r))
                        CGContextAddLineToPoint(context, x(c), y(r + 1))
                    }
                }
                for (r in 0..rows) for (c in 0 until cols) {
                    if (r == 0 || r == rows || owners[(r - 1) * cols + c] != owners[r * cols + c]) {
                        CGContextMoveToPoint(context, x(c), y(r))
                        CGContextAddLineToPoint(context, x(c + 1), y(r))
                    }
                }
                CGContextStrokePath(context)
            }
            val white = if (style.textColorMode == 1) style.manualTextColor == 0 else style.bgStyle == 0
            val base = (minOf(tableW / cols, tableH / rows) * 0.36f).coerceAtLeast(1f)
            val size = (base * style.valueScale / 100f).coerceAtLeast(1f).toDouble()
            val attributes = mapOf<Any?, Any>(NSFontAttributeName to UIFont.boldSystemFontOfSize(size),
                NSForegroundColorAttributeName to if (white) UIColor.whiteColor else UIColor.blackColor)
            val padding = (tableH * 0.08f).coerceIn(8f, 20f)
            for (cell in rects) {
                val value = resolvedTexts[cell.row * cols + cell.col]
                if (value.isEmpty()) continue
                val label = NSAttributedString.create(string = value, attributes = attributes)
                val (width, height) = label.size().useContents { width to height }
                val left = x(cell.col)
                val right = x(cell.col + cell.cols)
                val top = y(cell.row)
                val bottom = y(cell.row + cell.rows)
                val leftText = when (style.textAlign) {
                    1 -> (left + right - width) / 2
                    2 -> right - width - padding
                    else -> left + padding
                }
                CGContextSaveGState(context)
                CGContextClipToRect(context, CGRectMake(left, top, right - left, bottom - top))
                label.drawAtPoint(CGPointMake(leftText, (top + bottom - height) / 2))
                CGContextRestoreGState(context)
            }
            val data = checkNotNull(UIImagePNGRepresentation(checkNotNull(UIGraphicsGetImageFromCurrentImageContext())))
            return checkNotNull(data.bytes).readBytes(data.length.toInt())
        } finally {
            UIGraphicsEndImageContext()
        }
    }

    private data class FittedTable(val widths: List<Float>, val heights: List<Float>, val width: Float, val height: Float)

    private fun fitCameraTable(texts: List<String>, roots: List<IosCaptureCellRect>, rows: Int, cols: Int,
                               valueScale: Int, rowWeights: List<Float>?, colWeights: List<Float>?,
                               maxW: Float, maxH: Float): FittedTable {
        val font = UIFont.systemFontOfSize(28.0 * valueScale.coerceIn(60, 160) / 100)
        val attributes = mapOf<Any?, Any>(NSFontAttributeName to font)
        fun measuredWidth(line: String) = NSAttributedString.create(string = line, attributes = attributes).size().useContents { width }
        // Android's Noto Sans CJK KR hmtx gives modern Hangul syllables a
        // 920-unit advance on a 1000-unit em. Use that advance for grid sizing
        // while UIKit shapes and draws the original text with its native font.
        val hangulAdvanceDelta = font.pointSize * 0.92 - measuredWidth("가")
        val widths = MutableList(cols) { 72f }
        val heights = MutableList(rows) { 48f }
        for (cell in roots) {
            val lines = texts[cell.row * cols + cell.col].ifEmpty { " " }.split('\n')
            val longest = lines.maxOf { line ->
                measuredWidth(line) + line.count { it in '가'..'힣' } * hangulAdvanceDelta
            }
            val w = (longest.toFloat() + 24f) / cell.cols
            val h = ((font.ascender - font.descender).coerceAtLeast(12.0).toFloat() * lines.size + 18f) / cell.rows
            for (c in cell.col until cell.col + cell.cols) widths[c] = maxOf(widths[c], w)
            for (r in cell.row until cell.row + cell.rows) heights[r] = maxOf(heights[r], h)
        }
        val weightedW = widths.mapIndexed { i, w -> w * (colWeights?.get(i) ?: 1f) }
        val weightedH = heights.mapIndexed { i, h -> h * (rowWeights?.get(i) ?: 1f) }
        val w = weightedW.sum()
        val h = weightedH.sum()
        val fit = minOf(maxW / w, maxH / h)
        return FittedTable(weightedW, weightedH, w * fit, h * fit)
    }

    /**
     * Map the capture pipeline's resolved text by cell coordinates rather than
     * assuming the resolver preserves a particular list order.
     * Merged roots follow the existing editor rule; covered values remain in the document.
     */
    fun renderResolvedPreviewPng(
        imageWidth: Int,
        imageHeight: Int,
        template: TableTemplateState,
        preview: FinalCapturePreview,
        style: TableStyleState,
        widthRatio: Int = 60,
        heightRatio: Int = 20,
        offsetXRatio: Float = 0f,
        offsetYRatio: Float = 0f,
        rotationDegrees: Float = 0f,
        placement: IosCaptureTablePlacement? = null,
        onContentAspect: ((Float) -> Unit)? = null,
    ): ByteArray {
        val rects = iosCaptureCellRects(template)
        val expected = template.cells.associateBy { it.cellId }
        val byPosition = preview.resolvedCells.associate { cell ->
            val raw = requireNotNull(cell.raw) { "셀 좌표를 확인할 수 없습니다." }
            require(raw == expected[raw.cellId]) { "촬영 미리보기와 표 문서가 일치하지 않습니다." }
            (raw.rowIndex to raw.colIndex) to cell.resolvedText
        }
        require(byPosition.size == preview.resolvedCells.size) { "중복된 표 셀 좌표가 있습니다." }
        require(byPosition.size == template.cells.size) { "촬영 미리보기 셀이 누락됐습니다." }
        val texts = List(template.rows * template.cols) { index ->
            byPosition[index / template.cols to index % template.cols].orEmpty()
        }
        return renderPng(
            imageWidth, imageHeight, template.rows, template.cols, texts, style,
            widthRatio, heightRatio, template.rowWeights, template.colWeights,
            offsetXRatio, offsetYRatio, rotationDegrees, cellRects = rects, placement = placement, onContentAspect = onContentAspect,
        )
    }

    /**
     * Match Android's anchor and CUSTOM offset convention: offset percentages
     * are measured across the remaining free space, not the full image.
     * The renderer still rejects any rotated layout that crosses image bounds.
     */
    fun renderAndroidAnchoredPng(
        imageWidth: Int,
        imageHeight: Int,
        template: TableTemplateState,
        preview: FinalCapturePreview,
        style: TableStyleState,
        anchor: IosTableAnchor,
        offsetXPercent: Int,
        offsetYPercent: Int,
        widthRatio: Int,
        heightRatio: Int,
        rotationCwDeg: Int = 0,
        onContentAspect: ((Float) -> Unit)? = null,
    ): ByteArray {
        val placement = IosCaptureTablePlacement(anchor, offsetXPercent, offsetYPercent, widthRatio, heightRatio, rotationCwDeg)
        return renderResolvedPreviewPng(
            imageWidth, imageHeight, template, preview, style, widthRatio, heightRatio,
            rotationDegrees = rotationCwDeg.toFloat(), placement = placement, onContentAspect = onContentAspect,
        )
    }

    private fun edges(count: Int, weights: List<Float>?): List<Float> {
        require(weights == null || (weights.size == count &&
            weights.all { w -> w > 0f && !w.isNaN() && !w.isInfinite() })) {
            "표 행·열 비율이 올바르지 않습니다."
        }
        val values = weights ?: List(count) { 1f }
        val sum = values.sum()
        require(sum > 0f && !sum.isInfinite()) { "표 행·열 비율 합계가 올바르지 않습니다." }
        var running = 0f
        return buildList {
            add(0f)
            values.forEach { weight ->
                running += weight / sum
                add(running.coerceIn(0f, 1f))
            }
        }
    }
}

