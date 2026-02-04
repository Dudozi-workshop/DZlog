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
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import com.example.dzlog.data.counter.COUNTER_DIGITS_DEFAULT
import com.example.dzlog.data.counter.clampCounterDigits
import com.example.dzlog.data.counter.encodeCounterSet
import com.example.dzlog.data.counter.scanUsedCountersFromMediaStore
import com.example.dzlog.data.counterindex.CounterIndexRepository
import com.example.dzlog.data.preferences.KEY_CAPTURE_ASPECT
import com.example.dzlog.data.preferences.KEY_COUNTER_DIGITS
import com.example.dzlog.data.preferences.KEY_COUNTER_SUFFIX_ENABLED
import com.example.dzlog.data.preferences.KEY_TABLE_TEMPLATE_JSON
import com.example.dzlog.data.preferences.KEY_USED_COUNTER_VALUES_JSON
import com.example.dzlog.data.preferences.KEY_WM_TABLE_ANCHOR
import com.example.dzlog.data.preferences.KEY_WM_TABLE_HEIGHT
import com.example.dzlog.data.preferences.KEY_WM_TABLE_WIDTH
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.data.template.toJsonString
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
import com.example.dzlog.domain.naming.buildFileNamePrefixFromResolvedCells
import com.example.dzlog.domain.naming.buildGalleryRelativePath
import com.example.dzlog.domain.preview.computeNextDelayMillis
import com.example.dzlog.domain.preview.decideTickUnit
import com.example.dzlog.domain.table.TableResolver
import com.example.dzlog.domain.watermark.WatermarkBuilder
import com.example.dzlog.ui.common.DDZSectionHeader
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography
import com.example.dzlog.watermark.drawWatermarkTableOnCanvas
import kotlinx.coroutines.Job
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

    val dateFormatOptions = listOf("yyyy.MM.dd", "yyyy_MM_dd", "yyyyMMdd")
    // TIME 형식은 Step3부터 토글 UI(12/24, 초, 구분자)로 설정한다.

    // NOTE: formatPattern 기반이 아니라 현재는 고정값. (기존 코드 유지)
    val dateFormat = "yyyy.MM.dd"
    val timeFormat = "HH.mm.ss"

    var previewCounterDigits by remember { mutableIntStateOf(COUNTER_DIGITS_DEFAULT) }
    var previewCounterSuffixEnabled by remember { mutableStateOf(true) }

    var usedCounters by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showCounterDupDialog by remember { mutableStateOf(false) }
    var pendingCounterCommitValue by remember { mutableIntStateOf(0) }
    var pendingCounterCommitText by remember { mutableStateOf("") }
    var pendingCounterLatestValue by remember { mutableIntStateOf(0) }

    val currentRelativePath by remember(templateState.cells) {
        derivedStateOf { buildGalleryRelativePath(templateState.cells) }
    }

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
            previewCounterSuffixEnabled = prefs[KEY_COUNTER_SUFFIX_ENABLED] ?: true
        }.onFailure {
            previewCounterDigits = COUNTER_DIGITS_DEFAULT
            previewCounterSuffixEnabled = true
        }
    }

    val tableResolver = remember { TableResolver() }
    val planForScope = remember(templateState.cells, previewNow, previewCounterDigits, dateFormat, timeFormat) {
        tableResolver.plan(
            cells = templateState.cells,
            captureNow = previewNow,
            config = TableResolver.Config(
                counterDigits = previewCounterDigits,
                dateFormat = dateFormat,
                timeFormat = timeFormat
            )
        )
    }

    val currentPrefix by remember(planForScope.resolvedCells) {
        derivedStateOf {
            buildFileNamePrefixFromResolvedCells(
                resolvedCells = planForScope.resolvedCells,
                fnDelim = "_",
                includeDate = false,
                includeTime = false,
                now = previewNow
            )
        }
    }

    val currentScopeKey by remember(currentRelativePath, currentPrefix) {
        derivedStateOf { "$currentRelativePath|$currentPrefix" }
    }

    LaunchedEffect(currentScopeKey, previewCounterDigits) {
        val repo = CounterIndexRepository.getInstance(context)

        // 1) Room(CounterIndex) 우선
        val fromDb: Set<Int> = runCatching {
            repo.getUsedCounters(currentRelativePath, currentPrefix)
        }.getOrDefault(emptySet())

        if (fromDb.isNotEmpty()) {
            usedCounters = fromDb
            return@LaunchedEffect
        }

        // 2) DB가 비어있으면, 기존 사진(과거 데이터)용으로 MediaStore 스캔 후 placeholder 백필
        val scanned: Set<Int> = runCatching {
            scanUsedCountersFromMediaStore(
                context = context,
                relativePathPrefix = currentRelativePath,
                fileNamePrefix = currentPrefix,
                counterDigits = previewCounterDigits,
                fnDelim = "_"
            )
        }.getOrDefault(emptySet())

        usedCounters = scanned

        runCatching {
            context.dataStore.edit { it[KEY_USED_COUNTER_VALUES_JSON] = encodeCounterSet(scanned) }
        }

        // placeholder(mediaId=-1)로 DB에 예약해둬서 이후 중복/리셋이 DB 기반으로 동작하도록 함
        runCatching {
            repo.backfillPlaceholders(currentRelativePath, currentPrefix, scanned)
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

    fun commitInlineEditIfNeeded() {
        val id = editingCellId ?: return
        val cell = templateState.cells.firstOrNull { it.cellId == id }

        // COUNTER 중복 체크
        if (cell != null && cell.dataType == TableCellDataType.COUNTER) {
            val newV = editingValue.trim().toIntOrNull()
            val oldV = editingOriginalValue.trim().toIntOrNull()
            if (newV == null || newV < 0) return

            val isChanged = (oldV == null) || (newV != oldV)
            if (isChanged && usedCounters.contains(newV)) {
                pendingCounterCommitValue = newV
                pendingCounterCommitText = editingValue.trim()
                pendingCounterLatestValue = (usedCounters.maxOrNull() ?: 0) + 1
                showCounterDupDialog = true
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
        editingCellId = null
    }

    if (selectedCellId == null && templateState.cells.isNotEmpty()) {
        selectedCellId = templateState.cells.first().cellId
    }

    val selectedCell = templateState.cells.firstOrNull { it.cellId == selectedCellId }
    val hasGroup1 = templateState.cells.any { it.groupLevel == GroupLevel.G1 }

    val savePathPreview = remember(templateState.cells) {
        buildGalleryRelativePath(templateState.cells)
    }

    // ✅ 표 위치/크기(촬영 워터마크 표 렌더 파라미터) - 탭1에서 조절
    var wmAnchor by remember { mutableStateOf(WatermarkTableAnchor.BOTTOM_RIGHT) }
    var wmWidthRatio by remember { mutableIntStateOf(40) }
    var wmHeightRatio by remember { mutableIntStateOf(20) }

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

            captureAspect = CaptureAspect.from(
                prefs[KEY_CAPTURE_ASPECT] ?: CaptureAspect.R3_4.v
            )
        }
    }

    val plan = remember(templateState.cells, previewNow, previewCounterDigits, dateFormat, timeFormat) {
        tableResolver.plan(
            cells = templateState.cells,
            captureNow = previewNow,
            config = TableResolver.Config(
                counterDigits = previewCounterDigits,
                dateFormat = dateFormat,
                timeFormat = timeFormat
            )
        )
    }

    val filenamePreview = buildDisplayNameFromResolvedCells(
        resolvedCells = plan.resolvedCells,
        fnDelim = "_",
        includeDate = false,
        includeTime = false,
        now = previewNow
    )

    fun closeCounterDupDialog() {
        showCounterDupDialog = false
        // 중복 다이얼로그가 뜨는 경우 "편집 유지" 정책이므로 editingCellId는 여기서만 정리
        editingCellId = null
    }

    if (showCounterDupDialog) {
        AlertDialog(
            onDismissRequest = { closeCounterDupDialog() },
            title = { Text("중복 카운터") },
            text = {
                Text(
                    "이미 저장된 번호: $pendingCounterCommitValue\n" +
                            "현재 최신 추천: $pendingCounterLatestValue\n\n" +
                            "그래도 적용할까요?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = editingCellId
                    if (id != null) {
                        val updated = updateCell(templateState, id) { c ->
                            c.copy(
                                typedValue = CellValue.CounterSeed(
                                    pendingCounterCommitText.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
                                )
                            )
                        }
                        onTemplateChange(updated)
                    }
                    closeCounterDupDialog()
                }) { Text("적용", style = DDZTypography.ButtonText) }
                            },
            dismissButton = {
                TextButton(onClick = { closeCounterDupDialog() }) {
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
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DDZColor.Surface,
                contentColor = DDZColor.TextPrimary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { requestTabSwitch(0) },
                    text = { Text("표 구조설정", style = DDZTypography.Body) },
                    selectedContentColor = DDZColor.TextPrimary,
                    unselectedContentColor = DDZColor.TextMuted
                    )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { requestTabSwitch(1) },
                    text = { Text("표 미리보기", style = DDZTypography.Body) },
                    selectedContentColor = DDZColor.TextPrimary,
                    unselectedContentColor = DDZColor.TextMuted
                    )
            }

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

                            TableGridArea(
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
                                            if (editingCellId != null) return@TableGridArea
                                        }
                                        selectedCellId = id

                                            showCellSettingsPanel = true
                                    },
                                    onDoubleClickCell = { cell ->

                                        if (editingCellId != null && editingCellId != cell.cellId) {
                                            commitInlineEditIfNeeded()
                                            if (editingCellId != null) return@TableGridArea
                                            selectedCellId = cell.cellId
                                            showCellSettingsPanel = true
                                            return@TableGridArea
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
                                            editingCellId = cell.cellId
                                            editingValue = cell.toEditableText()
                                            editingOriginalValue = editingValue
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
                                onSetFileNameInclude = { checked ->
                                    val updated = updateCell(templateState, selectedCell.cellId) { c ->
                                        c.copy(fileNameInclude = checked)
                                    }
                                    onTemplateChange(updated)
                                },
                                onSetGroupEnabled = { enabled ->
                                    if (!enabled) {
                                        val updated = updateGroupLevel(templateState, selectedCell.cellId, GroupLevel.NONE)
                                        onTemplateChange(updated)
                                    } else {
                                        // ON 시 기본값은 G1 (MVP)
                                        val updated = updateGroupLevel(templateState, selectedCell.cellId, GroupLevel.G1)
                                        onTemplateChange(updated)
                                    }
                                },
                                onSetGroupLevel = { level ->
                                    val updated = updateGroupLevel(templateState, selectedCell.cellId, level)
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(previewTabScrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ✅ 촬영느낌 미리보기(비율 반영 워터마크 표 오버레이)
                        CameraLikeWatermarkPlacementPreview(
                            captureAspect = captureAspect,
                            rows = templateState.rows,
                            cols = templateState.cols,
                            watermarkCells = WatermarkBuilder.buildTableCells(plan.resolvedCells),
                            anchor = wmAnchor,
                            tableWidthRatio = wmWidthRatio,
                            tableHeightRatio = wmHeightRatio
                        )

                        // ✅ 표 위치/크기 설정 (기존 덩이 C를 탭1로 이식)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DDZColor.Card)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("표 위치/크기", style = DDZTypography.CardTitle, color = DDZColor.TextMuted)

                            fun persistAnchor(a: WatermarkTableAnchor) {
                                wmAnchor = a
                                scope.launch {
                                    context.dataStore.edit { prefs ->
                                        prefs[KEY_WM_TABLE_ANCHOR] = when (a) {
                                            WatermarkTableAnchor.TOP_LEFT -> 0
                                            WatermarkTableAnchor.TOP_RIGHT -> 1
                                            WatermarkTableAnchor.BOTTOM_LEFT -> 2
                                            else -> 3
                                        }
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = wmAnchor == WatermarkTableAnchor.TOP_LEFT,
                                    onClick = { persistAnchor(WatermarkTableAnchor.TOP_LEFT) }
                                )
                                Text("좌상", color = DDZColor.TextPrimary)
                                Spacer(Modifier.width(8.dp))
                                RadioButton(
                                    selected = wmAnchor == WatermarkTableAnchor.TOP_RIGHT,
                                    onClick = { persistAnchor(WatermarkTableAnchor.TOP_RIGHT) }
                                )
                                Text("우상", color = DDZColor.TextPrimary)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = wmAnchor == WatermarkTableAnchor.BOTTOM_LEFT,
                                    onClick = { persistAnchor(WatermarkTableAnchor.BOTTOM_LEFT) }
                                )
                                Text("좌하", color = DDZColor.TextPrimary)
                                Spacer(Modifier.width(8.dp))
                                RadioButton(
                                    selected = wmAnchor == WatermarkTableAnchor.BOTTOM_RIGHT,
                                    onClick = { persistAnchor(WatermarkTableAnchor.BOTTOM_RIGHT) }
                                )
                                Text("우하", color = DDZColor.TextPrimary)
                            }

                            Text(
                                text = "표 크기 (가로 ${wmWidthRatio}%, 세로 ${wmHeightRatio}%)",
                                color = DDZColor.TextPrimary
                            )
                            Slider(
                                value = wmWidthRatio.toFloat(),
                                onValueChange = { v ->
                                    val nv = v.toInt().coerceIn(40, 100)
                                    val nh = ((nv * 0.5f).toInt()).coerceIn(10, 35)
                                    wmWidthRatio = nv
                                    wmHeightRatio = nh
                                    scope.launch {
                                        context.dataStore.edit { prefs ->
                                            prefs[KEY_WM_TABLE_WIDTH] = nv
                                            prefs[KEY_WM_TABLE_HEIGHT] = nh
                                        }
                                    }
                                },
                                valueRange = 40f..100f
                            )
                            Text(
                                text = "※ 촬영 화면/홈/설정 미리보기에는 동일하게 반영됨",
                                style = DDZTypography.Caption,
                                color = DDZColor.TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactPathHeader(
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

@Composable
private fun TableGridArea(
    templateState: TableTemplateState,
    displayTextProvider: (String) -> String,
    selectedCellId: String?,
    editingCellId: String?,
    onSelectCell: (String) -> Unit,
    onDoubleClickCell: (TableCellState) -> Unit,
    editingValue: String,
    onEditingValueChange: (String) -> Unit,
    onCommitInline: () -> Unit,
    inlineFocusRequester: FocusRequester,
    onInlineFocusLostCommit: () -> Unit
    ) {
    // ✅ 셀 단일/더블 클릭 경쟁 제거:
    // 단일 클릭은 더블탭 타임아웃 이후 실행, 더블 클릭이 오면 단일 클릭 예약 취소
    val vc = LocalViewConfiguration.current
    val scope = rememberCoroutineScope()
    var pendingSingleClickJob by remember { mutableStateOf<Job?>(null) }

    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val safeCols = templateState.cols.coerceAtLeast(1)
        val cellW = remember(maxWidth, safeCols) { maxWidth / safeCols }

        // ===== Grid 가시성 정책 =====
        val minCellHeight = 48.dp
        val defaultCellHeight = 64.dp

        // 현재 화면에서 허용 가능한 최대 행 수 계산
        val maxVisibleRows =
            (maxHeight / minCellHeight).toInt().coerceAtLeast(1)

        val needsVerticalScroll = templateState.rows > maxVisibleRows

        val gridScrollState = rememberScrollState()

        // NOTE: 세로는 rows가 많아지면 다 안 보일 수 있으므로,
        // 여기서는 "가능한 범위 내 전체 가시"를 우선하고,
        // 추후 임계치 기반 scale/scroll 정책을 이 영역에 적용한다.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (needsVerticalScroll)
                        Modifier.verticalScroll(gridScrollState)
                    else Modifier
                )
        ) {
            repeat(templateState.rows) { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(templateState.cols) { col ->
                        val cell = templateState.cells.firstOrNull {
                            it.rowIndex == row && it.colIndex == col
                        }

                        val isSelectedCell = cell?.cellId == selectedCellId
                        val isEditingCell = cell?.cellId == editingCellId

                        val cellBackground =
                            if (isEditingCell) DDZColor.Success.copy(alpha = 0.2f) else DDZColor.Card

                        Box(
                            modifier = Modifier
                                .width(cellW)
                                .height(defaultCellHeight)
                                .padding(2.dp)
                                .background(cellBackground, RoundedCornerShape(8.dp))
                                .border(
                                    width = when {
                                        isEditingCell -> 2.dp
                                        isSelectedCell -> 2.dp
                                        else -> 1.dp
                                    },
                                    color = when {
                                        isEditingCell -> DDZColor.Success
                                        isSelectedCell -> DDZColor.Primary
                                        else -> DDZColor.Border
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .combinedClickable(
                                    enabled = (cell != null && !isEditingCell),
                                    onClick = {
                                        if (cell == null) return@combinedClickable
                                        pendingSingleClickJob?.cancel()
                                        pendingSingleClickJob = scope.launch {
                                            delay(vc.doubleTapTimeoutMillis.toLong())
                                            onSelectCell(cell.cellId)
                                        }
                                    },
                                    onDoubleClick = {
                                        if (cell == null) return@combinedClickable
                                        pendingSingleClickJob?.cancel()
                                        pendingSingleClickJob = null
                                        onDoubleClickCell(cell)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cell != null) {
                                val display = displayTextProvider(cell.cellId)

                                val canInlineEdit =
                                    (cell.dataType == TableCellDataType.TEXT ||
                                            cell.dataType == TableCellDataType.NUMBER ||
                                            cell.dataType == TableCellDataType.COUNTER)

                                if (isEditingCell && canInlineEdit) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(14.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) { CellHeaderBadges(cell) }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val keyboardType = when (cell.dataType) {
                                                TableCellDataType.NUMBER -> KeyboardType.Decimal
                                                TableCellDataType.COUNTER -> KeyboardType.Number
                                                else -> KeyboardType.Text
                                            }

                                            var hasEverFocused by remember(cell.cellId) {
                                                mutableStateOf(false)
                                            }

                                            BasicTextField(
                                                value = editingValue,
                                                onValueChange = onEditingValueChange,
                                                singleLine = true,
                                                textStyle = DDZTypography.Caption.copy(color = DDZColor.TextPrimary),
                                                cursorBrush = SolidColor(DDZColor.Success),
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = keyboardType,
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(onDone = { onCommitInline() }),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 2.dp, vertical = 2.dp)
                                                    .focusRequester(inlineFocusRequester)
                                                    .onFocusChanged { state ->
                                                        if (state.isFocused) {
                                                            if (!hasEverFocused) {
                                                                hasEverFocused = true
                                                            }
                                                        }
                                                        else if (hasEverFocused) {
                                                            onInlineFocusLostCommit()
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(14.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) { CellHeaderBadges(cell) }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(display, style = DDZTypography.Caption, color = DDZColor.TextPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomFixedActionBar(
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
private fun CellSettingsBottomPanel(
    modifier: Modifier,
    cell: TableCellState,
    hasGroup1: Boolean,
    onSetFileNameInclude: (Boolean) -> Unit,
    onSetGroupEnabled: (Boolean) -> Unit,
    onSetGroupLevel: (GroupLevel) -> Unit,
    onSetDataType: (TableCellDataType) -> Unit
    ) {
    val groupEnabled = cell.groupLevel == GroupLevel.G1 || cell.groupLevel == GroupLevel.G2

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
                Text("그룹 사용", style = DDZTypography.Body, color = DDZColor.TextMuted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(if (groupEnabled) "ON" else "OFF", style = DDZTypography.Body, color = DDZColor.TextPrimary)
                    Switch(
                        checked = groupEnabled,
                        onCheckedChange = onSetGroupEnabled
                    )
                }
            }
        }

        // ✅ Group ON일 때만 G1/G2 세그먼트 노출
        if (groupEnabled) {
            val g2Enabled = hasGroup1 || cell.groupLevel == GroupLevel.G2
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DDZColor.Card, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isG1 = cell.groupLevel == GroupLevel.G1
                val isG2 = cell.groupLevel == GroupLevel.G2

                // 세그먼트 버튼 (단순 구현: Button 2개)
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onSetGroupLevel(GroupLevel.G1) },
                    enabled = true
                ) { Text(if (isG1) "G1 ✓" else "G1", style = DDZTypography.ButtonText) }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = { onSetGroupLevel(GroupLevel.G2) },
                    enabled = g2Enabled
                ) { Text(if (isG2) "G2 ✓" else "G2", style = DDZTypography.ButtonText) }
            }
            if (!g2Enabled) {
                Text("※ G2는 G1 설정 후 사용 가능", style = DDZTypography.Caption, color = DDZColor.TextMuted)
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
private fun CameraLikeWatermarkPlacementPreview(
    captureAspect: CaptureAspect,
    rows: Int,
    cols: Int,
    watermarkCells: List<WatermarkBuilder.WatermarkCell>,
    anchor: WatermarkTableAnchor,
    tableWidthRatio: Int,
    tableHeightRatio: Int
) {
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
                        bgAlpha = 80,
                        labelScale = 100,
                        valueScale = 100
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

private fun updateGroupLevel(
    templateState: TableTemplateState,
    cellId: String,
    level: GroupLevel
): TableTemplateState {
    return templateState.copy(
        cells = templateState.cells.map { cell ->
            when {
                cell.cellId == cellId -> cell.copy(groupLevel = level)
                level == GroupLevel.G1 && cell.groupLevel == GroupLevel.G1 -> cell.copy(groupLevel = GroupLevel.NONE)
                level == GroupLevel.G2 && cell.groupLevel == GroupLevel.G2 -> cell.copy(groupLevel = GroupLevel.NONE)
                else -> cell
            }
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
    return templateState.copy(
        rows = templateState.rows + 1,
        cells = templateState.cells + newCells
    )
}

private fun removeRow(templateState: TableTemplateState): TableTemplateState {
    val lastRowIndex = templateState.rows - 1
    return templateState.copy(
        rows = templateState.rows - 1,
        cells = templateState.cells.filterNot { it.rowIndex == lastRowIndex }
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
    return templateState.copy(
        cols = templateState.cols + 1,
        cells = templateState.cells + newCells
    )
}

private fun removeColumn(templateState: TableTemplateState): TableTemplateState {
    val lastColIndex = templateState.cols - 1
    return templateState.copy(
        cols = templateState.cols - 1,
        cells = templateState.cells.filterNot { it.colIndex == lastColIndex }
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
