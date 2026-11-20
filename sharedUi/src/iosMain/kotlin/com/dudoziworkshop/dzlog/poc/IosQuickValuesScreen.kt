package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import com.dudoziworkshop.dzlog.feature.table.policy.QuickEditSelectionMemory
import com.dudoziworkshop.dzlog.feature.table.policy.adjacentQuickCellId
import com.dudoziworkshop.dzlog.feature.table.policy.quickCellPosition
import com.dudoziworkshop.dzlog.ui.table.template.templatePreviewCellBounds
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.dudoziworkshop.dzlog.ui.table.favorites.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
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
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgress
import com.dudoziworkshop.dzlog.feature.table.policy.QuickPhrasePeriodEdit
import com.dudoziworkshop.dzlog.feature.table.policy.applyQuickPhrasePeriodEdit
import com.dudoziworkshop.dzlog.feature.table.policy.updateQuickPhraseProgress
import com.dudoziworkshop.dzlog.ui.camera.controls.PhraseProgressControls
import com.dudoziworkshop.dzlog.feature.table.policy.selectQuickPhraseSet
import com.dudoziworkshop.dzlog.feature.table.policy.updateQuickDateFormat
import com.dudoziworkshop.dzlog.feature.table.policy.updateQuickCellValues
import com.dudoziworkshop.dzlog.ui.common.input.decimalInputOrPrevious
import com.dudoziworkshop.dzlog.ui.table.mock.tableEditorNow
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet

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

/** Replay in input order so switching away and back resets only the edited cell. */
internal sealed interface IosQuickPhraseEdit {
    val cellId: String
    fun applyTo(template: TableTemplateState): TableTemplateState
    data class Select(override val cellId: String, val setId: String) : IosQuickPhraseEdit {
        override fun applyTo(template: TableTemplateState) = selectQuickPhraseSet(template, cellId, setId)
    }
    data class Progress(override val cellId: String, val index: Int, val completed: Int) : IosQuickPhraseEdit {
        override fun applyTo(template: TableTemplateState) = updateQuickPhraseProgress(template, cellId, index, completed)
    }
    data class Period(val edit: QuickPhrasePeriodEdit, val choices: Map<String, CellPhraseProgress>) : IosQuickPhraseEdit {
        override val cellId get() = edit.cellId
        override fun applyTo(template: TableTemplateState) = requireNotNull(applyQuickPhrasePeriodEdit(template, edit, choices))
    }
}

internal data class IosQuickValueChanges(
    val manualValues: Map<String, String>,
    val dateFormats: Map<String, String> = emptyMap(),
    val phraseSetIds: Map<String, String> = emptyMap(),
    val phraseEdits: List<IosQuickPhraseEdit> = emptyList(),
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
    if (changes.phraseEdits.isNotEmpty()) {
        val table = changes.phraseEdits.fold(updated.template.templateState) { state, edit ->
            require(automatic[edit.cellId]?.dataType == TableCellDataType.ROTATING_TEXT)
            edit.applyTo(state)
        }
        updated = updated.copy(template = updated.template.copy(templateState = table))
    }
    return updated
}

/** Explicit Apply/Cancel draft: typing does not mutate the camera or persisted document. */
@Composable
internal fun IosQuickValuesScreen(document: EditorDocument, selectionMemory: QuickEditSelectionMemory, onBack: () -> Unit,
    availableDocuments: List<EditorDocument> = listOf(document),
    onSwitchTemplate: ((String, IosQuickValueChanges) -> Boolean)? = null,
    onSave: (IosQuickValueChanges) -> Boolean) {
    val favorites = LocalTextFavorites.current
    val fields = remember(document) { runCatching { iosQuickValueCells(document) } }
    val automaticFields = remember(document) { runCatching { iosQuickAutoValueCells(document) } }
    val cells = fields.getOrDefault(emptyList())
    val automaticCells = automaticFields.getOrDefault(emptyList())
    val table = document.template.templateState
    val bounds = remember(document) {
        if (fields.isSuccess) templatePreviewCellBounds(table)
        else emptyList()
    }
    val navigation = remember(bounds) { bounds.map { it.cell } }
    val ids = remember(navigation) { navigation.map { it.cellId } }
    val templateId = document.template.id
    var selectedId by remember(templateId, ids) { mutableStateOf(selectionMemory.initialSelection(templateId, ids)) }
    val selectedCell = navigation.firstOrNull { it.cellId == selectedId }
    val focus = LocalFocusManager.current
    val inputs = remember(document) {
        cells.map { it.cellId to TextFieldValue(it.rawText, TextRange(it.rawText.length)) }.toMutableStateMap()
    }
    var inputFocused by remember(document) { mutableStateOf(false) }
    var focusTarget by remember(document) { mutableStateOf<String?>(null) }
    fun select(id: String) {
        if (selectedId == id) return
        val targetInput = inputs[id]
        focusTarget = id.takeIf { inputFocused && targetInput != null }
        if (focusTarget == null) focus.clearFocus()
        if (targetInput != null) inputs[id] = TextFieldValue(targetInput.text, TextRange(targetInput.text.length))
        selectedId = id
        selectionMemory.recordSelection(templateId, id, ids)
    }
    val dateFormats = remember(document) { mutableStateMapOf<String, String>() }
    var phraseEdits by remember(document) { mutableStateOf(emptyList<IosQuickPhraseEdit>()) }
    var expandedAutomaticId by remember(document) { mutableStateOf<String?>(null) }
    var templateExpanded by remember(document) { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    val changedValues = cells.filter { inputs.getValue(it.cellId).text != it.rawText }
        .associate { it.cellId to inputs.getValue(it.cellId).text }
    val changes = IosQuickValueChanges(changedValues, dateFormats.toMap(), phraseEdits = phraseEdits)
    val draft = remember(document, changes) { runCatching { applyIosQuickValueChanges(document, changes) }.getOrNull() }
    val phraseTable = draft?.template?.templateState ?: document.template.templateState
    val preview = draft?.let {
        buildPreview(PreviewInput(it.template.templateState, tableEditorNow(), it.counterPadding,
            NamingFormatDefaults.DATE_FORMAT_DEFAULT, NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
            NamingFormatDefaults.FILE_NAME_DELIMITER, it.includePathInScope, it.includeFilenameInScope,
            it.saveMode, it.nextCounter, 0))
    }
    Column(Modifier.fillMaxSize().background(DDZColor.Background).safeDrawingPadding().imePadding()
        .padding(horizontal = DDZSpacing.screenPadding, vertical = DDZSpacing.itemGap),
        verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("취소") }
            Text("빠른 수정", style = DDZTypography.SettingLabel, color = DDZColor.TextStrong,
                modifier = Modifier.weight(1f))
            Button(enabled = selectedCell != null && draft != null, onClick = {
                if (!onSave(changes)) failure = "입력을 저장하지 못했습니다. 촬영 기록과 최신 문서를 확인해 주세요."
            }) { Text("완료") }
        }
        failure?.let { Text(it) }
        if (availableDocuments.size > 1 && onSwitchTemplate != null) {
            Button(onClick = { focus.clearFocus(); templateExpanded = !templateExpanded }, modifier = Modifier.fillMaxWidth()) {
                Text("템플릿 · ${document.template.name}")
            }
            if (templateExpanded) DDZBottomSheet(title = "템플릿 선택", onDismiss = { templateExpanded = false }) {
                Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                    availableDocuments.forEach { candidate ->
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
            }
        }
        (fields.exceptionOrNull() ?: automaticFields.exceptionOrNull())?.let {
            Text(it.message ?: "표 구조를 확인해 주세요.")
        }
        if (selectedCell == null && fields.isSuccess && automaticFields.isSuccess) {
            Text("빠르게 수정할 수 있는 셀이 없습니다.")
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)) {
            preview?.let {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DDZColor.Card,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(DDZLayout.Radius.Small),
                ) {
                    Column(Modifier.padding(DDZSpacing.itemGap),
                        verticalArrangement = Arrangement.spacedBy(DDZSpacing.xs)) {
                        Text("다음 촬영 저장 정보", style = DDZTypography.Secondary,
                            color = DDZColor.TextStrong)
                        Text("파일명 · ${it.previewNaming.displayName}", style = DDZTypography.Caption,
                            color = DDZColor.TextSecondary)
                        Text("저장경로 · ${it.previewNaming.relativePath}", style = DDZTypography.Caption,
                            color = DDZColor.TextSecondary)
                    }
                }
            }
            if (bounds.isNotEmpty() && preview != null) {
                val rendered = preview.plan.resolvedCells.associate { it.id to it.resolvedText }
                BoxWithConstraints(Modifier.fillMaxWidth().height((table.rows.coerceIn(2, 4) * 36).dp)) {
                    val gridWidth = maxWidth
                    val gridHeight = maxHeight
                    for (rect in bounds) {
                        val cell = rect.cell
                        val active = cell.cellId == selectedId
                        val label = "${quickCellPosition(cell)} 셀 선택"
                        Box(Modifier.offset(gridWidth * rect.left, gridHeight * rect.top)
                            .size(gridWidth * rect.width, gridHeight * rect.height)
                            .background(if (active) DDZColor.SelectedSoft else DDZColor.Surface)
                            .border(if (active) 2.dp else 1.dp, if (active) DDZColor.SelectionEmphasis else DDZColor.SageBorder)
                            .clickable(role = Role.Button) { select(cell.cellId) }
                            .semantics { selected = active; contentDescription = label },
                            contentAlignment = Alignment.Center) {
                            Text(rendered[cell.cellId].orEmpty(), Modifier.padding(horizontal = 2.dp),
                                style = DDZTypography.Caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            selectedCell?.let { cell ->
                key(cell.cellId) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(quickCellPosition(cell), Modifier.weight(1f), style = DDZTypography.SettingLabel)
                        val previous = adjacentQuickCellId(navigation, cell.cellId, -1)
                        val next = adjacentQuickCellId(navigation, cell.cellId, 1)
                        IconButton(enabled = previous != null, onClick = { previous?.let(::select) }) {
                            Icon(Icons.Filled.ChevronLeft, "이전 셀")
                        }
                        IconButton(enabled = next != null, onClick = { next?.let(::select) }) {
                            Icon(Icons.Filled.ChevronRight, "다음 셀")
                        }
                    }
                    if (cell.dataType == TableCellDataType.TEXT || cell.dataType == TableCellDataType.NUMBER) {
                        val kind = if (cell.dataType == TableCellDataType.NUMBER) "숫자" else "텍스트"
                        val fieldLabel = "${quickCellPosition(cell)} · $kind"
                        val number = cell.dataType == TableCellDataType.NUMBER
                        val input = inputs.getValue(cell.cellId)
                        val requester = remember { FocusRequester() }
                        var expanded by remember(document) { mutableStateOf(false) }
                        LaunchedEffect(focusTarget) {
                            if (focusTarget == cell.cellId) {
                                requester.requestFocus()
                                focusTarget = null
                            }
                        }
                        fun change(next: TextFieldValue) {
                            val accepted = if (number) decimalInputOrPrevious(next.text, input.text) else next.text
                            if (number && accepted != next.text.replace('−', '-')) return
                            inputs[cell.cellId] = next.copy(text = accepted)
                            failure = null
                        }
                        OutlinedTextField(value = input, onValueChange = ::change, label = { Text(fieldLabel) },
                            singleLine = number, maxLines = if (expanded) Int.MAX_VALUE else if (number) 1 else 2,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (number) KeyboardType.Decimal else KeyboardType.Text,
                                imeAction = if (number) ImeAction.Done else ImeAction.Default,
                            ),
                            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                            trailingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!number && favorites != null) TableEditorTextFavoriteStar(input.text, favorites)
                                    IconButton(onClick = {
                                        if (number) {
                                            val value = if (input.text.startsWith("-")) input.text.drop(1) else "-${input.text}"
                                            change(TextFieldValue(value, TextRange(value.length)))
                                        } else expanded = !expanded
                                    }) {
                                        if (number) Text("±") else Icon(
                                            if (expanded) Icons.Filled.CloseFullscreen else Icons.Filled.OpenInFull,
                                            if (expanded) "입력창 축소" else "입력창 확대",
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                                .then(if (expanded) Modifier.height(DDZLayout.QuickInput.ExpandedHeight) else Modifier)
                                .focusRequester(requester).onFocusChanged { inputFocused = it.isFocused }
                                .semantics { contentDescription = fieldLabel })
                        if (!number && favorites != null) {
                            TableEditorTextFavoritesRow(favorites, onApply = { text ->
                                change(TextFieldValue(text, TextRange(text.length)))
                            })
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { focus.clearFocus() }) { Text("입력") }
                        }
                    } else if (cell.dataType == TableCellDataType.COUNTER) {
                        val number = preview?.plan?.resolvedCells?.firstOrNull { it.id == cell.cellId }?.resolvedText.orEmpty()
                        Text("다음 촬영 번호 · $number", style = DDZTypography.SettingLabel)
                        Text("촬영을 저장하면 자동으로 증가해요.", style = DDZTypography.Caption)
                    } else {
                        val fieldLabel = "${cell.rowIndex + 1}행 ${cell.colIndex + 1}열 · " + when (cell.dataType) {
                            TableCellDataType.DATE -> "날짜"
                            TableCellDataType.TIME -> "시간"
                            else -> "순환문구"
                        }
                        Text(fieldLabel)
                        when (cell.dataType) {
                            TableCellDataType.DATE -> {
                                val selected = dateFormats[cell.cellId] ?: cell.formatPattern.ifBlank { "yyyyMMdd" }
                                Button(onClick = {
                                    expandedAutomaticId = cell.cellId.takeUnless { expandedAutomaticId == it }
                                }, modifier = Modifier.fillMaxWidth()) { Text("날짜 형식 · $selected") }
                            }
                            TableCellDataType.TIME -> Text("촬영 시각으로 자동 입력돼요.")
                            TableCellDataType.ROTATING_TEXT -> {
                                val current = phraseTable.cells.first { it.cellId == cell.cellId }
                                val sets = phraseTable.phraseSets.filter { it.items.isNotEmpty() }
                                val selected = sets.firstOrNull { it.id == current.phraseSetId }
                                Button(enabled = sets.isNotEmpty(), onClick = {
                                    expandedAutomaticId = cell.cellId.takeUnless { expandedAutomaticId == it }
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text("문구 세트 · ${selected?.name ?: "세트 선택"}")
                                }
                                if (selected != null) PhraseProgressControls(
                                    phraseTable, current, selected, enabled = draft != null,
                                    onProgress = { index, count ->
                                        phraseEdits += IosQuickPhraseEdit.Progress(cell.cellId, index, count)
                                    },
                                    onPeriod = { edit, choices -> phraseEdits += IosQuickPhraseEdit.Period(edit, choices) },
                                )
                                if (sets.isEmpty()) Text("표 상세설정에서 문구를 먼저 등록해 주세요.")
                            }
                            else -> Unit
                        }
                    }
                    val links = quickCellLinks(table, cell.cellId)
                    if (links.description.isNotEmpty()) Text(links.description)
                }
            }
        }
    }
    automaticCells.firstOrNull { it.cellId == expandedAutomaticId }?.let { cell ->
        val isDate = cell.dataType == TableCellDataType.DATE
        val options = if (isDate) quickDateFormats.map { pattern ->
            pattern to when (pattern) {
                "yyMMdd" -> "$pattern · 짧은 연월일"
                "MMdd" -> "$pattern · 월일"
                else -> "$pattern · 연월일"
            }
        } else document.template.templateState.phraseSets.filter { it.items.isNotEmpty() }
            .map { it.id to it.name }
        val selectedOption = if (isDate) {
            dateFormats[cell.cellId] ?: cell.formatPattern.ifBlank { "yyyyMMdd" }
        } else phraseTable.cells.first { it.cellId == cell.cellId }.phraseSetId
        DDZBottomSheet(
            title = if (isDate) "날짜 형식 선택" else "순환문구 세트 선택",
            onDismiss = { expandedAutomaticId = null },
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (id, label) ->
                    val active = id == selectedOption
                    OutlinedButton(
                        onClick = {
                            if (isDate) dateFormats[cell.cellId] = id else if (selectedOption != id) phraseEdits += IosQuickPhraseEdit.Select(cell.cellId, id)
                            expandedAutomaticId = null
                            failure = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (active) DDZColor.SelectedSoft else DDZColor.Surface),
                        modifier = Modifier.fillMaxWidth().semantics { selected = active },
                    ) { Text(label) }
                }
            }
        }
    }
}
