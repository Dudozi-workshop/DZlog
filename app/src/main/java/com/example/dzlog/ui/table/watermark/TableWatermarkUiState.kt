package com.example.dzlog.ui.table.watermark

import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.WatermarkManualTextColor
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.model.WatermarkTextAlign
import com.example.dzlog.domain.model.WatermarkTextColorMode

data class TableWatermarkUiState(
    val wmAnchor: WatermarkTableAnchor = WatermarkTableAnchor.BOTTOM_RIGHT,
    val wmWidthRatio: Int = 40,
    val wmHeightRatio: Int = 20,
    val wmBgStyle: Int = 0,
    val wmBgAlpha: Int = 80,
    val wmValueScale: Int = 100,
    val wmTextColorMode: Int = WatermarkTextColorMode.AUTO,
    val wmManualTextColor: Int = WatermarkManualTextColor.BLACK,
    val wmTextAlign: Int = WatermarkTextAlign.LEFT,
    val captureAspect: CaptureAspect = CaptureAspect.R3_4
)
