package com.dudoziworkshop.dzlog.feature.table.render

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import kotlin.math.max
import kotlin.math.min

data class TableShapeRatios(
    val tableWidthRatio: Int,
    val tableHeightRatio: Int,
)

object TableLayoutCalculator {
    const val DEFAULT_WIDTH_RATIO = 100
    const val DEFAULT_HEIGHT_RATIO = 50

    val defaultAspectRatio: Float = DEFAULT_WIDTH_RATIO / DEFAULT_HEIGHT_RATIO.toFloat()

    fun resolveWeights(weights: List<Float>?, count: Int): List<Float> {
        if (count <= 0) return emptyList()
        if (weights == null || weights.size != count) return List(count) { 1f }
        return weights.map { it.coerceAtLeast(0.0001f) }
    }

    fun computeSizes(total: Float, weights: List<Float>): List<Float> {
        if (weights.isEmpty()) return emptyList()
        val sum = weights.sum().takeIf { it > 0f } ?: return List(weights.size) { total / weights.size }
        val sizes = weights.map { total * (it / sum) }.toMutableList()
        val diff = total - sizes.sum()
        if (sizes.isNotEmpty()) {
            sizes[sizes.lastIndex] = (sizes.last() + diff).coerceAtLeast(0f)
        }
        return sizes
    }
}

fun resolveContentAspectRatio(rows: Int, cols: Int, rowWeights: List<Float>?, colWeights: List<Float>?): Float {
    val safeCols = cols.coerceAtLeast(1)
    val safeRows = rows.coerceAtLeast(1)
    val col = TableLayoutCalculator.resolveWeights(colWeights, safeCols).sum().coerceAtLeast(0.0001f)
    val row = TableLayoutCalculator.resolveWeights(rowWeights, safeRows).sum().coerceAtLeast(0.0001f)
    return (col / row).coerceIn(0.2f, 5f)
}

fun resolveContentAspectRatio(templateState: TableTemplateState): Float = resolveContentAspectRatio(
    rows = templateState.rows,
    cols = templateState.cols,
    rowWeights = templateState.rowWeights,
    colWeights = templateState.colWeights,
)

fun computeShapeLockedRatios(
    contentAspectRatio: Float,
    maxWidthRatio: Int,
    maxHeightRatio: Int,
    minWidthRatio: Int = 10,
    minHeightRatio: Int = 10,
    hardMaxRatio: Int = 100,
): TableShapeRatios {
    val safeAspect = contentAspectRatio.coerceAtLeast(0.0001f)
    val safeMaxWidth = max(maxWidthRatio, minWidthRatio).coerceIn(minWidthRatio, hardMaxRatio)
    val safeMaxHeight = max(maxHeightRatio, minHeightRatio).coerceIn(minHeightRatio, hardMaxRatio)

    val widthFromHeight = safeMaxHeight * safeAspect
    val resolvedWidth = min(safeMaxWidth.toFloat(), widthFromHeight)
    val resolvedHeight = resolvedWidth / safeAspect

    return TableShapeRatios(
        tableWidthRatio = resolvedWidth.toInt().coerceIn(minWidthRatio, safeMaxWidth),
        tableHeightRatio = resolvedHeight.toInt().coerceIn(minHeightRatio, safeMaxHeight),
    )
}
