package com.example.dzlog.ui.table.watermark

import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.WatermarkTableAnchor

data class TableWatermarkUiState(
    val wmAnchor: WatermarkTableAnchor = WatermarkTableAnchor.BOTTOM_RIGHT,
    val wmWidthRatio: Int = 40,
    val wmHeightRatio: Int = 20,
    val wmBgStyle: Int = 0,
    val wmBgAlpha: Int = 80,
    val wmValueScale: Int = 100,
    val captureAspect: CaptureAspect = CaptureAspect.R3_4
)
