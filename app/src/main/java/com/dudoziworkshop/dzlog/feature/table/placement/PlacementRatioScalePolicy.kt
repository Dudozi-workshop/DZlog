package com.dudoziworkshop.dzlog.feature.table.placement

import kotlin.math.roundToInt

data class RatioScaledSize(
    val widthRatio: Int,
    val heightRatio: Int,
    val scalePercent: Float,
)

data class RatioScaleRange(
    val minScalePercent: Float,
    val maxScalePercent: Float,
)

fun resolveRatioLockedScaleRange(
    baseWidthRatio: Int,
    baseHeightRatio: Int,
    minRatio: Int = 10,
    maxRatio: Int = 100,
): RatioScaleRange {
    val safeBaseW = baseWidthRatio.coerceAtLeast(1)
    val safeBaseH = baseHeightRatio.coerceAtLeast(1)
    val minScale = maxOf(minRatio.toFloat() / safeBaseW, minRatio.toFloat() / safeBaseH) * 100f
    val maxScale = minOf(maxRatio.toFloat() / safeBaseW, maxRatio.toFloat() / safeBaseH) * 100f
    return if (minScale <= maxScale) {
        RatioScaleRange(minScalePercent = minScale, maxScalePercent = maxScale)
    } else {
        RatioScaleRange(minScalePercent = maxScale, maxScalePercent = maxScale)
    }
}

fun resolveRatioLockedSizeFromScale(
    baseWidthRatio: Int,
    baseHeightRatio: Int,
    requestedScalePercent: Float,
    minRatio: Int = 10,
    maxRatio: Int = 100,
): RatioScaledSize {
    val safeBaseW = baseWidthRatio.coerceAtLeast(1)
    val safeBaseH = baseHeightRatio.coerceAtLeast(1)
    val scaleRange = resolveRatioLockedScaleRange(
        baseWidthRatio = safeBaseW,
        baseHeightRatio = safeBaseH,
        minRatio = minRatio,
        maxRatio = maxRatio,
    )
    val clampedScale = requestedScalePercent.coerceIn(scaleRange.minScalePercent, scaleRange.maxScalePercent)
    val width = (safeBaseW * (clampedScale / 100f)).roundToInt().coerceIn(minRatio, maxRatio)
    val height = (safeBaseH * (clampedScale / 100f)).roundToInt().coerceIn(minRatio, maxRatio)
    return RatioScaledSize(
        widthRatio = width,
        heightRatio = height,
        scalePercent = clampedScale,
    )
}

fun resolveRatioLockedSizeFromWidth(
    baseWidthRatio: Int,
    baseHeightRatio: Int,
    requestedWidthRatio: Int,
    minRatio: Int = 10,
    maxRatio: Int = 100,
): RatioScaledSize {
    val safeBaseW = baseWidthRatio.coerceAtLeast(1)
    val requestedScale = requestedWidthRatio.toFloat() / safeBaseW * 100f
    return resolveRatioLockedSizeFromScale(
        baseWidthRatio = baseWidthRatio,
        baseHeightRatio = baseHeightRatio,
        requestedScalePercent = requestedScale,
        minRatio = minRatio,
        maxRatio = maxRatio,
    )
}

fun resolveRatioLockedSizeFromHeight(
    baseWidthRatio: Int,
    baseHeightRatio: Int,
    requestedHeightRatio: Int,
    minRatio: Int = 10,
    maxRatio: Int = 100,
): RatioScaledSize {
    val safeBaseH = baseHeightRatio.coerceAtLeast(1)
    val requestedScale = requestedHeightRatio.toFloat() / safeBaseH * 100f
    return resolveRatioLockedSizeFromScale(
        baseWidthRatio = baseWidthRatio,
        baseHeightRatio = baseHeightRatio,
        requestedScalePercent = requestedScale,
        minRatio = minRatio,
        maxRatio = maxRatio,
    )
}
