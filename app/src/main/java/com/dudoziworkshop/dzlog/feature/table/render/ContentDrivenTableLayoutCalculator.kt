package com.dudoziworkshop.dzlog.feature.table.render

import android.text.TextPaint

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
