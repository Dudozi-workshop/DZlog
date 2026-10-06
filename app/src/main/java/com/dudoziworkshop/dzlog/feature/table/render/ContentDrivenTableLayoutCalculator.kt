package com.dudoziworkshop.dzlog.feature.table.render

import android.graphics.RectF
import android.text.TextPaint
import com.dudoziworkshop.dzlog.domain.model.TableCellState
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

data class ContentDrivenRenderedScene(
    val scene: RenderedTableScene,
    val resolvedLayout: ResolvedRenderLayout,
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
    rowWeights: List<Float>? = null,
    colWeights: List<Float>? = null,
    maxWidthPx: Float? = null,
    maxHeightPx: Float? = null,
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
    val baseIntrinsicColWidths = layout.colWidthsPx
    val intrinsicWidthFromCols = baseIntrinsicColWidths.sum().coerceAtLeast(1f)
    val widthUpscale = (layout.contentWidthPx / intrinsicWidthFromCols).coerceAtLeast(1f)
    val resolvedRowScales = TableLayoutCalculator.resolveWeights(rowWeights, safeRows)
    val resolvedColScales = TableLayoutCalculator.resolveWeights(colWeights, safeCols)
    val intrinsicColWidths = baseIntrinsicColWidths.mapIndexed { index, width ->
        width * widthUpscale * resolvedColScales[index]
    }
    val intrinsicRowHeights = layout.rowHeightsPx.mapIndexed { index, height ->
        height * resolvedRowScales[index]
    }
    val intrinsicTableWidth = intrinsicColWidths.sum().coerceAtLeast(1f)
    val intrinsicTableHeight = intrinsicRowHeights.sum().coerceAtLeast(1f)
    val widthFit = maxWidthPx?.takeIf { it > 0f }?.let { (it / intrinsicTableWidth).coerceAtMost(1f) } ?: 1f
    val heightFit = maxHeightPx?.takeIf { it > 0f }?.let { (it / intrinsicTableHeight).coerceAtMost(1f) } ?: 1f
    val fitScale = minOf(widthFit, heightFit).coerceAtMost(1f).coerceAtLeast(0.01f)
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

fun buildContentDrivenRenderedSceneFromPlacement(
    bounds: RectF,
    placement: TableRenderPlacement,
    cells: List<WatermarkBuilder.WatermarkCell>,
    templateCells: List<TableCellState>,
    rows: Int,
    cols: Int,
    valueScale: Int,
    baseScaleRatio: Int,
    rowWeights: List<Float>? = null,
    colWeights: List<Float>? = null,
): ContentDrivenRenderedScene {
    val rootCells = buildRenderRootCells(templateCells)
    val viewportTableRect = computeRenderedTableGeometry(
        bounds = bounds,
        placement = placement,
        rows = rows.coerceAtLeast(1),
        cols = cols.coerceAtLeast(1),
        rowWeights = null,
        colWeights = null,
    ).tableRect
    val resolvedLayout = computeResolvedRenderLayout(
        cells = cells,
        rows = rows,
        cols = cols,
        valueScale = valueScale,
        baseScaleRatio = baseScaleRatio,
        rootCells = rootCells,
        rowWeights = rowWeights,
        colWeights = colWeights,
        maxWidthPx = viewportTableRect.width(),
        maxHeightPx = viewportTableRect.height(),
    )
    val finalTableRect = RectF(
        viewportTableRect.centerX() - (resolvedLayout.scaledColWidthsPx.sum() / 2f),
        viewportTableRect.centerY() - (resolvedLayout.scaledRowHeightsPx.sum() / 2f),
        viewportTableRect.centerX() + (resolvedLayout.scaledColWidthsPx.sum() / 2f),
        viewportTableRect.centerY() + (resolvedLayout.scaledRowHeightsPx.sum() / 2f),
    )
    val scene = buildRenderedTableScene(
        tableRect = finalTableRect,
        rows = rows,
        cols = cols,
        rowWeights = resolvedLayout.finalRowWeights,
        colWeights = resolvedLayout.finalColWeights,
        rootCells = rootCells,
    )
    return ContentDrivenRenderedScene(
        scene = scene,
        resolvedLayout = resolvedLayout,
    )
}
