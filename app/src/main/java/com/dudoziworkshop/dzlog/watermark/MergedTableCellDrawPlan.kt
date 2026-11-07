package com.dudoziworkshop.dzlog.watermark

import com.dudoziworkshop.dzlog.feature.table.render.RenderRootCell

/**
 * Only multi-row/column roots change the standard row-major paint path.
 * Covered cells retain their original persisted values, but are not painted.
 */
internal data class MergedTableCellSpan(
    val lastRowExclusive: Int,
    val lastColExclusive: Int,
)

internal data class TableGridStroke(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
)

internal class MergedTableCellDrawPlan(
    val rows: Int,
    val cols: Int,
    private val owners: IntArray,
    private val spans: Map<Int, MergedTableCellSpan>,
) {
    fun isCovered(index: Int): Boolean = index in owners.indices && owners[index] != index

    fun spanAt(index: Int): MergedTableCellSpan? = spans[index]

    fun sameOwner(left: Int, right: Int): Boolean =
        left in owners.indices && right in owners.indices && owners[left] == owners[right]
}

internal fun createMergedTableCellDrawPlan(
    rows: Int,
    cols: Int,
    rootCells: List<RenderRootCell>,
): MergedTableCellDrawPlan {
    val safeRows = rows.coerceAtLeast(1)
    val safeCols = cols.coerceAtLeast(1)
    val owners = IntArray(safeRows * safeCols) { it }
    val spans = mutableMapOf<Int, MergedTableCellSpan>()
    rootCells.forEach { root ->
        if (root.rowSpan <= 1 && root.colSpan <= 1) return@forEach
        val row = root.rowIndex
        val col = root.colIndex
        if (row !in 0 until safeRows || col !in 0 until safeCols) return@forEach
        val endRow = (row + root.rowSpan.coerceAtLeast(1)).coerceAtMost(safeRows)
        val endCol = (col + root.colSpan.coerceAtLeast(1)).coerceAtMost(safeCols)
        val anchorIndex = row * safeCols + col
        spans[anchorIndex] = MergedTableCellSpan(endRow, endCol)
        for (r in row until endRow) {
            for (c in col until endCol) owners[r * safeCols + c] = anchorIndex
        }
    }
    return MergedTableCellDrawPlan(safeRows, safeCols, owners, spans)
}

/** Return only visible grid strokes; suppress separators inside a merged root. */
internal fun mergedTableGridStrokes(
    plan: MergedTableCellDrawPlan,
    rowEdges: List<Float>,
    colEdges: List<Float>,
): List<TableGridStroke> {
    val rows = plan.rows
    val cols = plan.cols
    if (rowEdges.size < rows + 1 || colEdges.size < cols + 1) return emptyList()
    return buildList {
        // Outer border remains continuous, even when a root touches an edge.
        add(TableGridStroke(colEdges[0], rowEdges[0], colEdges[0], rowEdges[rows]))
        add(TableGridStroke(colEdges[cols], rowEdges[0], colEdges[cols], rowEdges[rows]))
        add(TableGridStroke(colEdges[0], rowEdges[0], colEdges[cols], rowEdges[0]))
        add(TableGridStroke(colEdges[0], rowEdges[rows], colEdges[cols], rowEdges[rows]))
        for (column in 1 until cols) {
            for (row in 0 until rows) {
                val left = row * cols + column - 1
                val right = left + 1
                if (!plan.sameOwner(left, right)) {
                    add(TableGridStroke(colEdges[column], rowEdges[row], colEdges[column], rowEdges[row + 1]))
                }
            }
        }
        for (row in 1 until rows) {
            for (column in 0 until cols) {
                val upper = (row - 1) * cols + column
                val lower = row * cols + column
                if (!plan.sameOwner(upper, lower)) {
                    add(TableGridStroke(colEdges[column], rowEdges[row], colEdges[column + 1], rowEdges[row]))
                }
            }
        }
    }
}
