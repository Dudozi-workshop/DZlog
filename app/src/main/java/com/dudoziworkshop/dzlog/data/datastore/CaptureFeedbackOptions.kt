package com.dudoziworkshop.dzlog.data.datastore

/** Stable persisted values: never reorder or reuse an ID. */
enum class CaptureSoundChoice(val storedId: Int, val label: String) {
    SYSTEM(0, "기본 촬영음"),
    SOFT_CONTAREX(1, "소프트 Contarex"),
    CLASSIC_WERRA(2, "클래식 Werra"),
    DIGITAL_DSLR(3, "디지털 DSLR"),
    PPUPPYU(4, "뿌쀼!");

    // Every shipped choice now has a verified, packaged audio source.
    val available: Boolean get() = true

    // Existing original audio assets remain byte-for-byte unchanged.
    // First-pass loudness trims target a quiet, similar perceived volume:
    // reference shutter WAVs were roughly -29..-33 dBFS RMS vs. signature -10.
    // These are playback gains, not hard-coded system media-stream volume.
    // Physical device listening QA is required for final perceptual matching.
    val playbackGain: Float get() = when (this) {
        SYSTEM -> 1.00f // New 160ms default WAV: approximately -36 dBFS RMS.
        SOFT_CONTAREX -> 0.50f
        CLASSIC_WERRA -> 0.50f
        DIGITAL_DSLR -> 0.50f
        PPUPPYU -> 0.06f
    }

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
