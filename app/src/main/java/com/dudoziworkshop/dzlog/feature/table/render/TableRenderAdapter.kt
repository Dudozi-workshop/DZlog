package com.dudoziworkshop.dzlog.feature.table.render

import android.graphics.Canvas
import android.graphics.RectF
import com.dudoziworkshop.dzlog.domain.model.WatermarkManualTextColor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextAlign
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextColorMode
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import com.dudoziworkshop.dzlog.watermark.drawWatermarkTableOnCanvas

data class TableRenderPayload(
    val rows: Int,
    val cols: Int,
    val rowWeights: List<Float>?,
    val colWeights: List<Float>?,
    val cells: List<WatermarkBuilder.WatermarkCell>,
)

data class TableRenderStyle(
    val bgStyle: Int = 0,
    val bgAlpha: Int = 210,
    val valueScale: Int = 100,
    val textColorMode: Int = WatermarkTextColorMode.AUTO,
    val manualTextColor: Int = WatermarkManualTextColor.BLACK,
    val textAlign: Int = WatermarkTextAlign.LEFT,
    val drawGrid: Boolean = true,
)

data class TableRenderPlacement(
    val anchor: WatermarkTableAnchor = WatermarkTableAnchor.TOP_LEFT,
    val offsetXRatio: Int = 0,
    val offsetYRatio: Int = 0,
    val tableWidthRatio: Int = TableLayoutCalculator.DEFAULT_WIDTH_RATIO,
    val tableHeightRatio: Int = TableLayoutCalculator.DEFAULT_HEIGHT_RATIO,
    val rotationCwDeg: Int = 0,
    val overrideOffsetLeftPx: Float? = null,
    val overrideOffsetTopPx: Float? = null,
)

object TableRenderAdapter {
    fun draw(
        canvas: Canvas,
        bounds: RectF,
        payload: TableRenderPayload,
        style: TableRenderStyle,
        placement: TableRenderPlacement,
    ) {
        drawWatermarkTableOnCanvas(
            canvas = canvas,
            bounds = bounds,
            cells = payload.cells,
            rows = payload.rows.coerceAtLeast(1),
            cols = payload.cols.coerceAtLeast(1),
            anchor = placement.anchor,
            offsetXRatio = placement.offsetXRatio.coerceIn(0, 100),
            offsetYRatio = placement.offsetYRatio.coerceIn(0, 100),
            tableHeightRatio = placement.tableHeightRatio.coerceIn(10, 100),
            tableWidthRatio = placement.tableWidthRatio.coerceIn(10, 100),
            bgAlpha = style.bgAlpha.coerceIn(0, 255),
            bgStyle = style.bgStyle.coerceIn(0, 2),
            valueScale = TableScaleCalculator.clampValueScale(style.valueScale),
            textColorMode = style.textColorMode,
            manualTextColor = style.manualTextColor,
            textAlign = style.textAlign,
            drawGrid = style.drawGrid,
            rowWeights = payload.rowWeights,
            colWeights = payload.colWeights,
            overrideOffsetLeftPx = placement.overrideOffsetLeftPx,
            overrideOffsetTopPx = placement.overrideOffsetTopPx,
            rotationCwDeg = placement.rotationCwDeg,
        )
    }
}
