package com.dudoziworkshop.dzlog.qa

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell
import com.dudoziworkshop.dzlog.watermark.WatermarkRendererImpl
import java.io.File

class PhotoActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val words = assets.open("cells.txt").bufferedReader().use { it.readLines() }
        check(words.size == 9)
        val source = assets.open("source.jpg").use { BitmapFactory.decodeStream(it) }
        val result = WatermarkRendererImpl().renderTable(
            originalBmp = source, cells = words.map { WatermarkCell(it) },
            templateCells = words.mapIndexed { i, text -> TableCellState(i / 3, i % 3, rawText = text) },
            rows = 3, cols = 3, anchor = WatermarkTableAnchor.BOTTOM_RIGHT,
            offsetXRatio = 0, offsetYRatio = 0, tableWidthRatio = 75, tableHeightRatio = 30,
            bgAlpha = 255, valueScale = 100, textColorMode = 0, manualTextColor = 0, textAlign = 1,
        )
        val tmp = File(filesDir, "result.tmp")
        tmp.outputStream().use { check(result.compress(Bitmap.CompressFormat.JPEG, 94, it)) }
        check(tmp.renameTo(File(filesDir, "android.jpg")))
        finish()
    }
}
