package com.dudoziworkshop.dzlog.domain.model

import kotlin.math.roundToInt

enum class CaptureAspect(val v: Int, val label: String, val w: Int, val h: Int) {
    R3_4(0, "3:4", 3, 4),
    R9_16(1, "9:16", 9, 16),
    R1_1(2, "1:1", 1, 1);

    val ratioF: Float get() = w.toFloat() / h.toFloat()

    fun centerCropSize(sourceWidth: Int, sourceHeight: Int): Pair<Int, Int> {
        require(sourceWidth > 0 && sourceHeight > 0)
        val sourceRatio = sourceWidth.toFloat() / sourceHeight
        return if (sourceRatio > ratioF) {
            (sourceHeight * ratioF).roundToInt().coerceIn(1, sourceWidth) to sourceHeight
        } else {
            sourceWidth to (sourceWidth / ratioF).roundToInt().coerceIn(1, sourceHeight)
        }
    }

    companion object {
        fun from(v: Int) = entries.firstOrNull { it.v == v } ?: R3_4
    }
}
