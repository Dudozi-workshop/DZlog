@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.example.dzlog.ui.table

import android.graphics.RectF
import android.util.Log
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.example.dzlog.data.counter.COUNTER_DIGITS_DEFAULT
import com.example.dzlog.data.counter.clampCounterDigits
import com.example.dzlog.data.preferences.KEY_CAPTURE_ASPECT
import com.example.dzlog.data.preferences.KEY_COUNTER_DIGITS
import com.example.dzlog.data.preferences.KEY_TABLE_TEMPLATE_JSON
import com.example.dzlog.data.preferences.KEY_WM_BG_ALPHA
import com.example.dzlog.data.preferences.KEY_WM_TABLE_ANCHOR
import com.example.dzlog.data.preferences.KEY_WM_TABLE_BG_STYLE
import com.example.dzlog.data.preferences.KEY_WM_TABLE_HEIGHT
import com.example.dzlog.data.preferences.KEY_WM_TABLE_WIDTH
import com.example.dzlog.data.preferences.KEY_WM_VALUE_SCALE
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.data.template.toJsonString
import com.example.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.example.dzlog.domain.capturepolicy.CaptureStreamKey
import com.example.dzlog.domain.counter.CounterManager
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.CellValue
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.HourSystem
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellKind
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.TimeFormatOptions
import com.example.dzlog.domain.model.TimeSeparator
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.naming.buildDisplayNameFromResolvedCells
import com.example.dzlog.domain.naming.buildGalleryRelativePath
import com.example.dzlog.domain.naming.resolveGroupValue
import com.example.dzlog.domain.preview.computeNextDelayMillis
import com.example.dzlog.domain.preview.decideTickUnit
import com.example.dzlog.domain.table.TableResolver
import com.example.dzlog.domain.watermark.WatermarkBuilder
import com.example.dzlog.ui.common.DDZSectionHeader
import com.example.dzlog.ui.table.section.PreviewTabContent
import com.example.dzlog.ui.table.section.TableEditorTabs
import com.example.dzlog.ui.table.section.TableGridSection
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography
import com.example.dzlog.watermark.drawWatermarkTableOnCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale


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

    val dateFormatOptions = listOf("yyyy.MM.dd", "yyyy_MM_dd", "yyyyMMdd")
    // TIME 형식은 Step3부터 토글 UI(12/24, 초, 구분자)로 설정한다.

    // NOTE: formatPattern 기반이 아니라 현재는 고정값. (기존 코드 유지)
    val dateFormat = "yyyy.MM.dd"
    val timeFormat = "HH.mm.ss"

    var previewCounterDigits by remember { mutableIntStateOf(COUNTER_DIGITS_DEFAULT) }

    var usedCounters by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var scopeNextCounter by remember { mutableIntStateOf(1) }
    var showCounterConflictDialog by remember { mutableStateOf(false) }
    var pendingCounterCommitValue by remember { mutableIntStateOf(0) }
    var pendingCounterStreamNextValue by remember { mutableIntStateOf(1) }
    var preserveManualCounterSeed by remember { mutableStateOf(false) }
    var manualSeedOverride by remember { mutableStateOf<Int?>(null) }

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
            counterSeedOverride = scopeNextCounter
        )
    }
    val currentPrefix by remember(planForScope.resolvedCells) {
        derivedStateOf {
            // ✅ counter 스트림 prefix: 날짜/시간 제외(파일명에는 붙어도 카운터에는 영향 없음)
            CounterManager.computeCounterStreamPrefix(
                resolvedCells = planForScope.resolvedCells,
                fnDelim = "_"
            )
        }
    }

    val currentRelativePathKey by remember(planForScope.resolvedCells) {
        derivedStateOf {
            val g1 = resolveGroupValue(planForScope.resolvedCells, GroupLevel.G1)
            val g2 = resolveGroupValue(planForScope.resolvedCells, GroupLevel.G2)
            val baseRelativePath = buildGalleryRelativePath(g1, g2)
            val hasG2Group = planForScope.resolvedCells.any { it.raw?.groupLevel == GroupLevel.G2 }
            CounterManager.computeCounterStreamRelativePathKey(
                baseRelativePath = baseRelativePath,
                hasG2Group = hasG2Group,
                group2Value = g2
            )
        }
    }

    val currentScopeKey by remember(currentRelativePathKey, currentPrefix) {
        derivedStateOf { "$currentRelativePathKey|$currentPrefix" }
    }

    LaunchedEffect(currentScopeKey) {
        val g1 = resolveGroupValue(planForScope.resolvedCells, GroupLevel.G1)
        val g2 = resolveGroupValue(planForScope.resolvedCells, GroupLevel.G2)

        Log.d(
            "DZlogCounter",
            """
        [STEP1 scopeKey changed]
        g1=$g1
        g2=$g2
        relativePathKey=$currentRelativePathKey
        counterPrefix=$currentPrefix
        scopeKey=$currentScopeKey
        """.trimIndent()
        )
    }

    // ✅ 스트림 변경 감지용 (스트림이 바뀌면 seed를 "새 스트림 next"로 강제 동기화)
    var lastScopeKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentScopeKey) {
        Log.d(
            "DZlogCounter",
            "TableEditor scopeKey changed\n"+
            "relativePathKey=$currentRelativePathKey\n"+
            "counterPrefix=$currentPrefix\n"+
            "scopeKey=$currentScopeKey"
        )
    }

    LaunchedEffect(currentScopeKey, previewCounterDigits, templateState) {
        // ✅ 정책(스트림키=relativePathPrefix) 기준 usedCounters  nextCounter 계산
        val used = CounterManager.getUsedCounters(
            context = context,
            relativePath = currentRelativePathKey,
            counterPrefix = currentPrefix,
            counterDigits = previewCounterDigits,
            fnDelim = "_"
        )
        usedCounters = used

        val counterCell = templateState.cells.firstOrNull { it.dataType == TableCellDataType.COUNTER }
        val currentSeed = (counterCell?.typedValue as? CellValue.CounterSeed)?.start ?: 1
        val nextByHistory = ((used.maxOrNull() ?: 0) +  1).coerceAtLeast(1)

        val isNewStream = (lastScopeKey != null && lastScopeKey != currentScopeKey)
        // ✅ 스트림이 바뀌면 "새 스트림의 next"로 맞춘다.
        // ✅ 같은 스트림에서는 사용자 수동 seed(낮은 값 포함)를 유지한다.
        if (isNewStream) {
            preserveManualCounterSeed = false
        }

        val desiredSeed = when {
            counterCell == null -> nextByHistory
            manualSeedOverride != null -> manualSeedOverride!!.coerceAtLeast(1)
            isNewStream -> nextByHistory
            preserveManualCounterSeed -> currentSeed.coerceAtLeast(1)
            else -> maxOf(nextByHistory, currentSeed.coerceAtLeast(1))
        }

        if (manualSeedOverride != null) {
            manualSeedOverride = null
        }

        scopeNextCounter = desiredSeed
        lastScopeKey = currentScopeKey
        // IDE 경고(Assigned value is never read) 방지: 다음 실행을 위한 상태를 즉시 한 번 읽어둔다.
        val persistedScopeKey = lastScopeKey

        Log.d(
            "DZlogCounter",
            "TableEditor counter sync\n"+
            "usedCounters=$usedCounters\n"+
            "currentSeed=$currentSeed\n"+
            "nextByHistory=$nextByHistory\n"+
            "scopeNextCounter=$scopeNextCounter\n"+
            "persistedScopeKey=$persistedScopeKey"
        )

        // ✅ 표시 ON/OFF와 무관하게, COUNTER 셀이 존재하면 seed는 정책 기준으로 항상 최신으로 맞춰둔다.
        if (counterCell != null && currentSeed != desiredSeed) {
            val updated = updateCell(templateState, counterCell.cellId) { c ->
                c.copy(typedValue = CellValue.CounterSeed(desiredSeed))
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

    fun commitInlineEditIfNeeded() {
        val id = editingCellId ?: return
        val cell = templateState.cells.firstOrNull { it.cellId == id }

        // COUNTER 충돌 정책: stream next보다 작은 값을 입력하면 경고 후 진행 여부를 확인한다.
        if (cell != null && cell.dataType == TableCellDataType.COUNTER) {
            val newV = editingValue.trim().toIntOrNull()
            val oldV = editingOriginalValue.trim().toIntOrNull()
            if (newV == null || newV < 0) return

            val isChanged = (oldV == null) || (newV != oldV)
            val streamNext = ((usedCounters.maxOrNull() ?: 0) + 1).coerceAtLeast(1)
            if (isChanged && newV < streamNext) {
                pendingCounterCommitValue = newV
                pendingCounterStreamNextValue = streamNext
                showCounterConflictDialog = true
                return
            }
        }

        val target = templateState.cells.firstOrNull { it.cellId == id }
        val normalizedValueText = if (target?.dataType == TableCellDataType.COUNTER) {
            val v = editingValue.trim().toIntOrNull()
            if (v == null || v < 0) {
                editingCellId = null
                return
            }
            v.toString()
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
            preserveManualCounterSeed = true
            manualSeedOverride = normalizedSeed
            scopeNextCounter = normalizedSeed
            scope.launch {
                CaptureCounterPolicy.setNextCounter(
                    context = context,
                    key = CaptureStreamKey(
                        relativePathKey = currentRelativePathKey,
                        prefix = currentPrefix
                    ),
                    desired = normalizedSeed,
                    force = false,
                    counterDigits = previewCounterDigits,
                    fnDelim = "_"
                )
            }
        }
        editingCellId = null
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
            counterSeedOverride = scopeNextCounter
        )
    }

    val filenamePreview = buildDisplayNameFromResolvedCells(
        resolvedCells = plan.resolvedCells,
        fnDelim = "_",
        includeDate = false,
        includeTime = false,
        // ✅ 파일명 suffix counter는 항상 스트림 값(SSOT)을 사용
        // COUNTER 셀의 표기 ON/OFF는 "표/워터마크 표현"에만 영향, 카운터 스트림/파일명에는 영향 없음.
        // (파일명 뒤 숫자는 항상 붙는 정책)
        counterOverride = scopeNextCounter,
        now = previewNow
    )

    fun closeCounterConflictDialog() {
        showCounterConflictDialog = false
        editingCellId = null
        editingValue = ""
    }

    if (showCounterConflictDialog) {
        AlertDialog(
            onDismissRequest = {
                val id = editingCellId
                if (id != null) {
                    val restored = pendingCounterStreamNextValue.coerceAtLeast(1)
                    val updated = updateCell(templateState, id) { c ->
                        c.copy(typedValue = CellValue.CounterSeed(restored))
                    }
                    onTemplateChange(updated)
                    preserveManualCounterSeed = false
                    manualSeedOverride = restored
                    scopeNextCounter = restored
                    scope.launch {
                        CaptureCounterPolicy.setNextCounter(
                            context = context,
                            key = CaptureStreamKey(
                                relativePathKey = currentRelativePathKey,
                                prefix = currentPrefix
                            ),
                            desired = restored,
                            force = false,
                            counterDigits = previewCounterDigits,
                            fnDelim = "_"
                        )
                    }
                }
                closeCounterConflictDialog()
            },
            title = { Text("카운터 충돌 경고") },
            text = {
                Text(
                    "중복된 카운터가 발생할 수 있습니다. 계속 진행하시겠습니까?\n\n" +
                        "입력값: $pendingCounterCommitValue\n" +
                        "현재 스트림 next: $pendingCounterStreamNextValue"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = editingCellId
                    if (id != null) {
                        val applied = pendingCounterCommitValue.coerceAtLeast(1)
                        val updated = updateCell(templateState, id) { c ->
                            c.copy(typedValue = CellValue.CounterSeed(applied))
                        }
                        onTemplateChange(updated)
                        preserveManualCounterSeed = true
                        manualSeedOverride = applied
                        scopeNextCounter = applied
                        scope.launch {
                            CaptureCounterPolicy.setNextCounter(
                                context = context,
                                key = CaptureStreamKey(
                                    relativePathKey = currentRelativePathKey,
                                    prefix = currentPrefix
                                ),
                                desired = applied,
                                force = true,
                                counterDigits = previewCounterDigits,
                                fnDelim = "_"
                            )
                        }
                    }
                    closeCounterConflictDialog()
                }) { Text("진행", style = DDZTypography.ButtonText) }
            },
            dismissButton = {
                TextButton(onClick = {
                    val id = editingCellId
                    if (id != null) {
                        val restored = pendingCounterStreamNextValue.coerceAtLeast(1)
                        val updated = updateCell(templateState, id) { c ->
                            c.copy(typedValue = CellValue.CounterSeed(restored))
                        }
                        onTemplateChange(updated)
                        preserveManualCounterSeed = false
                        manualSeedOverride = restored
                        scopeNextCounter = restored
                        scope.launch {
                            CaptureCounterPolicy.setNextCounter(
                                context = context,
                                key = CaptureStreamKey(
                                    relativePathKey = currentRelativePathKey,
                                    prefix = currentPrefix
                                ),
                                desired = restored,
                                force = false,
                                counterDigits = previewCounterDigits,
                                fnDelim = "_"
                            )
                        }
                    }
                    closeCounterConflictDialog()
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
                            "Back",
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
                    // ==========================
                    // 탭0: 표 구조설정 (헤더 고정 그리드 영역 하단 고정바 셀 패널 오버레이)
                    // ==========================
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            // ✅ (변경) 상단/중앙은 스크롤 없음. (스크롤은 그리드 내부에서만)
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                CompactPathHeader(
                                    savePath = savePathPreview,
                                     fileName = filenamePreview
                                 )

                            Spacer(Modifier.height(10.dp))
                            // ✅ Grid 영역: 스샷처럼 "섹션 카드" 안에, 높이 제한
                                DDZSectionHeader(title = "GRID LAYOUT")
                            Spacer(Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp) // ✅ 여기로 영역 제한 (원하면 360/480으로 조정)
                                    .background(DDZColor.Card, RoundedCornerShape(14.dp))
                                    .padding(10.dp)
                            ) {

                            TableGridSection(
                                    templateState = templateState,
                                    displayTextProvider = { cellId ->
                                        plan.resolvedCells
                                            .firstOrNull { it.id == cellId }
                                            ?.resolvedText
                                            .orEmpty()
                                    },
                                    selectedCellId = selectedCellId,
                                    editingCellId = editingCellId,
                                    onSelectCell = { id ->

                                        if (editingCellId != null && editingCellId != id) {
                                            commitInlineEditIfNeeded()
                                            if (editingCellId != null) return@TableGridSection
                                        }
                                        selectedCellId = id

                                            showCellSettingsPanel = true
                                    },
                                    onDoubleClickCell = { cell ->

                                        if (editingCellId != null && editingCellId != cell.cellId) {
                                            commitInlineEditIfNeeded()
                                            if (editingCellId != null) return@TableGridSection
                                            selectedCellId = cell.cellId
                                            showCellSettingsPanel = true
                                            return@TableGridSection
                                        }

                                        selectedCellId = cell.cellId
                                        val canInline =
                                            (cell.dataType == TableCellDataType.TEXT ||
                                                    cell.dataType == TableCellDataType.NUMBER ||
                                                    cell.dataType == TableCellDataType.COUNTER)

                                        if (canInline) {
                                            // ✅ 인라인 편집은 그리드 안에서 보여야 하므로,
                                            // 패널이 떠 있는 상태면 가려져서 "안 되는 것처럼" 보임 → 강제 닫기
                                            showCellSettingsPanel = false
                                            startInlineEditing(cell.cellId, cell.toEditableText())
                                        } else {
                                            if (cell.dataType == TableCellDataType.DATE ||
                                                cell.dataType == TableCellDataType.TIME
                                            ) {
                                                openFormatDialog(cell.cellId, cell.dataType)
                                            } else {
                                                showCellSettingsPanel = true
                                            }
                                        }
                                    },
                                    editingValue = editingValue,
                                    onEditingValueChange = { editingValue = it },
                                    onCommitInline = {
                                        commitInlineEditIfNeeded()
                                        editingCellId = null
                                        keyboardController?.hide()
                                    },
                                    inlineFocusRequester = inlineFocusRequester,
                                    onInlineFocusLostCommit = {
                                        commitInlineEditIfNeeded()
                                        editingCellId = null
                                    }
                                )
                            }

                            Spacer(Modifier.height(10.dp))
                            } // ✅ (변경) 상단/중앙 영역 끝

                            // ✅ 하단 고정 액션바(A 구성)
                            BottomFixedActionBar(
                                rows = templateState.rows,
                                cols = templateState.cols,
                                isSaving = isSavingTemplate,
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
                                        runCatching {
                                            context.dataStore.edit { prefs ->
                                                prefs[KEY_TABLE_TEMPLATE_JSON] = templateState.toJsonString()
                                            }
                                        }.onFailure {
                                            Toast.makeText(
                                                context,
                                                "Save failed: ${it.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            isSavingTemplate = false
                                        }.onSuccess {
                                            Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                                            onBack()
                                        }
                                    }
                                }
                            )
                        }

                        // ✅ 셀 설정 패널(오버레이): 하단바를 덮는 방식
                        if (showCellSettingsPanel && selectedCell != null && editingCellId == null) {
                            // 배경 터치로 닫기
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(DDZColor.PrimaryDark.copy(alpha = 0.4f))
                                    .clickable {
                                        commitInlineEditIfNeeded()
                                        showCellSettingsPanel = false
                                    }
                            )

                            CellSettingsBottomPanel(
                                modifier = Modifier.align(Alignment.BottomCenter),
                                cell = selectedCell,
                                hasGroup1 = hasGroup1,
                                hasGroup2 = hasGroup2,
                                onSetFileNameInclude = { checked ->
                                    val updated = updateCell(templateState, selectedCell.cellId) { c ->
                                        c.copy(fileNameInclude = checked)
                                    }
                                    onTemplateChange(updated)
                                },
                                onPathGroupAction = { action ->
                                    val updated = applyPathGroupAction(
                                        state = templateState,
                                        targetCellId = selectedCell.cellId,
                                        action = action
                                    )
                                    onTemplateChange(updated)
                                },
                                onSetDataType = { type ->
                                    val updated = updateCell(templateState, selectedCell.cellId) { c ->
                                        c.withDataType(type)
                                    }
                                    onTemplateChange(updated)
                                }
                            )
                        }
                    }
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
                        onAnchorChange = { anchor ->
                            wmAnchor = anchor
                            scope.launch {
                                context.dataStore.edit { prefs ->
                                    prefs[KEY_WM_TABLE_ANCHOR] = when (anchor) {
                                        WatermarkTableAnchor.TOP_LEFT -> 0
                                        WatermarkTableAnchor.TOP_RIGHT -> 1
                                        WatermarkTableAnchor.BOTTOM_LEFT -> 2
                                        else -> 3
                                    }
                                }
                            }
                        },
                        onWidthRatioChange = { width ->
                            val nv = width.coerceIn(40, 100)
                            wmWidthRatio = nv
                            scope.launch { context.dataStore.edit { it[KEY_WM_TABLE_WIDTH] = nv } }
                        },
                        onHeightRatioChange = { height ->
                            val nv = height.coerceIn(10, 35)
                            wmHeightRatio = nv
                            scope.launch { context.dataStore.edit { it[KEY_WM_TABLE_HEIGHT] = nv } }
                        },
                        onBgStyleChange = { bgStyle ->
                            wmBgStyle = bgStyle
                            scope.launch { context.dataStore.edit { it[KEY_WM_TABLE_BG_STYLE] = bgStyle } }
                        },
                        onBgAlphaChange = { alpha ->
                            val nv = alpha.coerceIn(0, 255)
                            wmBgAlpha = nv
                            scope.launch { context.dataStore.edit { it[KEY_WM_BG_ALPHA] = nv } }
                        },
                        onValueScaleChange = { scale ->
                            val nv = scale.coerceIn(60, 160)
                            wmValueScale = nv
                            scope.launch { context.dataStore.edit { it[KEY_WM_VALUE_SCALE] = nv } }
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun CompactPathHeader(
    savePath: String,
    fileName: String
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DDZColor.Card, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("SAVE PATH", style = DDZTypography.Caption, color = DDZColor.TextMuted)
        Text(
            savePath,
            style = DDZTypography.Body,
            color = DDZColor.TextPrimary,
            maxLines = 2
        )
        Spacer(Modifier.height(2.dp))
        Text("FILENAME", style = DDZTypography.Caption, color = DDZColor.TextMuted)
        Text(
            fileName,
            style = DDZTypography.Body,
            color = DDZColor.TextPrimary,
            maxLines = 2
        )
    }
    }

// =========================
// Stage 4: Row/Col size controls (weights)
// =========================
private fun ensureRowWeights(state: TableTemplateState): List<Float> {
    val n = state.rows.coerceAtLeast(1)
    val w = state.rowWeights
    return if (w == null || w.size != n) List(n) { 1f } else w
}

private fun ensureColWeights(state: TableTemplateState): List<Float> {
    val n = state.cols.coerceAtLeast(1)
    val w = state.colWeights
    return if (w == null || w.size != n) List(n) { 1f } else w
}

@Composable
internal fun TableRowColSizeSection(
    templateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit
) {
    val rowWeights = ensureRowWeights(templateState)
    val colWeights = ensureColWeights(templateState)
    val innerScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DDZColor.Card, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("행/열 크기(비율)", style = DDZTypography.CardTitle, color = DDZColor.TextMuted)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("균등 초기화", style = DDZTypography.Body, color = DDZColor.TextPrimary)
            TextButton(onClick = {
                onTemplateChange(
                    templateState.copy(
                        rowWeights = List(templateState.rows.coerceAtLeast(1)) { 1f },
                        colWeights = List(templateState.cols.coerceAtLeast(1)) { 1f }
                    )
                )
            }) { Text("RESET", style = DDZTypography.ButtonText) }
        }

        // 행/열이 많을 때 레이아웃 섹션만 스크롤되도록 제한
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 260.dp)
                .verticalScroll(innerScroll),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("행 높이", style = DDZTypography.Body, color = DDZColor.TextPrimary)
            for (r in 0 until templateState.rows.coerceAtLeast(1)) {
                val v = rowWeights.getOrNull(r) ?: 1f
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("R${r + 1}", modifier = Modifier.width(34.dp), style = DDZTypography.Caption, color = DDZColor.TextMuted)
                    Slider(
                        value = v.coerceIn(0.3f, 3.0f),
                        onValueChange = { nv ->
                            val next = rowWeights.toMutableList()
                            next[r] = nv.coerceIn(0.3f, 3.0f)
                            onTemplateChange(templateState.copy(rowWeights = next))
                        },
                        valueRange = 0.3f..3.0f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(String.format(Locale.US, "%.2f", v), modifier = Modifier.width(52.dp), style = DDZTypography.Caption, color = DDZColor.TextMuted)
                }
            }

            Spacer(Modifier.height(6.dp))
            Text("열 너비", style = DDZTypography.Body, color = DDZColor.TextPrimary)
            for (c in 0 until templateState.cols.coerceAtLeast(1)) {
                val v = colWeights.getOrNull(c) ?: 1f
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("C${c + 1}", modifier = Modifier.width(34.dp), style = DDZTypography.Caption, color = DDZColor.TextMuted)
                    Slider(
                        value = v.coerceIn(0.3f, 3.0f),
                        onValueChange = { nv ->
                            val next = colWeights.toMutableList()
                            next[c] = nv.coerceIn(0.3f, 3.0f)
                            onTemplateChange(templateState.copy(colWeights = next))
                        },
                        valueRange = 0.3f..3.0f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(String.format(Locale.US, "%.2f", v), modifier = Modifier.width(52.dp), style = DDZTypography.Caption, color = DDZColor.TextMuted)
                }
            }
        }

        Text(
            "※ 값은 ‘비율’이며, 표 전체 크기 안에서 행/열 분배만 바뀜",
            style = DDZTypography.Caption,
            color = DDZColor.TextMuted
        )
    }
}

@Composable
internal fun BottomFixedActionBar(
    rows: Int,
    cols: Int,
    isSaving: Boolean,
    onAddRow: () -> Unit,
    onRemoveRow: () -> Unit,
    onAddCol: () -> Unit,
    onRemoveCol: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DDZColor.Surface, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(modifier = Modifier.weight(1f), onClick = onAddRow) {
                Text("+Row", style = DDZTypography.ButtonText)
            }
            Button(modifier = Modifier.weight(1f), onClick = onRemoveRow, enabled = rows > 1) {
                Text("-Row", style = DDZTypography.ButtonText)
            }
            Button(modifier = Modifier.weight(1f), onClick = onAddCol) {
                Text("+Col", style = DDZTypography.ButtonText)
            }
            Button(modifier = Modifier.weight(1f), onClick = onRemoveCol, enabled = cols > 1) {
                Text("-Col", style = DDZTypography.ButtonText)
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(modifier = Modifier.weight(1f), onClick = onReset) {
                Text("Reset", style = DDZTypography.ButtonText)
            }
            Button(modifier = Modifier.weight(1f), onClick = onSave, enabled = !isSaving) {
                if (isSaving) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Saving...", style = DDZTypography.ButtonText)
                } else {
                    Text("Save", style = DDZTypography.ButtonText)
                }
            }
        }
    }
    }

@Composable
internal fun CellSettingsBottomPanel(
    modifier: Modifier,
    cell: TableCellState,
    hasGroup1: Boolean,
    hasGroup2: Boolean,
    onSetFileNameInclude: (Boolean) -> Unit,
    onPathGroupAction: (PathGroupAction) -> Unit,
    onSetDataType: (TableCellDataType) -> Unit
    ) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(DDZColor.Surface, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ✅ 상단 핸들(중앙만) - 공간 최소화
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(4.dp)
                    .background(DDZColor.Border, RoundedCornerShape(4.dp))
            )
        }

        // ✅ 파일명 그룹 (한 줄 병기)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("파일명 포함", style = DDZTypography.Body, color = DDZColor.TextMuted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(if (cell.fileNameInclude) "ON" else "OFF", style = DDZTypography.Body, color = DDZColor.TextPrimary)
                    Switch(
                        checked = cell.fileNameInclude,
                        onCheckedChange = onSetFileNameInclude
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text("저장경로", style = DDZTypography.Body, color = DDZColor.TextMuted)
    // G2 선택 가능 조건
    // - G1이 반드시 존재해야 함
    // - 현재 셀이 G1이면: "G2가 이미 존재하는 경우에만" G1<->G2 스왑을 위해 허용
    val canSelectG2 = hasGroup1 && (
        cell.groupLevel != GroupLevel.G1 || hasGroup2
    )
                val isNone = cell.groupLevel == GroupLevel.NONE
                val isG1 = cell.groupLevel == GroupLevel.G1
                val isG2 = cell.groupLevel == GroupLevel.G2

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DDZColor.Card, RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onPathGroupAction(PathGroupAction.NONE) }
                    ) { Text(if (isNone) "없음 ✓" else "없음", style = DDZTypography.ButtonText) }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onPathGroupAction(PathGroupAction.G1) }
                    ) { Text(if (isG1) "G1 ✓" else "G1", style = DDZTypography.ButtonText) }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = { onPathGroupAction(PathGroupAction.G2) },
                        enabled = canSelectG2
                    ) { Text(if (isG2) "G2 ✓" else "G2", style = DDZTypography.ButtonText) }
                }

                if (!canSelectG2) {
                    Text("※ G2는 G1 설정 후 사용 가능 (현재 G1 셀에는 G2 설정 불가)", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                }
            }
        }

        // ✅ Data Format: 카드형 3열
        Text("데이터 형식", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary)
        DataTypeCardGrid3(
            selected = cell.dataType,
            onSelect = onSetDataType
        )
    }
}

@Composable
private fun DataTypeCardGrid3(
    selected: TableCellDataType,
    onSelect: (TableCellDataType) -> Unit
    ) {
    val items = listOf(
        TableCellDataType.TEXT to "Text",
        TableCellDataType.NUMBER to "Number",
        TableCellDataType.DATE to "Date",
        TableCellDataType.TIME to "Time",
        TableCellDataType.COUNTER to "Counter"
    )

    val rows = items.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (type, label) ->
                    val isSelected = selected == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(74.dp)
                            .background(
                                color = if (isSelected) DDZColor.Success.copy(alpha = 0.2f) else DDZColor.Surface,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) DDZColor.Success else DDZColor.Border,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelect(type) }
                            .padding(10.dp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(label, style = DDZTypography.Body, color = DDZColor.TextPrimary)
                            Text(type.name, style = DDZTypography.Caption, color = DDZColor.TextMuted)
                        }
                    }
                }
                // 3열 맞추기: row가 3개 미만이면 빈 칸 채움
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
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
            "비율: ${captureAspect.label} / 위치: ${anchor.name} / 크기: ${tableWidthRatio}%×${tableHeightRatio}%",
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
    if (this.dataType == newType) return this

    return when (newType) {
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
    }
}
