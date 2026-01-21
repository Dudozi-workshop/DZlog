package com.example.dzlog.domain.model

import android.util.Rational
import androidx.camera.core.AspectRatio

enum class CaptureAspect(val v: Int, val label: String, val w: Int, val h: Int) {
    R3_4(0, "3:4", 3, 4),
    R9_16(1, "9:16", 9, 16),
    R1_1(2, "1:1", 1, 1);

    val ratioF: Float get() = w.toFloat() / h.toFloat()

    fun toRational(): Rational = Rational(w, h)

    fun toCameraXAspectRatio(): Int? {
        return when (this) {
            R3_4 -> AspectRatio.RATIO_4_3
            R9_16 -> AspectRatio.RATIO_16_9
            R1_1 -> null
        }
    }

    companion object {
        fun from(v: Int) = values().firstOrNull { it.v == v } ?: R3_4
    }
}
