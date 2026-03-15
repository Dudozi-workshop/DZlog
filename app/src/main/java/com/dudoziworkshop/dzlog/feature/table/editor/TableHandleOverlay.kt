package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

object TableHandleOverlay {
    fun applyRowWeightDragEnd(
        templateState: TableTemplateState,
        nextRowWeights: List<Float>
    ): TableTemplateState {
        val normalized = normalizeWeights(nextRowWeights, templateState.rows)
        return templateState.copy(rowWeights = normalized)
    }

    fun applyColumnWeightDragEnd(
        templateState: TableTemplateState,
        nextColumnWeights: List<Float>
    ): TableTemplateState {
        val normalized = normalizeWeights(nextColumnWeights, templateState.cols)
        return templateState.copy(colWeights = normalized)
    }

    private fun normalizeWeights(weights: List<Float>, count: Int): List<Float> {
        if (count <= 0) return emptyList()
        val safe = if (weights.size == count) weights else List(count) { 1f }
        return safe.map { it.coerceAtLeast(0.0001f) }
    }
}
