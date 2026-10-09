package com.dudoziworkshop.dzlog.feature.table.render

import android.graphics.RectF
import android.text.TextPaint
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import com.dudoziworkshop.dzlog.watermark.computeWatermarkBoundsRect

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
    allowUpscaleToFit: Boolean = false,
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
    val widthFit = maxWidthPx?.takeIf { it > 0f }?.let { it / intrinsicTableWidth } ?: 1f
    val heightFit = maxHeightPx?.takeIf { it > 0f }?.let { it / intrinsicTableHeight } ?: 1f
    val rawFitScale = minOf(widthFit, heightFit)
    val fitScale = if (allowUpscaleToFit) {
        rawFitScale.coerceAtLeast(0.01f)
    } else {
        rawFitScale.coerceAtMost(1f).coerceAtLeast(0.01f)
    }
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
    allowUpscaleToFit: Boolean = false,
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
        allowUpscaleToFit = allowUpscaleToFit,
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


/**
 * Camera preview and saved photo share a normalized placement rule for the
 * *visible* (content fitted) table, not its larger aspect-ratio viewport.
 * This allows 0/10000 to reach the photo edge even when text-driven layout
 * has letterboxing within the viewport.
 */
fun buildCameraTableSceneFromPlacement(
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
    val candidate = buildContentDrivenRenderedSceneFromPlacement(
        bounds = bounds,
        placement = placement,
        cells = cells,
        templateCells = templateCells,
        rows = rows,
        cols = cols,
        valueScale = valueScale,
        baseScaleRatio = baseScaleRatio,
        rowWeights = rowWeights,
        colWeights = colWeights,
        allowUpscaleToFit = true,
    )
    val viewport = computeRenderedTableGeometry(
        bounds = bounds,
        placement = placement,
        rows = rows.coerceAtLeast(1),
        cols = cols.coerceAtLeast(1),
        rowWeights = null,
        colWeights = null,
    ).tableRect
    val rotatedViewport = computeWatermarkBoundsRect(viewport, placement.rotationCwDeg)
    val actualBounds = computeWatermarkBoundsRect(candidate.scene.tableRect, placement.rotationCwDeg)
    val availableViewportX = (bounds.width() - rotatedViewport.width()).coerceAtLeast(0f)
    val availableViewportY = (bounds.height() - rotatedViewport.height()).coerceAtLeast(0f)
    val normalizedX = if (availableViewportX < 0.001f) 0.5f else
        ((rotatedViewport.left - bounds.left) / availableViewportX).coerceIn(0f, 1f)
    val normalizedY = if (availableViewportY < 0.001f) 0.5f else
        ((rotatedViewport.top - bounds.top) / availableViewportY).coerceIn(0f, 1f)
    return moveCameraTableSceneToVisibleBounds(
        candidate,
        bounds,
        placement.rotationCwDeg,
        leftPx = normalizedX * (bounds.width() - actualBounds.width()).coerceAtLeast(0f),
        topPx = normalizedY * (bounds.height() - actualBounds.height()).coerceAtLeast(0f),
        templateCells = templateCells,
        rows = rows,
        cols = cols,
    )
}

/** Transient finger drag works in visible-table pixels; persistence uses ratios. */
fun moveCameraTableSceneToVisibleBounds(
    rendered: ContentDrivenRenderedScene,
    photoBounds: RectF,
    rotationCwDeg: Int,
    leftPx: Float,
    topPx: Float,
    templateCells: List<TableCellState>,
    rows: Int,
    cols: Int,
): ContentDrivenRenderedScene {
    val actualRect = rendered.scene.tableRect
    val bounds = computeWatermarkBoundsRect(actualRect, rotationCwDeg)
    val maxX = (photoBounds.width() - bounds.width()).coerceAtLeast(0f)
    val maxY = (photoBounds.height() - bounds.height()).coerceAtLeast(0f)
    val targetCenterX = photoBounds.left + leftPx.coerceIn(0f, maxX) + bounds.width() / 2f
    val targetCenterY = photoBounds.top + topPx.coerceIn(0f, maxY) + bounds.height() / 2f
    val newRect = RectF(
        targetCenterX - actualRect.width() / 2f,
        targetCenterY - actualRect.height() / 2f,
        targetCenterX + actualRect.width() / 2f,
        targetCenterY + actualRect.height() / 2f,
    )
    return rendered.copy(
        scene = buildRenderedTableScene(
            tableRect = newRect,
            rows = rows,
            cols = cols,
            rowWeights = rendered.resolvedLayout.finalRowWeights,
            colWeights = rendered.resolvedLayout.finalColWeights,
            rootCells = buildRenderRootCells(templateCells),
        )
    )
}
