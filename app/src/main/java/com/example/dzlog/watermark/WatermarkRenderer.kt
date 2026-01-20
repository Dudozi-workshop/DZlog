package com.example.dzlog.watermark

import android.graphics.Bitmap
import com.example.dzlog.domain.model.ResolvedCell
import com.example.dzlog.domain.model.WatermarkTableAnchor

interface WatermarkRenderer {
    fun renderTable(
        originalBmp: Bitmap,
        cells: List<ResolvedCell>,
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
    ): Bitmap
}
