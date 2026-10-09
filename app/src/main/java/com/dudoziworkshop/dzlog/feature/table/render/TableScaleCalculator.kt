package com.dudoziworkshop.dzlog.feature.table.render

object TableScaleCalculator {
    private const val MIN_VALUE_SCALE = 60
    private const val MAX_VALUE_SCALE = 160

    fun clampValueScale(scale: Int): Int = scale.coerceIn(MIN_VALUE_SCALE, MAX_VALUE_SCALE)
}
