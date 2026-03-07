package com.dudoziworkshop.dzlog.domain.model

/**
 * 하드웨어 음량키를 앱 기능에 매핑하는 정책.
 */
enum class VolumeKeyAction(val v: Int, val label: String) {
    NONE(0, "없음"),
    CAPTURE(1, "촬영"),
    ZOOM(2, "배율");

    companion object {
        fun from(v: Int): VolumeKeyAction = entries.firstOrNull { it.v == v } ?: NONE
    }
}
