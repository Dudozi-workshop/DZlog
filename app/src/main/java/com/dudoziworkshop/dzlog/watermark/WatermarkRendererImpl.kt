package com.dudoziworkshop.dzlog.watermark

import android.graphics.Bitmap
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell

class WatermarkRendererImpl : WatermarkRenderer {
    override fun renderTable(
        originalBmp: Bitmap,
        cells: List<WatermarkCell>,
        rows: Int,
        cols: Int,
        anchor: WatermarkTableAnchor,
        offsetXRatio: Int,
        offsetYRatio: Int,
        boundsOffsetX10000: Int,
        boundsOffsetY10000: Int,
        tableHeightRatio: Int,
        tableWidthRatio: Int,
        bgAlpha: Int,
        valueScale: Int,
        textColorMode: Int,
        manualTextColor: Int,
        textAlign: Int,
        rowWeights: List<Float>?,
        colWeights: List<Float>?,
        bgStyle: Int,
        drawGrid: Boolean,
        rotationCwDeg: Int
    ): Bitmap {
        return drawWatermarkTableFromResolvedCells(
            src = originalBmp,
            cells = cells,
            rows = rows,
            cols = cols,
            anchor = anchor,
            offsetXRatio = offsetXRatio,
            offsetYRatio = offsetYRatio,
            boundsOffsetX10000 = boundsOffsetX10000,
            boundsOffsetY10000 = boundsOffsetY10000,
            tableHeightRatio = tableHeightRatio,
            tableWidthRatio = tableWidthRatio,
            bgAlpha = bgAlpha,
            valueScale = valueScale,
            textColorMode = textColorMode,
            manualTextColor = manualTextColor,
            textAlign = textAlign,
            rowWeights = rowWeights,
            colWeights = colWeights,
            bgStyle = bgStyle,
            drawGrid = drawGrid,
            rotationCwDeg = rotationCwDeg
        )
    }
}
