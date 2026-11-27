package com.dudoziworkshop.dzlog.feature.table.render

import kotlin.math.roundToInt

/** Both stored dimensions use photo-width units, including after rotation. */
class CameraTableSizeLimits(photoAspect: Float, rotationCwDeg: Int) {
    private val photoHeight = (100f / photoAspect).coerceAtMost(TableLayoutCalculator.CAMERA_MAX_RATIO.toFloat())
    private val rotated = rotationCwDeg % 180 != 0
    private val widthLimit = if (rotated) photoHeight else 100f
    private val heightLimit = if (rotated) 100f else photoHeight

    val maxWidthRatio = widthLimit.toInt()
    val maxHeightRatio = heightLimit.toInt()

    init {
        require(photoAspect.isFinite() && photoAspect > 0f && minOf(maxWidthRatio, maxHeightRatio) >= 10)
    }

    fun scaleRange(widthRatio: Int, heightRatio: Int): ClosedFloatingPointRange<Float> {
        val width = widthRatio.coerceIn(10, TableLayoutCalculator.CAMERA_MAX_RATIO)
        val height = heightRatio.coerceIn(10, TableLayoutCalculator.CAMERA_MAX_RATIO)
        val maximum = minOf(widthLimit / width, heightLimit / height)
        return minOf(maxOf(10f / width, 10f / height), maximum)..maximum
    }

    fun fit(widthRatio: Int, heightRatio: Int, scale: Float = 1f): TableShapeRatios {
        val applied = scale.coerceIn(scaleRange(widthRatio, heightRatio))
        return TableShapeRatios(
            (widthRatio * applied).roundToInt().coerceIn(10, maxWidthRatio),
            (heightRatio * applied).roundToInt().coerceIn(10, maxHeightRatio),
        )
    }
}
