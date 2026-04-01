package com.dudoziworkshop.dzlog.feature.table.render

import android.text.TextPaint
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder

data class ContentDrivenLayoutCell(
    val rowIndex: Int,
    val colIndex: Int,
    val rowSpan: Int,
    val colSpan: Int,
    val displayText: String,
    val isCovered: Boolean,
)

data class ContentDrivenLayout(
    val colWidthsPx: List<Float>,
    val rowHeightsPx: List<Float>,
    val contentWidthPx: Float,
    val contentHeightPx: Float,
    val scale: Float,
)

data class ResolvedRenderLayout(
    val finalRowWeights: List<Float>,
    val finalColWeights: List<Float>,
)

fun computeContentDrivenLayout(
    cells: List<ContentDrivenLayoutCell>,
    rows: Int,
    cols: Int,
    baseScaleRatio: Int,
    valueScale: Int,
): ContentDrivenLayout {
    val paint = TextPaint().apply { textSize = (valueScale.coerceIn(60, 160) / 100f) * 28f }
    val minColWidth = 72f
    val minRowHeight = 48f
    val horizontalPadding = 24f
    val lineHeight = paint.fontMetrics.let { it.descent - it.ascent }.coerceAtLeast(12f)

    val colWidths = MutableList(cols) { minColWidth }
    val rowHeights = MutableList(rows) { minRowHeight }

    cells.forEach { entry ->
        if (entry.isCovered) return@forEach
        val lines = entry.displayText.ifEmpty { " " }.split('\n')
        val longestLineWidth = lines.maxOfOrNull { paint.measureText(it) } ?: 0f
        val perColWidth = (longestLineWidth + horizontalPadding) / entry.colSpan.coerceAtLeast(1)
        for (col in entry.colIndex until (entry.colIndex + entry.colSpan).coerceAtMost(cols)) {
            colWidths[col] = maxOf(colWidths[col], perColWidth)
        }
        val perRowHeight = ((lineHeight * lines.size) + 18f) / entry.rowSpan.coerceAtLeast(1)
        for (row in entry.rowIndex until (entry.rowIndex + entry.rowSpan).coerceAtMost(rows)) {
            rowHeights[row] = maxOf(rowHeights[row], perRowHeight)
        }
    }

    val contentWidth = colWidths.sum().coerceAtLeast(1f)
    val contentHeight = rowHeights.sum().coerceAtLeast(1f)
    val baseWidth = (baseScaleRatio.coerceIn(10, 100) / 100f) * (cols * minColWidth)
    val effectiveWidth = maxOf(contentWidth, baseWidth)
    val maxDisplayWidth = 100f * minColWidth
    val scale = if (effectiveWidth > maxDisplayWidth) maxDisplayWidth / effectiveWidth else 1f

    return ContentDrivenLayout(
        colWidthsPx = colWidths,
        rowHeightsPx = rowHeights,
        contentWidthPx = effectiveWidth,
        contentHeightPx = contentHeight,
        scale = scale,
    )
}

fun toRelativeWeights(sizes: List<Float>): List<Float> {
    if (sizes.isEmpty()) return emptyList()
    val total = sizes.sum().coerceAtLeast(0.001f)
    return sizes.map { (it / total).coerceAtLeast(0.0001f) }
}

fun spanSize(start: Int, span: Int, sizes: List<Float>): Float {
    return (start until (start + span))
        .sumOf { index -> sizes.getOrNull(index)?.toDouble() ?: 0.0 }
        .toFloat()
}

fun computeResolvedRenderLayout(
    cells: List<WatermarkBuilder.WatermarkCell>,
    rows: Int,
    cols: Int,
    valueScale: Int,
    baseScaleRatio: Int = 100,
    rootCells: List<RenderRootCell>? = null,
): ResolvedRenderLayout {
    val safeRows = rows.coerceAtLeast(1)
    val safeCols = cols.coerceAtLeast(1)
    val cellsByIndex = cells.withIndex().associate { it.index to it.value }
    val rootByKey = rootCells?.associateBy { it.rowIndex to it.colIndex } ?: emptyMap()
    val coveredKeys = buildSet {
        rootCells?.forEach { root ->
            for (row in root.rowIndex until (root.rowIndex + root.rowSpan).coerceAtMost(safeRows)) {
                for (col in root.colIndex until (root.colIndex + root.colSpan).coerceAtMost(safeCols)) {
                    if (row == root.rowIndex && col == root.colIndex) continue
                    add(row to col)
                }
            }
        }
    }
    val layoutCells = buildList(safeRows * safeCols) {
        for (row in 0 until safeRows) {
            for (col in 0 until safeCols) {
                val idx = row * safeCols + col
                val root = rootByKey[row to col]
                add(
                    ContentDrivenLayoutCell(
                        rowIndex = row,
                        colIndex = col,
                        rowSpan = root?.rowSpan ?: 1,
                        colSpan = root?.colSpan ?: 1,
                        displayText = cellsByIndex[idx]?.valueText.orEmpty(),
                        isCovered = (row to col) in coveredKeys,
                    )
                )
            }
        }
    }
    val layout = computeContentDrivenLayout(
        cells = layoutCells,
        rows = safeRows,
        cols = safeCols,
        baseScaleRatio = baseScaleRatio,
        valueScale = valueScale,
    )
    return ResolvedRenderLayout(
        finalRowWeights = toRelativeWeights(layout.rowHeightsPx),
        finalColWeights = toRelativeWeights(layout.colWidthsPx),
    )
}
