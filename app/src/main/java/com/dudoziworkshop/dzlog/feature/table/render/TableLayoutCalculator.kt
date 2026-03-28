package com.dudoziworkshop.dzlog.feature.table.render

import kotlin.math.max
import kotlin.math.min

data class TableShapeRatios(
    val tableWidthRatio: Int,
    val tableHeightRatio: Int,
)

private const val DESIGN_PREVIEW_MIN_RATIO = 20
private const val DESIGN_PREVIEW_MAX_FILL_RATIO = 98

object TableLayoutCalculator {
    const val DEFAULT_WIDTH_RATIO = 100
    const val DEFAULT_HEIGHT_RATIO = 50

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

/**
 * 표 외곽 비율 SSOT 계산.
 * - 화면 공통: wmWidthRatio:wmHeightRatio의 비율만 사용한다.
 * - 2:4 와 20:40 은 동일 shape로 해석된다.
 */
fun computeDisplayTableAspectRatio(
    tableWidthRatio: Int,
    tableHeightRatio: Int,
): Float {
    val safeWidth = tableWidthRatio.coerceAtLeast(1)
    val safeHeight = tableHeightRatio.coerceAtLeast(1)
    return safeWidth.toFloat() / safeHeight.toFloat()
}

/**
 * 표 외곽 비율(표 자체 속성)만으로 화면 표시 shape를 계산한다.
 * row/col/weights는 여기서 사용하지 않는다.
 */
fun computeRatioOnlyTableShape(
    tableWidthRatio: Int,
    tableHeightRatio: Int,
    maxWidthRatio: Int,
    maxHeightRatio: Int,
    minWidthRatio: Int = 10,
    minHeightRatio: Int = 10,
    hardMaxRatio: Int = 100,
): TableShapeRatios {
    val safeAspectRatio = computeDisplayTableAspectRatio(tableWidthRatio, tableHeightRatio)
    val safeMaxWidth = max(maxWidthRatio, minWidthRatio).coerceIn(minWidthRatio, hardMaxRatio)
    val safeMaxHeight = max(maxHeightRatio, minHeightRatio).coerceIn(minHeightRatio, hardMaxRatio)

    val widthFromHeight = safeMaxHeight * safeAspectRatio
    val resolvedWidth = min(safeMaxWidth.toFloat(), widthFromHeight)
    val resolvedHeight = resolvedWidth / safeAspectRatio

    return TableShapeRatios(
        tableWidthRatio = resolvedWidth.toInt().coerceIn(minWidthRatio, safeMaxWidth),
        tableHeightRatio = resolvedHeight.toInt().coerceIn(minHeightRatio, safeMaxHeight),
    )
}

/**
 * Design Preview(표 상세/홈) 공통 스케일 정책.
 * - 작은 표는 과도하게 커 보이지 않게 완만하게 축소한다.
 * - 표 자체 비율(wmWidthRatio:wmHeightRatio)은 절대 변경하지 않는다.
 */
fun resolveDesignPreviewScale(rows: Int, cols: Int): Float {
    val gridCellCount = (rows.coerceAtLeast(1) * cols.coerceAtLeast(1))
    return when {
        gridCellCount <= 4 -> 0.72f
        gridCellCount <= 6 -> 0.82f
        gridCellCount <= 8 -> 0.90f
        else -> 1.00f
    }
}

/**
 * Design Preview(표 상세/홈) 공통 fit shape 계산.
 * - 외곽 비율 SSOT: wmWidthRatio:wmHeightRatio
 * - 내부 셀 분배(weights)는 이 함수에서 다루지 않는다.
 */
fun computeDesignPreviewFitShape(
    boundsWidth: Float,
    boundsHeight: Float,
    tableWidthRatio: Int,
    tableHeightRatio: Int,
    rows: Int,
    cols: Int,
): TableShapeRatios {
    val safeWidth = boundsWidth.coerceAtLeast(1f)
    val safeHeight = boundsHeight.coerceAtLeast(1f)
    val adaptiveMaxFillRatio = (DESIGN_PREVIEW_MAX_FILL_RATIO * resolveDesignPreviewScale(rows, cols))
        .toInt()
        .coerceIn(DESIGN_PREVIEW_MIN_RATIO, DESIGN_PREVIEW_MAX_FILL_RATIO)
    val maxHeightByBounds = ((safeHeight / safeWidth) * adaptiveMaxFillRatio)
        .toInt()
        .coerceAtLeast(DESIGN_PREVIEW_MIN_RATIO)

    return computeRatioOnlyTableShape(
        tableWidthRatio = tableWidthRatio,
        tableHeightRatio = tableHeightRatio,
        maxWidthRatio = adaptiveMaxFillRatio,
        maxHeightRatio = maxHeightByBounds.coerceAtMost(adaptiveMaxFillRatio),
        minWidthRatio = DESIGN_PREVIEW_MIN_RATIO,
        minHeightRatio = DESIGN_PREVIEW_MIN_RATIO,
        hardMaxRatio = DESIGN_PREVIEW_MAX_FILL_RATIO,
    )
}
