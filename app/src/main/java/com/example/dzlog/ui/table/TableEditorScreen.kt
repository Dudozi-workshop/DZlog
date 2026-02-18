@file:Suppress("UNUSED_VALUE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.dzlog.ui.table

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.dzlog.R
import com.example.dzlog.data.counter.COUNTER_DIGITS_DEFAULT
import com.example.dzlog.data.counter.clampCounterDigits
import com.example.dzlog.data.datastore.AppSettingsStore
import com.example.dzlog.data.preferences.KEY_COUNTER_DIGITS
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.domain.counter.policy.CounterScopeSnapshot
import com.example.dzlog.domain.model.CellValue
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.HourSystem
import com.example.dzlog.domain.model.RotatingPhraseSet
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellKind
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.TimeFormatOptions
import com.example.dzlog.domain.model.TimeSeparator
import com.example.dzlog.domain.naming.NamingFormatDefaults
import com.example.dzlog.domain.naming.buildDisplayNameFromResolvedCells
import com.example.dzlog.domain.naming.buildGalleryRelativePath
import com.example.dzlog.domain.preview.computeNextDelayMillis
import com.example.dzlog.domain.preview.decideTickUnit
import com.example.dzlog.domain.table.TableResolver
import com.example.dzlog.feature.table.policy.confirmCounterConflictDialog
import com.example.dzlog.feature.table.policy.dismissCounterConflictDialog
import com.example.dzlog.feature.table.policy.saveTableTemplate
import com.example.dzlog.ui.table.counter.TableCounterUiState
import com.example.dzlog.ui.table.counter.applyCounterConflictDialogEffect
import com.example.dzlog.ui.table.counter.buildTableCounterStreamContext
import com.example.dzlog.ui.table.counter.buildTableScopedCounterStream
import com.example.dzlog.ui.table.counter.restoreCounterCellToAutoNext
import com.example.dzlog.ui.table.counter.syncCounterStateForScope
import com.example.dzlog.ui.table.counter.updateCounterCellAndPolicy
import com.example.dzlog.ui.table.counter.updateCounterUiConflictDialogState
import com.example.dzlog.ui.table.counter.updateCounterUiScopeFlags
import com.example.dzlog.ui.table.editor.InlineEditState
import com.example.dzlog.ui.table.editor.commitInlineEditIfNeeded as commitInlineEdit
import com.example.dzlog.ui.table.editor.isEditing
import com.example.dzlog.ui.table.editor.clearInlineEditing
import com.example.dzlog.ui.table.editor.startInlineEditing
import com.example.dzlog.ui.table.watermark.TableWatermarkUiState
import com.example.dzlog.ui.table.watermark.applyBgAlphaChange
import com.example.dzlog.ui.table.watermark.applyBgStyleChange
import com.example.dzlog.ui.table.watermark.applyHeightRatioChange
import com.example.dzlog.ui.table.watermark.applyValueScaleChange
import com.example.dzlog.ui.table.watermark.applyWidthRatioChange
import com.example.dzlog.ui.table.watermark.loadTableWatermarkUiState
import com.example.dzlog.ui.table.rotating.RotatingPhraseSetEditDialog
import com.example.dzlog.ui.table.rotating.RotatingPhraseTemplateDialog
import com.example.dzlog.ui.table.section.LayoutTabActions
import com.example.dzlog.ui.table.section.LayoutTabContent
import com.example.dzlog.ui.table.section.LayoutTabUiState
import com.example.dzlog.ui.table.section.PreviewTabContent
import com.example.dzlog.ui.table.section.TableEditorTabs
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID


data class RotatingPhraseUiState(
    val isTemplateDialogOpen: Boolean = false,
    val templateDialogCellId: String? = null,
    val templateDialogRestore: TableTemplateState? = null,
    val isCreateSetDialogOpen: Boolean = false,
    val createSetName: String = "",
    val pendingDeleteSetId: String? = null,
    val isSetEditDialogOpen: Boolean = false,
    val editingSetId: String? = null
)


@Composable
fun TableEditorScreen(
    templateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // ✅ 탭 상태
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    var selectedCellId by remember { mutableStateOf(templateState.cells.firstOrNull()?.cellId) }
    // ✅ 탭0: 셀 설정 패널 표시 여부
    var showCellSettingsPanel by remember { mutableStateOf(false) }

    // 탭1 스크롤 (분리)
    val previewTabScrollState = rememberScrollState()

    val scope = rememberCoroutineScope()
    var isSavingTemplate by remember { mutableStateOf(false) }

    var showFormatDialog by remember { mutableStateOf(false) }
    var formatTargetCellId by remember { mutableStateOf<String?>(null) }
    var formatTargetType by remember { mutableStateOf<TableCellDataType?>(null) }

    fun closeFormatDialog() {
        showFormatDialog = false
        formatTargetCellId = null
        formatTargetType = null
    }

    fun openFormatDialog(targetCellId: String, targetType: TableCellDataType) {
        formatTargetCellId = targetCellId
        formatTargetType = targetType
        showFormatDialog = true
    }

    var rotatingUi by remember { mutableStateOf(RotatingPhraseUiState()) }

    fun openRotatingPhraseTemplateDialog(targetCellId: String) {
        rotatingUi = rotatingUi.copy(
            isTemplateDialogOpen = true,
            templateDialogCellId = targetCellId,
            templateDialogRestore = templateState,
            isCreateSetDialogOpen = false,
            createSetName = "",
            pendingDeleteSetId = null,
            isSetEditDialogOpen = false,
            editingSetId = null
        )
        showCellSettingsPanel = false
    }

    fun closeRotatingPhraseTemplateDialog() {
        rotatingUi = RotatingPhraseUiState()
    }

    val dateFormatOptions = listOf(NamingFormatDefaults.DATE_FORMAT_DEFAULT, "yyyy_MM_dd", "yyyyMMdd")
    // TIME 형식은 Step3부터 토글 UI(12/24, 초, 구분자)로 설정한다.

    // NOTE: formatPattern 기반이 아니라 현재는 고정값. (기존 코드 유지)
    val dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT
    val timeFormat = NamingFormatDefaults.TIME_FORMAT_CAPTURE_DEFAULT

    var previewCounterDigits by remember { mutableIntStateOf(COUNTER_DIGITS_DEFAULT) }

    var counterUi by remember { mutableStateOf(TableCounterUiState()) }

    var previewNow by remember { mutableStateOf(Date()) }

    LaunchedEffect(dateFormat, timeFormat) {
        val unit = decideTickUnit(dateFormat, timeFormat)
        while (true) {
            val delayMs = computeNextDelayMillis(unit)
            delay(delayMs)
            previewNow = Date()
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            val prefs = context.dataStore.data.first()
            previewCounterDigits = clampCounterDigits(prefs[KEY_COUNTER_DIGITS] ?: COUNTER_DIGITS_DEFAULT)
        }.onFailure {
            previewCounterDigits = COUNTER_DIGITS_DEFAULT
        }

        runCatching {
            val settings = AppSettingsStore.flow(context).first()
            counterUi = updateCounterUiScopeFlags(
                counterUi = counterUi,
                includePathInCounterScope = settings.includePathInCounterScope,
                includeFilenameInCounterScope = settings.includeFilenameInCounterScope,
            )
        }
    }

    val tableResolver = remember { TableResolver() }
    val planForScope = remember(templateState, previewNow, previewCounterDigits, counterUi.scopeNextCounter, dateFormat, timeFormat) {
        tableResolver.plan(
            cells = templateState.cells,
            captureNow = previewNow,
            config = TableResolver.Config(
                counterDigits = previewCounterDigits,
                dateFormat = dateFormat,
                timeFormat = timeFormat
            ),
            counterSeedOverride = counterUi.scopeNextCounter,
            phraseSets = templateState.phraseSets
        )
    }
    val isManualCounterModeDisplay by remember(
        counterUi.isManualCounterMode,
        counterUi.preserveManualCounterSeed,
        counterUi.scopeNextCounter,
        counterUi.autoNextCounterValue
    ) {
        derivedStateOf {
            counterUi.isManualCounterMode ||
                (counterUi.preserveManualCounterSeed && counterUi.scopeNextCounter > counterUi.autoNextCounterValue)
        }
    }

    val counterStreamContext by remember(planForScope.resolvedCells, counterUi.scopeNextCounter, isManualCounterModeDisplay) {
        derivedStateOf {
            buildTableCounterStreamContext(
                resolvedCells = planForScope.resolvedCells,
                scopeNextCounter = counterUi.scopeNextCounter,
                isManualCounterModeDisplay = isManualCounterModeDisplay,
            )
        }
    }

    val scopedCounterStream by remember(
        counterStreamContext,
        counterUi.includePathInCounterScope,
        counterUi.includeFilenameInCounterScope
    ) {
        derivedStateOf {
            buildTableScopedCounterStream(
                counterStreamContext = counterStreamContext,
                includePathInCounterScope = counterUi.includePathInCounterScope,
                includeFilenameInCounterScope = counterUi.includeFilenameInCounterScope,
            )
        }
    }

    // ✅ 스트림 변경 감지용 (스트림이 바뀌면 seed를 "새 스트림 next"로 강제 동기화)
    var lastScopeSnapshot by remember { mutableStateOf<CounterScopeSnapshot?>(null) }

    LaunchedEffect(scopedCounterStream.scopeParts.scopeKey, previewCounterDigits, templateState) {
        val syncResult = syncCounterStateForScope(
            context = context,
            templateState = templateState,
            counterUi = counterUi,
            counterStreamContext = counterStreamContext,
            scopedCounterStream = scopedCounterStream,
            previewCounterDigits = previewCounterDigits,
            isManualCounterModeDisplay = isManualCounterModeDisplay,
            lastScopeSnapshot = lastScopeSnapshot,
            updateCell = ::updateCell
        )
        counterUi = syncResult.counterUi
        lastScopeSnapshot = syncResult.nextScopeSnapshot
        syncResult.updatedTemplateState?.let(onTemplateChange)
    }

    var inlineEdit by remember { mutableStateOf(InlineEditState()) }

    val keyboardController = LocalSoftwareKeyboardController.current
    val inlineFocusRequester = remember { FocusRequester() }

    LaunchedEffect(inlineEdit.editingCellId) {
        if (inlineEdit.editingCellId != null) {
            delay(30)
            inlineFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    fun clearInlineEditingState() {
        inlineEdit = clearInlineEditing(inlineEdit)
    }

    fun commitInlineEditIfNeeded() {
        val result = commitInlineEdit(
            inlineState = inlineEdit,
            templateState = templateState,
            autoNextCounterValue = counterUi.autoNextCounterValue,
            updateCell = ::updateCell
        )
        val committedCellId = inlineEdit.editingCellId
        inlineEdit = result.nextInlineState
        result.openedCounterConflict?.let {
            counterUi = updateCounterUiConflictDialogState(counterUi, it)
        }
        result.committedCounterSeed?.let { seed ->
            val cellId = committedCellId ?: return@let
            updateCounterCellAndPolicy(
                context = context,
                templateState = templateState,
                cellId = cellId,
                seed = seed,
                preserveManual = true,
                forcePolicyUpdate = false,
                scopedCounterStream = scopedCounterStream,
                previewCounterDigits = previewCounterDigits,
                counterUi = counterUi,
                onTemplateChange = onTemplateChange,
                setCounterUi = { counterUi = it },
                updateCell = ::updateCell,
                scope = scope
            )
        } ?: result.updatedTemplateState?.let(onTemplateChange)
    }

    if (selectedCellId == null && templateState.cells.isNotEmpty()) {
        selectedCellId = templateState.cells.first().cellId
    }

    val selectedCell = templateState.cells.firstOrNull { it.cellId == selectedCellId }
    val hasGroup1 = templateState.cells.any { it.groupLevel == GroupLevel.G1 }
    val hasGroup2 = templateState.cells.any { it.groupLevel == GroupLevel.G2 }

    val savePathPreview = remember(templateState.cells) {
        buildGalleryRelativePath(templateState.cells)
    }

    var watermarkUi by remember { mutableStateOf(TableWatermarkUiState()) }

    LaunchedEffect(Unit) {
        runCatching {
            watermarkUi = loadTableWatermarkUiState(context)
        }
    }

    val plan = remember(templateState.cells, previewNow, previewCounterDigits, counterUi.scopeNextCounter, dateFormat, timeFormat) {
        tableResolver.plan(
            cells = templateState.cells,
            captureNow = previewNow,
            config = TableResolver.Config(
                counterDigits = previewCounterDigits,
                dateFormat = dateFormat,
                timeFormat = timeFormat
            ),
            counterSeedOverride = counterUi.scopeNextCounter,
            phraseSets = templateState.phraseSets
        )
    }

    val filenamePreview = buildDisplayNameFromResolvedCells(
        resolvedCells = plan.resolvedCells,
        fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
        includeDate = false,
        includeTime = false,
        counterDigits = previewCounterDigits,
        // ✅ 파일명 suffix counter는 항상 스트림 값(SSOT)을 사용
        // COUNTER 셀의 표기 ON/OFF는 "표/워터마크 표현"에만 영향, 카운터 스트림/파일명에는 영향 없음.
        // (파일명 뒤 숫자는 항상 붙는 정책)
        counterOverride = counterStreamContext.nextCounter,
        now = previewNow
    )

    fun handleCounterConflictDialogEffect(effect: com.example.dzlog.feature.table.policy.TableCounterConflictDialogEffect) {
        applyCounterConflictDialogEffect(
            effect = effect,
            context = context,
            templateState = templateState,
            scopedCounterStream = scopedCounterStream,
            previewCounterDigits = previewCounterDigits,
            counterUi = counterUi,
            onTemplateChange = onTemplateChange,
            setCounterUi = { counterUi = it },
            updateCell = ::updateCell,
            scope = scope
        )
    }

    if (counterUi.counterConflictDialogState.isVisible) {
        AlertDialog(
            onDismissRequest = {
                val (nextState, effect) = dismissCounterConflictDialog(counterUi.counterConflictDialogState)
                counterUi = updateCounterUiConflictDialogState(counterUi, nextState)
                handleCounterConflictDialogEffect(effect)
                clearInlineEditingState()
            },
            title = { Text("카운터 충돌 경고") },
            text = {
                Text(
                    "중복된 카운터가 발생할 수 있습니다. 계속 진행하시겠습니까?\n\n" +
                        "입력값: ${counterUi.counterConflictDialogState.pendingCounterCommitValue}\n" +
                        "현재 스트림 next: ${counterUi.counterConflictDialogState.pendingCounterStreamNextValue}"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val (nextState, effect) = confirmCounterConflictDialog(counterUi.counterConflictDialogState)
                    counterUi = updateCounterUiConflictDialogState(counterUi, nextState)
                    handleCounterConflictDialogEffect(effect)
                    clearInlineEditingState()
                }) { Text("진행", style = DDZTypography.ButtonText) }
            },
            dismissButton = {
                TextButton(onClick = {
                    val (nextState, effect) = dismissCounterConflictDialog(counterUi.counterConflictDialogState)
                    counterUi = updateCounterUiConflictDialogState(counterUi, nextState)
                    handleCounterConflictDialogEffect(effect)
                    clearInlineEditingState()
                }) {
                    Text("취소", style = DDZTypography.ButtonText)
                }
            }
        )
    }

    if (showFormatDialog) {
        val targetId = formatTargetCellId
        val targetType = formatTargetType
        val targetCell = templateState.cells.firstOrNull { it.cellId == targetId }
        val isDate = (targetType == TableCellDataType.DATE)
        val isTime = (targetType == TableCellDataType.TIME)

        AlertDialog(
            onDismissRequest = { closeFormatDialog() },
            title = {
                Text(
                    when {
                        isDate -> "DATE 형식"
                        isTime -> "TIME 형식"
                        else -> "형식"
                    }
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    when {
                        isDate -> {
                            val current = targetCell?.formatPattern.orEmpty()
                            dateFormatOptions.forEach { p ->
                                TextButton(
                                    onClick = {
                                        if (targetId != null) {
                                            val updated = updateCell(templateState, targetId) { c ->
                                                c.copy(formatPattern = p)
                                            }
                                            onTemplateChange(updated)
                                        }
                                        closeFormatDialog()
                                    }
                                ) {
                                    Text(if (current == p) "✓  $p" else p, style = DDZTypography.ButtonText)
                                }
                            }
                        }

                        isTime -> {
                            val initial = targetCell?.timeFormatOptions ?: TimeFormatOptions()
                            var hourSystem by remember(targetId) { mutableStateOf(initial.hourSystem) }
                            var includeSeconds by remember(targetId) { mutableStateOf(initial.includeSeconds) }
                            var separator by remember(targetId) { mutableStateOf(initial.separator) }

                            fun apply() {
                                if (targetId == null) return
                                val updated = updateCell(templateState, targetId) { c ->
                                    c.copy(
                                        timeFormatOptions = TimeFormatOptions(
                                            hourSystem = hourSystem,
                                            includeSeconds = includeSeconds,
                                            separator = separator
                                        ),
                                        formatPattern = ""
                                    )
                                }
                                onTemplateChange(updated)
                            }

                            Text("시간 표시 설정", style = DDZTypography.SectionTitle)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("시간제", modifier = Modifier.width(72.dp))
                                RadioButton(
                                    selected = hourSystem == HourSystem.H24,
                                    onClick = { hourSystem = HourSystem.H24; apply() }
                                )
                                Text("24h")
                                Spacer(Modifier.width(12.dp))
                                RadioButton(
                                    selected = hourSystem == HourSystem.H12,
                                    onClick = { hourSystem = HourSystem.H12; apply() }
                                )
                                Text("12h")
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("초 포함", modifier = Modifier.width(72.dp))
                                Switch(
                                    checked = includeSeconds,
                                    onCheckedChange = { includeSeconds = it; apply() }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("구분자", modifier = Modifier.width(72.dp))
                                listOf(TimeSeparator.COLON, TimeSeparator.NONE).forEach { s ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = separator == s,
                                            onClick = { separator = s; apply() }
                                        )
                                        Text(if (s == TimeSeparator.NONE) "붙이기" else s.token)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                }
                            }

                            val preview = buildString {
                                append(if (hourSystem == HourSystem.H24) "HH" else "hh")
                                append(separator.token)
                                append("mm")
                                if (includeSeconds) {
                                    append(separator.token)
                                    append("ss")
                                }
                                if (hourSystem == HourSystem.H12) append(" a")
                            }
                            Text("미리보기: $preview", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                        }

                        else -> Text("지원되지 않는 타입")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { closeFormatDialog() }) {
                    Text("닫기", style = DDZTypography.ButtonText)
                }
            }
        )
    }

    if (rotatingUi.isTemplateDialogOpen) {
        val dialogCell = templateState.cells.firstOrNull { it.cellId == rotatingUi.templateDialogCellId }
        if (dialogCell == null || dialogCell.dataType != TableCellDataType.ROTATING_TEXT) {
            closeRotatingPhraseTemplateDialog()
        } else {
            RotatingPhraseTemplateDialog(
                cell = dialogCell,
                phraseSets = templateState.phraseSets,
                onDismiss = { closeRotatingPhraseTemplateDialog() },
                onRestore = {
                    rotatingUi.templateDialogRestore?.let { onTemplateChange(it) }
                    closeRotatingPhraseTemplateDialog()
                },
                onSelectSet = { phraseSetId ->
                    val updated = updateCell(templateState, dialogCell.cellId) { current ->
                        if (phraseSetId == null) {
                            current.copy(phraseSetId = null, everyOverride = null)
                        } else {
                            current.copy(phraseSetId = phraseSetId)
                        }
                    }
                    onTemplateChange(updated)
                },
                onEveryChange = { every ->
                    val updated = updateCell(templateState, dialogCell.cellId) { current ->
                        current.copy(everyOverride = every.coerceAtLeast(1))
                    }
                    onTemplateChange(updated)
                },
                onIncreaseEvery = {
                    val selectedSet = templateState.phraseSets.firstOrNull { it.id == dialogCell.phraseSetId }
                    val currentEvery = dialogCell.everyOverride ?: selectedSet?.defaultEvery ?: 1
                    val nextEvery = (currentEvery + 1).coerceAtLeast(1)
                    val updated = updateCell(templateState, dialogCell.cellId) { current ->
                        current.copy(everyOverride = nextEvery)
                    }
                    onTemplateChange(updated)
                },
                onDecreaseEvery = {
                    val selectedSet = templateState.phraseSets.firstOrNull { it.id == dialogCell.phraseSetId }
                    val currentEvery = dialogCell.everyOverride ?: selectedSet?.defaultEvery ?: 1
                    val nextEvery = (currentEvery - 1).coerceAtLeast(1)
                    val updated = updateCell(templateState, dialogCell.cellId) { current ->
                        current.copy(everyOverride = nextEvery)
                    }
                    onTemplateChange(updated)
                },
                onRequestCreateSet = {
                    rotatingUi = rotatingUi.copy(isCreateSetDialogOpen = true, createSetName = "")
                },
                onRequestDeleteSet = { phraseSetId ->
                    rotatingUi = rotatingUi.copy(pendingDeleteSetId = phraseSetId)
                },
                onRequestEditSet = { phraseSetId ->
                    rotatingUi = rotatingUi.copy(isSetEditDialogOpen = true, editingSetId = phraseSetId)
                }
            )
        }
    }

    if (rotatingUi.isSetEditDialogOpen) {
        val editingSet = templateState.phraseSets.firstOrNull { it.id == rotatingUi.editingSetId }
        if (editingSet == null) {
            rotatingUi = rotatingUi.copy(isSetEditDialogOpen = false, editingSetId = null)
        } else {
            RotatingPhraseSetEditDialog(
                phraseSet = editingSet,
                onClose = {
                    rotatingUi = rotatingUi.copy(isSetEditDialogOpen = false, editingSetId = null)
                },
                onUpdateSet = { transform ->
                    val updated = templateState.copy(
                        phraseSets = templateState.phraseSets.map { set ->
                            if (set.id == editingSet.id) transform(set) else set
                        }
                    )
                    onTemplateChange(updated)
                },
                onDeleteSet = { deleteId ->
                    val updated = templateState.copy(
                        phraseSets = templateState.phraseSets.filterNot { it.id == deleteId },
                        cells = templateState.cells.map { cell ->
                            if (cell.phraseSetId == deleteId) {
                                cell.copy(phraseSetId = null, everyOverride = null)
                            } else {
                                cell
                            }
                        }
                    )
                    onTemplateChange(updated)
                    rotatingUi = rotatingUi.copy(isSetEditDialogOpen = false, editingSetId = null)
                }
            )
        }
    }

    if (rotatingUi.isCreateSetDialogOpen) {
        AlertDialog(
            onDismissRequest = { rotatingUi = rotatingUi.copy(isCreateSetDialogOpen = false, createSetName = "") },
            title = { Text("새 템플릿 추가") },
            text = {
                OutlinedTextField(
                    value = rotatingUi.createSetName,
                    onValueChange = { rotatingUi = rotatingUi.copy(createSetName = it) },
                    singleLine = true,
                    label = { Text("세트 이름") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = rotatingUi.createSetName.trim()
                    if (name.isNotEmpty() && rotatingUi.templateDialogCellId != null) {
                        val created = RotatingPhraseSet(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            items = emptyList(),
                            defaultEvery = 1
                        )
                        val updatedTemplate = templateState.copy(
                            phraseSets = templateState.phraseSets + created,
                            cells = templateState.cells.map { cell ->
                                if (cell.cellId == rotatingUi.templateDialogCellId) {
                                    cell.copy(phraseSetId = created.id, everyOverride = null)
                                } else {
                                    cell
                                }
                            }
                        )
                        onTemplateChange(updatedTemplate)
                    }
                    rotatingUi = rotatingUi.copy(isCreateSetDialogOpen = false, createSetName = "")
                }) {
                    Text("추가", style = DDZTypography.ButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = { rotatingUi = rotatingUi.copy(isCreateSetDialogOpen = false, createSetName = "") }) {
                    Text("취소", style = DDZTypography.ButtonText)
                }
            }
        )
    }

    rotatingUi.pendingDeleteSetId?.let { deleteId ->
        val deleteTarget = templateState.phraseSets.firstOrNull { it.id == deleteId }
        if (deleteTarget != null) {
            AlertDialog(
                onDismissRequest = { rotatingUi = rotatingUi.copy(pendingDeleteSetId = null) },
                title = { Text("세트 삭제") },
                text = { Text("${deleteTarget.name} 세트를 삭제하시겠습니까?") },
                confirmButton = {
                    TextButton(onClick = {
                        val updated = templateState.copy(
                            phraseSets = templateState.phraseSets.filterNot { it.id == deleteId },
                            cells = templateState.cells.map { cell ->
                                if (cell.phraseSetId == deleteId) {
                                    cell.copy(phraseSetId = null, everyOverride = null)
                                } else {
                                    cell
                                }
                            }
                        )
                        onTemplateChange(updated)
                        rotatingUi = rotatingUi.copy(pendingDeleteSetId = null)
                    }) {
                        Text("삭제", style = DDZTypography.ButtonText)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { rotatingUi = rotatingUi.copy(pendingDeleteSetId = null) }) {
                        Text("취소", style = DDZTypography.ButtonText)
                    }
                }
            )
        } else {
            rotatingUi = rotatingUi.copy(pendingDeleteSetId = null)
        }
    }

    // ✅ 탭 전환: 편집 중이면 먼저 commit (중복 다이얼로그 등으로 commit이 보류되면 탭 전환 막기)
    fun requestTabSwitch(targetIndex: Int) {
        if (selectedTabIndex == targetIndex) return

        if (inlineEdit.isEditing()) {
            commitInlineEditIfNeeded()
            // commit이 보류되면(=editingCellId가 유지됨) 전환 막음
            if (inlineEdit.isEditing()) return
        }
        selectedTabIndex = targetIndex
    }

    Scaffold(
        containerColor = DDZColor.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "표 상세설정",
                        style = DDZTypography.ScreenTitle,
                        color = DDZColor.TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DDZColor.Background,
                    navigationIconContentColor = DDZColor.TextPrimary
                ),
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(
                            stringResource(R.string.action_back),
                            style = DDZTypography.ButtonText,
                            color = DDZColor.TextPrimary
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TableEditorTabs(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { requestTabSwitch(it) }
            )

            when (selectedTabIndex) {
                0 -> {
                    LayoutTabContent(
                        uiState = LayoutTabUiState(
                            savePathPreview = savePathPreview,
                            filenamePreview = filenamePreview,
                            counterModeLabel = if (isManualCounterModeDisplay) "메뉴얼" else "오토",
                            templateState = templateState,
                            plan = plan,
                            selectedCellId = selectedCellId,
                            editingCellId = inlineEdit.editingCellId,
                            editingValue = inlineEdit.editingValue,
                            inlineFocusRequester = inlineFocusRequester,
                            showCellSettingsPanel = showCellSettingsPanel,
                            selectedCell = selectedCell,
                            hasGroup1 = hasGroup1,
                            hasGroup2 = hasGroup2,
                            isSavingTemplate = isSavingTemplate,
                            autoNextCounterValue = counterUi.autoNextCounterValue
                        ),
                        actions = LayoutTabActions(
                            onSelectCellId = { selectedCellId = it },
                            onShowCellSettingsPanel = { showCellSettingsPanel = it },
                            onStartInlineEditing = { cellId, value ->
                                inlineEdit = startInlineEditing(inlineEdit, cellId, value)
                            },
                            onOpenFormatDialog = ::openFormatDialog,
                            onEditingValueChange = { inlineEdit = inlineEdit.copy(editingValue = it) },
                            onCommitInline = ::commitInlineEditIfNeeded,
                            onTryCommitInlineAndContinue = {
                                commitInlineEditIfNeeded()
                                !inlineEdit.isEditing()
                            },
                            onInlineFocusLostCommit = {
                                commitInlineEditIfNeeded()
                                showCellSettingsPanel = true
                            },
                            onAddRow = { onTemplateChange(addRow(templateState)) },
                            onRemoveRow = {
                                val updated = removeRow(templateState)
                                onTemplateChange(updated)
                                if (updated.cells.none { it.cellId == selectedCellId }) {
                                    selectedCellId = updated.cells.firstOrNull()?.cellId
                                }
                            },
                            onAddCol = { onTemplateChange(addColumn(templateState)) },
                            onRemoveCol = {
                                val updated = removeColumn(templateState)
                                onTemplateChange(updated)
                                if (updated.cells.none { it.cellId == selectedCellId }) {
                                    selectedCellId = updated.cells.firstOrNull()?.cellId
                                }
                            },
                            onReset = onReset,
                            onSave = {
                                commitInlineEditIfNeeded()
                                isSavingTemplate = true
                                scope.launch {
                                    saveTableTemplate(context, templateState)
                                        .onFailure {
                                            Toast.makeText(
                                                context,
                                                "Save failed: ${it.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            isSavingTemplate = false
                                        }
                                        .onSuccess {
                                            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                                            onBack()
                                        }
                                }
                            },
                            onDismissSettingsPanel = {
                                commitInlineEditIfNeeded()
                                showCellSettingsPanel = false
                            },
                            onSetFileNameIncludeForSelected = { checked ->
                                selectedCell?.let { cell ->
                                    val updated = updateCell(templateState, cell.cellId) { c ->
                                        c.copy(fileNameInclude = checked)
                                    }
                                    onTemplateChange(updated)
                                }
                            },
                            onPathGroupActionForSelected = { action ->
                                selectedCell?.let { cell ->
                                    val updated = applyPathGroupAction(
                                        state = templateState,
                                        targetCellId = cell.cellId,
                                        action = action
                                    )
                                    onTemplateChange(updated)
                                }
                            },
                            onSetDataTypeForSelected = { type ->
                                selectedCell?.let { cell ->
                                    val updated = updateCell(templateState, cell.cellId) { c ->
                                        c.withDataType(type)
                                    }
                                    onTemplateChange(updated)
                                }
                            },
                            onResetCounterSeedForSelected = {
                                selectedCell?.let { cell ->
                                    if (cell.dataType != TableCellDataType.COUNTER) return@let
                                    restoreCounterCellToAutoNext(
                                        context = context,
                                        templateState = templateState,
                                        cellId = cell.cellId,
                                        scopedCounterStream = scopedCounterStream,
                                        previewCounterDigits = previewCounterDigits,
                                        counterUi = counterUi,
                                        onTemplateChange = onTemplateChange,
                                        setCounterUi = { counterUi = it },
                                        updateCell = ::updateCell,
                                        scope = scope
                                    )
                                    Toast.makeText(context, "카운터를 자동 기준으로 초기화했습니다.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onOpenRotatingTemplateDialogForSelected = { cellId ->
                                val cell = templateState.cells.firstOrNull { it.cellId == cellId }
                                if (cell != null && cell.dataType == TableCellDataType.ROTATING_TEXT) {
                                    openRotatingPhraseTemplateDialog(cell.cellId)
                                } else {
                                    showCellSettingsPanel = true
                                }
                            }
                        )
                    )
                }

                1 -> {
                    // ==========================
                    // 탭1: 표 미리보기
                    // ==========================
                    PreviewTabContent(
                        scrollState = previewTabScrollState,
                        captureAspect = watermarkUi.captureAspect,
                        templateState = templateState,
                        resolvedCells = plan.resolvedCells,
                        wmAnchor = watermarkUi.wmAnchor,
                        wmWidthRatio = watermarkUi.wmWidthRatio,
                        wmHeightRatio = watermarkUi.wmHeightRatio,
                        wmBgStyle = watermarkUi.wmBgStyle,
                        wmBgAlpha = watermarkUi.wmBgAlpha,
                        wmValueScale = watermarkUi.wmValueScale,
                        onRowColWeightsChange = { updated -> onTemplateChange(updated) },
                        onWidthRatioChange = { width ->
                            scope.launch {
                                watermarkUi = applyWidthRatioChange(context, width, watermarkUi)
                            }
                        },
                        onHeightRatioChange = { height ->
                            scope.launch {
                                watermarkUi = applyHeightRatioChange(context, height, watermarkUi)
                            }
                        },
                        onBgStyleChange = { bgStyle ->
                            scope.launch {
                                watermarkUi = applyBgStyleChange(context, bgStyle, watermarkUi)
                            }
                        },
                        onBgAlphaChange = { alpha ->
                            scope.launch {
                                watermarkUi = applyBgAlphaChange(context, alpha, watermarkUi)
                            }
                        },
                        onValueScaleChange = { scale ->
                            scope.launch {
                                watermarkUi = applyValueScaleChange(context, scale, watermarkUi)
                            }
                        }
                    )
                }
            }
        }
    }
}


private fun updateCell(
    templateState: TableTemplateState,
    cellId: String,
    transform: (TableCellState) -> TableCellState
): TableTemplateState {
    return templateState.copy(
        cells = templateState.cells.map { cell ->
            if (cell.cellId == cellId) transform(cell) else cell
        }
    )
}

private fun addRow(templateState: TableTemplateState): TableTemplateState {
    val newRowIndex = templateState.rows
    val newCells = (0 until templateState.cols).map { col ->
        TableCellState(
            rowIndex = newRowIndex,
            colIndex = col,
            kind = TableCellKind.INPUT,
            dataType = TableCellDataType.TEXT,
            rawText = "",
            typedValue = CellValue.Text(""),
            fileNameInclude = false,
            groupLevel = GroupLevel.NONE,
            label = ""
        )
    }

    // Stage 1: rowWeights는 "행 단위 높이 비율"을 위한 데이터. 아직 렌더링에는 반영하지 않는다.
    val baseRowWeights = templateState.rowWeights ?: List(templateState.rows.coerceAtLeast(1)) { 1f }
    val nextRowWeights = baseRowWeights + 1f

    return templateState.copy(
        rows = templateState.rows + 1,
        cells = templateState.cells + newCells,
        rowWeights = nextRowWeights
    )
}

private fun removeRow(templateState: TableTemplateState): TableTemplateState {
    val lastRowIndex = templateState.rows - 1

    val baseRowWeights = templateState.rowWeights ?: List(templateState.rows.coerceAtLeast(1)) { 1f }
    val nextRowWeights = if (baseRowWeights.isNotEmpty()) baseRowWeights.dropLast(1) else baseRowWeights

    return templateState.copy(
        rows = templateState.rows - 1,
        cells = templateState.cells.filterNot { it.rowIndex == lastRowIndex },
        rowWeights = nextRowWeights
    )
}

private fun addColumn(templateState: TableTemplateState): TableTemplateState {
    val newColIndex = templateState.cols
    val newCells = (0 until templateState.rows).map { row ->
        TableCellState(
            rowIndex = row,
            colIndex = newColIndex,
            kind = TableCellKind.INPUT,
            dataType = TableCellDataType.TEXT,
            rawText = "",
            typedValue = CellValue.Text(""),
            fileNameInclude = false,
            groupLevel = GroupLevel.NONE,
            label = ""
        )
    }

    // Stage 1: colWeights는 "열 단위 너비 비율"을 위한 데이터. 아직 렌더링에는 반영하지 않는다.
    val baseColWeights = templateState.colWeights ?: List(templateState.cols.coerceAtLeast(1)) { 1f }
    val nextColWeights = baseColWeights + 1f

    return templateState.copy(
        cols = templateState.cols + 1,
        cells = templateState.cells + newCells,
        colWeights = nextColWeights
    )
}

private fun removeColumn(templateState: TableTemplateState): TableTemplateState {
    val lastColIndex = templateState.cols - 1

    val baseColWeights = templateState.colWeights ?: List(templateState.cols.coerceAtLeast(1)) { 1f }
    val nextColWeights = if (baseColWeights.isNotEmpty()) baseColWeights.dropLast(1) else baseColWeights

    return templateState.copy(
        cols = templateState.cols - 1,
        cells = templateState.cells.filterNot { it.colIndex == lastColIndex },
        colWeights = nextColWeights
    )
}

/**
 * DataType 변경 시 공통 규칙(일관 UX)
 * - rawText는 절대 자동 변경하지 않는다 (사용자가 TEXT 편집할 때만 변경)
 * - TEXT/NUMBER로 전환 시 typedValue를 rawText 기반으로 동기화
 * - DATE/TIME은 captureNow 기준 자동 적용(typedValue=Auto)
 * - TIME은 옵션이 없으면 기본 옵션을 부여
 * - COUNTER는 seed가 없으면 1로 초기화
 */
private fun TableCellState.withDataType(newType: TableCellDataType): TableCellState {
    val next = when (newType) {
        TableCellDataType.TEXT -> this.copy(
            dataType = newType,
            typedValue = CellValue.Text(this.rawText),
            timeFormatOptions = null
        )

        TableCellDataType.NUMBER -> this.copy(
            dataType = newType,
            typedValue = CellValue.Number(this.rawText),
            timeFormatOptions = null
        )

        TableCellDataType.DATE -> this.copy(
            dataType = newType,
            typedValue = CellValue.Auto,
            timeFormatOptions = null
        )

        TableCellDataType.TIME -> this.copy(
            dataType = newType,
            typedValue = CellValue.Auto,
            timeFormatOptions = this.timeFormatOptions ?: TimeFormatOptions()
        )

        TableCellDataType.COUNTER -> this.copy(
            dataType = newType,
            typedValue = (this.typedValue as? CellValue.CounterSeed) ?: CellValue.CounterSeed(1),
            timeFormatOptions = null
        )

        TableCellDataType.ROTATING_TEXT -> this.copy(
            dataType = newType,
            typedValue = CellValue.Auto,
            timeFormatOptions = null
        )
    }

    return if (newType == TableCellDataType.ROTATING_TEXT) {
        next
    } else {
        next.copy(
            phraseSetId = null,
            everyOverride = null
        )
    }
}
