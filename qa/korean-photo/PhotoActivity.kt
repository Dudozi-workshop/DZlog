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
        val source = assets.open("source.jpg").use { BitmapFactory.decodeStream(it) }
        for (file in assets.list("").orEmpty().filter { it.startsWith("cells") && it.endsWith(".txt") }) {
            val table = assets.open(file).bufferedReader().use { PhotoTable(it.readText()) }
            render(source, table, file.removeSuffix(".txt"))
        }
        finish()
    }

    private fun render(source: Bitmap, table: PhotoTable, name: String) {
        val result = WatermarkRendererImpl().renderTable(
            originalBmp = source, cells = table.labels.map { WatermarkCell(it) },
            templateCells = table.labels.mapIndexed { i, text -> TableCellState(i / table.cols, i % table.cols, rawText = text) },
            rows = table.rows, cols = table.cols, anchor = WatermarkTableAnchor.BOTTOM_RIGHT,
            offsetXRatio = 0, offsetYRatio = 0, tableWidthRatio = 75, tableHeightRatio = 30,
            bgAlpha = 255, valueScale = 100, textColorMode = 0, manualTextColor = 0, textAlign = 1,
        )
        val tmp = File(filesDir, "result.tmp")
        tmp.outputStream().use { check(result.compress(Bitmap.CompressFormat.JPEG, 94, it)) }
        check(tmp.renameTo(File(filesDir, "android-$name.jpg")))
    }
}
