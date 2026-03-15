package com.dudoziworkshop.dzlog.feature.table.render

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

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

fun resolveContentAspectRatio(templateState: TableTemplateState): Float {
    val cols = templateState.cols.coerceAtLeast(1)
    val rows = templateState.rows.coerceAtLeast(1)
    val col = TableLayoutCalculator.resolveWeights(templateState.colWeights, cols).sum().coerceAtLeast(0.0001f)
    val row = TableLayoutCalculator.resolveWeights(templateState.rowWeights, rows).sum().coerceAtLeast(0.0001f)
    return (col / row).coerceAtLeast(0.2f).coerceAtMost(5f)
}
