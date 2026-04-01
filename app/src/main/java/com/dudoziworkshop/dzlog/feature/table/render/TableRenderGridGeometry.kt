package com.dudoziworkshop.dzlog.feature.table.render

import android.graphics.RectF
import com.dudoziworkshop.dzlog.watermark.computeWatermarkTableLayout
import com.dudoziworkshop.dzlog.watermark.computeWatermarkTableLayoutPxClampedForRotation
import kotlin.math.abs

data class RenderGridGeometry(
    val colEdges: List<Float>,
    val rowEdges: List<Float>,
)

data class RenderedTableGeometry(
    val tableRect: RectF,
    val grid: RenderGridGeometry,
)

data class RenderCellRect(
    val rowIndex: Int,
    val colIndex: Int,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class RenderRootCell(
    val cellId: String,
    val rowIndex: Int,
    val colIndex: Int,
    val rowSpan: Int,
    val colSpan: Int,
)

data class RenderRootRect(
    val cellId: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

data class RenderedTableScene(
    val tableRect: RectF,
    val geometry: RenderedTableGeometry,
    val baseCellRects: List<RenderCellRect>,
    val baseCellRectsByKey: Map<Pair<Int, Int>, RenderCellRect>,
    val rootRects: List<RenderRootRect>,
)

fun computeRenderedTableGeometry(
    bounds: RectF,
    placement: TableRenderPlacement,
    rows: Int,
    cols: Int,
    rowWeights: List<Float>?,
    colWeights: List<Float>?,
): RenderedTableGeometry {
    val layout = if (
        placement.anchor == com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor.CUSTOM &&
        placement.overrideOffsetLeftPx != null &&
        placement.overrideOffsetTopPx != null
    ) {
        computeWatermarkTableLayoutPxClampedForRotation(
            bounds = bounds,
            anchor = placement.anchor,
            offsetLeftPx = placement.overrideOffsetLeftPx,
            offsetTopPx = placement.overrideOffsetTopPx,
            tableHeightRatio = placement.tableHeightRatio,
            tableWidthRatio = placement.tableWidthRatio,
            rotationCwDeg = placement.rotationCwDeg,
        )
    } else {
        computeWatermarkTableLayout(
            bounds = bounds,
            anchor = placement.anchor,
            offsetXRatio = placement.offsetXRatio,
            offsetYRatio = placement.offsetYRatio,
            tableHeightRatio = placement.tableHeightRatio,
            tableWidthRatio = placement.tableWidthRatio,
        )
    }
    val tableRect = layout.rect
    val grid = computeRenderGridGeometry(
        rows = rows,
        cols = cols,
        rowWeights = rowWeights,
        colWeights = colWeights,
        tableWidthPx = tableRect.width(),
        tableHeightPx = tableRect.height(),
    )
    return RenderedTableGeometry(
        tableRect = RectF(tableRect),
        grid = grid,
    )
}

fun buildRenderedTableScene(
    tableRect: RectF,
    rows: Int,
    cols: Int,
    rowWeights: List<Float>?,
    colWeights: List<Float>?,
    rootCells: List<RenderRootCell>,
): RenderedTableScene {
    val safeRows = rows.coerceAtLeast(1)
    val safeCols = cols.coerceAtLeast(1)
    val geometry = RenderedTableGeometry(
        tableRect = RectF(tableRect),
        grid = computeRenderGridGeometry(
            rows = safeRows,
            cols = safeCols,
            rowWeights = rowWeights,
            colWeights = colWeights,
            tableWidthPx = tableRect.width(),
            tableHeightPx = tableRect.height(),
        )
    )
    val baseCellRects = computeRenderedCellRects(
        geometry = geometry,
        rows = safeRows,
        cols = safeCols,
    )
    val baseCellRectsByKey = baseCellRects.associateBy { it.rowIndex to it.colIndex }
    val rootRects = rootCells.mapNotNull { cell ->
        if (cell.rowIndex !in 0 until safeRows || cell.colIndex !in 0 until safeCols) return@mapNotNull null
        val minRow = cell.rowIndex
        val maxRow = (cell.rowIndex + cell.rowSpan - 1).coerceAtMost(safeRows - 1)
        val minCol = cell.colIndex
        val maxCol = (cell.colIndex + cell.colSpan - 1).coerceAtMost(safeCols - 1)
        val included = buildList {
            for (row in minRow..maxRow) {
                for (col in minCol..maxCol) {
                    baseCellRectsByKey[row to col]?.let(::add)
                }
            }
        }
        if (included.isEmpty()) return@mapNotNull null
        val left = included.minOf { it.left } - geometry.tableRect.left
        val top = included.minOf { it.top } - geometry.tableRect.top
        val right = included.maxOf { it.right } - geometry.tableRect.left
        val bottom = included.maxOf { it.bottom } - geometry.tableRect.top
        RenderRootRect(
            cellId = cell.cellId,
            left = left,
            top = top,
            right = right,
            bottom = bottom,
        )
    }
    return RenderedTableScene(
        tableRect = RectF(geometry.tableRect),
        geometry = geometry,
        baseCellRects = baseCellRects,
        baseCellRectsByKey = baseCellRectsByKey,
        rootRects = rootRects,
    )
}

fun buildRenderedTableSceneFromPlacement(
    bounds: RectF,
    placement: TableRenderPlacement,
    rows: Int,
    cols: Int,
    rowWeights: List<Float>?,
    colWeights: List<Float>?,
    rootCells: List<RenderRootCell> = emptyList(),
): RenderedTableScene {
    val geometry = computeRenderedTableGeometry(
        bounds = bounds,
        placement = placement,
        rows = rows.coerceAtLeast(1),
        cols = cols.coerceAtLeast(1),
        rowWeights = rowWeights,
        colWeights = colWeights,
    )
    return buildRenderedTableScene(
        tableRect = geometry.tableRect,
        rows = rows,
        cols = cols,
        rowWeights = rowWeights,
        colWeights = colWeights,
        rootCells = rootCells,
    )
}

fun computeRenderGridGeometry(
    rows: Int,
    cols: Int,
    rowWeights: List<Float>?,
    colWeights: List<Float>?,
    tableWidthPx: Float,
    tableHeightPx: Float,
): RenderGridGeometry {
    val safeRows = rows.coerceAtLeast(1)
    val safeCols = cols.coerceAtLeast(1)
    val resolvedRowWeights = normalizeWeights(rowWeights, safeRows)
    val resolvedColWeights = normalizeWeights(colWeights, safeCols)
    val rowEdges = buildEdges(resolvedRowWeights, tableHeightPx)
    val colEdges = buildEdges(resolvedColWeights, tableWidthPx)
    return RenderGridGeometry(
        colEdges = colEdges,
        rowEdges = rowEdges,
    )
}

fun computeRenderedCellRects(
    geometry: RenderedTableGeometry,
    rows: Int,
    cols: Int,
): List<RenderCellRect> {
    val safeRows = rows.coerceAtLeast(1)
    val safeCols = cols.coerceAtLeast(1)
    return buildList(safeRows * safeCols) {
        for (row in 0 until safeRows) {
            for (col in 0 until safeCols) {
                val left = geometry.tableRect.left + geometry.grid.colEdges[col]
                val right = geometry.tableRect.left + geometry.grid.colEdges[col + 1]
                val top = geometry.tableRect.top + geometry.grid.rowEdges[row]
                val bottom = geometry.tableRect.top + geometry.grid.rowEdges[row + 1]
                add(
                    RenderCellRect(
                        rowIndex = row,
                        colIndex = col,
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                    )
                )
            }
        }
    }
}

private fun normalizeWeights(weights: List<Float>?, count: Int): List<Float> {
    if (count <= 0) return emptyList()
    if (weights == null || weights.size != count) return List(count) { 1f }
    return weights.map { it.coerceAtLeast(0f) }
}

private fun buildEdges(weights: List<Float>, extent: Float): List<Float> {
    val safeWeights = if (weights.isEmpty()) listOf(1f) else weights
    val sizes = computeSizes(extent, safeWeights)
    val edges = MutableList(sizes.size + 1) { 0f }
    var cursor = 0f
    sizes.forEachIndexed { index, size ->
        cursor += size
        edges[index + 1] = cursor
    }
    return edges
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
