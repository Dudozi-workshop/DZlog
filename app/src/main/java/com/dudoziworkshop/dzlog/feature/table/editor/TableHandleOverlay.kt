package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator

/**
 * 이름은 Overlay지만, 현재는 UI 컴포넌트가 아닌
 * drag-end weight commit 정규화 helper 역할만 담당한다.
 */
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
        return TableLayoutCalculator.resolveWeights(
            weights = if (weights.size == count) weights else null,
            count = count,
        )
    }
}
