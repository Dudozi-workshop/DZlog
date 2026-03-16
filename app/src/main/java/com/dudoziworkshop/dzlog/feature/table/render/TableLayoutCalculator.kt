package com.dudoziworkshop.dzlog.feature.table.render

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
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

/**
 * 셀 내부 분배(행/열 + weights) 기반 aspect 계산.
 *
 * 주의: 이 값은 표 "외곽 박스 비율" SSOT가 아니다.
 * - 외곽 비율은 wmWidthRatio:wmHeightRatio 로만 계산한다.
 * - 이 함수는 셀 내부 체감 분배/shape 해석용으로만 사용한다.
 */
fun computeCellDistributionAspectRatio(rows: Int, cols: Int, rowWeights: List<Float>?, colWeights: List<Float>?): Float {
    val safeCols = cols.coerceAtLeast(1)
    val safeRows = rows.coerceAtLeast(1)
    val col = TableLayoutCalculator.resolveWeights(colWeights, safeCols).sum().coerceAtLeast(0.0001f)
    val row = TableLayoutCalculator.resolveWeights(rowWeights, safeRows).sum().coerceAtLeast(0.0001f)
    return (col / row).coerceIn(0.2f, 5f)
}

fun computeCellDistributionAspectRatio(templateState: TableTemplateState): Float = computeCellDistributionAspectRatio(
    rows = templateState.rows,
    cols = templateState.cols,
    rowWeights = templateState.rowWeights,
    colWeights = templateState.colWeights,
)

@Deprecated(
    message = "Use computeCellDistributionAspectRatio for cell distribution and computeRatioOnlyTableShape for outer ratio SSOT.",
    replaceWith = ReplaceWith("computeCellDistributionAspectRatio(rows, cols, rowWeights, colWeights)"),
)
fun resolveContentAspectRatio(rows: Int, cols: Int, rowWeights: List<Float>?, colWeights: List<Float>?): Float =
    computeCellDistributionAspectRatio(rows, cols, rowWeights, colWeights)

@Deprecated(
    message = "Use computeCellDistributionAspectRatio(templateState) for cell distribution and computeRatioOnlyTableShape for outer ratio SSOT.",
    replaceWith = ReplaceWith("computeCellDistributionAspectRatio(templateState)"),
)
fun resolveContentAspectRatio(templateState: TableTemplateState): Float = computeCellDistributionAspectRatio(templateState)

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

@Deprecated(
    message = "Use resolveDesignPreviewScale(rows, cols). Design Preview scale SSOT is rows*cols.",
    replaceWith = ReplaceWith("resolveDesignPreviewScale(rows, cols)"),
)
fun resolveDesignPreviewScale(cellCount: Int): Float =
    resolveDesignPreviewScale(rows = 1, cols = cellCount.coerceAtLeast(1))

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


@Deprecated(
    message = "Use computeDesignPreviewFitShape with rows/cols. Design Preview scale SSOT is rows*cols.",
    replaceWith = ReplaceWith(
        "computeDesignPreviewFitShape(boundsWidth, boundsHeight, tableWidthRatio, tableHeightRatio, rows, cols)",
    ),
)
fun computeDesignPreviewFitShape(
    boundsWidth: Float,
    boundsHeight: Float,
    tableWidthRatio: Int,
    tableHeightRatio: Int,
    cellCount: Int,
): TableShapeRatios = computeDesignPreviewFitShape(
    boundsWidth = boundsWidth,
    boundsHeight = boundsHeight,
    tableWidthRatio = tableWidthRatio,
    tableHeightRatio = tableHeightRatio,
    rows = 1,
    cols = cellCount.coerceAtLeast(1),
)

@Deprecated(
    message = "Use computeDesignPreviewFitShape with rows/cols for Design Preview policy.",
    replaceWith = ReplaceWith(
        "computeDesignPreviewFitShape(boundsWidth, boundsHeight, tableWidthRatio, tableHeightRatio, rows = 3, cols = 3)",
    ),
)
fun computeHomePreviewRatio(
    boundsWidth: Float,
    boundsHeight: Float,
    tableWidthRatio: Int,
    tableHeightRatio: Int,
): TableShapeRatios = computeDesignPreviewFitShape(
    boundsWidth = boundsWidth,
    boundsHeight = boundsHeight,
    tableWidthRatio = tableWidthRatio,
    tableHeightRatio = tableHeightRatio,
    rows = 3,
    cols = 3,
)
