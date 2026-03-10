package com.dudoziworkshop.dzlog.ui.table.naming

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.deriveLegacyFileNameSlotsFromDrafts
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.ui.table.section.FileNameFormatType
import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotKind
import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotUiItem
import com.dudoziworkshop.dzlog.ui.table.section.PathFormatType
import com.dudoziworkshop.dzlog.ui.table.section.PathSlotKind
import com.dudoziworkshop.dzlog.ui.table.section.PathSlotUiItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NamingSlotDisplayItem(
    val structureLabel: String,
    val resolvedValue: String,
    val isEmpty: Boolean
)

data class TableEditorNamingUiModel(
    val filenamePreview: String,
    val savePathPreview: String,
    val fileNameSlotDisplays: List<NamingSlotDisplayItem>,
    val pathSlotDisplays: List<NamingSlotDisplayItem>,
    val fileNameSlotsForCounterScope: List<String?>
)

private fun toFileNameSlotDomainDraft(slot: FileNameSlotUiItem?): TableEditorSlotDraft? {
    if (slot == null) return null
    return TableEditorSlotDraft(
        kind = slot.kind.name,
        label = slot.label,
        cellId = slot.cellId,
        manualText = slot.manualText,
        formatType = slot.formatType?.name
    )
}

fun deriveLegacyFileNameSlotsFromUiSlots(slots: List<FileNameSlotUiItem?>): List<String?> {
    return deriveLegacyFileNameSlotsFromDrafts(slots.map(::toFileNameSlotDomainDraft))
}

// 주요 정책: naming 해석(상단 프리뷰/슬롯 카드/counter-scope용 파생값)은 이 함수 단일 경로를 SSOT로 사용한다.
fun buildTableEditorNamingUiModel(
    templateState: TableTemplateState,
    fileNameSlots: List<FileNameSlotUiItem?>,
    pathSlots: List<PathSlotUiItem?>,
    resolvedByCellId: Map<String, String>,
    previewNow: Date,
    dateFormat: String,
    timeFormat: String,
    previewCounterDigits: Int,
    counterValue: Int
): TableEditorNamingUiModel {
    val normalizedFileNameSlots = List(3) { idx -> fileNameSlots.getOrNull(idx) }
    val normalizedPathSlots = List(2) { idx -> pathSlots.getOrNull(idx) }

    val fileNameSlotDisplays = normalizedFileNameSlots.map { slot ->
        NamingSlotDisplayItem(
            structureLabel = fileNameStructureLabel(slot, templateState),
            resolvedValue = resolveFileNameSlotValue(
                slot = slot,
                templateState = templateState,
                resolvedByCellId = resolvedByCellId,
                previewNow = previewNow,
                dateFormat = dateFormat,
                timeFormat = timeFormat,
                previewCounterDigits = previewCounterDigits,
                counterValue = counterValue
            ),
            isEmpty = slot == null
        )
    }

    val pathSlotDisplays = normalizedPathSlots.map { slot ->
        NamingSlotDisplayItem(
            structureLabel = pathStructureLabel(slot, templateState),
            resolvedValue = resolvePathSlotValue(
                slot = slot,
                templateState = templateState,
                resolvedByCellId = resolvedByCellId,
                previewNow = previewNow,
                dateFormat = dateFormat,
                timeFormat = timeFormat
            ),
            isEmpty = slot == null
        )
    }

    val filenameTokens = fileNameSlotDisplays
        .map { it.resolvedValue.trim() }
        .filter { it.isNotBlank() }
    val filenameBase = if (filenameTokens.isEmpty()) "DZlog" else filenameTokens.joinToString(separator = NamingFormatDefaults.FILE_NAME_DELIMITER)
    val filenamePreview = if (filenameBase.endsWith(".jpg", ignoreCase = true) || filenameBase.endsWith(".jpeg", ignoreCase = true)) {
        filenameBase
    } else {
        "$filenameBase.jpg"
    }

    val pathTokens = pathSlotDisplays
        .map { it.resolvedValue.trim().replace("/", "_") }
        .filter { it.isNotBlank() }
    val savePathPreview = if (pathTokens.isEmpty()) {
        "Pictures/DZlog/"
    } else {
        "Pictures/DZlog/${pathTokens.joinToString(separator = "/")}/"
    }

    return TableEditorNamingUiModel(
        filenamePreview = filenamePreview,
        savePathPreview = savePathPreview,
        fileNameSlotDisplays = fileNameSlotDisplays,
        pathSlotDisplays = pathSlotDisplays,
        fileNameSlotsForCounterScope = deriveLegacyFileNameSlotsFromUiSlots(normalizedFileNameSlots)
    )
}

private fun fileNameStructureLabel(slot: FileNameSlotUiItem?, templateState: TableTemplateState): String {
    if (slot == null) return "빈 슬롯"
    return when (slot.kind) {
        FileNameSlotKind.CELL -> {
            val cell = templateState.cells.firstOrNull { it.cellId == slot.cellId }
            if (cell == null) "셀" else "셀(${cell.rowIndex + 1},${cell.colIndex + 1})"
        }
        FileNameSlotKind.MANUAL -> "직접입력"
        FileNameSlotKind.FORMAT -> when (slot.formatType) {
            FileNameFormatType.DATE -> "날짜"
            FileNameFormatType.TIME -> "시간"
            FileNameFormatType.COUNTER -> "시스템 카운터(자동)"
            FileNameFormatType.ROTATING_TEXT -> "순환문구"
            null -> "서식"
        }
    }
}

private fun pathStructureLabel(slot: PathSlotUiItem?, templateState: TableTemplateState): String {
    if (slot == null) return "빈 슬롯"
    return when (slot.kind) {
        PathSlotKind.CELL -> {
            val cell = templateState.cells.firstOrNull { it.cellId == slot.cellId }
            if (cell == null) "셀" else "셀(${cell.rowIndex + 1},${cell.colIndex + 1})"
        }
        PathSlotKind.MANUAL -> "직접입력"
        PathSlotKind.FORMAT -> when (slot.formatType) {
            PathFormatType.DATE -> "날짜"
            PathFormatType.TIME -> "시간"
            PathFormatType.ROTATING_TEXT -> "순환문구"
            null -> "서식"
        }
    }
}

@Suppress("UNUSED_PARAMETER")
private fun resolveFileNameSlotValue(
    slot: FileNameSlotUiItem?,
    templateState: TableTemplateState,
    resolvedByCellId: Map<String, String>,
    previewNow: Date,
    dateFormat: String,
    timeFormat: String,
    previewCounterDigits: Int,
    counterValue: Int
): String {
    if (slot == null) return ""
    return when (slot.kind) {
        FileNameSlotKind.CELL -> resolvedByCellId[slot.cellId].orEmpty()
        FileNameSlotKind.MANUAL -> slot.manualText.orEmpty()
        FileNameSlotKind.FORMAT -> when (slot.formatType) {
            FileNameFormatType.DATE -> runCatching { SimpleDateFormat(dateFormat, Locale.getDefault()).format(previewNow) }.getOrElse { "" }
            FileNameFormatType.TIME -> runCatching { SimpleDateFormat(timeFormat, Locale.getDefault()).format(previewNow) }.getOrElse { "" }
            // 정책 정렬: COUNTER는 시스템이 파일명 마지막에 자동 부여하므로 슬롯 해석값에는 포함하지 않는다.
            FileNameFormatType.COUNTER -> ""
            FileNameFormatType.ROTATING_TEXT -> {
                val rotatingCell = templateState.cells.firstOrNull { it.dataType == TableCellDataType.ROTATING_TEXT }
                resolvedByCellId[rotatingCell?.cellId].orEmpty()
            }
            null -> ""
        }
    }
}

private fun resolvePathSlotValue(
    slot: PathSlotUiItem?,
    templateState: TableTemplateState,
    resolvedByCellId: Map<String, String>,
    previewNow: Date,
    dateFormat: String,
    timeFormat: String
): String {
    if (slot == null) return ""
    return when (slot.kind) {
        PathSlotKind.CELL -> resolvedByCellId[slot.cellId].orEmpty()
        PathSlotKind.MANUAL -> slot.manualText.orEmpty()
        PathSlotKind.FORMAT -> when (slot.formatType) {
            PathFormatType.DATE -> runCatching { SimpleDateFormat(dateFormat, Locale.getDefault()).format(previewNow) }.getOrElse { "" }
            PathFormatType.TIME -> runCatching { SimpleDateFormat(timeFormat, Locale.getDefault()).format(previewNow) }.getOrElse { "" }
            PathFormatType.ROTATING_TEXT -> {
                val rotatingCell = templateState.cells.firstOrNull { it.dataType == TableCellDataType.ROTATING_TEXT }
                resolvedByCellId[rotatingCell?.cellId].orEmpty()
            }
            null -> ""
        }
    }
}
