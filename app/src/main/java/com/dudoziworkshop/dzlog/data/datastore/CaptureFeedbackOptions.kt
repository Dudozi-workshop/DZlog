package com.dudoziworkshop.dzlog.data.datastore

/** Stable persisted values: never reorder or reuse an ID. */
enum class CaptureSoundChoice(val storedId: Int, val label: String) {
    SYSTEM(0, "Android 기본 셔터"),
    SOFT_CONTAREX(1, "소프트 Contarex"),
    CLASSIC_WERRA(2, "클래식 Werra"),
    DIGITAL_DSLR(3, "디지털 DSLR"),
    PPUPPYU(4, "뿌쀼!");

    // Every shipped choice now has a verified, packaged audio source.
    val available: Boolean get() = true

    // Preserve all original audio bytes; compensate for signature WAV's
    // stronger measured RMS (-10 dBFS vs. roughly -29 to -33 dBFS shutter WAVs).
    // This is an initial level trim, pending perceived-loudness device QA.
    val playbackGain: Float get() = if (this == PPUPPYU) 0.10f else 1f

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
