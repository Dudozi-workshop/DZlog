package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.preview.PreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildPreview
import com.dudoziworkshop.dzlog.feature.table.policy.quickCellLinks
import com.dudoziworkshop.dzlog.feature.table.policy.quickDateFormats
import com.dudoziworkshop.dzlog.feature.table.policy.quickEditableCells
import com.dudoziworkshop.dzlog.feature.table.policy.quickSelectableCells
import com.dudoziworkshop.dzlog.feature.table.policy.selectQuickPhraseSet
import com.dudoziworkshop.dzlog.feature.table.policy.updateQuickDateFormat
import com.dudoziworkshop.dzlog.feature.table.policy.updateQuickCellValues
import com.dudoziworkshop.dzlog.ui.common.input.decimalInputOrPrevious
import com.dudoziworkshop.dzlog.ui.table.mock.tableEditorNow
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/** Match Android manual quick-edit eligibility without changing its production code. */
internal fun iosQuickValueCells(document: EditorDocument): List<TableCellState> {
    val table = document.template.templateState
    iosCaptureCellRects(table) // Never edit an ambiguous/malformed merged layout.
    return quickEditableCells(table)
}

internal fun iosQuickAutoValueCells(document: EditorDocument): List<TableCellState> {
    val table = document.template.templateState
    iosCaptureCellRects(table)
    return quickSelectableCells(table).filter {
        it.dataType == TableCellDataType.DATE ||
            it.dataType == TableCellDataType.TIME ||
            it.dataType == TableCellDataType.ROTATING_TEXT
    }
}

internal data class IosQuickValueChanges(
    val manualValues: Map<String, String>,
    val dateFormats: Map<String, String> = emptyMap(),
    val phraseSetIds: Map<String, String> = emptyMap(),
)

internal fun updateIosQuickValues(document: EditorDocument, values: Map<String, String>): EditorDocument {
    val editable = iosQuickValueCells(document).associateBy { it.cellId }
    require(values.keys.all { it in editable }) { "편집할 수 없는 셀이 포함되어 있습니다." }
    values.forEach { (id, value) ->
        if (editable.getValue(id).dataType == TableCellDataType.NUMBER) {
            require(decimalInputOrPrevious(value, "invalid") == value) { "숫자 형식을 확인해 주세요." }
        }
    }
    return document.copy(template = document.template.copy(templateState =
        updateQuickCellValues(document.template.templateState, values)))
}

/** Apply only the fields offered by quick edit; all counter, placement and save metadata stay intact. */
internal fun applyIosQuickValueChanges(
    document: EditorDocument,
    changes: IosQuickValueChanges,
): EditorDocument {
    var updated = updateIosQuickValues(document, changes.manualValues)
    val automatic = iosQuickAutoValueCells(document).associateBy { it.cellId }
    changes.dateFormats.forEach { (cellId, pattern) ->
        require(automatic[cellId]?.dataType == TableCellDataType.DATE && pattern in quickDateFormats) {
            "변경할 수 없는 날짜 형식이 포함되어 있습니다."
        }
        updated = updated.copy(template = updated.template.copy(templateState =
            updateQuickDateFormat(updated.template.templateState, cellId, pattern)))
    }
    changes.phraseSetIds.forEach { (cellId, setId) ->
        require(automatic[cellId]?.dataType == TableCellDataType.ROTATING_TEXT) {
            "변경할 수 없는 순환문구 셀이 포함되어 있습니다."
        }
        require(updated.template.templateState.phraseSets.any { it.id == setId && it.items.isNotEmpty() }) {
            "사용할 수 없는 순환문구 세트입니다."
        }
        updated = updated.copy(template = updated.template.copy(templateState =
            selectQuickPhraseSet(updated.template.templateState, cellId, setId)))
    }
    return updated
}

/** Explicit Apply/Cancel draft: typing does not mutate the camera or persisted document. */
@Composable
internal fun IosQuickValuesScreen(document: EditorDocument, onBack: () -> Unit,
    availableDocuments: List<EditorDocument> = listOf(document),
    onSwitchTemplate: ((String, IosQuickValueChanges) -> Boolean)? = null,
    onSave: (IosQuickValueChanges) -> Boolean) {
    val fields = remember(document) { runCatching { iosQuickValueCells(document) } }
    val automaticFields = remember(document) { runCatching { iosQuickAutoValueCells(document) } }
    val cells = fields.getOrDefault(emptyList())
    val automaticCells = automaticFields.getOrDefault(emptyList())
    val values = remember(document) { cells.map { it.cellId to it.rawText }.toMutableStateMap() }
    val dateFormats = remember(document) { automaticCells.filter { it.dataType == TableCellDataType.DATE }
        .map { it.cellId to (it.formatPattern.takeIf { pattern -> pattern in quickDateFormats } ?: "yyyyMMdd") }
        .toMutableStateMap() }
    val phraseSetIds = remember(document) { automaticCells.filter { it.dataType == TableCellDataType.ROTATING_TEXT }
        .mapNotNull { cell -> cell.phraseSetId?.let { cell.cellId to it } }.toMutableStateMap() }
    var expandedAutomaticId by remember(document) { mutableStateOf<String?>(null) }
    var templateExpanded by remember(document) { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    val changes = IosQuickValueChanges(values.toMap(), dateFormats.toMap(), phraseSetIds.toMap())
    val draft = runCatching { applyIosQuickValueChanges(document, changes) }.getOrNull()
    val preview = draft?.let {
        buildPreview(PreviewInput(it.template.templateState, tableEditorNow(), it.counterPadding,
            NamingFormatDefaults.DATE_FORMAT_DEFAULT, NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
            NamingFormatDefaults.FILE_NAME_DELIMITER, it.includePathInScope, it.includeFilenameInScope,
            it.saveMode, it.nextCounter, 0))
    }
    Column(Modifier.fillMaxSize().background(DDZColor.Background).safeDrawingPadding().imePadding()
        .padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("빠른 입력")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onBack) { Text("입력 취소") }
            Button(enabled = (cells.isNotEmpty() || automaticCells.isNotEmpty()) && draft != null, onClick = {
                if (!onSave(changes)) failure = "입력을 저장하지 못했습니다. 촬영 기록과 최신 문서를 확인해 주세요."
            }) { Text("입력 완료") }
        }
        failure?.let { Text(it) }
        if (availableDocuments.size > 1 && onSwitchTemplate != null) {
            Button(onClick = { templateExpanded = !templateExpanded }, modifier = Modifier.fillMaxWidth()) {
                Text("템플릿 · ${document.template.name}")
            }
            if (templateExpanded) availableDocuments.forEach { candidate ->
                OutlinedButton(
                    enabled = candidate.template.id != document.template.id,
                    onClick = {
                        if (onSwitchTemplate(candidate.template.id, changes)) {
                            templateExpanded = false
                            failure = null
                        } else {
                            failure = "템플릿을 변경하지 못했습니다. 촬영 기록과 최신 문서를 확인해 주세요."
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(candidate.template.name) }
            }
        }
        (fields.exceptionOrNull() ?: automaticFields.exceptionOrNull())?.let {
            Text(it.message ?: "표 구조를 확인해 주세요.")
        }
        if (cells.isEmpty() && automaticCells.isEmpty() && fields.isSuccess && automaticFields.isSuccess) {
            Text("빠르게 수정할 수 있는 셀이 없습니다.")
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (cell in cells) {
                val kind = if (cell.dataType == TableCellDataType.NUMBER) "숫자" else "텍스트"
                val fieldLabel = "${cell.rowIndex + 1}행 ${cell.colIndex + 1}열 · $kind"
                OutlinedTextField(value = values[cell.cellId].orEmpty(), onValueChange = { value ->
                    values[cell.cellId] = if (cell.dataType == TableCellDataType.NUMBER)
                        decimalInputOrPrevious(value, values[cell.cellId].orEmpty()) else value
                    failure = null
                }, label = { Text(fieldLabel) },
                    // iOS Compose exports the editable value but can omit the
                    // Material floating label. Preserve position/type for VoiceOver
                    // and touch-based UI selection even when the value is empty.
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = fieldLabel })
                val links = quickCellLinks(document.template.templateState, cell.cellId)
                if (links.description.isNotEmpty()) Text(links.description)
            }
            for (cell in automaticCells) {
                val fieldLabel = "${cell.rowIndex + 1}행 ${cell.colIndex + 1}열 · " + when (cell.dataType) {
                    TableCellDataType.DATE -> "날짜"
                    TableCellDataType.TIME -> "시간"
                    else -> "순환문구"
                }
                Text(fieldLabel)
                when (cell.dataType) {
                    TableCellDataType.DATE -> {
                        val selected = dateFormats[cell.cellId] ?: "yyyyMMdd"
                        Button(onClick = {
                            expandedAutomaticId = cell.cellId.takeUnless { expandedAutomaticId == it }
                        }, modifier = Modifier.fillMaxWidth()) { Text("날짜 형식 · $selected") }
                        if (expandedAutomaticId == cell.cellId) quickDateFormats.forEach { pattern ->
                            OutlinedButton(onClick = {
                                dateFormats[cell.cellId] = pattern
                                expandedAutomaticId = null
                                failure = null
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(when (pattern) {
                                    "yyMMdd" -> "$pattern · 짧은 연월일"
                                    "MMdd" -> "$pattern · 월일"
                                    else -> "$pattern · 연월일"
                                })
                            }
                        }
                    }
                    TableCellDataType.TIME -> Text("촬영 시각으로 자동 입력돼요.")
                    TableCellDataType.ROTATING_TEXT -> {
                        val sets = document.template.templateState.phraseSets.filter { it.items.isNotEmpty() }
                        val selected = sets.firstOrNull { it.id == phraseSetIds[cell.cellId] }
                        Button(enabled = sets.isNotEmpty(), onClick = {
                            expandedAutomaticId = cell.cellId.takeUnless { expandedAutomaticId == it }
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("문구 세트 · ${selected?.name ?: "세트 선택"}")
                        }
                        if (sets.isEmpty()) Text("표 상세설정에서 문구를 먼저 등록해 주세요.")
                        if (expandedAutomaticId == cell.cellId) sets.forEach { set ->
                            OutlinedButton(onClick = {
                                phraseSetIds[cell.cellId] = set.id
                                expandedAutomaticId = null
                                failure = null
                            }, modifier = Modifier.fillMaxWidth()) { Text(set.name) }
                        }
                    }
                    else -> Unit
                }
                val links = quickCellLinks(document.template.templateState, cell.cellId)
                if (links.description.isNotEmpty()) Text(links.description)
            }
            preview?.let {
                Text("파일명: ${it.previewNaming.displayName}")
                Text("저장경로: ${it.previewNaming.relativePath}")
            }
        }
    }
}
