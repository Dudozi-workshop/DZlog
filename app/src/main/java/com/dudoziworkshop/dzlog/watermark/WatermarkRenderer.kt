package com.dudoziworkshop.dzlog.watermark

import android.graphics.Bitmap
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell

interface WatermarkRenderer {
    fun renderTable(
        originalBmp: Bitmap,
        cells: List<WatermarkCell>,
        templateCells: List<TableCellState>,
        rows: Int,
        cols: Int,
        anchor: WatermarkTableAnchor,
        offsetXRatio: Int,
        offsetYRatio: Int,
        boundsOffsetX10000: Int = 0,
        boundsOffsetY10000: Int = 0,
        tableHeightRatio: Int,
        tableWidthRatio: Int,
        bgAlpha: Int,
        valueScale: Int,
        textColorMode: Int,
        manualTextColor: Int,
        textAlign: Int,
        rowWeights: List<Float>? = null,
        colWeights: List<Float>? = null,
        bgStyle: Int = 0,
        drawGrid: Boolean = true,
        rotationCwDeg: Int = 0
    ): Bitmap
}
