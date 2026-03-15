package com.dudoziworkshop.dzlog.feature.table.model

import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.WatermarkManualTextColor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextAlign
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextColorMode

data class TablePlacementState(
    val wmAnchor: WatermarkTableAnchor = WatermarkTableAnchor.BOTTOM_RIGHT,
    val wmOffsetXRatio: Int = 0,
    val wmOffsetYRatio: Int = 0,
    val wmWidthRatio: Int = 40,
    val wmHeightRatio: Int = 20,
    val wmBgStyle: Int = 0,
    val wmBgAlpha: Int = 80,
    val wmValueScale: Int = 100,
    val wmTextColorMode: Int = WatermarkTextColorMode.AUTO,
    val wmManualTextColor: Int = WatermarkManualTextColor.BLACK,
    val wmTextAlign: Int = WatermarkTextAlign.LEFT,
    val wmGridEnabled: Boolean = true,
    val captureAspect: CaptureAspect = CaptureAspect.R3_4
)

typealias TableWatermarkUiState = TablePlacementState
