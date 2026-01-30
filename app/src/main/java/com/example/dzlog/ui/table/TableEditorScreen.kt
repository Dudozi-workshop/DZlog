@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.dzlog.ui.table

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import com.example.dzlog.data.counter.COUNTER_DIGITS_DEFAULT
import com.example.dzlog.data.counter.clampCounterDigits
import com.example.dzlog.data.counter.decodeCounterSet
import com.example.dzlog.data.counter.encodeCounterSet
import com.example.dzlog.data.counter.scanUsedCountersFromMediaStore
import com.example.dzlog.data.preferences.KEY_COUNTER_DIGITS
import com.example.dzlog.data.preferences.KEY_COUNTER_SUFFIX_ENABLED
import com.example.dzlog.data.preferences.KEY_TABLE_TEMPLATE_JSON
import com.example.dzlog.data.preferences.KEY_USED_COUNTER_VALUES_JSON
import com.example.dzlog.data.preferences.KEY_WM_TABLE_ANCHOR
import com.example.dzlog.data.preferences.KEY_WM_TABLE_HEIGHT
import com.example.dzlog.data.preferences.KEY_WM_TABLE_WIDTH
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.data.template.toJsonString
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.CellValue
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
import com.example.dzlog.domain.preview.computeNextDelayMillis
import com.example.dzlog.domain.preview.decideTickUnit
import com.example.dzlog.domain.table.TableResolver
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
    var selectedCellId by remember { mutableStateOf(templateState.cells.firstOrNull()?.cellId) }
    val scrollState = rememberScrollState()

    val scope = rememberCoroutineScope()
    var isSavingTemplate by remember { mutableStateOf(false) }

    var showFormatDialog by remember { mutableStateOf(false) }
    var formatTargetCellId by remember { mutableStateOf<String?>(null) }
    var formatTargetType by remember { mutableStateOf<TableCellDataType?>(null) }

    val dateFormatOptions = listOf("yyyy.MM.dd", "yyyy_MM_dd", "yyyyMMdd")
    // TIME 형식은 Step3부터 토글 UI(12/24, 초, 구분자)로 설정한다.

    var usedCounters by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showCounterDupDialog by remember { mutableStateOf(false) }
    var pendingCounterCommitValue by remember { mutableIntStateOf(0) }
    var pendingCounterCommitText by remember { mutableStateOf("") }
    var pendingCounterLatestValue by remember { mutableStateOf(0) }

    val currentRelativePath by remember(templateState.cells) {
        derivedStateOf {
            buildGalleryRelativePath(templateState.cells)
        }
    }

    LaunchedEffect(currentRelativePath) {
        val prefs = runCatching { context.dataStore.data.first() }.getOrNull()
        val cached = decodeCounterSet(prefs?.get(KEY_USED_COUNTER_VALUES_JSON))

        val scanned: Set<Int>? = runCatching { scanUsedCountersFromMediaStore(context, currentRelativePath) }.getOrNull()
        val effective: Set<Int> = scanned ?: cached

        usedCounters = effective

        runCatching {
            context.dataStore.edit { it[KEY_USED_COUNTER_VALUES_JSON] = encodeCounterSet(effective) }
        }
    }

    var editingCellId by remember { mutableStateOf<String?>(null) }
    var editingValue by remember { mutableStateOf("") }
    var editingOriginalValue by remember { mutableStateOf("") }

    val keyboardController = LocalSoftwareKeyboardController.current
    val inlineFocusRequester = remember { FocusRequester() }

    var inlineHasFocusedOnce by remember { mutableStateOf(false) }
    var suppressNextCommit by remember { mutableStateOf(false) }

    LaunchedEffect(editingCellId) {
        inlineHasFocusedOnce = false
        if (editingCellId != null) {
            delay(30)
            inlineFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    fun commitInlineEditIfNeeded() {
        if (suppressNextCommit) {
            suppressNextCommit = false
            editingCellId = null
            return
        }

        val id = editingCellId ?: return
        val cell = templateState.cells.firstOrNull { it.cellId == id }

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
            TableCellDataType.TEXT -> c.copy(rawText = normalizedValueText, typedValue = CellValue.Text(normalizedValueText))
            TableCellDataType.NUMBER -> c.copy(rawText = normalizedValueText, typedValue = CellValue.Number(normalizedValueText))
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

    val dateFormat = "yyyy.MM.dd"
    val timeFormat = "HH.mm.ss"

    var previewCounterDigits by remember { mutableIntStateOf(COUNTER_DIGITS_DEFAULT) }
    var previewCounterSuffixEnabled by remember { mutableStateOf(true) }
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

    // ✅ 표 위치/크기(촬영 워터마크 표 렌더 파라미터)
    // - 촬영설정에서 제거하고, 표 상세설정에서만 조절하도록 이동 (MVP: 4분면 + 크기)
    var wmAnchor by remember { mutableStateOf(WatermarkTableAnchor.BOTTOM_RIGHT) }
    var wmWidthRatio by remember { mutableIntStateOf(40) }
    var wmHeightRatio by remember { mutableIntStateOf(20) }
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
        }
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

    val tableResolver = remember { TableResolver() }
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
        counterSuffixEnabled = previewCounterSuffixEnabled,
        now = previewNow
    )

    if (showCounterDupDialog) {
        AlertDialog(
            onDismissRequest = {
                editingValue = editingOriginalValue
                showCounterDupDialog = false
                editingCellId = null
            },
            title = { Text("중복 카운터") },
            text = {
                Text(
                    "이미 저장된 번호: ${pendingCounterCommitValue}\n" +
                            "현재 최신 추천: ${pendingCounterLatestValue}\n\n" +
                            "그래도 적용할까요?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = editingCellId
                    if (id != null) {
                        val updated = updateCell(templateState, id) { c ->
                            c.copy(typedValue = CellValue.CounterSeed(pendingCounterCommitText.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0))
                        }
                        onTemplateChange(updated)
                    }
                    showCounterDupDialog = false
                    editingCellId = null
                }) { Text("적용") }
            },
            dismissButton = {
                TextButton(onClick = {
                    editingValue = editingOriginalValue
                    showCounterDupDialog = false
                    editingCellId = null
                }) { Text("취소") }
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
            onDismissRequest = {
                showFormatDialog = false
                formatTargetCellId = null
                formatTargetType = null
            },
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
                                        showFormatDialog = false
                                        formatTargetCellId = null
                                        formatTargetType = null
                                    }
                                ) { Text(if (current == p) "✓  $p" else p) }
                            }
                        }

                        isTime -> {
                            // 초기값: 셀에 저장된 옵션이 없으면 기본값 사용
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
                                        // TIME은 formatPattern을 직접 쓰지 않는 방향(옵션으로 생성)으로 통일
                                        formatPattern = ""
                                    )
                                }
                                onTemplateChange(updated)
                            }

                            Text("시간 표시 설정", fontSize = 14.sp)

                            // 12/24
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("시간제", modifier = Modifier.width(72.dp))
                                RadioButton(
                                    selected = hourSystem == HourSystem.H24,
                                    onClick = {
                                        hourSystem = HourSystem.H24
                                        apply()
                                    }
                                )
                                Text("24h")
                                Spacer(Modifier.width(12.dp))
                                RadioButton(
                                    selected = hourSystem == HourSystem.H12,
                                    onClick = {
                                        hourSystem = HourSystem.H12
                                        apply()
                                    }
                                )
                                Text("12h")
                            }

                            // seconds
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("초 포함", modifier = Modifier.width(72.dp))
                                Switch(
                                    checked = includeSeconds,
                                    onCheckedChange = {
                                        includeSeconds = it
                                        apply()
                                    }
                                )
                            }

                            // separator
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("구분자", modifier = Modifier.width(72.dp))

                                listOf(TimeSeparator.COLON, TimeSeparator.NONE).forEach { s ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = separator == s,
                                            onClick = {
                                                separator = s
                                                apply()
                                            }
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
                                if (hourSystem == HourSystem.H12) {
                                    append(" a")
                                }
                            }
                            Text("미리보기: $preview", fontSize = 12.sp, color = Color.DarkGray)
                        }

                        else -> {
                            Text("지원되지 않는 타입")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showFormatDialog = false
                    formatTargetCellId = null
                    formatTargetType = null
                }) { Text("닫기") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("표 상세설정") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1EDE3))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFEFEAE0))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Save Path Preview", fontSize = 12.sp, color = Color.DarkGray)
                    Text(savePathPreview, color = Color.Black)
                    Spacer(Modifier.height(6.dp))
                    Text("Filename Preview", fontSize = 12.sp, color = Color.DarkGray)
                    val templateHasCounter = templateState.cells.any { it.dataType == TableCellDataType.COUNTER }
                    if (previewCounterSuffixEnabled && templateHasCounter) {
                        Text("COUNTER 태그 부착", fontSize = 11.sp, color = Color(0xFF3F7D4C))
                    }
                    Text(filenamePreview, color = Color.Black)

                    Spacer(Modifier.height(10.dp))
                    // ✅ 표 상세설정은 "편집형" 미리보기(배지/스티커 포함)로 표시
                    // - 홈/촬영/설정의 미리보기(표+값만)와 UI를 분리
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    ) {
                        EditorTableMiniPreview(
                            templateState = templateState,
                            plan = plan,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // ✅ 표 위치/크기 설정 (MVP: 4분면 + 크기)
                    // - 촬영설정에서 제거하고, 표 상세설정에서만 조절
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF5F1E8))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("표 위치/크기", fontSize = 13.sp, color = Color.DarkGray)

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
                            Text("좌상", color = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            RadioButton(
                                selected = wmAnchor == WatermarkTableAnchor.TOP_RIGHT,
                                onClick = { persistAnchor(WatermarkTableAnchor.TOP_RIGHT) }
                            )
                            Text("우상", color = Color.Black)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = wmAnchor == WatermarkTableAnchor.BOTTOM_LEFT,
                                onClick = { persistAnchor(WatermarkTableAnchor.BOTTOM_LEFT) }
                            )
                            Text("좌하", color = Color.Black)
                            Spacer(Modifier.width(8.dp))
                            RadioButton(
                                selected = wmAnchor == WatermarkTableAnchor.BOTTOM_RIGHT,
                                onClick = { persistAnchor(WatermarkTableAnchor.BOTTOM_RIGHT) }
                            )
                            Text("우하", color = Color.Black)
                        }

                        // 크기: 기존 width/height ratio를 비율 유지(기본 40:20)로 함께 조절
                        Text("표 크기 (${wmWidthRatio}%)", color = Color.Black)
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
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val cellW = maxWidth / templateState.cols.coerceAtLeast(1)

                    repeat(templateState.rows) { row ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                        repeat(templateState.cols) { col ->
                            val cell = templateState.cells.firstOrNull {
                                it.rowIndex == row && it.colIndex == col
                            }
                            // ✅ 상태는 먼저 계산
                            val isSelectedCell = cell?.cellId == selectedCellId
                            val isEditingCell = cell?.cellId == editingCellId
                            // ✅ 편집 중 배경 강조
                            val cellBackground =
                                if (isEditingCell) Color(0xFFEAF3EC)   // ✨ 편집 중: 연한 강조
                                else Color(0xFFF7F4EE)

                            Box(
                                modifier = Modifier
                                    .width(cellW)
                                    .height(64.dp)
                                    .padding(2.dp)
                                    .background(cellBackground)
                                    .border(
                                        width = when {
                                            isEditingCell -> 2.dp
                                            isSelectedCell -> 2.dp
                                            else -> 1.dp
                                        },
                                        color = when {
                                            isEditingCell -> Color(0xFF3F7D4C)   // ✨ 편집 중: 조금 더 진한 색
                                            isSelectedCell -> Color(0xFF5B7F60)
                                            else -> Color(0xFFBDBDBD)
                                        }
                                    )

                                    // ✅ 단일/더블을 한 곳에서 처리 (저장/전환 안정화)
                                    .combinedClickable(
                                        enabled = (cell != null),
                                        onClick = {
                                            if (cell == null) return@combinedClickable

                                            // ✅ 편집 중 다른 셀로 이동이면 "항상 저장"
                                            if (editingCellId != null && editingCellId != cell.cellId) {
                                                commitInlineEditIfNeeded()
                                                // counter 중복 다이얼로그 등으로 commit이 보류되면 이동/선택도 막는다
                                                if (editingCellId != null) return@combinedClickable
                                            }

                                            selectedCellId = cell.cellId
                                        },
                                        onDoubleClick = {
                                            if (cell == null) return@combinedClickable

                                            // ✅ MVP 정책(B):
                                            // 편집 중에 "다른 셀 더블클릭"은 편집 전환을 하지 않고
                                            // 단일 선택 동작(저장  선택)으로만 처리한다.
                                            if (editingCellId != null && editingCellId != cell.cellId) {
                                                commitInlineEditIfNeeded()
                                                // commit이 보류(예: 카운터 중복 다이얼로그)면 이동/선택도 막음
                                                if (editingCellId != null) return@combinedClickable
                                                selectedCellId = cell.cellId
                                                return@combinedClickable
                                            }

                                            // (편집 중이 아니거나, 같은 셀 더블클릭이면) 정상 더블클릭 처리
                                            selectedCellId = cell.cellId

                                            val canInline = (
                                                    cell.dataType == TableCellDataType.TEXT ||
                                                            cell.dataType == TableCellDataType.NUMBER ||
                                                            cell.dataType == TableCellDataType.COUNTER
                                                    )

                                            if (canInline) {
                                                editingCellId = cell.cellId
                                                editingValue = cell.toEditableText()
                                                editingOriginalValue = editingValue
                                            } else {
                                                if (cell.dataType == TableCellDataType.DATE || cell.dataType == TableCellDataType.TIME) {
                                                    formatTargetCellId = cell.cellId
                                                    formatTargetType = cell.dataType
                                                    showFormatDialog = true
                                                }
                                            }
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (cell != null) {
                                    val display = plan.resolvedCells
                                        .firstOrNull { it.id == cell.cellId }
                                        ?.resolvedText
                                        .orEmpty()
                                    val isEditing = (editingCellId == cell.cellId)
                                    val canInlineEdit =
                                        (cell.dataType == TableCellDataType.TEXT ||
                                                cell.dataType == TableCellDataType.NUMBER ||
                                                cell.dataType == TableCellDataType.COUNTER)

                                    if (isEditing && canInlineEdit) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            // 🔹 Header 고정
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(14.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                CellHeaderBadges(cell)
                                            }

                                            // 🔹 Editor 영역: 남은 공간 (TextField ❌ / BasicTextField ⭕)
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

                                                BasicTextField(
                                                    value = editingValue,
                                                    onValueChange = { editingValue = it },
                                                    singleLine = true,
                                                    textStyle = TextStyle(
                                                        fontSize = 12.sp,
                                                        color = Color.Black
                                                    ),
                                                    cursorBrush = SolidColor(Color(0xFF5B7F60)),
                                                    keyboardOptions = KeyboardOptions(
                                                        keyboardType = keyboardType,
                                                        imeAction = ImeAction.Done
                                                    ),
                                                    keyboardActions = KeyboardActions(
                                                        onDone = {
                                                            commitInlineEditIfNeeded()
                                                            editingCellId = null
                                                            keyboardController?.hide()
                                                        }
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 2.dp, vertical = 2.dp)
                                                        .focusRequester(inlineFocusRequester)
                                                        .onFocusChanged { state ->
                                                            if (state.isFocused) {
                                                                inlineHasFocusedOnce = true
                                                            } else {
                                                                if (inlineHasFocusedOnce) {
                                                                    commitInlineEditIfNeeded()
                                                                    editingCellId = null
                                                                }
                                                            }
                                                        }
                                                )
                                            }
                                        }

                                    } else {
// 🔹 상단 배지  중앙 텍스트(표시 전용)
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            // Header: 고정 높이로 확보 (아이콘이 있어도 콘텐츠 영역 안 침범)
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(14.dp),
                                                contentAlignment =
                                                    Alignment.CenterStart
                                            ) {
                                                CellHeaderBadges(cell)
                                            }
                                            // Content: 남은 공간만 사용
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(display, fontSize = 12.sp, color = Color.Black)
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val updated = addRow(templateState)
                        onTemplateChange(updated)
                    }
                ) { Text("+Row", fontSize = 12.sp, maxLines = 1, softWrap = false) }

                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val updated = removeRow(templateState)
                        onTemplateChange(updated)
                        if (updated.cells.none { it.cellId == selectedCellId }) {
                            selectedCellId = updated.cells.firstOrNull()?.cellId
                        }
                    },
                    enabled = templateState.rows > 1
                ) { Text("-Row", fontSize = 12.sp, maxLines = 1, softWrap = false) }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val updated = addColumn(templateState)
                        onTemplateChange(updated)
                    }
                ) { Text("+Col", fontSize = 12.sp, maxLines = 1, softWrap = false) }
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val updated = removeColumn(templateState)
                        onTemplateChange(updated)
                        if (updated.cells.none { it.cellId == selectedCellId }) {
                            selectedCellId = updated.cells.firstOrNull()?.cellId
                        }
                    },
                    enabled = templateState.cols > 1
                ) { Text("-Col", fontSize = 12.sp, maxLines = 1, softWrap = false) }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        Toast.makeText(context, "Saved (stub)", Toast.LENGTH_SHORT).show()
                        commitInlineEditIfNeeded()
                        isSavingTemplate = true
                        scope.launch {
                            runCatching {
                                context.dataStore.edit { prefs ->
                                    prefs[KEY_TABLE_TEMPLATE_JSON] = templateState.toJsonString()
                                }
                            }.onFailure {
                                Toast.makeText(context, "Save failed: ${it.message}", Toast.LENGTH_SHORT).show()
                            }.onSuccess {
                                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                            isSavingTemplate = false
                        }
                    },
                    enabled = !isSavingTemplate
                ) { Text("Save") }
                Button(onClick = onReset) { Text("Reset") }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7F4EE))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Cell Detail Panel", fontSize = 16.sp, color = Color.Black)
                if (selectedCell == null) {
                    Text("셀을 선택하세요.", color = Color.DarkGray)
                } else {
                    Text("Filename include", color = Color.Black)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (selectedCell.fileNameInclude) "ON" else "OFF", color = Color.DarkGray)
                        Switch(
                            checked = selectedCell.fileNameInclude,
                            onCheckedChange = { checked ->
                                val updated = updateCell(templateState, selectedCell.cellId) { cell ->
                                    cell.copy(fileNameInclude = checked)
                                }
                                onTemplateChange(updated)
                            }
                        )
                    }

                    Text("Data Type", color = Color.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedCell.dataType == TableCellDataType.TEXT,
                            onClick = {
                                val updated = updateCell(templateState, selectedCell.cellId) { cell ->
                                    cell.withDataType(TableCellDataType.TEXT)
                                }
                                onTemplateChange(updated)
                            }
                        )
                        Text("TEXT", color = Color.Black)
                        Spacer(Modifier.width(12.dp))
                        RadioButton(
                            selected = selectedCell.dataType == TableCellDataType.NUMBER,
                            onClick = {
                                val updated = updateCell(templateState, selectedCell.cellId) { cell ->
                                    cell.withDataType(TableCellDataType.NUMBER)
                                }
                                onTemplateChange(updated)
                            }
                        )
                        Text("NUMBER", color = Color.Black)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedCell.dataType == TableCellDataType.DATE,
                            onClick = {
                                val updated = updateCell(templateState, selectedCell.cellId) { cell ->
                                    cell.withDataType(TableCellDataType.DATE)
                                }
                                onTemplateChange(updated)
                            }
                        )
                        Text("DATE", color = Color.Black)
                        Spacer(Modifier.width(12.dp))
                        RadioButton(
                            selected = selectedCell.dataType == TableCellDataType.TIME,
                            onClick = {
                                val updated = updateCell(templateState, selectedCell.cellId) { cell ->
                                    cell.withDataType(TableCellDataType.TIME)
                                }
                                onTemplateChange(updated)
                            }
                        )
                        Text("TIME", color = Color.Black)
                        Spacer(Modifier.width(12.dp))
                        RadioButton(
                            selected = selectedCell.dataType == TableCellDataType.COUNTER,
                            onClick = {
                                val updated = updateCell(templateState, selectedCell.cellId) { cell ->
                                    cell.withDataType(TableCellDataType.COUNTER)
                                }
                                onTemplateChange(updated)
                            }
                        )
                        Text("COUNTER", color = Color.Black)
                    }

                    Text("Group", color = Color.Black)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GroupLevel.entries.forEach { level ->
                            val enabled = level != GroupLevel.G2 || hasGroup1
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = selectedCell.groupLevel == level,
                                    enabled = enabled,
                                    onClick = {
                                        if (enabled) {
                                            val hadExisting = templateState.cells.any {
                                                it.cellId != selectedCell.cellId && it.groupLevel == level
                                            }
                                            val updated = updateGroupLevel(
                                                templateState = templateState,
                                                cellId = selectedCell.cellId,
                                                level = level
                                            )
                                            onTemplateChange(updated)
                                            if (hadExisting && level != GroupLevel.NONE) {
                                                Toast.makeText(
                                                    context,
                                                    "${level.name} moved to selected cell",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                )
                                Text(level.name, color = if (enabled) Color.Black else Color.LightGray)
                                Spacer(Modifier.width(8.dp))
                            }
                        }
                    }

                    val canInlineEditSelected =
                        (selectedCell.dataType == TableCellDataType.TEXT ||
                                selectedCell.dataType == TableCellDataType.NUMBER ||
                                selectedCell.dataType == TableCellDataType.COUNTER)

                    when {
                        canInlineEditSelected -> {
                            Text(
                                "값 입력: 셀을 다시 눌러(더블클릭) 입력",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                        selectedCell.dataType == TableCellDataType.DATE || selectedCell.dataType == TableCellDataType.TIME -> {
                            Text(
                                "DATE/TIME: 저장 시각(captureNow) 기준 자동 적용됨\n(더블클릭: 형식 설정)",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                        else -> {
                            Text(
                                "이 셀은 값 입력 대상이 아님",
                                fontSize = 12.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
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
            if (cell.cellId == cellId) {
                transform(cell)
            } else {
                cell
            }
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
