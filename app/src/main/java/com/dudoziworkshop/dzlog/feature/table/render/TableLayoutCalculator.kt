package com.dudoziworkshop.dzlog.feature.table.render

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
