package com.example.dzlog.watermark

import android.graphics.Bitmap
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell

interface WatermarkRenderer {
    fun renderTable(
        originalBmp: Bitmap,
        cells: List<WatermarkCell>,
        rows: Int,
        cols: Int,
        anchor: WatermarkTableAnchor,
        offsetXRatio: Int,
        offsetYRatio: Int,
        tableHeightRatio: Int,
        tableWidthRatio: Int,
        bgAlpha: Int,
        valueScale: Int,
        textColorMode: Int,
        manualTextColor: Int,
        textAlign: Int,
        rowWeights: List<Float>? = null,
        colWeights: List<Float>? = null,
        bgStyle: Int = 0
    ): Bitmap
}
