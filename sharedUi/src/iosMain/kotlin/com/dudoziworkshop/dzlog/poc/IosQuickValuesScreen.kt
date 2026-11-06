package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.preview.PreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildPreview
import com.dudoziworkshop.dzlog.domain.table.resolveTableRootCells
import com.dudoziworkshop.dzlog.feature.table.policy.quickCellLinks
import com.dudoziworkshop.dzlog.ui.common.input.decimalInputOrPrevious
import com.dudoziworkshop.dzlog.ui.table.mock.tableEditorNow
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/** Match Android manual quick-edit eligibility without changing its production code. */
internal fun iosQuickValueCells(document: EditorDocument): List<TableCellState> {
    val table = document.template.templateState
    iosCaptureCellRects(table) // Never edit an ambiguous/malformed merged layout.
    return resolveTableRootCells(table.cells).filter {
        it.quickEditable && (it.dataType == TableCellDataType.TEXT || it.dataType == TableCellDataType.NUMBER)
    }
}

internal fun updateIosQuickValues(document: EditorDocument, values: Map<String, String>): EditorDocument {
    val editable = iosQuickValueCells(document).associateBy { it.cellId }
    require(values.keys.all { it in editable }) { "편집할 수 없는 셀이 포함되어 있습니다." }
    values.forEach { (id, value) ->
        if (editable.getValue(id).dataType == TableCellDataType.NUMBER) {
            require(decimalInputOrPrevious(value, "invalid") == value) { "숫자 형식을 확인해 주세요." }
        }
    }
    val table = document.template.templateState
    return document.copy(template = document.template.copy(templateState = table.copy(cells = table.cells.map { cell ->
        val value = values[cell.cellId]
        when {
            value == null || value == cell.rawText -> cell
            cell.dataType == TableCellDataType.TEXT -> cell.copy(rawText = value, typedValue = CellValue.Text(value))
            else -> cell.copy(rawText = value, typedValue = CellValue.Number(value))
        }
    })))
}

/** Explicit Apply/Cancel draft: typing does not mutate the camera or persisted document. */
@Composable
internal fun IosQuickValuesScreen(document: EditorDocument, onBack: () -> Unit,
    onSave: (Map<String, String>) -> Boolean) {
    val fields = remember(document) { runCatching { iosQuickValueCells(document) } }
    val cells = fields.getOrDefault(emptyList())
    val values = remember(document) { cells.map { it.cellId to it.rawText }.toMutableStateMap() }
    var failure by remember { mutableStateOf<String?>(null) }
    val draft = runCatching { updateIosQuickValues(document, values.toMap()) }.getOrNull()
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
            Button(enabled = cells.isNotEmpty() && draft != null, onClick = {
                if (!onSave(values.toMap())) failure = "입력을 저장하지 못했습니다. 촬영 기록과 최신 문서를 확인해 주세요."
            }) { Text("입력 완료") }
        }
        failure?.let { Text(it) }
        fields.exceptionOrNull()?.let { Text(it.message ?: "표 구조를 확인해 주세요.") }
        if (cells.isEmpty() && fields.isSuccess) Text("빠르게 입력할 텍스트·숫자 셀이 없습니다.")
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (cell in cells) {
                val kind = if (cell.dataType == TableCellDataType.NUMBER) "숫자" else "텍스트"
                OutlinedTextField(value = values[cell.cellId].orEmpty(), onValueChange = { value ->
                    values[cell.cellId] = if (cell.dataType == TableCellDataType.NUMBER)
                        decimalInputOrPrevious(value, values[cell.cellId].orEmpty()) else value
                    failure = null
                }, label = { Text("${cell.rowIndex + 1}행 ${cell.colIndex + 1}열 · $kind") },
                    modifier = Modifier.fillMaxWidth())
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
