package com.dudoziworkshop.dzlog.domain.model

enum class ContinuousPreviewMode(val v: Int) {
    OFF(0),
    SHORT(1),
    HOLD(2);

    companion object {
        fun from(v: Int) = entries.firstOrNull { it.v == v } ?: OFF
    }
}
