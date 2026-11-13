package com.dudoziworkshop.dzlog.data.template

/**
 * Persisted placement independent of Android DataStore.
 * A null placement in EditorDocument means that the user has not configured it.
 */
enum class EditorCaptureAnchor {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CUSTOM,
}

data class EditorCapturePlacement(
    val anchor: EditorCaptureAnchor,
    val offsetXPercent: Int,
    val offsetYPercent: Int,
    val widthRatio: Int,
    val heightRatio: Int,
    val rotationCwDeg: Int = 0,
) {
    init {
        require(offsetXPercent in 0..100 && offsetYPercent in 0..100) {
            "표 위치 비율이 올바르지 않습니다."
        }
        require(widthRatio in 10..100 && heightRatio in 10..100) {
            "표 크기 비율이 올바르지 않습니다."
        }
        require(rotationCwDeg in 0..359) {
            "표 회전 각도가 올바르지 않습니다."
        }
    }
}
