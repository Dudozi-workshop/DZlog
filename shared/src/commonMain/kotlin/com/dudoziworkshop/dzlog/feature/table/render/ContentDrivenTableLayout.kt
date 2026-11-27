package com.dudoziworkshop.dzlog.feature.table.render

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
)

data class ResolvedRenderLayout(
    val intrinsicColWidthsPx: List<Float>,
    val intrinsicRowHeightsPx: List<Float>,
    val intrinsicTableWidthPx: Float,
    val intrinsicTableHeightPx: Float,
    val fitScale: Float,
    val scaledColWidthsPx: List<Float>,
    val scaledRowHeightsPx: List<Float>,
    val finalRowWeights: List<Float>,
    val finalColWeights: List<Float>,
)

fun contentMeasureTextSize(valueScale: Int) = 28f * (valueScale.coerceIn(60, 160) / 100f)

fun measureContentDrivenLayout(
    cells: List<ContentDrivenLayoutCell>,
    rows: Int,
    cols: Int,
    baseScaleRatio: Int,
    lineHeightPx: Float,
    measureLine: (String) -> Float,
): ContentDrivenLayout {
    val minColWidth = 72f
    val minRowHeight = 48f
    val horizontalPadding = 24f
    val lineHeight = lineHeightPx.coerceAtLeast(12f)

    val colWidths = MutableList(cols) { minColWidth }
    val rowHeights = MutableList(rows) { minRowHeight }

    cells.forEach { entry ->
        if (entry.isCovered) return@forEach
        val lines = entry.displayText.ifEmpty { " " }.split('\n')
        val longestLineWidth = lines.maxOf(measureLine)
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
    return ContentDrivenLayout(
        colWidthsPx = colWidths,
        rowHeightsPx = rowHeights,
        contentWidthPx = effectiveWidth,
        contentHeightPx = contentHeight,
    )
}

fun toRelativeWeights(sizes: List<Float>): List<Float> {
    if (sizes.isEmpty()) return emptyList()
    val total = sizes.sum().coerceAtLeast(0.001f)
    return sizes.map { (it / total).coerceAtLeast(0.0001f) }
}

fun fitContentDrivenLayout(
    layout: ContentDrivenLayout,
    rowWeights: List<Float>?,
    colWeights: List<Float>?,
    maxWidthPx: Float?,
    maxHeightPx: Float?,
    allowUpscaleToFit: Boolean,
): ResolvedRenderLayout {
    val baseIntrinsicColWidths = layout.colWidthsPx
    val intrinsicWidthFromCols = baseIntrinsicColWidths.sum().coerceAtLeast(1f)
    val widthUpscale = (layout.contentWidthPx / intrinsicWidthFromCols).coerceAtLeast(1f)
    val resolvedRowScales = TableLayoutCalculator.resolveWeights(rowWeights, layout.rowHeightsPx.size)
    val resolvedColScales = TableLayoutCalculator.resolveWeights(colWeights, layout.colWidthsPx.size)
    val intrinsicColWidths = baseIntrinsicColWidths.mapIndexed { index, width ->
        width * widthUpscale * resolvedColScales[index]
    }
    val intrinsicRowHeights = layout.rowHeightsPx.mapIndexed { index, height ->
        height * resolvedRowScales[index]
    }
    val intrinsicTableWidth = intrinsicColWidths.sum().coerceAtLeast(1f)
    val intrinsicTableHeight = intrinsicRowHeights.sum().coerceAtLeast(1f)
    val widthFit = maxWidthPx?.takeIf { it > 0f }?.let { it / intrinsicTableWidth } ?: 1f
    val heightFit = maxHeightPx?.takeIf { it > 0f }?.let { it / intrinsicTableHeight } ?: 1f
    val rawFitScale = minOf(widthFit, heightFit)
    val fitScale = if (allowUpscaleToFit) rawFitScale else rawFitScale.coerceAtMost(1f)
    val scaledColWidths = intrinsicColWidths.map { it * fitScale }
    val scaledRowHeights = intrinsicRowHeights.map { it * fitScale }
    return ResolvedRenderLayout(
        intrinsicColWidthsPx = intrinsicColWidths,
        intrinsicRowHeightsPx = intrinsicRowHeights,
        intrinsicTableWidthPx = intrinsicTableWidth,
        intrinsicTableHeightPx = intrinsicTableHeight,
        fitScale = fitScale,
        scaledColWidthsPx = scaledColWidths,
        scaledRowHeightsPx = scaledRowHeights,
        finalRowWeights = toRelativeWeights(scaledRowHeights),
        finalColWeights = toRelativeWeights(scaledColWidths),
    )
}
