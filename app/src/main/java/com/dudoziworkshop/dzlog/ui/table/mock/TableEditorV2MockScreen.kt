package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

private enum class MockMode { EDIT, LAYOUT }
private enum class MockCellType(val label: String) {
    TEXT("텍스트"),
    NUMBER("숫자"),
    COUNTER("자동번호"),
    DATE("날짜"),
    TIME("시간"),
    ROTATING_TEXT("순환문구"),
}

private data class MockCell(
    val id: Int,
    val value: String,
    val type: MockCellType = MockCellType.TEXT,
)

private enum class MockPathSourceType(val label: String) {
    CELL("셀에서 가져오기"),
    MANUAL("직접 입력"),
    DATE("날짜"),
    TIME("시간"),
    ROTATING_TEXT("순환문구"),
}

private data class MockPathItem(
    val sourceType: MockPathSourceType,
    val value: String,
    val cellId: Int? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableEditorV2MockScreen(
    onBack: () -> Unit,
) {
    val cells = remember {
        mutableStateListOf(
            MockCell(0, "Draper"),
            MockCell(1, "0012", MockCellType.COUNTER),
            MockCell(2, "2026.10.06", MockCellType.DATE),
            MockCell(3, "천안"),
            MockCell(4, "처리구 A"),
            MockCell(5, "반복 1"),
        )
    }
    var rows by remember { mutableIntStateOf(3) }
    var cols by remember { mutableIntStateOf(2) }
    var selectedId by remember { mutableStateOf<Int?>(null) }
    var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var mode by remember { mutableStateOf(MockMode.EDIT) }
    var showStyle by remember { mutableStateOf(false) }
    var showSaveRules by remember { mutableStateOf(false) }
    var darkTable by remember { mutableStateOf(false) }
    var gridEnabled by remember { mutableStateOf(true) }
    var fontScale by remember { mutableFloatStateOf(1f) }
    var textAlignIndex by remember { mutableIntStateOf(1) }

    Scaffold(
        containerColor = Color(0xFFF7F7FA),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                navigationIcon = {
                    IconButton(onClick = {
                        if (mode == MockMode.LAYOUT) {
                            mode = MockMode.EDIT
                            selectedIds = emptySet()
                        } else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                title = {
                    Text(
                        if (mode == MockMode.LAYOUT) "레이아웃 편집" else "표 편집",
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    if (mode == MockMode.LAYOUT) {
                        Button(onClick = {
                            mode = MockMode.EDIT
                            selectedIds = emptySet()
                        }) { Text("완료") }
                    } else {
                        IconButton(onClick = { }) {
                            Icon(Icons.Filled.Undo, contentDescription = "실행 취소")
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Filled.Redo, contentDescription = "다시 실행")
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Filled.Save, contentDescription = "저장")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (mode == MockMode.EDIT) {
                MockBottomBar(
                    onLayout = {
                        selectedId = null
                        mode = MockMode.LAYOUT
                    },
                    onStyle = { showStyle = true },
                    onSaveRules = { showSaveRules = true },
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = if (mode == MockMode.EDIT)
                    "셀을 직접 눌러 수정해보세요."
                else
                    "여러 셀을 선택한 뒤 구조를 조정해보세요.",
                color = DDZColor.TextMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )

            MockTableCanvas(
                cells = cells,
                rows = rows,
                cols = cols,
                selectedId = selectedId,
                selectedIds = selectedIds,
                darkTable = darkTable,
                gridEnabled = gridEnabled,
                layoutMode = mode == MockMode.LAYOUT,
                onCellClick = { id ->
                    if (mode == MockMode.LAYOUT) {
                        selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
                    } else {
                        selectedId = id
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            if (mode == MockMode.EDIT) {
                val selected = selectedId?.let { id -> cells.firstOrNull { it.id == id } }
                if (selected != null) {
                    MockCellEditor(
                        cell = selected,
                        onValueChange = { next ->
                            val index = cells.indexOfFirst { it.id == selected.id }
                            if (index >= 0) cells[index] = selected.copy(value = next)
                        },
                        onTypeChange = { nextType ->
                            val index = cells.indexOfFirst { it.id == selected.id }
                            if (index >= 0) cells[index] = selected.copy(type = nextType)
                        },
                        onClose = { selectedId = null },
                    )
                }
            } else {
                MockLayoutPanel(
                    selectedCount = selectedIds.size,
                    rows = rows,
                    cols = cols,
                    onAddRow = {
                        rows += 1
                        val start = cells.size
                        repeat(cols) { offset -> cells += MockCell(start + offset, "새 셀") }
                    },
                    onAddCol = {
                        cols += 1
                        val start = cells.size
                        repeat(rows) { offset -> cells += MockCell(start + offset, "새 셀") }
                    },
                    onDeleteSelection = {
                        selectedIds = emptySet()
                    },
                )
            }
        }
    }

    if (showStyle) {
        ModalBottomSheet(onDismissRequest = { showStyle = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text("표 스타일", fontWeight = FontWeight.Bold)

                Text("배경", color = DDZColor.TextMuted)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("밝게", "어둡게", "투명").forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = when (index) {
                                0 -> !darkTable
                                1 -> darkTable
                                else -> false
                            },
                            onClick = {
                                if (index == 0) darkTable = false
                                if (index == 1) darkTable = true
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, 3)
                        ) { Text(label) }
                    }
                }

                Text("글자 크기", color = DDZColor.TextMuted)
                Slider(
                    value = fontScale,
                    onValueChange = { fontScale = it },
                    valueRange = 0.8f..1.4f,
                )

                Text("정렬", color = DDZColor.TextMuted)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("왼쪽", "가운데", "오른쪽").forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = textAlignIndex == index,
                            onClick = { textAlignIndex = index },
                            shape = SegmentedButtonDefaults.itemShape(index, 3)
                        ) { Text(label) }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("테두리 표시")
                    Switch(checked = gridEnabled, onCheckedChange = { gridEnabled = it })
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { },
                ) { Text("더보기") }
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    onClick = { showStyle = false },
                ) { Text("적용") }
            }
        }
    }

    if (showSaveRules) {
        MockSaveRulesSheet(
            cells = cells,
            rows = rows,
            cols = cols,
            onDismiss = { showSaveRules = false },
        )
    }
}

@Composable
private fun ColumnScope.MockTableCanvas(
    cells: List<MockCell>,
    rows: Int,
    cols: Int,
    selectedId: Int?,
    selectedIds: Set<Int>,
    darkTable: Boolean,
    gridEnabled: Boolean,
    layoutMode: Boolean,
    onCellClick: (Int) -> Unit,
) {
    val background = if (darkTable) Color(0xFF1E2220) else Color(0xFFE9EFE7)
    val cellBackground = if (darkTable) Color(0xFF202522) else Color.White.copy(alpha = 0.94f)
    val textColor = if (darkTable) Color.White else Color(0xFF202124)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(background)
            .padding(26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, DDZColor.Primary, RoundedCornerShape(4.dp))
        ) {
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until cols) {
                        val index = row * cols + col
                        val cell = cells.getOrNull(index) ?: MockCell(index, "새 셀")
                        val isSelected = if (layoutMode) cell.id in selectedIds else selectedId == cell.id
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(74.dp)
                                .background(
                                    if (isSelected) DDZColor.Primary.copy(alpha = 0.16f) else cellBackground
                                )
                                .then(
                                    if (gridEnabled) Modifier.border(
                                        0.5.dp,
                                        if (darkTable) Color.White.copy(alpha = 0.28f) else Color(0xFFB8B8BE)
                                    ) else Modifier
                                )
                                .clickable { onCellClick(cell.id) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                cell.value,
                                color = if (isSelected) DDZColor.PrimaryDark else textColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MockCellEditor(
    cell: MockCell,
    onValueChange: (String) -> Unit,
    onTypeChange: (MockCellType) -> Unit,
    onClose: () -> Unit,
) {
    var showTypePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("선택한 셀", fontWeight = FontWeight.Bold)
            Text(
                "×",
                modifier = Modifier
                    .clickable(onClick = onClose)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                color = DDZColor.TextMuted,
            )
        }

        when (cell.type) {
            MockCellType.TEXT,
            MockCellType.NUMBER -> {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = cell.value,
                    onValueChange = onValueChange,
                    label = { Text("값") },
                    singleLine = true,
                )
            }

            MockCellType.COUNTER -> {
                Text(cell.value, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("다음 번호", color = DDZColor.TextMuted)
                    Text("0013")
                }
            }

            MockCellType.DATE -> {
                Text(cell.value.ifBlank { "2026.10.06" }, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("날짜 형식", color = DDZColor.TextMuted)
                    Text("2026.10.06  ›")
                }
            }

            MockCellType.TIME -> {
                Text(cell.value.ifBlank { "22:04" }, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("시간 형식", color = DDZColor.TextMuted)
                    Text("22:04  ›")
                }
            }

            MockCellType.ROTATING_TEXT -> {
                Text(cell.value.ifBlank { "Draper" }, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("문구 세트", color = DDZColor.TextMuted)
                    Text("품종  ›")
                }
            }
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { showTypePicker = true },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("셀 종류")
                Text(cell.type.label + "  ›")
            }
        }
    }

    if (showTypePicker) {
        ModalBottomSheet(onDismissRequest = { showTypePicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("셀에 무엇을 표시할까요?", fontWeight = FontWeight.Bold)
                MockCellType.entries.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { type ->
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onTypeChange(type)
                                    showTypePicker = false
                                },
                            ) {
                                Text(
                                    if (type == cell.type) type.label + " ✓" else type.label
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun MockLayoutPanel(
    selectedCount: Int,
    rows: Int,
    cols: Int,
    onAddRow: () -> Unit,
    onAddCol: () -> Unit,
    onDeleteSelection: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (selectedCount == 0) "셀을 선택하세요" else selectedCount.toString() + "개 셀 선택됨",
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(modifier = Modifier.weight(1f), onClick = onAddRow) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(" 행")
            }
            OutlinedButton(modifier = Modifier.weight(1f), onClick = onAddCol) {
                Icon(Icons.Filled.GridView, contentDescription = null)
                Text(" 열")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = selectedCount > 1,
                onClick = { },
            ) {
                Text("병합")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                enabled = selectedCount > 0,
                onClick = onDeleteSelection,
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
            }
        }
        Text("현재 " + rows + "행 × " + cols + "열", color = DDZColor.TextMuted)
    }
}

@Composable
private fun MockBottomBar(
    onLayout: () -> Unit,
    onStyle: () -> Unit,
    onSaveRules: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(modifier = Modifier.weight(1f), onClick = onLayout) {
            Icon(Icons.Filled.GridView, contentDescription = null)
            Text(" 레이아웃")
        }
        OutlinedButton(modifier = Modifier.weight(1f), onClick = onStyle) {
            Icon(Icons.Filled.Palette, contentDescription = null)
            Text(" 스타일")
        }
        OutlinedButton(modifier = Modifier.weight(1f), onClick = onSaveRules) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Text(" 저장 규칙")
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MockSaveRulesSheet(
    cells: List<MockCell>,
    rows: Int,
    cols: Int,
    onDismiss: () -> Unit,
) {
    val pathItems = remember {
        mutableStateListOf<MockPathItem?>(
            MockPathItem(MockPathSourceType.CELL, "Draper", cellId = 0),
            MockPathItem(MockPathSourceType.MANUAL, "처리구 A"),
            MockPathItem(MockPathSourceType.DATE, "2026.10.06"),
        )
    }
    var editingStage by remember { mutableStateOf<Int?>(null) }
    var showCellPicker by remember { mutableStateOf(false) }
    var manualDraft by remember { mutableStateOf("") }
    var showManualEditor by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    var includePathInScope by remember { mutableStateOf(true) }
    var includeFilenameInScope by remember { mutableStateOf(true) }

    val pathPreview = pathItems
        .mapNotNull { it?.value?.trim()?.takeIf(String::isNotBlank) }
        .joinToString(" / ")

    fun returnToSummary() {
        editingStage = null
        showCellPicker = false
        showManualEditor = false
        manualDraft = ""
    }

    fun setStage(index: Int, item: MockPathItem) {
        pathItems[index] = item
        returnToSummary()
    }

    fun removeStage(index: Int) {
        val compacted = pathItems.filterNotNull().toMutableList()
        if (index in compacted.indices) compacted.removeAt(index)
        while (compacted.size < 3) compacted += null
        pathItems.clear()
        pathItems.addAll(compacted.take(3))
        returnToSummary()
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when {
                editingStage == null -> {
                    Text("저장 규칙", fontWeight = FontWeight.Bold)

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("파일명", color = DDZColor.TextMuted)
                        Text("Draper_20261006_0012.jpg", fontWeight = FontWeight.Bold)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("저장경로", color = DDZColor.TextMuted)
                        Text(
                            text = if (pathPreview.isBlank()) "경로가 아직 비어 있어요" else pathPreview,
                            fontWeight = FontWeight.Bold,
                        )

                        repeat(3) { index ->
                            val item = pathItems.getOrNull(index)
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    editingStage = index
                                    showCellPicker = false
                                    showManualEditor = false
                                    manualDraft = item?.takeIf { it.sourceType == MockPathSourceType.MANUAL }?.value.orEmpty()
                                },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("${index + 1}단계")
                                    Text(
                                        text = item?.let { pathItemSummary(it) } ?: "+ 추가",
                                        color = if (item == null) DDZColor.TextMuted else DDZColor.TextPrimary,
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (pathPreview.isBlank()) {
                                "Pictures/DZlog/"
                            } else {
                                "Pictures/DZlog/${pathPreview.replace(" / ", "/")}/"
                            },
                            color = DDZColor.TextMuted,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("자동번호", color = DDZColor.TextMuted)
                            Text("다음 번호 0012", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showAdvanced = !showAdvanced },
                    ) {
                        Text(if (showAdvanced) "고급 설정 접기" else "고급 설정")
                    }

                    if (showAdvanced) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("자동번호 범위", color = DDZColor.TextMuted)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("저장경로 변경 시 번호 분리")
                                Switch(
                                    checked = includePathInScope,
                                    onCheckedChange = { includePathInScope = it },
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("파일명 구성 변경 시 번호 분리")
                                Switch(
                                    checked = includeFilenameInScope,
                                    onCheckedChange = { includeFilenameInScope = it },
                                )
                            }
                        }
                    }

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        onClick = onDismiss,
                    ) {
                        Text("적용")
                    }
                }

                showCellPicker -> {
                    val stage = editingStage ?: 0
                    Text("${stage + 1}단계 · 셀에서 가져오기", fontWeight = FontWeight.Bold)
                    Text("사용할 셀을 눌러주세요.", color = DDZColor.TextMuted)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DDZColor.Border, RoundedCornerShape(10.dp))
                    ) {
                        for (row in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                for (col in 0 until cols) {
                                    val index = row * cols + col
                                    val cell = cells.getOrNull(index)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(64.dp)
                                            .border(0.5.dp, DDZColor.Border)
                                            .clickable(enabled = cell != null) {
                                                if (cell != null) {
                                                    setStage(
                                                        stage,
                                                        MockPathItem(
                                                            sourceType = MockPathSourceType.CELL,
                                                            value = cell.value,
                                                            cellId = cell.id,
                                                        )
                                                    )
                                                }
                                            }
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = cell?.value ?: "",
                                            color = if (cell == null) DDZColor.TextMuted else DDZColor.TextPrimary,
                                            maxLines = 2,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showCellPicker = false
                        },
                    ) { Text("다른 방식 선택") }

                    TextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp),
                        onClick = ::returnToSummary,
                    ) { Text("취소") }
                }

                showManualEditor -> {
                    val stage = editingStage ?: 0
                    Text("${stage + 1}단계 · 직접 입력", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = manualDraft,
                        onValueChange = { manualDraft = it },
                        label = { Text("폴더명") },
                        singleLine = true,
                    )
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = manualDraft.trim().isNotBlank(),
                        onClick = {
                            setStage(
                                stage,
                                MockPathItem(
                                    sourceType = MockPathSourceType.MANUAL,
                                    value = manualDraft.trim(),
                                )
                            )
                        },
                    ) { Text("적용") }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showManualEditor = false
                        },
                    ) { Text("다른 방식 선택") }
                    TextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp),
                        onClick = ::returnToSummary,
                    ) { Text("취소") }
                }

                else -> {
                    val stage = editingStage ?: 0
                    Text("${stage + 1}단계 설정", fontWeight = FontWeight.Bold)
                    Text("어떤 값을 폴더명으로 사용할까요?", color = DDZColor.TextMuted)

                    MockPathSourceType.entries.forEach { source ->
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                when (source) {
                                    MockPathSourceType.CELL -> {
                                        showCellPicker = true
                                        showManualEditor = false
                                    }
                                    MockPathSourceType.MANUAL -> {
                                        showManualEditor = true
                                        showCellPicker = false
                                    }
                                    MockPathSourceType.DATE -> setStage(
                                        stage,
                                        MockPathItem(source, "2026.10.06")
                                    )
                                    MockPathSourceType.TIME -> setStage(
                                        stage,
                                        MockPathItem(source, "11.48")
                                    )
                                    MockPathSourceType.ROTATING_TEXT -> setStage(
                                        stage,
                                        MockPathItem(source, "품종")
                                    )
                                }
                            },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(source.label)
                                if (pathItems.getOrNull(stage)?.sourceType == source) {
                                    Text("✓")
                                }
                            }
                        }
                    }

                    if (pathItems.getOrNull(stage) != null) {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { removeStage(stage) },
                        ) { Text("이 단계 삭제") }
                    }

                    TextButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp),
                        onClick = ::returnToSummary,
                    ) { Text("취소") }
                }
            }
        }
    }
}

private fun pathItemSummary(item: MockPathItem): String =
    when (item.sourceType) {
        MockPathSourceType.CELL,
        MockPathSourceType.MANUAL -> item.value
        MockPathSourceType.DATE -> "날짜"
        MockPathSourceType.TIME -> "시간"
        MockPathSourceType.ROTATING_TEXT -> "순환문구"
    }
