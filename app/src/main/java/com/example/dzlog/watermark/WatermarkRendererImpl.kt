package com.example.dzlog.watermark

import android.graphics.Bitmap
import com.example.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell
import com.example.dzlog.domain.model.WatermarkTableAnchor

class WatermarkRendererImpl : WatermarkRenderer {
    override fun renderTable(
        originalBmp: Bitmap,
        cells: List<WatermarkCell>,
        rows: Int,
        cols: Int,
        showLabel: Boolean,
        anchor: WatermarkTableAnchor,
        offsetXRatio: Int,
        offsetYRatio: Int,
        tableHeightRatio: Int,
        tableWidthRatio: Int,
        bgAlpha: Int,
        labelScale: Int,
        valueScale: Int
    ): Bitmap {
        return drawWatermarkTableFromResolvedCells(
            src = originalBmp,
            cells = cells,
            rows = rows,
            cols = cols,
            showLabel = showLabel,
            anchor = anchor,
            offsetXRatio = offsetXRatio,
            offsetYRatio = offsetYRatio,
            tableHeightRatio = tableHeightRatio,
            tableWidthRatio = tableWidthRatio,
            bgAlpha = bgAlpha,
            labelScale = labelScale,
            valueScale = valueScale
        )
    }
}
