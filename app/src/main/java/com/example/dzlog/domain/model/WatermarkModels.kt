package com.example.dzlog.domain.model


data class ResolvedCell(
    val label: String,
    val valueText: String
)

enum class EmptyValuePolicy(val v: Int, val label: String) {
    BLANK(0, "빈칸"),
    DASH(1, "-"),
    CUSTOM(2, "커스텀");

    companion object {
        fun from(v: Int) = entries.firstOrNull { it.v == v } ?: BLANK
    }
}

enum class WatermarkGridPreset(val v: Int, val label: String, val rows: Int, val cols: Int) {
    G2X3(0, "2 x 3 (기본)", 2, 3),
    G2X4(1, "2 x 4", 2, 4),
    G3X3(2, "3 x 3", 3, 3);

    val totalCells: Int get() = rows * cols

    companion object {
        fun from(v: Int) = entries.firstOrNull { it.v == v } ?: G2X3
    }
}

enum class WatermarkTemplatePreset(val v: Int, val label: String) {
    BASIC(0, "기본"),
    FIELD(1, "현장"),
    META(2, "메타");

    companion object {
        fun from(v: Int) = entries.firstOrNull { it.v == v } ?: BASIC
    }
}
