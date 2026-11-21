package com.dudoziworkshop.dzlog.feature.table.render

import android.graphics.RectF
import android.text.TextPaint
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import com.dudoziworkshop.dzlog.watermark.computeWatermarkBoundsRect

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
    val paint = TextPaint().apply { textSize = contentMeasureTextSize(valueScale) }
    val metrics = paint.fontMetrics
    return measureContentDrivenLayout(cells, rows, cols, baseScaleRatio, metrics.descent - metrics.ascent) {
        paint.measureText(it)
    }
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
    return fitContentDrivenLayout(layout, rowWeights, colWeights, maxWidthPx, maxHeightPx, allowUpscaleToFit)
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
