package com.dudoziworkshop.dzlog.poc

import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.preview.*
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.ui.table.mock.tableEditorNow
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image

/** Called only by the explicit XCTest fixture; validates actual raster pixels and rejection paths. */
internal fun checkIosMergedCaptureRaster() {
    fun cell(r: Int, c: Int, rs: Int = 1, cs: Int = 1, text: String = "") =
        TableCellState(r, c, cellId = "cell-$r-$c", rowSpan = rs, colSpan = cs, rawText = text)
    fun render(table: TableTemplateState): ByteArray {
        val now = tableEditorNow()
        val scope = buildScope(CaptureScopeInput(table, now, 3,
            NamingFormatDefaults.DATE_FORMAT_DEFAULT, NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
            NamingFormatDefaults.FILE_NAME_DELIMITER, false, SaveMode.WATERMARK_ONLY, 0))
        val preview = buildCapturePreview(scope, FinalCapturePreviewInput(table, now, 3,
            NamingFormatDefaults.DATE_FORMAT_DEFAULT, NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
            NamingFormatDefaults.FILE_NAME_DELIMITER, false, false, SaveMode.WATERMARK_ONLY, 12, 0))
        return IosTableOverlayRenderer.renderResolvedPreviewPng(320, 320, table, preview,
            TableStyleState(bgStyle = 2), widthRatio = 100, heightRatio = 40)
    }
    val horizontal = TableTemplateState(2, 2, listOf(cell(0, 0, cs = 2),
        cell(0, 1, text = "HIDDEN"), cell(1, 0), cell(1, 1)))
    val bytes = render(horizontal)
    val bitmap = Bitmap.makeFromImage(Image.makeFromEncoded(bytes))
    check(bitmap.getColor(160, 20) ushr 24 == 0) { "가로 병합 내부에 구분선이 남았습니다." }
    check(bitmap.getColor(160, 100) ushr 24 > 0) { "일반 셀의 구분선이 사라졌습니다." }
    check(bitmap.getColor(80, 64) ushr 24 > 0) { "병합 셀 외곽선이 사라졌습니다." }
    check(bytes.contentEquals(render(horizontal.copy(cells = horizontal.cells.map {
        if (it.cellId == "cell-0-1") it.copy(rawText = "DIFFERENT COVERED VALUE") else it
    })))) { "가려진 셀의 값이 결과 사진에 나타납니다." }
    val sparse = horizontal.copy(cells = horizontal.cells.filterNot { it.cellId == "cell-0-1" })
    check(bytes.contentEquals(render(sparse))) { "가려진 셀 보존 여부에 따라 병합 결과가 달라집니다." }
    val weighted = horizontal.copy(rowWeights = listOf(1f, 3f), colWeights = listOf(1f, 3f))
    val weightedBitmap = Bitmap.makeFromImage(Image.makeFromEncoded(render(weighted)))
    check(weightedBitmap.getColor(80, 16) ushr 24 == 0)
    check(weightedBitmap.getColor(80, 80) ushr 24 > 0)
    val vertical = TableTemplateState(2, 2, listOf(cell(0, 0, rs = 2), cell(0, 1),
        cell(1, 0, text = "HIDDEN"), cell(1, 1)))
    val verticalBitmap = Bitmap.makeFromImage(Image.makeFromEncoded(render(vertical)))
    check(verticalBitmap.getColor(80, 64) ushr 24 == 0) { "세로 병합 내부에 구분선이 남았습니다." }
    check(verticalBitmap.getColor(240, 64) ushr 24 > 0)
    val invalid = listOf(
        horizontal.copy(cells = listOf(cell(0, 0, cs = 3))),
        horizontal.copy(cells = listOf(cell(0, 0, rs = 0))),
        horizontal.copy(cells = horizontal.cells + cell(1, 1)),
        horizontal.copy(cells = listOf(cell(0, 0), cell(0, 1, rs = 2), cell(1, 0, cs = 2))),
        horizontal.copy(cells = listOf(cell(0, 0, cs = 2))),
    )
    invalid.forEach { table ->
        check(runCatching { render(table) }.exceptionOrNull() is IllegalArgumentException) {
            "잘못된 병합 구조가 차단되지 않았습니다."
        }
    }
}
