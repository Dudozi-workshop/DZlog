package com.example.dzlog.ui.table

import com.example.dzlog.domain.model.TableTemplateState
import kotlin.math.abs

/**
 * Stage 2:
 * - "행 높이/열 너비 가중치"를 실제 픽셀/좌표로 변환하는 계산 유틸만 도입한다.
 * - 아직 렌더링(촬영화면 표 그리기)에는 연결하지 않는다.
 *
 * 목표:
 * - weights == null 이거나 길이가 안 맞으면 "균등(전부 1.0)"으로 안전하게 fallback
 * - weights 합이 0에 가까우면 균등으로 fallback
 * - 결과 sizes 합은 total과 일치하도록 마지막 원소에 오차를 흡수
 */
object TableGridMetrics {

    fun resolveRowWeights(state: TableTemplateState): List<Float> {
        val n = state.rows.coerceAtLeast(1)
        val w = state.rowWeights
        if (w == null || w.size != n) return List(n) { 1f }
        return w.map { it.coerceAtLeast(0f) }
    }

    fun resolveColWeights(state: TableTemplateState): List<Float> {
        val n = state.cols.coerceAtLeast(1)
        val w = state.colWeights
        if (w == null || w.size != n) return List(n) { 1f }
        return w.map { it.coerceAtLeast(0f) }
    }

    /**
     * total(픽셀 등)을 weights 비율로 분배한 sizes를 만든다.
     * sizes.size == weights.size
     */
    fun computeSizes(total: Float, weights: List<Float>): List<Float> {
        val n = weights.size.coerceAtLeast(1)
        val safeWeights = weights.ifEmpty { List(n) { 1f } }
        val sum = safeWeights.sum()
        if (abs(sum) < 1e-6f) {
            // 합이 0이면 균등
            val each = total / n
            val sizes = MutableList(n) { each }
            // 누적 오차 보정
            val diff = total - sizes.sum()
            sizes[n - 1] = sizes[n - 1] + diff
            return sizes
        }

        val sizes = MutableList(n) { idx ->
            total * (safeWeights[idx] / sum)
        }
        // 누적 오차 보정 (float 연산)
        val diff = total - sizes.sum()
        sizes[n - 1] = sizes[n - 1] + diff
        return sizes
    }

    /**
     * sizes로부터 누적 오프셋을 만든다.
     * - 반환 길이 = sizes.size + 1
     * - offsets[0] = 0
     * - offsets[i+1] = offsets[i] + sizes[i]
     */
    fun computeOffsets(sizes: List<Float>): List<Float> {
        val offsets = ArrayList<Float>(sizes.size + 1)
        var acc = 0f
        offsets.add(0f)
        for (s in sizes) {
            acc += s
            offsets.add(acc)
        }
        return offsets
    }

    /**
     * tableW/tableH(전체 표 영역) 기준으로 행/열의 실제 분할 결과를 만든다.
     * (Stage 3에서 렌더링에 연결할 예정)
     */
    data class Grid(
        val rowHeights: List<Float>,
        val colWidths: List<Float>,
        val rowOffsets: List<Float>,
        val colOffsets: List<Float>,
    )

    fun buildGrid(state: TableTemplateState, tableW: Float, tableH: Float): Grid {
        val rowW = resolveRowWeights(state)
        val colW = resolveColWeights(state)

        val rowHeights = computeSizes(tableH, rowW)
        val colWidths = computeSizes(tableW, colW)

        val rowOffsets = computeOffsets(rowHeights)
        val colOffsets = computeOffsets(colWidths)

        return Grid(
            rowHeights = rowHeights,
            colWidths = colWidths,
            rowOffsets = rowOffsets,
            colOffsets = colOffsets
        )
    }
}
