package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
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
private enum class MockCellType(val label: String) { TEXT("텍스트"), NUMBER("숫자"), COUNTER("자동번호") }

private data class MockCell(
    val id: Int,
    val value: String,
    val type: MockCellType = MockCellType.TEXT,
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
            MockCell(2, "2026.10.06"),
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
    var darkTable by remember { mutableStateOf(false) }
    var gridEnabled by remember { mutableStateOf(true) }
    var fontScale by remember { mutableFloatStateOf(1f) }

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
                        if (mode == MockMode.LAYOUT) "레이아웃 편집" else "표 편집 · V2 목업",
                        fontWeight = FontWeight.Bold,
                    )
                },
                actions = {
                    if (mode == MockMode.LAYOUT) {
                        Button(onClick = {
                            mode = MockMode.EDIT
                            selectedIds = emptySet()
                        }) { Text("완료") }
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
                    onSave = { },
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
                if (selected == null) {
                    MockStartHint()
                } else {
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
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(false to "밝게", true to "어둡게").forEachIndexed { index, item ->
                        SegmentedButton(
                            selected = darkTable == item.first,
                            onClick = { darkTable = item.first },
                            shape = SegmentedButtonDefaults.itemShape(index, 2)
                        ) { Text(item.second) }
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
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    onClick = { showStyle = false },
                ) { Text("적용") }
            }
        }
    }
}

@Composable
private fun MockTableCanvas(
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

@Composable
private fun MockStartHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("표를 눌러 바로 수정", fontWeight = FontWeight.Bold)
        Text("셀을 선택하면 필요한 설정만 아래에 나타납니다.", color = DDZColor.TextMuted)
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("선택한 셀", color = DDZColor.TextMuted)
                Text(cell.value, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onClose) { Text("닫기") }
        }

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = cell.value,
            onValueChange = onValueChange,
            label = { Text("값") },
            singleLine = true,
        )

        Text("셀 종류", color = DDZColor.TextMuted)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            MockCellType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = cell.type == type,
                    onClick = { onTypeChange(type) },
                    shape = SegmentedButtonDefaults.itemShape(index, MockCellType.entries.size),
                ) { Text(type.label) }
            }
        }

        if (cell.type == MockCellType.COUNTER) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("자동 증가")
                Switch(checked = true, onCheckedChange = { })
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
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(modifier = Modifier.weight(1f), onClick = { }) {
            Icon(Icons.Filled.Undo, contentDescription = null)
        }
        OutlinedButton(modifier = Modifier.weight(1f), onClick = onLayout) {
            Icon(Icons.Filled.GridView, contentDescription = null)
            Text(" 레이아웃")
        }
        OutlinedButton(modifier = Modifier.weight(1f), onClick = onStyle) {
            Icon(Icons.Filled.Palette, contentDescription = null)
            Text(" 스타일")
        }
        Button(
            modifier = Modifier.weight(1f),
            onClick = onSave,
            colors = ButtonDefaults.buttonColors(containerColor = DDZColor.Primary),
        ) {
            Icon(Icons.Filled.Save, contentDescription = null)
            Text(" 저장")
        }
    }
}
