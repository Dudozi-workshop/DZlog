package com.dudoziworkshop.dzlog.feature.help.ui

import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.ui.camera.preview.WatermarkUiArgs
import com.dudoziworkshop.dzlog.ui.camera.preview.CameraPreviewAreaArgs
import kotlin.math.roundToInt

/** Presentation-only copies: no preferences, capture queue or table edits are changed. */
internal fun guidePreviewArgs(args: CameraPreviewAreaArgs, page: String?, progress: Float): CameraPreviewAreaArgs {
    val fraction = progress.coerceIn(0f, 1f)
    val tableDemo = page in setOf("select-table", "select", "move", "resize-button", "size", "corner-size", "rotation", "lock", "details")
    return args.copy(
        captureAspect = if (page == "ratio") guideAspect(progress) else args.captureAspect,
        saveMode = if (page == "quality") guideSaveMode(progress) else args.saveMode,
        showGrid = if (page == "grid") fraction >= 0.5f else args.showGrid,
        showWmPreview = if (page == "table") fraction >= 0.5f else tableDemo || args.showWmPreview,
        zoomRatioTenths = if (page == "zoom") (10 + (args.maxZoomTenths.coerceIn(10, 40) - 10) * fraction).roundToInt() else args.zoomRatioTenths,
        watermarkUi = guideWatermarkArgs(args.watermarkUi, page, fraction),
    )
}

internal fun guideWatermarkArgs(wm: WatermarkUiArgs, page: String?, progress: Float): WatermarkUiArgs {
    if (page !in setOf("select-table", "select", "move", "resize-button", "size", "corner-size", "rotation", "lock", "details")) return wm
    val fraction = progress.coerceIn(0f, 1f)
    return wm.copy(
        anchor = WatermarkTableAnchor.CUSTOM,
        tableWidthRatio = if (page in setOf("size", "corner-size")) (35 + 20 * fraction).roundToInt() else 40,
        tableHeightRatio = if (page in setOf("size", "corner-size")) (18 + 10 * fraction).roundToInt() else 20,
        boundsOffsetX10000 = if (page == "move") (1500 + 3500 * fraction).roundToInt() else 3500,
        boundsOffsetY10000 = if (page == "move") (6500 - 3000 * fraction).roundToInt() else 4000,
        rotationCwDeg = if (page == "lock" || (page == "rotation" && fraction >= 0.5f)) 90 else 0,
    )
}

/** Sample values are shown only inside the real quick editor; never saved. */
internal fun guideQuickTemplate(): TableTemplateState {
    val types = listOf(
        TableCellDataType.TEXT,
        TableCellDataType.NUMBER,
        TableCellDataType.DATE,
        TableCellDataType.TIME,
        TableCellDataType.ROTATING_TEXT,
        TableCellDataType.COUNTER,
    )
    return TableTemplateState(2, 3,
        types.mapIndexed { index, type -> TableCellState(
            rowIndex = index / 3, colIndex = index % 3, cellId = "guide-$index", dataType = type,
            rawText = if (index == 0) "관찰 기록" else if (index == 1) "12" else "",
            typedValue = if (index == 0) CellValue.Text("관찰 기록")
                else if (index == 1) CellValue.Number("12")
                else CellValue.Auto,
            phraseSetId = if (index == 4) "guide-set-a" else null,
        ) },
        phraseSets = listOf(
            RotatingPhraseSet("guide-set-a", "생육 단계", listOf("관찰 전", "관찰 후")),
            RotatingPhraseSet("guide-set-b", "조사 구역", listOf("A 구역", "B 구역")),
        ),
        fileNameSlotDrafts = List(FILE_NAME_SLOT_COUNT) { if (it == 0) TableEditorSlotDraft("CELL", "기록명", "guide-0") else null },
        pathSlotDrafts = List(PATH_SLOT_UI_MAX_COUNT) { if (it == 0) TableEditorSlotDraft("CELL", "구역", "guide-4") else null },
    )
}

internal fun guideCycleIndex(progress: Float, count: Int): Int =
    (progress.coerceIn(0f, 1f) * count).toInt().coerceAtMost(count - 1)
internal fun guideAspect(progress: Float) = CaptureAspect.entries[guideCycleIndex(progress, CaptureAspect.entries.size)]
internal fun guideSaveMode(progress: Float) = SaveMode.entries[guideCycleIndex(progress, SaveMode.entries.size)]
internal fun guidePreviewMode(progress: Float) = ContinuousPreviewMode.entries[guideCycleIndex(progress, 3)]
internal fun guidePreviewVisible(progress: Float): Boolean = when (guidePreviewMode(progress)) {
    ContinuousPreviewMode.OFF -> false
    ContinuousPreviewMode.SHORT -> progress < 0.56f
    ContinuousPreviewMode.HOLD -> true
}
internal fun guidePendingCount(progress: Float) = if (progress < 0.5f) 2 else 1
