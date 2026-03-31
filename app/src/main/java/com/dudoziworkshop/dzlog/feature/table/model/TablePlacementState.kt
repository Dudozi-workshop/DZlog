package com.dudoziworkshop.dzlog.feature.table.model

import com.dudoziworkshop.dzlog.domain.model.CaptureAspect

data class TablePlacementState(
    val wmOffsetXRatio: Int = 0,
    val wmOffsetYRatio: Int = 0,
    val wmWidthRatio: Int = 40,
    // Legacy compatibility field: table placement now uses single scale axis,
    // so this value is mirrored from wmWidthRatio in load/save/apply flows.
    val wmHeightRatio: Int = 20,
    val keepAspectRatio: Boolean = false,
    val rotationCwDeg: Int = 0,
    val captureAspect: CaptureAspect = CaptureAspect.R3_4,
)
