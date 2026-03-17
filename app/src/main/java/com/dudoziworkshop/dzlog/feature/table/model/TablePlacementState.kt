package com.dudoziworkshop.dzlog.feature.table.model

import com.dudoziworkshop.dzlog.domain.model.CaptureAspect

data class TablePlacementState(
    val wmOffsetXRatio: Int = 0,
    val wmOffsetYRatio: Int = 0,
    val wmWidthRatio: Int = 40,
    val wmHeightRatio: Int = 20,
    val keepAspectRatio: Boolean = false,
    val rotationCwDeg: Int = 0,
    val captureAspect: CaptureAspect = CaptureAspect.R3_4,
)
