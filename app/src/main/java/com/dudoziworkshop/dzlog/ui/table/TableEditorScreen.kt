@file:Suppress("UNUSED_VALUE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.dudoziworkshop.dzlog.ui.table

import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dudoziworkshop.dzlog.ui.common.dzScaffoldContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.dudoziworkshop.dzlog.data.counter.COUNTER_DIGITS_DEFAULT
import com.dudoziworkshop.dzlog.data.counter.clampCounterDigits
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.data.preferences.KEY_COUNTER_DIGITS
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureContext
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureNamingPolicy
import com.dudoziworkshop.dzlog.domain.counter.policy.CounterScopeSnapshot
import com.dudoziworkshop.dzlog.domain.counter.policy.normalizeTimeToMinute
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.CounterScopeMode
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.TimeFormatOptions
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.preview.computeNextDelayMillis
import com.dudoziworkshop.dzlog.domain.preview.decideTickUnitFromTemplate
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.feature.table.policy.confirmCounterConflictDialog
import com.dudoziworkshop.dzlog.feature.table.policy.dismissCounterConflictDialog
import com.dudoziworkshop.dzlog.feature.table.policy.saveTableTemplate
import com.dudoziworkshop.dzlog.ui.table.counter.TableCounterUiState
import com.dudoziworkshop.dzlog.ui.table.counter.applyCounterConflictDialogEffect
import com.dudoziworkshop.dzlog.ui.table.counter.buildTableCounterStreamContext
import com.dudoziworkshop.dzlog.ui.table.counter.buildTableScopedCounterStream
import com.dudoziworkshop.dzlog.ui.table.counter.restoreCounterCellToAutoNext
import com.dudoziworkshop.dzlog.ui.table.counter.syncCounterStateForScope
import com.dudoziworkshop.dzlog.ui.table.counter.updateCounterCellAndPolicy
import com.dudoziworkshop.dzlog.ui.table.counter.updateCounterUiConflictDialogState
import com.dudoziworkshop.dzlog.ui.table.counter.updateCounterUiScopeFlags
import com.dudoziworkshop.dzlog.ui.table.editor.InlineEditState
import com.dudoziworkshop.dzlog.ui.table.editor.clearInlineEditing
import com.dudoziworkshop.dzlog.ui.table.editor.isEditing
import com.dudoziworkshop.dzlog.ui.table.editor.shouldBlockTabSwitchAfterCommit
import com.dudoziworkshop.dzlog.ui.table.editor.startInlineEditing
import com.dudoziworkshop.dzlog.ui.table.format.TableFormatDialog
import com.dudoziworkshop.dzlog.ui.table.format.TableFormatDialogState
import com.dudoziworkshop.dzlog.ui.table.format.close
import com.dudoziworkshop.dzlog.ui.table.format.open
import com.dudoziworkshop.dzlog.ui.table.rotating.RotatingPhraseSetEditDialog
import com.dudoziworkshop.dzlog.ui.table.rotating.RotatingPhraseTemplateDialog
import com.dudoziworkshop.dzlog.ui.table.rotating.RotatingPhraseUiState
import com.dudoziworkshop.dzlog.ui.table.section.LayoutTabActions
import com.dudoziworkshop.dzlog.ui.table.section.LayoutTabContent
import com.dudoziworkshop.dzlog.ui.table.section.LayoutTabUiState
import com.dudoziworkshop.dzlog.ui.table.section.PreviewTabContent
import com.dudoziworkshop.dzlog.ui.table.section.TableEditorTabs
import com.dudoziworkshop.dzlog.ui.table.template.addColumn
import com.dudoziworkshop.dzlog.ui.table.template.addRow
import com.dudoziworkshop.dzlog.ui.table.template.addToFileNameSlots
import com.dudoziworkshop.dzlog.ui.table.template.removeColumn
import com.dudoziworkshop.dzlog.ui.table.template.removeFromFileNameSlots
import com.dudoziworkshop.dzlog.ui.table.template.removeRow
import com.dudoziworkshop.dzlog.ui.table.template.updateCell
import com.dudoziworkshop.dzlog.ui.table.watermark.TableWatermarkUiState
import com.dudoziworkshop.dzlog.ui.table.watermark.applyAnchorOffsetDragChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyBgAlphaChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyBgStyleChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyCaptureAspectChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyGridEnabledChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyHeightRatioChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyManualTextColorChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyTextAlignChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyTextColorModeChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyValueScaleChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyWidthRatioChange
import com.dudoziworkshop.dzlog.ui.table.watermark.loadTableWatermarkUiState
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitCancellation
import java.util.Date
import java.util.UUID
import com.dudoziworkshop.dzlog.ui.table.editor.commitInlineEditIfNeeded as commitInlineEdit


@Composable
fun TableEditorScreen(
    templateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ✅ 탭 상태
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    var selectedCellId by remember { mutableStateOf(templateState.cells.firstOrNull()?.cellId) }
    // ✅ 탭0: 셀 설정 패널 표시 여부
    var showCellSettingsPanel by remember { mutableStateOf(false) }

    // 탭1 스크롤 (분리)
    val previewTabScrollState = rememberScrollState()

    val scope = rememberCoroutineScope()
    var isSavingTemplate by remember { mutableStateOf(false) }

    var formatDialog by remember { mutableStateOf(TableFormatDialogState()) }

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

    val settings by AppSettingsStore.flow(context).collectAsState(
        initial = AppSettings(
            saveMode = SaveMode.BOTH,
            continuousPreviewMode = ContinuousPreviewMode.OFF,
            photoQualityMode = PhotoQualityMode.BALANCED,
            counterPadding = previewCounterDigits,
            includePathInCounterScope = true,
            includeFilenameInCounterScope = true,
            toastEnabled = true,
            hapticEnabled = true,
            captureHapticEnabled = true,
            blankWarningEnabled = true,
        )
    )
    val tableSaveMode = settings.saveMode

    var counterUi by remember { mutableStateOf(TableCounterUiState()) }
    var resumeTick by remember { mutableIntStateOf(0) }
    var scopeKeySnapshot by remember { mutableStateOf<String?>(null) }

    var previewNow by remember { mutableStateOf(Date()) }

    LaunchedEffect(templateState.cells, lifecycleOwner) {
        val unit = decideTickUnitFromTemplate(templateState.cells)
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val delayMs = computeNextDelayMillis(unit)
                delay(delayMs)
                previewNow = Date()
            }
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            resumeTick += 1
            awaitCancellation()
        }
    }

    LaunchedEffect(Unit) {
        runCatching {
            val prefs = context.dataStore.data.first()
            previewCounterDigits = clampCounterDigits(prefs[KEY_COUNTER_DIGITS] ?: COUNTER_DIGITS_DEFAULT)
        }.onFailure {
            previewCounterDigits = COUNTER_DIGITS_DEFAULT
        }
    }

    LaunchedEffect(settings.includePathInCounterScope, settings.includeFilenameInCounterScope) {
        counterUi = updateCounterUiScopeFlags(
            counterUi = counterUi,
            includePathInCounterScope = settings.includePathInCounterScope,
            includeFilenameInCounterScope = settings.includeFilenameInCounterScope,
        )
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

    val scopeDateTimeValues = remember(templateState.cells, planForScope.resolvedCells) {
        buildCounterScopeDateTimeValues(templateState.cells, planForScope.resolvedCells)
    }

    val counterStreamContext by remember(
        planForScope.resolvedCells,
        counterUi.scopeNextCounter,
        isManualCounterModeDisplay,
        scopeDateTimeValues.dateScopeValues,
        scopeDateTimeValues.timeScopeValues,
    ) {
        derivedStateOf {
            buildTableCounterStreamContext(
                resolvedCells = planForScope.resolvedCells,
                fileNameSlots = templateState.fileNameSlots,
                includeFilenameInCounterScope = counterUi.includeFilenameInCounterScope,
                dateScopeValues = scopeDateTimeValues.dateScopeValues,
                timeScopeValues = scopeDateTimeValues.timeScopeValues,
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

    LaunchedEffect(resumeTick) {
        scopeKeySnapshot = scopedCounterStream.scopeParts.scopeKey
    }
    val activeScopeKey = scopeKeySnapshot ?: scopedCounterStream.scopeParts.scopeKey

    // ✅ 스트림 변경 감지용 (스트림이 바뀌면 seed를 "새 스트림 next"로 강제 동기화)


    var lastScopeSnapshot by remember { mutableStateOf<CounterScopeSnapshot?>(null) }
    var lastProcessedResumeTick by remember { mutableIntStateOf(-1) }

    LaunchedEffect(
        activeScopeKey,
        previewCounterDigits,
        tableSaveMode,
        resumeTick,
    ) {
        val isExternalResync = resumeTick != lastProcessedResumeTick
        val syncResult = syncCounterStateForScope(
            context = context,
            templateState = templateState,
            counterUi = counterUi,
            counterStreamContext = counterStreamContext,
            scopedCounterStream = scopedCounterStream,
            previewCounterDigits = previewCounterDigits,
            saveMode = tableSaveMode,
            isManualCounterModeDisplay = isManualCounterModeDisplay,
            lastScopeSnapshot = lastScopeSnapshot,
            isExternalResync = isExternalResync,
            updateCell = ::updateCell
        )
        counterUi = syncResult.counterUi
        lastScopeSnapshot = syncResult.nextScopeSnapshot
        lastProcessedResumeTick = resumeTick
        syncResult.updatedTemplateState?.let(onTemplateChange)
    }

    var inlineEdit by remember { mutableStateOf(InlineEditState()) }
    val deletedRowsStack = remember { mutableStateListOf<List<TableCellState>>() }
    val deletedColsStack = remember { mutableStateListOf<List<TableCellState>>() }

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

    fun sanitizePathGroupAfterStructureChange(state: TableTemplateState): TableTemplateState {
        val hasG1 = state.cells.any { it.groupLevel == GroupLevel.G1 }
        if (hasG1) return state

        val nextCells = state.cells.map { cellState ->
            if (cellState.groupLevel == GroupLevel.G2) {
                cellState.copy(groupLevel = GroupLevel.NONE)
            } else {
                cellState
            }
        }
        return state.copy(cells = nextCells)
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
                saveMode = tableSaveMode,
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

    val namingPreview = remember(
        planForScope.resolvedCells,
        templateState.fileNameSlots,
        previewCounterDigits,
        counterUi.includePathInCounterScope,
        counterUi.includeFilenameInCounterScope,
        scopeDateTimeValues.dateScopeValues,
        scopeDateTimeValues.timeScopeValues,
        counterStreamContext.nextCounter,
    ) {
        CaptureNamingPolicy.buildForCaptureWithCounter(
            captureContext = CaptureContext(
                resolvedCells = planForScope.resolvedCells,
                fileNameSlots = templateState.fileNameSlots,
                fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
                counterDigits = previewCounterDigits,
                dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
                timeFormat = NamingFormatDefaults.TIME_FORMAT_CAPTURE_DEFAULT,
                includePathInCounterScope = counterUi.includePathInCounterScope,
                includeFilenameInCounterScope = counterUi.includeFilenameInCounterScope,
                dateScopeValues = scopeDateTimeValues.dateScopeValues,
                timeScopeValues = scopeDateTimeValues.timeScopeValues,
            ),
            usedCounter = counterStreamContext.nextCounter,
        )
    }
    val savePathPreview = namingPreview.relativePath
    val filenamePreview = namingPreview.displayName

    fun handleCounterConflictDialogEffect(effect: com.dudoziworkshop.dzlog.feature.table.policy.TableCounterConflictDialogEffect) {
        applyCounterConflictDialogEffect(
            effect = effect,
            context = context,
            templateState = templateState,
            scopedCounterStream = scopedCounterStream,
            previewCounterDigits = previewCounterDigits,
            saveMode = tableSaveMode,
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

    TableFormatDialog(
        state = formatDialog,
        templateState = templateState,
        onTemplateChange = onTemplateChange,
        onClose = { formatDialog = formatDialog.close() },
        dateFormatOptions = dateFormatOptions
    )

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
            if (shouldBlockTabSwitchAfterCommit(inlineEdit)) return
        }
        selectedTabIndex = targetIndex
    }

    Scaffold(
        containerColor = DDZColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier,
                title = {
                    Text(
                        "표 상세설정",
                        style = DDZTypography.ScreenTitle,
                        color = DDZColor.Primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = DDZColor.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DDZColor.Background,
                    navigationIconContentColor = DDZColor.Primary,
                    titleContentColor = DDZColor.Primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .dzScaffoldContent()
                .padding(innerPadding)
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
                            counterModeLabel = if (isManualCounterModeDisplay) "수동" else "자동",
                            templateState = templateState,
                            plan = plan,
                            previewNow = previewNow,
                            previewCounterDigits = previewCounterDigits,
                            scopeNextCounter = counterUi.scopeNextCounter,
                            dateFormat = dateFormat,
                            timeFormat = timeFormat,
                            selectedCellId = selectedCellId,
                            editingCellId = inlineEdit.editingCellId,
                            editingValue = inlineEdit.editingValue,
                            inlineFocusRequester = inlineFocusRequester,
                            showCellSettingsPanel = showCellSettingsPanel,
                            selectedCell = selectedCell,
                            hasGroup1 = hasGroup1,
                            hasGroup2 = hasGroup2,
                            isSavingTemplate = isSavingTemplate,
                            autoNextCounterValue = counterUi.autoNextCounterValue,
                            phraseSets = templateState.phraseSets,
                            captureAspect = watermarkUi.captureAspect
                        ),
                        actions = LayoutTabActions(
                            onSelectCellId = { selectedCellId = it },
                            onShowCellSettingsPanel = { showCellSettingsPanel = it },
                            onStartInlineEditing = { cellId, value ->
                                inlineEdit = startInlineEditing(inlineEdit, cellId, value)
                            },
                            onOpenFormatDialog = { cellId, type ->
                                formatDialog = formatDialog.open(cellId, type)
                            },
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
                            onAddRow = {
                                val restored = deletedRowsStack.lastOrNull()
                                if (restored != null) {
                                    deletedRowsStack.removeAt(deletedRowsStack.lastIndex)
                                    val newRowIndex = templateState.rows
                                    val restoredReindexed = restored.map { it.copy(rowIndex = newRowIndex) }
                                    val baseRowWeights = templateState.rowWeights
                                        ?: List(templateState.rows.coerceAtLeast(1)) { 1f }
                                    val updated = templateState.copy(
                                        rows = templateState.rows + 1,
                                        cells = templateState.cells + restoredReindexed,
                                        rowWeights = baseRowWeights + 1f
                                    )
                                    onTemplateChange(sanitizePathGroupAfterStructureChange(updated))
                                } else {
                                    onTemplateChange(sanitizePathGroupAfterStructureChange(addRow(templateState)))
                                }
                            },
                            onRemoveRow = {
                                val lastRowIndex = templateState.rows - 1
                                val lastRowCells = templateState.cells
                                    .filter { it.rowIndex == lastRowIndex }
                                    .sortedBy { it.colIndex }
                                if (lastRowCells.isNotEmpty()) {
                                    deletedRowsStack.add(lastRowCells)
                                }

                                val updated = removeRow(templateState)
                                val sanitized = sanitizePathGroupAfterStructureChange(updated)
                                onTemplateChange(sanitized)
                                if (sanitized.cells.none { it.cellId == selectedCellId }) {
                                    selectedCellId = sanitized.cells.firstOrNull()?.cellId
                                }
                            },
                            onAddCol = {
                                val restored = deletedColsStack.lastOrNull()
                                if (restored != null) {
                                    deletedColsStack.removeAt(deletedColsStack.lastIndex)
                                    val newColIndex = templateState.cols
                                    val restoredReindexed = restored.map { it.copy(colIndex = newColIndex) }
                                    val baseColWeights = templateState.colWeights
                                        ?: List(templateState.cols.coerceAtLeast(1)) { 1f }
                                    val updated = templateState.copy(
                                        cols = templateState.cols + 1,
                                        cells = templateState.cells + restoredReindexed,
                                        colWeights = baseColWeights + 1f
                                    )
                                    onTemplateChange(sanitizePathGroupAfterStructureChange(updated))
                                } else {
                                    onTemplateChange(sanitizePathGroupAfterStructureChange(addColumn(templateState)))
                                }
                            },
                            onRemoveCol = {
                                val lastColIndex = templateState.cols - 1
                                val lastColCells = templateState.cells
                                    .filter { it.colIndex == lastColIndex }
                                    .sortedBy { it.rowIndex }
                                if (lastColCells.isNotEmpty()) {
                                    deletedColsStack.add(lastColCells)
                                }

                                val updated = removeColumn(templateState)
                                val sanitized = sanitizePathGroupAfterStructureChange(updated)
                                onTemplateChange(sanitized)
                                if (sanitized.cells.none { it.cellId == selectedCellId }) {
                                    selectedCellId = sanitized.cells.firstOrNull()?.cellId
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
                                                "저장 실패: ${it.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            isSavingTemplate = false
                                        }
                                        .onSuccess {
                                            Toast.makeText(context, "저장됨", Toast.LENGTH_SHORT).show()
                                            onBack()
                                        }
                                }
                            },
                            onDismissSettingsPanel = {
                                commitInlineEditIfNeeded()
                                showCellSettingsPanel = false
                            },
                            onToggleFileNameForSelected = { cellId, enabled ->
                                val nextSlots = if (enabled) {
                                    addToFileNameSlots(templateState.fileNameSlots, cellId)
                                } else {
                                    removeFromFileNameSlots(templateState.fileNameSlots, cellId)
                                }
                                if (nextSlots != templateState.fileNameSlots) {
                                    onTemplateChange(templateState.copy(fileNameSlots = nextSlots))
                                }
                            },
                            onReorderFileNameSlots = { fromIndex, toIndex ->
                                if (fromIndex != toIndex && fromIndex in 0..2 && toIndex in 0..2) {
                                    val slots = templateState.fileNameSlots.take(3).toMutableList()
                                    while (slots.size < 3) slots.add(null)

                                    val temp = slots[fromIndex]
                                    slots[fromIndex] = slots[toIndex]
                                    slots[toIndex] = temp

                                    if (slots != templateState.fileNameSlots) {
                                        onTemplateChange(templateState.copy(fileNameSlots = slots))
                                    }
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
                            onSetCounterScopeModeForSelected = { mode ->
                                selectedCell?.let { cell ->
                                    if (cell.dataType != TableCellDataType.DATE && cell.dataType != TableCellDataType.TIME) return@let
                                    val updated = updateCell(templateState, cell.cellId) { c ->
                                        c.copy(counterScopeMode = mode)
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
                                        saveMode = tableSaveMode,
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
                        wmOffsetXRatio = watermarkUi.wmOffsetXRatio,
                        wmOffsetYRatio = watermarkUi.wmOffsetYRatio,
                        wmWidthRatio = watermarkUi.wmWidthRatio,
                        wmHeightRatio = watermarkUi.wmHeightRatio,
                        wmBgStyle = watermarkUi.wmBgStyle,
                        wmBgAlpha = watermarkUi.wmBgAlpha,
                        wmValueScale = watermarkUi.wmValueScale,
                        wmTextColorMode = watermarkUi.wmTextColorMode,
                        wmManualTextColor = watermarkUi.wmManualTextColor,
                        wmTextAlign = watermarkUi.wmTextAlign,
                        wmGridEnabled = watermarkUi.wmGridEnabled,
                        onCaptureAspectChange = { aspect ->
                            scope.launch {
                                watermarkUi = applyCaptureAspectChange(context, aspect, watermarkUi)
                            }
                        },
                        onWatermarkDragPreview = { _, _ ->
                            // 드래그 중에는 로컬 프리뷰만 갱신하고 상위 상태/SSOT 갱신은 하지 않음
                        },
                        onWatermarkDragCommit = { offsetX, offsetY ->
                            scope.launch {
                                watermarkUi = applyAnchorOffsetDragChange(context, offsetX, offsetY, watermarkUi)
                            }
                        },
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
                        },
                        onTextColorModeChange = { mode ->
                            scope.launch {
                                watermarkUi = applyTextColorModeChange(context, mode, watermarkUi)
                            }
                        },
                        onManualTextColorChange = { color ->
                            scope.launch {
                                watermarkUi = applyManualTextColorChange(context, color, watermarkUi)
                            }
                        },
                        onTextAlignChange = { align ->
                            scope.launch {
                                watermarkUi = applyTextAlignChange(context, align, watermarkUi)
                            }
                        },
                        onGridEnabledChange = { enabled ->
                            scope.launch {
                                watermarkUi = applyGridEnabledChange(context, enabled, watermarkUi)
                            }
                        }
                    )
                }
            }
        }
    }
}



private data class CounterScopeDateTimeValues(
    val dateScopeValues: List<String>,
    val timeScopeValues: List<String>
)


private fun buildCounterScopeDateTimeValues(
    cells: List<TableCellState>,
    resolvedCells: List<com.dudoziworkshop.dzlog.domain.table.ResolvedCell>
): CounterScopeDateTimeValues {
    val resolvedById = resolvedCells.associateBy { it.id }
    val ordered = cells.sortedWith(compareBy<TableCellState> { it.rowIndex }.thenBy { it.colIndex }.thenBy { it.cellId })
    val dateValues = ordered
        .asSequence()
        .filter { it.dataType == TableCellDataType.DATE && it.counterScopeMode == CounterScopeMode.INCLUDE }
        .mapNotNull { resolvedById[it.cellId]?.resolvedText?.takeIf { text -> text.isNotBlank() } }
        .toList()
    val timeValues = ordered
        .asSequence()
        .filter { it.dataType == TableCellDataType.TIME && it.counterScopeMode == CounterScopeMode.INCLUDE }
        .mapNotNull { resolvedById[it.cellId]?.resolvedText }
        .map(::normalizeTimeToMinute)
        .filter { it.isNotBlank() }
        .toList()
    return CounterScopeDateTimeValues(dateScopeValues = dateValues, timeScopeValues = timeValues)
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
            timeFormatOptions = null,
            counterScopeMode = null
        )

        TableCellDataType.NUMBER -> this.copy(
            dataType = newType,
            typedValue = CellValue.Number(this.rawText),
            timeFormatOptions = null,
            counterScopeMode = null
        )

        TableCellDataType.DATE -> this.copy(
            dataType = newType,
            typedValue = CellValue.Auto,
            timeFormatOptions = null,
            counterScopeMode = CounterScopeMode.EXCLUDE
        )

        TableCellDataType.TIME -> this.copy(
            dataType = newType,
            typedValue = CellValue.Auto,
            timeFormatOptions = this.timeFormatOptions ?: TimeFormatOptions(),
            counterScopeMode = CounterScopeMode.EXCLUDE
        )

        TableCellDataType.COUNTER -> this.copy(
            dataType = newType,
            typedValue = (this.typedValue as? CellValue.CounterSeed) ?: CellValue.CounterSeed(1),
            timeFormatOptions = null,
            counterScopeMode = null
        )

        TableCellDataType.ROTATING_TEXT -> this.copy(
            dataType = newType,
            typedValue = CellValue.Auto,
            timeFormatOptions = null,
            counterScopeMode = null
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
