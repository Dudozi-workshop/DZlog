package com.dudoziworkshop.dzlog.domain.model

import androidx.camera.core.ImageCapture

enum class PhotoQualityMode(val v: Int, val label: String) {
    SPEED(0, "속도 우선"),
    BALANCED(1, "균형"),
    QUALITY(2, "화질 우선");

    fun toCaptureMode(): Int = when (this) {
        SPEED, BALANCED -> ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        QUALITY -> ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
    }

    val jpegQuality: Int
        get() = when (this) {
            SPEED -> 88
            BALANCED -> 94
            QUALITY -> 99
        }

    val maxLongEdgePx: Int?
        get() = when (this) {
            SPEED -> 1800
            BALANCED, QUALITY -> null
        }

    companion object {
        fun from(v: Int): PhotoQualityMode = entries.firstOrNull { it.v == v } ?: BALANCED
    }
}
