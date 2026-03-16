package com.dudoziworkshop.dzlog.feature.table.render

import android.graphics.Canvas
import android.graphics.RectF
import com.dudoziworkshop.dzlog.domain.model.WatermarkManualTextColor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextAlign
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextColorMode
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import com.dudoziworkshop.dzlog.watermark.drawWatermarkTableOnCanvas

/**
 * 표 자체 속성(구조 + 내부 분배 + 셀 값) payload.
 * Design Preview / Camera Preview 모두에서 공통으로 사용한다.
 */
data class TableRenderPayload(
    val rows: Int,
    val cols: Int,
    val rowWeights: List<Float>?,
    val colWeights: List<Float>?,
    val cells: List<WatermarkBuilder.WatermarkCell>,
    val placeholderCellIndexes: Set<Int> = emptySet(),
)

/**
 * 표 자체 서식 속성.
 * Design Preview / Camera Preview 공통.
 */
data class TableRenderStyle(
    val bgStyle: Int = 0,
    val bgAlpha: Int = 210,
    val valueScale: Int = 100,
    val textColorMode: Int = WatermarkTextColorMode.AUTO,
    val manualTextColor: Int = WatermarkManualTextColor.BLACK,
    val textAlign: Int = WatermarkTextAlign.LEFT,
    val drawGrid: Boolean = true,
    val placeholderTextColorArgb: Int? = null,
)

/**
 * 촬영 배치 속성(위치/회전/offset override).
 * Camera Preview 문맥에서 의미가 있고, Design Preview에서는 중립값을 사용한다.
 */
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


fun buildDesignPreviewPlacement(
    tableWidthRatio: Int,
    tableHeightRatio: Int,
): TableRenderPlacement = TableRenderPlacement(
    anchor = WatermarkTableAnchor.CUSTOM,
    offsetXRatio = 50,
    offsetYRatio = 50,
    tableWidthRatio = tableWidthRatio,
    tableHeightRatio = tableHeightRatio,
    rotationCwDeg = 0,
)

fun buildCameraPreviewPlacement(
    anchor: WatermarkTableAnchor,
    offsetXRatio: Int,
    offsetYRatio: Int,
    tableWidthRatio: Int,
    tableHeightRatio: Int,
    rotationCwDeg: Int = 0,
    overrideOffsetLeftPx: Float? = null,
    overrideOffsetTopPx: Float? = null,
): TableRenderPlacement = TableRenderPlacement(
    anchor = anchor,
    offsetXRatio = offsetXRatio,
    offsetYRatio = offsetYRatio,
    tableWidthRatio = tableWidthRatio,
    tableHeightRatio = tableHeightRatio,
    rotationCwDeg = rotationCwDeg,
    overrideOffsetLeftPx = overrideOffsetLeftPx,
    overrideOffsetTopPx = overrideOffsetTopPx,
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
            placeholderCellIndexes = payload.placeholderCellIndexes,
            placeholderTextColorArgb = style.placeholderTextColorArgb,
        )
    }
}
