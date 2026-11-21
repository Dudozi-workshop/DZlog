package com.dudoziworkshop.dzlog.qa

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.graphics.text.TextRunShaper
import android.text.TextPaint
import org.json.JSONArray
import org.json.JSONObject
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell
import com.dudoziworkshop.dzlog.watermark.WatermarkRendererImpl
import java.io.File

class PhotoActivity : Activity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val source = assets.open("source.jpg").use { BitmapFactory.decodeStream(it) }
        val texts = mutableListOf("A") // The primary text face comes first in the fallback list.
        for (file in assets.list("").orEmpty().filter { it.startsWith("cells") && it.endsWith(".txt") }) {
            val table = assets.open(file).bufferedReader().use { PhotoTable(it.readText()) }
            texts.addAll(table.labels)
            render(source, table, file.removeSuffix(".txt"))
        }
        val paint = TextPaint().apply { textSize = 28f } // Android reference renderer at valueScale 100.
        val metrics = JSONObject()
        val faces = linkedMapOf<String, JSONObject>()
        for (text in texts.filter { it.isNotEmpty() }) {
            metrics.put(text, paint.measureText(text).toDouble())
            val glyphs = TextRunShaper.shapeTextRun(text, 0, text.length, 0, text.length, 0f, 0f, false, paint)
            for (index in 0 until glyphs.glyphCount()) {
                val font = glyphs.getFont(index)
                val axes = JSONObject()
                font.axes?.forEach { axes.put(it.tag, it.styleValue.toDouble()) }
                val face = JSONObject().put("path", checkNotNull(font.file).absolutePath)
                    .put("index", font.ttcIndex).put("axes", axes)
                faces[face.toString()] = face
            }
        }
        File(filesDir, "android-metrics.json").writeText(JSONObject()
            .put("lineHeight", (paint.fontMetrics.descent - paint.fontMetrics.ascent).toDouble())
            .put("widths", metrics).toString())
        val temporary = File(filesDir, "faces.tmp")
        temporary.writeText(JSONArray(faces.values.toList()).toString())
        check(temporary.renameTo(File(filesDir, "measured-faces.json")))
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
