@file:Suppress("UNUSED_VALUE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.dzlog.ui.table

import android.graphics.RectF
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.UUID
import com.example.dzlog.R
import com.example.dzlog.data.counter.COUNTER_DIGITS_DEFAULT
import com.example.dzlog.data.counter.clampCounterDigits
import com.example.dzlog.data.datastore.AppSettingsStore
import com.example.dzlog.data.preferences.KEY_CAPTURE_ASPECT
import com.example.dzlog.data.preferences.KEY_COUNTER_DIGITS
import com.example.dzlog.data.preferences.KEY_WM_BG_ALPHA
import com.example.dzlog.data.preferences.KEY_WM_TABLE_ANCHOR
import com.example.dzlog.data.preferences.KEY_WM_TABLE_BG_STYLE
import com.example.dzlog.data.preferences.KEY_WM_TABLE_HEIGHT
import com.example.dzlog.data.preferences.KEY_WM_TABLE_WIDTH
import com.example.dzlog.data.preferences.KEY_WM_VALUE_SCALE
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.domain.counter.CounterManager
import com.example.dzlog.domain.counter.buildCounterStreamContext
import com.example.dzlog.domain.counter.policy.CounterScopeSnapshot
import com.example.dzlog.domain.counter.policy.buildCounterScopeSnapshot
import com.example.dzlog.domain.counter.toCaptureScopedCounterStream
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.CellValue
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.HourSystem
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellKind
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.model.RotatingPhraseSet
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.TimeFormatOptions
import com.example.dzlog.domain.model.TimeSeparator
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.naming.NamingFormatDefaults
import com.example.dzlog.domain.naming.buildDisplayNameFromResolvedCells
import com.example.dzlog.domain.naming.buildGalleryRelativePath
import com.example.dzlog.domain.preview.computeNextDelayMillis
import com.example.dzlog.domain.preview.decideTickUnit
import com.example.dzlog.domain.table.TableResolver
import com.example.dzlog.domain.watermark.WatermarkBuilder
import com.example.dzlog.feature.table.policy.TableCounterConflictDialogEffect
import com.example.dzlog.feature.table.policy.TableCounterConflictDialogState
import com.example.dzlog.feature.table.policy.TableCounterPolicyCoordinator
import com.example.dzlog.feature.table.policy.TableWatermarkAction
import com.example.dzlog.feature.table.policy.applyTableWatermarkAction
import com.example.dzlog.feature.table.policy.confirmCounterConflictDialog
import com.example.dzlog.feature.table.policy.dismissCounterConflictDialog
import com.example.dzlog.feature.table.policy.evaluateCounterEditConflict
import com.example.dzlog.feature.table.policy.openCounterConflictDialog
import com.example.dzlog.feature.table.policy.parseNonNegativeInt
import com.example.dzlog.feature.table.policy.saveTableTemplate
import com.example.dzlog.ui.table.section.LayoutTabActions
import com.example.dzlog.ui.table.section.LayoutTabContent
import com.example.dzlog.ui.table.section.LayoutTabUiState
import com.example.dzlog.ui.table.section.PreviewTabContent
import com.example.dzlog.ui.table.section.TableEditorTabs
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography
import com.example.dzlog.watermark.drawWatermarkTableOnCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date


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

    var showRotatingTemplateDialog by remember { mutableStateOf(false) }
    var rotatingDialogCellId by remember { mutableStateOf<String?>(null) }
    var rotatingDialogRestoreState by remember { mutableStateOf<TableTemplateState?>(null) }
    var showCreatePhraseSetDialog by remember { mutableStateOf(false) }
    var newPhraseSetName by remember { mutableStateOf("") }
    var pendingDeletePhraseSetId by remember { mutableStateOf<String?>(null) }
    var showPhraseSetEditDialog by remember { mutableStateOf(false) }
    var editingPhraseSetId by remember { mutableStateOf<String?>(null) }

    fun openRotatingTemplateDialog(targetCellId: String) {
        rotatingDialogCellId = targetCellId
        rotatingDialogRestoreState = templateState
        showRotatingTemplateDialog = true
        showCellSettingsPanel = false
    }

    fun closeRotatingTemplateDialog() {
        showRotatingTemplateDialog = false
        rotatingDialogCellId = null
        rotatingDialogRestoreState = null
        pendingDeletePhraseSetId = null
        showCreatePhraseSetDialog = false
        newPhraseSetName = ""
        showPhraseSetEditDialog = false
        editingPhraseSetId = null
    }

    val dateFormatOptions = listOf(NamingFormatDefaults.DATE_FORMAT_DEFAULT, "yyyy_MM_dd", "yyyyMMdd")
    // TIME 형식은 Step3부터 토글 UI(12/24, 초, 구분자)로 설정한다.

    // NOTE: formatPattern 기반이 아니라 현재는 고정값. (기존 코드 유지)
    val dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT
    val timeFormat = NamingFormatDefaults.TIME_FORMAT_CAPTURE_DEFAULT

    var previewCounterDigits by remember { mutableIntStateOf(COUNTER_DIGITS_DEFAULT) }

    var scopeNextCounter by remember { mutableIntStateOf(1) }
    var counterConflictDialogState by remember { mutableStateOf(TableCounterConflictDialogState()) }
    var preserveManualCounterSeed by remember { mutableStateOf(false) }
    var manualSeedOverride by remember { mutableStateOf<Int?>(null) }
    var isManualCounterMode by remember { mutableStateOf(false) }
    var autoNextCounterValue by remember { mutableIntStateOf(1) }
    var includePathInCounterScope by remember { mutableStateOf(true) }
    var includeFilenameInCounterScope by remember { mutableStateOf(true) }

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
            includePathInCounterScope = settings.includePathInCounterScope
            includeFilenameInCounterScope = settings.includeFilenameInCounterScope
        }
    }

    val tableResolver = remember { TableResolver() }
    val planForScope = remember(templateState, previewNow, previewCounterDigits, scopeNextCounter, dateFormat, timeFormat) {
        tableResolver.plan(
            cells = templateState.cells,
            captureNow = previewNow,
            config = TableResolver.Config(
                counterDigits = previewCounterDigits,
                dateFormat = dateFormat,
                timeFormat = timeFormat
            ),
            counterSeedOverride = scopeNextCounter,
            phraseSets = templateState.phraseSets
        )
    }
    val isManualCounterModeDisplay by remember(
        isManualCounterMode,
        preserveManualCounterSeed,
        scopeNextCounter,
        autoNextCounterValue
    ) {
        derivedStateOf {
            isManualCounterMode || (preserveManualCounterSeed && scopeNextCounter > autoNextCounterValue)
        }
    }

    val counterStreamContext by remember(planForScope.resolvedCells, scopeNextCounter, isManualCounterModeDisplay) {
        derivedStateOf {
            buildCounterStreamContext(
                resolvedCells = planForScope.resolvedCells,
                nextCounter = scopeNextCounter,
                isManualMode = isManualCounterModeDisplay,
                fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER
            )
        }
    }

    val scopedCounterStream by remember(
        counterStreamContext,
        includePathInCounterScope,
        includeFilenameInCounterScope
    ) {
        derivedStateOf {
            toCaptureScopedCounterStream(
                streamContext = counterStreamContext,
                includePathInScope = includePathInCounterScope,
                includeFilenameInScope = includeFilenameInCounterScope
            )
        }
    }

    fun applyCounterSeed(seed: Int, preserveManual: Boolean) {
        val normalizedSeed = seed.coerceAtLeast(1)
        preserveManualCounterSeed = preserveManual
        manualSeedOverride = if (preserveManual) normalizedSeed else null
        scopeNextCounter = normalizedSeed
    }

    fun updateCounterCellAndPolicy(
        cellId: String,
        seed: Int,
        preserveManual: Boolean,
        forcePolicyUpdate: Boolean
    ) {
        val normalizedSeed = seed.coerceAtLeast(1)
        val updated = updateCell(templateState, cellId) { c ->
            c.copy(typedValue = CellValue.CounterSeed(normalizedSeed))
        }
        onTemplateChange(updated)
        applyCounterSeed(seed = normalizedSeed, preserveManual = preserveManual)
        if (preserveManual || forcePolicyUpdate) {
            scope.launch {
                TableCounterPolicyCoordinator.setNextCounter(
                    context = context,
                    scopedStream = scopedCounterStream,
                    desired = normalizedSeed,
                    force = forcePolicyUpdate,
                    counterDigits = previewCounterDigits,
                )
            }
        }
    }

    // ✅ 스트림 변경 감지용 (스트림이 바뀌면 seed를 "새 스트림 next"로 강제 동기화)
    var lastScopeSnapshot by remember { mutableStateOf<CounterScopeSnapshot?>(null) }

    LaunchedEffect(scopedCounterStream.scopeParts.scopeKey, previewCounterDigits, templateState) {
        // ✅ 정책(스트림키=relativePathPrefix) 기준 nextCounter 계산

        val counterCell = templateState.cells.firstOrNull { it.dataType == TableCellDataType.COUNTER }
        val currentSeed = (counterCell?.typedValue as? CellValue.CounterSeed)?.start ?: 1
        val streamNext = TableCounterPolicyCoordinator.getNextCounter(
            context = context,
            scopedStream = scopedCounterStream,
            counterDigits = previewCounterDigits,
        ).coerceAtLeast(1)
        isManualCounterMode = TableCounterPolicyCoordinator.isManualOverrideActive(
            context = context,
            scopedStream = scopedCounterStream
        )
        autoNextCounterValue = CounterManager.getNextCounter(
            context = context,
            relativePath = scopedCounterStream.captureStreamKey.relativePathKey,
            counterPrefix = scopedCounterStream.captureStreamKey.prefix,
            counterDigits = previewCounterDigits,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER
        ).coerceAtLeast(1)

        // ✅ 스트림이 바뀌면 "새 스트림의 next"로 맞춘다.
        // ✅ 같은 스트림에서는 사용자 수동 seed(낮은 값 포함)를 유지한다.
        val syncResult = TableCounterPolicyCoordinator.resolveSeedForScope(
            input = TableCounterPolicyCoordinator.CounterSeedSyncInput(
                currentScopeSnapshot = buildCounterScopeSnapshot(
                    streamContext = counterStreamContext,
                    includePathInScope = includePathInCounterScope,
                    includeFilenameInScope = includeFilenameInCounterScope,
                ),
                isManualMode = isManualCounterModeDisplay,
                hasCounterCell = (counterCell != null),
                currentSeed = currentSeed,
                streamNext = streamNext,
                previousScopeSnapshot = lastScopeSnapshot,
                preserveManualCounterSeed = preserveManualCounterSeed,
                manualSeedOverride = manualSeedOverride
            )
        )

        preserveManualCounterSeed = syncResult.preserveManualCounterSeed

        if (syncResult.shouldClearManualOverride) {
            manualSeedOverride = null
        }

        scopeNextCounter = syncResult.desiredSeed
        lastScopeSnapshot = buildCounterScopeSnapshot(
            streamContext = counterStreamContext,
            includePathInScope = includePathInCounterScope,
            includeFilenameInScope = includeFilenameInCounterScope,
        )

        // ✅ 표시 ON/OFF와 무관하게, COUNTER 셀이 존재하면 seed는 정책 기준으로 항상 최신으로 맞춰둔다.
        if (counterCell != null && currentSeed != syncResult.desiredSeed) {
            val updated = updateCell(templateState, counterCell.cellId) { c ->
                c.copy(typedValue = CellValue.CounterSeed(syncResult.desiredSeed))
            }
            onTemplateChange(updated)
        }
    }

    var editingCellId by remember { mutableStateOf<String?>(null) }
    var editingValue by remember { mutableStateOf("") }
    var editingOriginalValue by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val inlineFocusRequester = remember { FocusRequester() }

    LaunchedEffect(editingCellId) {
        if (editingCellId != null) {
            delay(30)
            inlineFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    fun startInlineEditing(cellId: String, value: String) {
        editingCellId = cellId
        editingValue = value
        editingOriginalValue = value
    }

    fun clearInlineEditingState() {
        editingCellId = null
        editingValue = ""
        editingOriginalValue = ""
    }

    fun commitInlineEditIfNeeded() {
        val id = editingCellId ?: return
        val cell = templateState.cells.firstOrNull { it.cellId == id }

        // COUNTER 충돌 정책: stream next보다 작은 값을 입력하면 경고 후 진행 여부를 확인한다.
        if (cell != null && cell.dataType == TableCellDataType.COUNTER) {
            val conflict = evaluateCounterEditConflict(
                oldValueText = editingOriginalValue,
                newValueText = editingValue,
                streamNext = autoNextCounterValue
            )
            if (parseNonNegativeInt(editingValue) == null) return
            if (conflict != null) {
                counterConflictDialogState = openCounterConflictDialog(
                    editingCellId = id,
                    conflict = conflict
                )
                return
            }
        }

        val target = templateState.cells.firstOrNull { it.cellId == id }
        val normalizedValueText = if (target?.dataType == TableCellDataType.COUNTER) {
            val value = parseNonNegativeInt(editingValue)
            if (value == null) {
                clearInlineEditingState()
                return
            }
            value.toString()
        } else {
            editingValue
        }

        val updated = updateCell(templateState, id) { c ->
            when (c.dataType) {
                TableCellDataType.TEXT -> c.copy(
                    rawText = normalizedValueText,
                    typedValue = CellValue.Text(normalizedValueText)
                )
                TableCellDataType.NUMBER -> c.copy(
                    rawText = normalizedValueText,
                    typedValue = CellValue.Number(normalizedValueText)
                )
                TableCellDataType.COUNTER -> {
                    val seed = normalizedValueText.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
                    c.copy(typedValue = CellValue.CounterSeed(seed))
                }
                else -> c
            }
        }

        onTemplateChange(updated)
        if (target?.dataType == TableCellDataType.COUNTER) {
            val seed = normalizedValueText.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
            val normalizedSeed = seed.coerceAtLeast(1)
            applyCounterSeed(seed = normalizedSeed, preserveManual = true)
            scope.launch {
                TableCounterPolicyCoordinator.setNextCounter(
                    context = context,
                    scopedStream = scopedCounterStream,
                    desired = normalizedSeed,
                    force = false,
                    counterDigits = previewCounterDigits,
                )
            }
        }
        clearInlineEditingState()
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

    // ✅ 표 위치/크기(촬영 워터마크 표 렌더 파라미터) - 탭1에서 조절
    var wmAnchor by remember { mutableStateOf(WatermarkTableAnchor.BOTTOM_RIGHT) }
    var wmWidthRatio by remember { mutableIntStateOf(40) }
    var wmHeightRatio by remember { mutableIntStateOf(20) }
    // 0=BLACK, 1=WHITE, 2=TRANSPARENT
    var wmBgStyle by remember { mutableIntStateOf(0) }
    // 0~255
    var wmBgAlpha by remember { mutableIntStateOf(80) }
    // 60~160 (기본 100)
    var wmValueScale by remember { mutableIntStateOf(100) }

    // ✅ 촬영 프레임 비율(탭1 미리보기에서 사용)
    var captureAspect by remember { mutableStateOf(CaptureAspect.R3_4) }


    LaunchedEffect(Unit) {
        runCatching {
            val prefs = context.dataStore.data.first()
            wmAnchor = when (prefs[KEY_WM_TABLE_ANCHOR] ?: 3) {
                0 -> WatermarkTableAnchor.TOP_LEFT
                1 -> WatermarkTableAnchor.TOP_RIGHT
                2 -> WatermarkTableAnchor.BOTTOM_LEFT
                else -> WatermarkTableAnchor.BOTTOM_RIGHT
            }
            wmWidthRatio = (prefs[KEY_WM_TABLE_WIDTH] ?: 40).coerceIn(40, 100)
            wmHeightRatio = (prefs[KEY_WM_TABLE_HEIGHT] ?: 20).coerceIn(10, 35)
            wmBgStyle = (prefs[KEY_WM_TABLE_BG_STYLE] ?: 0).coerceIn(0, 2)
            wmBgAlpha = (prefs[KEY_WM_BG_ALPHA] ?: 80).coerceIn(0, 255)
            wmValueScale = (prefs[KEY_WM_VALUE_SCALE] ?: 100).coerceIn(60, 160)

            captureAspect = CaptureAspect.from(
                prefs[KEY_CAPTURE_ASPECT] ?: CaptureAspect.R3_4.v
            )
        }
    }

    val plan = remember(templateState.cells, previewNow, previewCounterDigits, scopeNextCounter, dateFormat, timeFormat) {
        tableResolver.plan(
            cells = templateState.cells,
            captureNow = previewNow,
            config = TableResolver.Config(
                counterDigits = previewCounterDigits,
                dateFormat = dateFormat,
                timeFormat = timeFormat
            ),
            counterSeedOverride = scopeNextCounter,
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

    suspend fun fetchAutoNextCounter(): Int = TableCounterPolicyCoordinator.resetToAutoNext(
        context = context,
        scopedStream = scopedCounterStream,
        counterDigits = previewCounterDigits,
    ).coerceAtLeast(1)

    fun restoreCounterCellToAutoNext(cellId: String) {
        scope.launch {
            val restored = fetchAutoNextCounter()
            updateCounterCellAndPolicy(
                cellId = cellId,
                seed = restored,
                preserveManual = false,
                forcePolicyUpdate = false
            )
        }
    }

    fun applyCounterConflictDialogEffect(effect: TableCounterConflictDialogEffect) {
        when (effect) {
            is TableCounterConflictDialogEffect.ApplyManualSeed -> {
                updateCounterCellAndPolicy(
                    cellId = effect.cellId,
                    seed = effect.seed,
                    preserveManual = true,
                    forcePolicyUpdate = true
                )
            }

            is TableCounterConflictDialogEffect.RestoreAutoNext -> {
                restoreCounterCellToAutoNext(effect.cellId)
            }

            TableCounterConflictDialogEffect.None -> Unit
        }
    }

    if (counterConflictDialogState.isVisible) {
        AlertDialog(
            onDismissRequest = {
                val (nextState, effect) = dismissCounterConflictDialog(counterConflictDialogState)
                counterConflictDialogState = nextState
                applyCounterConflictDialogEffect(effect)
                clearInlineEditingState()
            },
            title = { Text("카운터 충돌 경고") },
            text = {
                Text(
                    "중복된 카운터가 발생할 수 있습니다. 계속 진행하시겠습니까?\n\n" +
                        "입력값: ${counterConflictDialogState.pendingCounterCommitValue}\n" +
                        "현재 스트림 next: ${counterConflictDialogState.pendingCounterStreamNextValue}"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val (nextState, effect) = confirmCounterConflictDialog(counterConflictDialogState)
                    counterConflictDialogState = nextState
                    applyCounterConflictDialogEffect(effect)
                    clearInlineEditingState()
                }) { Text("진행", style = DDZTypography.ButtonText) }
            },
            dismissButton = {
                TextButton(onClick = {
                    val (nextState, effect) = dismissCounterConflictDialog(counterConflictDialogState)
                    counterConflictDialogState = nextState
                    applyCounterConflictDialogEffect(effect)
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

    if (showRotatingTemplateDialog) {
        val dialogCell = templateState.cells.firstOrNull { it.cellId == rotatingDialogCellId }
        if (dialogCell == null || dialogCell.dataType != TableCellDataType.ROTATING_TEXT) {
            closeRotatingTemplateDialog()
        } else {
            RotatingTemplateDialog(
                cell = dialogCell,
                phraseSets = templateState.phraseSets,
                onDismiss = { closeRotatingTemplateDialog() },
                onRestore = {
                    rotatingDialogRestoreState?.let { onTemplateChange(it) }
                    closeRotatingTemplateDialog()
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
                    newPhraseSetName = ""
                    showCreatePhraseSetDialog = true
                },
                onRequestDeleteSet = { phraseSetId ->
                    pendingDeletePhraseSetId = phraseSetId
                },
                onRequestEditSet = { phraseSetId ->
                    editingPhraseSetId = phraseSetId
                    showPhraseSetEditDialog = true
                }
            )
        }
    }

    if (showPhraseSetEditDialog) {
        val editingSet = templateState.phraseSets.firstOrNull { it.id == editingPhraseSetId }
        if (editingSet == null) {
            showPhraseSetEditDialog = false
            editingPhraseSetId = null
        } else {
            PhraseSetEditDialog(
                phraseSet = editingSet,
                onClose = {
                    showPhraseSetEditDialog = false
                    editingPhraseSetId = null
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
                    showPhraseSetEditDialog = false
                    editingPhraseSetId = null
                }
            )
        }
    }

    if (showCreatePhraseSetDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePhraseSetDialog = false },
            title = { Text("새 템플릿 추가") },
            text = {
                OutlinedTextField(
                    value = newPhraseSetName,
                    onValueChange = { newPhraseSetName = it },
                    singleLine = true,
                    label = { Text("세트 이름") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newPhraseSetName.trim()
                    if (name.isNotEmpty() && rotatingDialogCellId != null) {
                        val created = RotatingPhraseSet(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            items = emptyList(),
                            defaultEvery = 1
                        )
                        val updatedTemplate = templateState.copy(
                            phraseSets = templateState.phraseSets + created,
                            cells = templateState.cells.map { cell ->
                                if (cell.cellId == rotatingDialogCellId) {
                                    cell.copy(phraseSetId = created.id, everyOverride = null)
                                } else {
                                    cell
                                }
                            }
                        )
                        onTemplateChange(updatedTemplate)
                    }
                    showCreatePhraseSetDialog = false
                }) {
                    Text("추가", style = DDZTypography.ButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePhraseSetDialog = false }) {
                    Text("취소", style = DDZTypography.ButtonText)
                }
            }
        )
    }

    pendingDeletePhraseSetId?.let { deleteId ->
        val deleteTarget = templateState.phraseSets.firstOrNull { it.id == deleteId }
        if (deleteTarget != null) {
            AlertDialog(
                onDismissRequest = { pendingDeletePhraseSetId = null },
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
                        pendingDeletePhraseSetId = null
                    }) {
                        Text("삭제", style = DDZTypography.ButtonText)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeletePhraseSetId = null }) {
                        Text("취소", style = DDZTypography.ButtonText)
                    }
                }
            )
        } else {
            pendingDeletePhraseSetId = null
        }
    }

    // ✅ 탭 전환: 편집 중이면 먼저 commit (중복 다이얼로그 등으로 commit이 보류되면 탭 전환 막기)
    fun requestTabSwitch(targetIndex: Int) {
        if (selectedTabIndex == targetIndex) return

        if (editingCellId != null) {
            commitInlineEditIfNeeded()
            // commit이 보류되면(=editingCellId가 유지됨) 전환 막음
            if (editingCellId != null) return
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
                            editingCellId = editingCellId,
                            editingValue = editingValue,
                            inlineFocusRequester = inlineFocusRequester,
                            showCellSettingsPanel = showCellSettingsPanel,
                            selectedCell = selectedCell,
                            hasGroup1 = hasGroup1,
                            hasGroup2 = hasGroup2,
                            isSavingTemplate = isSavingTemplate,
                            autoNextCounterValue = autoNextCounterValue
                        ),
                        actions = LayoutTabActions(
                            onSelectCellId = { selectedCellId = it },
                            onShowCellSettingsPanel = { showCellSettingsPanel = it },
                            onStartInlineEditing = ::startInlineEditing,
                            onOpenFormatDialog = ::openFormatDialog,
                            onEditingValueChange = { editingValue = it },
                            onCommitInline = ::commitInlineEditIfNeeded,
                            onTryCommitInlineAndContinue = {
                                commitInlineEditIfNeeded()
                                editingCellId == null
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
                                    scope.launch {
                                        val restored = fetchAutoNextCounter()
                                        updateCounterCellAndPolicy(
                                            cellId = cell.cellId,
                                            seed = restored,
                                            preserveManual = false,
                                            forcePolicyUpdate = false
                                        )
                                        Toast.makeText(context, "카운터를 자동 기준으로 초기화했습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            onOpenRotatingTemplateDialogForSelected = { cellId ->
                                val cell = templateState.cells.firstOrNull { it.cellId == cellId }
                                if (cell != null && cell.dataType == TableCellDataType.ROTATING_TEXT) {
                                    openRotatingTemplateDialog(cell.cellId)
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
                        captureAspect = captureAspect,
                        templateState = templateState,
                        resolvedCells = plan.resolvedCells,
                        wmAnchor = wmAnchor,
                        wmWidthRatio = wmWidthRatio,
                        wmHeightRatio = wmHeightRatio,
                        wmBgStyle = wmBgStyle,
                        wmBgAlpha = wmBgAlpha,
                        wmValueScale = wmValueScale,
                        onRowColWeightsChange = { updated -> onTemplateChange(updated) },
                        onWidthRatioChange = { width ->
                            scope.launch {
                                val patch = applyTableWatermarkAction(
                                    context,
                                    TableWatermarkAction.WidthRatioChanged(width)
                                )
                                patch.widthRatio?.let { wmWidthRatio = it }
                            }
                        },
                        onHeightRatioChange = { height ->
                            scope.launch {
                                val patch = applyTableWatermarkAction(
                                    context,
                                    TableWatermarkAction.HeightRatioChanged(height)
                                )
                                patch.heightRatio?.let { wmHeightRatio = it }
                            }
                        },
                        onBgStyleChange = { bgStyle ->
                            scope.launch {
                                val patch = applyTableWatermarkAction(
                                    context,
                                    TableWatermarkAction.BgStyleChanged(bgStyle)
                                )
                                patch.bgStyle?.let { wmBgStyle = it }
                            }
                        },
                        onBgAlphaChange = { alpha ->
                            scope.launch {
                                val patch = applyTableWatermarkAction(
                                    context,
                                    TableWatermarkAction.BgAlphaChanged(alpha)
                                )
                                patch.bgAlpha?.let { wmBgAlpha = it }
                            }
                        },
                        onValueScaleChange = { scale ->
                            scope.launch {
                                val patch = applyTableWatermarkAction(
                                    context,
                                    TableWatermarkAction.ValueScaleChanged(scale)
                                )
                                patch.valueScale?.let { wmValueScale = it }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RotatingTemplateDialog(
    cell: TableCellState,
    phraseSets: List<RotatingPhraseSet>,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onSelectSet: (String?) -> Unit,
    onEveryChange: (Int) -> Unit,
    onIncreaseEvery: () -> Unit,
    onDecreaseEvery: () -> Unit,
    onRequestCreateSet: () -> Unit,
    onRequestDeleteSet: (String) -> Unit,
    onRequestEditSet: (String) -> Unit
) {
    val selectedSet = phraseSets.firstOrNull { it.id == cell.phraseSetId }
    val everyEnabled = selectedSet != null
    val everyDisplay = (cell.everyOverride ?: selectedSet?.defaultEvery ?: 1).coerceAtLeast(1)
    val listState = rememberLazyListState()
    var showScrollIndicator by remember(cell.cellId, cell.phraseSetId) { mutableStateOf(false) }
    val indicatorAlpha by animateFloatAsState(if (showScrollIndicator) 1f else 0f, label = "rotatingDialogScrollIndicator")
    var everyInput by remember(cell.cellId, cell.phraseSetId, cell.everyOverride, selectedSet?.defaultEvery) {
        mutableStateOf(everyDisplay.toString())
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            showScrollIndicator = true
        } else {
            delay(600)
            showScrollIndicator = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(9.dp),
            shape = RoundedCornerShape(16.dp),
            color = DDZColor.BrownBg
        ) {
            Column(
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "문구 템플릿 설정",
                    style = DDZTypography.CardTitle,
                    color = DDZColor.TextPrimary
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 320.dp)
                        .background(DDZColor.Card, RoundedCornerShape(10.dp))
                        .padding(6.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(items = phraseSets, key = { it.id }) { set ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectSet(set.id) }
                                    .padding(horizontal = 6.dp, vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = cell.phraseSetId == set.id,
                                    onClick = { onSelectSet(set.id) }
                                )
                                Text(
                                    text = "${set.name} (${set.items.size}개)",
                                    modifier = Modifier.weight(1f),
                                    color = DDZColor.TextPrimary,
                                    style = DDZTypography.Body
                                )
                                TextButton(onClick = { onRequestEditSet(set.id) }) { Text("✏️") }
                                TextButton(onClick = { onRequestDeleteSet(set.id) }) { Text("🗑") }
                            }
                        }
                    }

                    if (phraseSets.isEmpty()) {
                        Text(
                            text = "항목을 추가해주세요.",
                            modifier = Modifier.align(Alignment.Center),
                            color = DDZColor.TextMuted,
                            style = DDZTypography.Body
                        )
                    }

                    val layoutInfo = listState.layoutInfo
                    val visibleItems = layoutInfo.visibleItemsInfo
                    val canScroll = layoutInfo.totalItemsCount > visibleItems.size

                    if (showScrollIndicator && canScroll && layoutInfo.totalItemsCount > 0 && visibleItems.isNotEmpty()) {
                        val density = LocalDensity.current
                        val viewportHeightPx = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(1)
                        val avgItemHeightPx = visibleItems.map { it.size }.average().toFloat().takeIf { it > 0f } ?: 1f
                        val totalContentHeightPx = (avgItemHeightPx * layoutInfo.totalItemsCount).coerceAtLeast(viewportHeightPx.toFloat())
                        val thumbHeightPx = ((viewportHeightPx.toFloat() / totalContentHeightPx) * viewportHeightPx)
                            .coerceIn(24f, viewportHeightPx.toFloat())
                        val firstVisible = visibleItems.first()
                        val scrollOffsetPx = (firstVisible.index * avgItemHeightPx) - firstVisible.offset
                        val maxScrollPx = (totalContentHeightPx - viewportHeightPx).coerceAtLeast(1f)
                        val thumbOffsetPx = ((scrollOffsetPx / maxScrollPx) * (viewportHeightPx - thumbHeightPx))
                            .coerceIn(0f, (viewportHeightPx - thumbHeightPx).coerceAtLeast(0f))

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 1.dp)
                                .width(3.dp)
                                .height(with(density) { thumbHeightPx.toDp() })
                                .offset(y = with(density) { thumbOffsetPx.toDp() })
                                .alpha(indicatorAlpha)
                                .background(DDZColor.TextMuted.copy(alpha = 0.5f), RoundedCornerShape(99.dp))
                        )
                    }
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onRequestCreateSet
                ) {
                    Text("+ 새 템플릿 추가", style = DDZTypography.ButtonText)
                }

                Spacer(modifier = Modifier.height(11.dp))

                Text(
                    text = "N장마다 다음 문구로 변경",
                    color = DDZColor.TextMuted,
                    style = DDZTypography.Caption
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        modifier = Modifier.height(38.dp),
                        onClick = onDecreaseEvery,
                        enabled = everyEnabled
                    ) { Text("-") }
                    OutlinedTextField(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        value = everyInput,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            everyInput = digits
                            val parsed = digits.toIntOrNull()
                            if (parsed != null) {
                                val clamped = parsed.coerceAtLeast(1)
                                onEveryChange(clamped)
                                if (clamped.toString() != digits) {
                                    everyInput = clamped.toString()
                                }
                            }
                        },
                        enabled = everyEnabled,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = DDZTypography.Body.copy(lineHeight = 20.sp)
                    )
                    OutlinedButton(
                        modifier = Modifier.height(38.dp),
                        onClick = onIncreaseEvery,
                        enabled = everyEnabled
                    ) { Text("+") }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onRestore) {
                        Text("복구", style = DDZTypography.ButtonText)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("닫기", style = DDZTypography.ButtonText)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhraseSetEditDialog(
    phraseSet: RotatingPhraseSet,
    onClose: () -> Unit,
    onUpdateSet: ((RotatingPhraseSet) -> RotatingPhraseSet) -> Unit,
    onDeleteSet: (String) -> Unit
) {
    var showRenameDialog by remember(phraseSet.id) { mutableStateOf(false) }
    var renameInput by remember(phraseSet.id) { mutableStateOf(phraseSet.name) }
    var showDeleteConfirm by remember(phraseSet.id) { mutableStateOf(false) }

    var showItemInputDialog by remember(phraseSet.id) { mutableStateOf(false) }
    var editingItemIndex by remember(phraseSet.id) { mutableStateOf<Int?>(null) }
    var itemInput by remember(phraseSet.id) { mutableStateOf("") }
    val itemIds = remember(phraseSet.id) { mutableStateListOf<String>() }

    LaunchedEffect(phraseSet.id, phraseSet.items.size) {
        val targetSize = phraseSet.items.size
        while (itemIds.size < targetSize) {
            itemIds.add(UUID.randomUUID().toString())
        }
        while (itemIds.size > targetSize) {
            itemIds.removeAt(itemIds.lastIndex)
        }

        val idx = editingItemIndex
        if (idx != null && idx !in phraseSet.items.indices) {
            editingItemIndex = null
        }
    }

    val lazyListState = rememberLazyListState()
    var showScrollIndicator by remember(phraseSet.id) { mutableStateOf(false) }
    val indicatorAlpha by animateFloatAsState(if (showScrollIndicator) 1f else 0f, label = "phraseSetScrollIndicator")

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) {
            showScrollIndicator = true
        } else {
            delay(600)
            showScrollIndicator = false
        }
    }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onUpdateSet { set ->
            val items = set.items
            if (items.isEmpty()) return@onUpdateSet set

            val fromIndex = from.index
            val toIndex = to.index.coerceIn(0, items.lastIndex)
            if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) {
                return@onUpdateSet set
            }

            val newList = items.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
            set.copy(items = newList)
        }

        if (from.index in itemIds.indices) {
            val idToMove = itemIds.removeAt(from.index)
            val insertIndex = to.index.coerceIn(0, itemIds.size)
            itemIds.add(insertIndex, idToMove)
        }
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = DDZColor.Card
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClose) {
                        Text("< 뒤로", style = DDZTypography.ButtonText)
                    }
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text("🗑", style = DDZTypography.ButtonText)
                    }
                }

                TextButton(onClick = {
                    renameInput = phraseSet.name
                    showRenameDialog = true
                }) {
                    Text(phraseSet.name, style = DDZTypography.CardTitle, color = DDZColor.TextPrimary)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 340.dp)
                        .background(DDZColor.Surface, RoundedCornerShape(10.dp))
                        .padding(6.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds(),
                        state = lazyListState,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(
                            phraseSet.items.size,
                            key = { idx -> itemIds.getOrNull(idx) ?: "${phraseSet.id}-$idx" }
                        ) { index ->
                            val item = phraseSet.items.getOrNull(index) ?: return@items
                            val stableId = itemIds.getOrNull(index) ?: "${phraseSet.id}-$index"

                            ReorderableItem(reorderableState, key = stableId) { isDragging ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (isDragging) DDZColor.Card.copy(alpha = 0.92f) else DDZColor.Card,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            editingItemIndex = index
                                            itemInput = item
                                            showItemInputDialog = true
                                        }
                                        .padding(horizontal = 6.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = DDZTypography.Body,
                                        color = DDZColor.TextPrimary
                                    )
                                    Text(
                                        "☰",
                                        modifier = with(this@ReorderableItem) { Modifier.draggableHandle() },
                                        style = DDZTypography.Body,
                                        color = DDZColor.TextMuted
                                    )
                                    TextButton(onClick = {
                                        if (index in itemIds.indices) {
                                            itemIds.removeAt(index)
                                        }
                                        onUpdateSet { set ->
                                            set.copy(items = set.items.filterIndexed { idx, _ -> idx != index })
                                        }
                                    }) { Text("🗑") }
                                }
                            }
                        }
                    }

                    if (phraseSet.items.isEmpty()) {
                        Text(
                            text = "항목을 추가해주세요.",
                            modifier = Modifier.align(Alignment.Center),
                            color = DDZColor.TextMuted,
                            style = DDZTypography.Body
                        )
                    }

                    val layoutInfo = lazyListState.layoutInfo
                    val visibleItems = layoutInfo.visibleItemsInfo
                    val canScroll = layoutInfo.totalItemsCount > visibleItems.size

                    if (showScrollIndicator && canScroll && layoutInfo.totalItemsCount > 0 && visibleItems.isNotEmpty()) {
                        val density = LocalDensity.current
                        val viewportHeightPx = (layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset).coerceAtLeast(1)
                        val avgItemHeightPx = visibleItems.map { it.size }.average().toFloat().takeIf { it > 0f } ?: 1f
                        val totalContentHeightPx = (avgItemHeightPx * layoutInfo.totalItemsCount).coerceAtLeast(viewportHeightPx.toFloat())
                        val thumbHeightPx = ((viewportHeightPx.toFloat() / totalContentHeightPx) * viewportHeightPx)
                            .coerceIn(24f, viewportHeightPx.toFloat())
                        val firstVisible = visibleItems.first()
                        val scrollOffsetPx = (firstVisible.index * avgItemHeightPx) - firstVisible.offset
                        val maxScrollPx = (totalContentHeightPx - viewportHeightPx).coerceAtLeast(1f)
                        val thumbOffsetPx = ((scrollOffsetPx / maxScrollPx) * (viewportHeightPx - thumbHeightPx))
                            .coerceIn(0f, (viewportHeightPx - thumbHeightPx).coerceAtLeast(0f))

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(end = 1.dp)
                                .width(3.dp)
                                .height(with(density) { thumbHeightPx.toDp() })
                                .offset(y = with(density) { thumbOffsetPx.toDp() })
                                .alpha(indicatorAlpha)
                                .background(DDZColor.TextMuted.copy(alpha = 0.5f), RoundedCornerShape(99.dp))
                        )
                    }
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        editingItemIndex = null
                        itemInput = ""
                        showItemInputDialog = true
                    }
                ) {
                    Text("+ 문구 추가", style = DDZTypography.ButtonText)
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("세트 이름 변경") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    label = { Text("세트 이름") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = renameInput.trim()
                    if (trimmed.isNotBlank()) {
                        onUpdateSet { it.copy(name = trimmed) }
                    }
                    showRenameDialog = false
                }) {
                    Text("적용", style = DDZTypography.ButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("취소", style = DDZTypography.ButtonText)
                }
            }
        )
    }

    if (showItemInputDialog) {
        val isEdit = editingItemIndex != null
        AlertDialog(
            onDismissRequest = { showItemInputDialog = false },
            title = { Text(if (isEdit) "문구 수정" else "문구 추가") },
            text = {
                OutlinedTextField(
                    value = itemInput,
                    onValueChange = { itemInput = it },
                    singleLine = true,
                    label = { Text("문구") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = itemInput.trim()
                    if (trimmed.isNotBlank()) {
                        if (editingItemIndex == null) {
                            itemIds.add(UUID.randomUUID().toString())
                            onUpdateSet { it.copy(items = it.items + trimmed) }
                        } else {
                            val idx = editingItemIndex!!
                            onUpdateSet { set ->
                                if (idx !in set.items.indices) {
                                    set
                                } else {
                                    set.copy(
                                        items = set.items.mapIndexed { i, value ->
                                            if (i == idx) trimmed else value
                                        }
                                    )
                                }
                            }
                        }
                    }
                    showItemInputDialog = false
                }) {
                    Text("적용", style = DDZTypography.ButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showItemInputDialog = false }) {
                    Text("취소", style = DDZTypography.ButtonText)
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("세트 삭제") },
            text = { Text("${phraseSet.name} 세트를 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSet(phraseSet.id)
                    showDeleteConfirm = false
                }) {
                    Text("삭제", style = DDZTypography.ButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("취소", style = DDZTypography.ButtonText)
                }
            }
        )
    }
}

@Composable
internal fun CameraLikeWatermarkPlacementPreview(
    captureAspect: CaptureAspect,
    rows: Int,
    cols: Int,
    rowWeights: List<Float>?,
    colWeights: List<Float>?,
    watermarkCells: List<WatermarkBuilder.WatermarkCell>,
    anchor: WatermarkTableAnchor,
    tableWidthRatio: Int,
    tableHeightRatio: Int,
    bgStyle: Int,
    bgAlpha: Int,
    valueScale: Int
) {
    // bgStyle: 워터마크 표 배경 스타일
    // - 0: BLACK
    // - 1: WHITE
    // - 2: TRANSPARENT (배경 렌더링 안 함)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DDZColor.PrimaryDark)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("촬영 미리보기", color = DDZColor.Surface, style = DDZTypography.CardTitle)

        // 카메라 프레임(비율 반영) 박스
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(captureAspect.ratioF)
                .background(DDZColor.PrimaryDark)
                .border(1.dp, DDZColor.Border)
                .clipToBounds()
        ) {
            // 실제 카메라 영상 대신 “프레임 느낌” 배경 (단색+가이드 정도)
            Canvas(modifier = Modifier.fillMaxSize()) {
                // 아주 약한 가이드(중앙 십자선) – 원하면 나중에 제거 가능
                val w = size.width
                val h = size.height

                // center lines
                drawRect(
                    color = DDZColor.Surface.copy(alpha = 0.13f),
                    topLeft = Offset(w / 2f - 0.5f, 0f),
                    size = Size(1f, h)
                )
                drawRect(
                    color = DDZColor.Surface.copy(alpha = 0.13f),
                    topLeft = Offset(0f, h / 2f - 0.5f),
                    size = Size(w, 1f)
                )

                // 워터마크 표 오버레이 (CameraScreen과 동일 엔진)
                drawIntoCanvas { canvas ->
                    val bounds = RectF(0f, 0f, w, h)

                    drawWatermarkTableOnCanvas(
                        canvas = canvas.nativeCanvas,
                        bounds = bounds,
                        cells = watermarkCells,
                        rows = rows.coerceAtLeast(1),
                        cols = cols.coerceAtLeast(1),
                        showLabel = false,
                        anchor = anchor,
                        offsetXRatio = 0,
                        offsetYRatio = 0,
                        tableHeightRatio = tableHeightRatio,
                        tableWidthRatio = tableWidthRatio,
                        bgAlpha = bgAlpha.coerceIn(0, 255),
                        bgStyle = bgStyle,
                        labelScale = 100,
                        valueScale = valueScale.coerceIn(60, 160),
                        rowWeights = rowWeights,
                        colWeights = colWeights
                    )
                }
            }
        }

        Text(
            "비율: ${captureAspect.label} / 크기: ${tableWidthRatio}%×${tableHeightRatio}% (위치는 카메라 화면에서 드래그)",
            color = DDZColor.IconMuted,
            style = DDZTypography.Caption
        )
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
