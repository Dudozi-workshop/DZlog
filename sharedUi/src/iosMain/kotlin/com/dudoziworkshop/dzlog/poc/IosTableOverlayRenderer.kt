package com.dudoziworkshop.dzlog.poc

import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.Typeface

/**
 * First native overlay slice: resolved row-major text and weighted table geometry.
 * Caller resolves dynamic cell values before rendering. Position/rotation parity
 * with Android's camera scene is a separate integration gate.
 */
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
    ): ByteArray {
        require(imageWidth > 0 && imageHeight > 0)
        require(rows > 0 && cols > 0 && rows.toLong() * cols <= 10000)
        require(resolvedTexts.size == rows * cols) { "표 셀 개수가 일치하지 않습니다." }
        val xEdges = edges(cols, colWeights)
        val yEdges = edges(rows, rowWeights)
        val tableW = imageWidth * (widthRatio.coerceIn(10, 100) / 100f)
        val tableH = imageWidth * (heightRatio.coerceIn(10, 100) / 100f)
        require(tableH <= imageHeight) { "표가 이미지 높이를 초과합니다." }
        val surface = Surface.makeRasterN32Premul(imageWidth, imageHeight)
        surface.canvas.clear(0x00000000)
        val backgroundColor = if (style.bgStyle == 1) 0x00FFFFFF else 0x00000000
        if (style.bgStyle != 2 && style.bgAlpha > 0) {
            val alpha = style.bgAlpha.coerceIn(0, 255)
            val bg = Paint().apply { color = (alpha shl 24) or backgroundColor }
            surface.canvas.drawRect(Rect.makeXYWH(0f, 0f, tableW, tableH), bg)
        }
        val textColor = if (style.textColorMode == 1) {
            if (style.manualTextColor == 0) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        } else if (style.bgStyle == 0) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
        val textPaint = Paint().apply { color = textColor; isAntiAlias = true }
        val fontSize = (minOf(tableW / cols, tableH / rows) * 0.36f *
            (style.valueScale.coerceIn(20, 300) / 100f)).coerceAtLeast(1f)
        val font = Font(Typeface.makeDefault(), fontSize)
        for (r in 0 until rows) for (c in 0 until cols) {
            val left = xEdges[c] * tableW
            val right = xEdges[c + 1] * tableW
            val top = yEdges[r] * tableH
            val bottom = yEdges[r + 1] * tableH
            val value = resolvedTexts[r * cols + c]
            val textW = font.measureTextWidth(value)
            val x = when (style.textAlign) {
                1 -> (left + right - textW) / 2f
                2 -> right - textW - 4f
                else -> left + 4f
            }
            val y = (top + bottom) / 2f + fontSize * 0.35f
            surface.canvas.save()
            surface.canvas.clipRect(Rect.makeLTRB(left, top, right, bottom))
            surface.canvas.drawString(value, x, y, font, textPaint)
            surface.canvas.restore()
        }
        if (style.gridEnabled) {
            val grid = Paint().apply {
                color = if (style.bgStyle == 0) 0x6EFFFFFF else 0x6E000000
                strokeWidth = 1.2f
            }
            for (x in xEdges) surface.canvas.drawLine(x * tableW, 0f, x * tableW, tableH, grid)
            for (y in yEdges) surface.canvas.drawLine(0f, y * tableH, tableW, y * tableH, grid)
        }
        return checkNotNull(surface.makeImageSnapshot().encodeToData(EncodedImageFormat.PNG)) {
            "표 오버레이 인코딩 실패"
        }.bytes
    }

    /**
     * Map the capture pipeline's resolved text by cell coordinates rather than
     * assuming the resolver preserves a particular list order.
     * Span and positioning parity are intentionally withheld until implemented.
     */
    fun renderResolvedPreviewPng(
        imageWidth: Int,
        imageHeight: Int,
        template: TableTemplateState,
        preview: FinalCapturePreview,
        style: TableStyleState,
        widthRatio: Int = 60,
        heightRatio: Int = 20,
    ): ByteArray {
        require(template.rows > 0 && template.cols > 0)
        val byPosition = preview.resolvedCells.associate { cell ->
            val raw = requireNotNull(cell.raw) { "셀 좌표를 확인할 수 없습니다." }
            require(raw.rowSpan == 1 && raw.colSpan == 1) {
                "병합된 셀은 iOS 표 합성에서 아직 지원하지 않습니다."
            }
            require(raw.rowIndex in 0 until template.rows && raw.colIndex in 0 until template.cols)
            (raw.rowIndex to raw.colIndex) to cell.resolvedText
        }
        require(byPosition.size == preview.resolvedCells.size) { "중복된 표 셀 좌표가 있습니다." }
        val texts = List(template.rows * template.cols) { index ->
            val coordinate = index / template.cols to index % template.cols
            requireNotNull(byPosition[coordinate]) { "표 셀 좌표가 누락됐습니다." }
        }
        return renderPng(
            imageWidth, imageHeight, template.rows, template.cols, texts, style,
            widthRatio, heightRatio, template.rowWeights, template.colWeights,
        )
    }

    private fun edges(count: Int, weights: List<Float>?): List<Float> {
        val values = weights?.takeIf {
            it.size == count && it.all { w -> w > 0f && !w.isNaN() && !w.isInfinite() }
        } ?: List(count) { 1f }
        val sum = values.sum()
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
