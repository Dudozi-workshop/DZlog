package com.dudoziworkshop.dzlog.data.datastore

/** Stable persisted values: never reorder or reuse an ID. */
enum class CaptureSoundChoice(val storedId: Int, val label: String) {
    SYSTEM(0, "Android 기본 셔터"),
    SOFT_CONTAREX(1, "소프트 Contarex"),
    CLASSIC_WERRA(2, "클래식 Werra"),
    DIGITAL_DSLR(3, "디지털 DSLR"),
    PPUPPYU(4, "뿌쀼!");

    // Approved signature WAV must be recovered unchanged before release.
    val available: Boolean get() = this != PPUPPYU

    companion object {
        fun fromStored(value: Int) = entries.firstOrNull { it.storedId == value } ?: SYSTEM
    }
}

enum class CaptureVibrationStrength(val storedId: Int, val label: String) {
    OFF(0, "OFF"),
    LOW(1, "약함"),
    MEDIUM(2, "보통"),
    HIGH(3, "강함");

    companion object {
        fun fromStored(value: Int) = entries.firstOrNull { it.storedId == value } ?: MEDIUM
    }
}
