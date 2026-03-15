package com.dudoziworkshop.dzlog.feature.table.render

import kotlin.math.min

object TableScaleCalculator {
    private const val MIN_VALUE_SCALE = 60
    private const val MAX_VALUE_SCALE = 160
    private const val MIN_ALPHA = 0
    private const val MAX_ALPHA = 255

    fun calculate(width: Float, height: Float): Float = min(width, height)

    fun clampValueScale(scale: Int): Int = scale.coerceIn(MIN_VALUE_SCALE, MAX_VALUE_SCALE)

    fun clampAlpha(alpha: Int): Int = alpha.coerceIn(MIN_ALPHA, MAX_ALPHA)
}
