package com.dudoziworkshop.dzlog.domain.model

enum class SaveMode(val v: Int) {
    ORIGINAL_ONLY(0),
    WATERMARK_ONLY(1),
    BOTH(2);

    companion object {
        fun from(v: Int) = entries.firstOrNull { it.v == v } ?: BOTH
    }
}
