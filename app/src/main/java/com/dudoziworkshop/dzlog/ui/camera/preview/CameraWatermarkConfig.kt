package com.dudoziworkshop.dzlog.ui.camera.preview

import com.dudoziworkshop.dzlog.domain.model.WatermarkConfig
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor

internal fun buildWatermarkConfig(
    anchor: WatermarkTableAnchor,
    offsetXRatio: Int,
    offsetYRatio: Int,
    boundsOffsetX10000: Int,
    boundsOffsetY10000: Int,
    tableWidthRatio: Int,
    tableHeightRatio: Int,
    tableBgAlpha: Int,
    bgStyle: Int,
    valueScale: Int,
    textColorMode: Int,
    manualTextColor: Int,
    textAlign: Int,
    gridEnabled: Boolean,
    rotationCwDeg: Int
): WatermarkConfig {
    return WatermarkConfig(
        anchor = anchor,
        offsetXRatio = offsetXRatio,
        offsetYRatio = offsetYRatio,
        boundsOffsetX10000 = boundsOffsetX10000.coerceIn(0, 10000),
        boundsOffsetY10000 = boundsOffsetY10000.coerceIn(0, 10000),
        tableWidthRatio = tableWidthRatio,
        tableHeightRatio = tableHeightRatio,
        tableBgAlpha = tableBgAlpha,
        bgStyle = bgStyle,
        valueScale = valueScale,
        textColorMode = textColorMode,
        manualTextColor = manualTextColor,
        textAlign = textAlign,
        gridEnabled = gridEnabled,
        rotationCwDeg = if (rotationCwDeg == 90) 90 else 0
    )
}
