package com.dudoziworkshop.dzlog.data.datastore

enum class CaptureVibrationStrength(val storedId: Int, val label: String) {
    OFF(0, "OFF"),
    LOW(1, "약함"),
    MEDIUM(2, "보통"),
    HIGH(3, "강함");

    companion object {
        fun fromStored(value: Int) = entries.firstOrNull { it.storedId == value } ?: MEDIUM
    }
}
