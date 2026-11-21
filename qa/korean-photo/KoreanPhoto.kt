@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.dudoziworkshop.dzlog.poc

import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.preview.*
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.ui.table.mock.tableEditorNow
import com.dudoziworkshop.dzlog.qa.PhotoTable
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.UIKit.UIFont

fun writeKoreanPhoto(source: String, labels: String, output: String, probeFont: UIFont?) {
    val fixture = PhotoTable(labels)
    val table = TableTemplateState(fixture.rows, fixture.cols, fixture.labels.mapIndexed { i, text ->
        TableCellState(i / fixture.cols, i % fixture.cols, rawText = text, cellId = "cell-$i")
    })
    val now = tableEditorNow()
    val date = NamingFormatDefaults.DATE_FORMAT_DEFAULT
    val time = NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT
    val delim = NamingFormatDefaults.FILE_NAME_DELIMITER
    val mode = SaveMode.WATERMARK_ONLY
    val scope = buildScope(CaptureScopeInput(table, now, 3, date, time, delim, false, mode, 0))
    val preview = buildCapturePreview(scope, FinalCapturePreviewInput(table, now, 3, date, time, delim, false, false, mode, 12, 0))
    val overlay = IosTableOverlayRenderer.renderAndroidAnchoredPng(
        800, 600, table, preview, TableStyleState(bgAlpha = 255, textAlign = 1), IosTableAnchor.BOTTOM_RIGHT, 0, 0, 75, 30, probeFont = probeFont)
    val photo = checkNotNull(NSData.dataWithContentsOfFile(source))
    val bytes = IosCaptureResultRenderer.compositeJpeg(checkNotNull(photo.bytes).readBytes(photo.length.toInt()), overlay)
    bytes.usePinned {
        check(NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()).writeToFile(output, atomically = true))
    }
}
