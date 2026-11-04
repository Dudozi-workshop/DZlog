package com.dudoziworkshop.dzlog.poc

import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.impl.use
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * First native overlay slice: resolved row-major text and weighted table geometry.
 * Caller resolves dynamic cell values before rendering. Translation and rotation
 * are supported, but Android camera placement parity is a separate integration gate.
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
        val xEdges = edges(cols, colWeights)
        val yEdges = edges(rows, rowWeights)
        require(widthRatio in 10..100 && heightRatio in 10..100) {
            "표 크기 비율이 올바르지 않습니다."
        }
        val tableW = imageWidth * (widthRatio / 100f)
        val tableH = imageWidth * (heightRatio / 100f)
        // A 90-degree CUSTOM anchor can have a negative *unrotated* origin;
        // only the final rotated bounds must stay within the photograph.
        require(offsetXRatio.isFinite() && offsetYRatio.isFinite()) {
            "표 위치 비율이 올바르지 않습니다."
        }
        val originX = imageWidth * offsetXRatio
        val originY = imageHeight * offsetYRatio
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
        return Surface.makeRasterN32Premul(imageWidth, imageHeight).use { surface ->
            surface.canvas.clear(0x00000000)
            surface.canvas.save()
            surface.canvas.translate(centerX, centerY)
            surface.canvas.rotate(rotationDegrees)
            surface.canvas.translate(-centerX, -centerY)
            val backgroundColor = if (style.bgStyle == 1) 0x00FFFFFF else 0x00000000
            if (style.bgStyle != 2 && style.bgAlpha > 0) {
                val alpha = style.bgAlpha.coerceIn(0, 255)
                Paint().use { bg ->
                    bg.color = (alpha shl 24) or backgroundColor
                    surface.canvas.drawRect(Rect.makeXYWH(originX, originY, tableW, tableH), bg)
                }
            }
            val textColor = if (style.textColorMode == 1) {
                if (style.manualTextColor == 0) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
            } else if (style.bgStyle == 0) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
            Paint().use { textPaint ->
                textPaint.color = textColor
                textPaint.isAntiAlias = true
                val fontSize = (minOf(tableW / cols, tableH / rows) * 0.36f *
                    (style.valueScale.coerceIn(20, 300) / 100f)).coerceAtLeast(1f)
                Font(null, fontSize).use { font ->
                    for (cell in rects) {
                        val left = originX + xEdges[cell.col] * tableW
                        val right = originX + xEdges[cell.col + cell.cols] * tableW
                        val top = originY + yEdges[cell.row] * tableH
                        val bottom = originY + yEdges[cell.row + cell.rows] * tableH
                        val value = resolvedTexts[cell.row * cols + cell.col]
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
                }
            }
            if (style.gridEnabled) {
                Paint().use { grid ->
                    grid.color = if (style.bgStyle == 0) 0x6EFFFFFF else 0x6E000000
                    grid.strokeWidth = 1.2f
                    // Draw each shared boundary once; covered coordinates have no internal grid.
                    for (r in 0 until rows) for (c in 0..cols) {
                        if (c == 0 || c == cols || owners[r * cols + c - 1] != owners[r * cols + c]) {
                            val x = originX + xEdges[c] * tableW
                            surface.canvas.drawLine(x, originY + yEdges[r] * tableH,
                                x, originY + yEdges[r + 1] * tableH, grid)
                        }
                    }
                    for (r in 0..rows) for (c in 0 until cols) {
                        if (r == 0 || r == rows || owners[(r - 1) * cols + c] != owners[r * cols + c]) {
                            val y = originY + yEdges[r] * tableH
                            surface.canvas.drawLine(originX + xEdges[c] * tableW, y,
                                originX + xEdges[c + 1] * tableW, y, grid)
                        }
                    }
                }
            }
            surface.canvas.restore()
            surface.makeImageSnapshot().use { snapshot ->
                checkNotNull(snapshot.encodeToData(EncodedImageFormat.PNG)) {
                    "표 오버레이 인코딩 실패"
                }.use { it.bytes }
            }
        }
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
            offsetXRatio, offsetYRatio, rotationDegrees, cellRects = rects,
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
    ): ByteArray {
        val bounds = iosCaptureTableBounds(imageWidth, imageHeight,
            IosCaptureTablePlacement(anchor, offsetXPercent, offsetYPercent, widthRatio, heightRatio, rotationCwDeg))
        return renderResolvedPreviewPng(
            imageWidth, imageHeight, template, preview, style,
            widthRatio, heightRatio,
            offsetXRatio = bounds.rawX / imageWidth,
            offsetYRatio = bounds.rawY / imageHeight,
            rotationDegrees = rotationCwDeg.toFloat(),
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

