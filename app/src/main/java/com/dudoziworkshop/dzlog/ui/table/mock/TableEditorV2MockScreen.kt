package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableUndoManager
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator
import com.dudoziworkshop.dzlog.ui.table.rotating.RotatingPhraseSetEditDialog
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

private enum class MockMode { EDIT, LAYOUT }
private data class MockEditorSnapshot(
    val templateState: TableTemplateState,
    val styleState: TableStyleState,
    val includePathInCounterScope: Boolean,
    val includeFilenameInCounterScope: Boolean,
)
internal enum class MockCellType(val label: String) {
    TEXT("텍스트"),
    NUMBER("숫자"),
    COUNTER("자동번호"),
    DATE("날짜"),
    TIME("시간"),
    ROTATING_TEXT("순환문구"),
}

internal data class MockCell(
    val id: Int,
    val value: String,
    val type: MockCellType = MockCellType.TEXT,
    val domainCellId: String? = null,
    val formatPattern: String = "",
    val phraseSetId: String? = null,
    val everyOverride: Int? = null,
    val rowIndex: Int = 0,
    val colIndex: Int = 0,
    val rowSpan: Int = 1,
    val colSpan: Int = 1,
    val isCovered: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableEditorV2MockScreen(
    templateState: TableTemplateState,
    includePathInCounterScope: Boolean,
    includeFilenameInCounterScope: Boolean,
    styleState: TableStyleState,
    isUnsavedNewTemplate: Boolean = false,
    onSave: (TableTemplateState, TableStyleState, Boolean, Boolean) -> Unit,
    onDiscardUnsavedNewTemplate: () -> Unit = {},
    onBack: () -> Unit,
) {
    var draftTemplateState by remember { mutableStateOf(templateState) }
    var draftStyleState by remember { mutableStateOf(styleState) }

    var savedTemplateBaseline by remember { mutableStateOf(templateState) }
    var savedStyleBaseline by remember { mutableStateOf(styleState) }

    val cells = remember(draftTemplateState.cells, draftTemplateState.rows, draftTemplateState.cols) {
        mutableStateListOf<MockCell>().apply {
            addAll(mockCellsFromTemplate(draftTemplateState))
        }
    }
    var rows by remember(draftTemplateState.rows) { mutableIntStateOf(draftTemplateState.rows) }
    var cols by remember(draftTemplateState.cols) { mutableIntStateOf(draftTemplateState.cols) }
    var selectedId by remember { mutableStateOf<Int?>(null) }
    var layoutSelection by remember { mutableStateOf(MockLayoutSelection()) }
    var showLayoutDeleteSheet by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(MockMode.EDIT) }
    var showStyle by remember { mutableStateOf(false) }
    var showSaveRules by remember { mutableStateOf(false) }
    var saveRulesDraft by remember {
        mutableStateOf(
            mockSaveRulesDraftFromTemplate(
                templateState = templateState,
                includePathInScope = includePathInCounterScope,
                includeFilenameInScope = includeFilenameInCounterScope,
            )
        )
    }
    var savedSaveRulesBaseline by remember { mutableStateOf(saveRulesDraft) }
    var showBackSaveDialog by remember { mutableStateOf(false) }
    var darkTable by remember(draftStyleState.bgStyle) { mutableStateOf(draftStyleState.bgStyle == 0) }
    var transparentTable by remember(draftStyleState.bgStyle) { mutableStateOf(draftStyleState.bgStyle == 2) }
    var gridEnabled by remember(draftStyleState.gridEnabled) { mutableStateOf(draftStyleState.gridEnabled) }
    var fontScale by remember(draftStyleState.valueScale) { mutableFloatStateOf(draftStyleState.valueScale / 100f) }
    var textAlignIndex by remember(draftStyleState.textAlign) { mutableIntStateOf(draftStyleState.textAlign.coerceIn(0, 2)) }
    var bgAlpha by remember(draftStyleState.bgAlpha) { mutableIntStateOf(draftStyleState.bgAlpha) }
    var textColorMode by remember(draftStyleState.textColorMode) { mutableIntStateOf(draftStyleState.textColorMode) }
    var manualTextColor by remember(draftStyleState.manualTextColor) { mutableIntStateOf(draftStyleState.manualTextColor) }
    var showAdvancedStyle by remember { mutableStateOf(false) }
    var layoutBoundaryDragActive by remember { mutableStateOf(false) }
    val undoManager = remember { TableUndoManager<MockEditorSnapshot>() }
    var historyRevision by remember { mutableIntStateOf(0) }

    fun currentSnapshot(): MockEditorSnapshot =
        MockEditorSnapshot(
            templateState = draftTemplateState,
            styleState = draftStyleState,
            includePathInCounterScope = saveRulesDraft.includePathInScope,
            includeFilenameInCounterScope = saveRulesDraft.includeFilenameInScope,
        )

    fun commitTemplateChange(updated: TableTemplateState) {
        if (updated == draftTemplateState) return
        undoManager.pushSnapshotBeforeAction(
            currentSnapshot()
        )
        historyRevision += 1
        draftTemplateState = updated
    }

    fun commitStyleChange(updated: TableStyleState) {
        if (updated == draftStyleState) return
        undoManager.pushSnapshotBeforeAction(
            currentSnapshot()
        )
        historyRevision += 1
        draftStyleState = updated
    }

    fun applyHistorySnapshot(snapshot: MockEditorSnapshot) {
        draftTemplateState = snapshot.templateState
        draftStyleState = snapshot.styleState
        saveRulesDraft = mockSaveRulesDraftFromTemplate(
            templateState = snapshot.templateState,
            includePathInScope = snapshot.includePathInCounterScope,
            includeFilenameInScope = snapshot.includeFilenameInCounterScope,
        )
        historyRevision += 1
    }


    fun saveCurrentSession() {
        val finalTemplate = applyMockSaveRulesDraft(draftTemplateState, saveRulesDraft)
        draftTemplateState = finalTemplate
        onSave(
            finalTemplate,
            draftStyleState,
            saveRulesDraft.includePathInScope,
            saveRulesDraft.includeFilenameInScope,
        )
        savedTemplateBaseline = finalTemplate
        savedStyleBaseline = draftStyleState
        savedSaveRulesBaseline = saveRulesDraft
    }

    fun requestBack() {
        if (mode == MockMode.LAYOUT) {
            mode = MockMode.EDIT
            layoutSelection = MockLayoutSelection()
            return
        }
        val isDirty =
            draftTemplateState != savedTemplateBaseline ||
                draftStyleState != savedStyleBaseline ||
                saveRulesDraft != savedSaveRulesBaseline
        if (isDirty || isUnsavedNewTemplate) {
            showBackSaveDialog = true
        } else {
            onBack()
        }
    }

    BackHandler {
        requestBack()
    }


    Scaffold(
        containerColor = Color(0xFFF7F7FA),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                navigationIcon = {
                    IconButton(onClick = { requestBack() }) {
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
                            layoutSelection = MockLayoutSelection()
                        }) { Text("완료") }
                    } else {
                        IconButton(
                            enabled = undoManager.canUndo() && historyRevision >= 0,
                            onClick = {
                                val current = currentSnapshot()
                                val restored = undoManager.undo(current)
                                if (restored != current) applyHistorySnapshot(restored)
                            },
                        ) {
                            Icon(Icons.Filled.Undo, contentDescription = "실행 취소")
                        }
                        IconButton(
                            enabled = undoManager.canRedo() && historyRevision >= 0,
                            onClick = {
                                val current = currentSnapshot()
                                val restored = undoManager.redo(current)
                                if (restored != current) applyHistorySnapshot(restored)
                            },
                        ) {
                            Icon(Icons.Filled.Redo, contentDescription = "다시 실행")
                        }
                        IconButton(onClick = { saveCurrentSession() }) {
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
                selectedIds = cells
                    .filter { it.domainCellId in layoutSelection.selectedCellIds }
                    .map { it.id }
                    .toSet(),
                darkTable = darkTable,
                transparentTable = transparentTable,
                gridEnabled = gridEnabled,
                bgAlpha = bgAlpha,
                fontScale = fontScale,
                textAlignIndex = textAlignIndex,
                textColorMode = textColorMode,
                manualTextColor = manualTextColor,
                layoutMode = mode == MockMode.LAYOUT,
                rowWeights = draftTemplateState.rowWeights,
                colWeights = draftTemplateState.colWeights,
                onBoundaryDragStart = {
                    if (!layoutBoundaryDragActive) {
                        undoManager.pushSnapshotBeforeAction(
                            currentSnapshot()
                        )
                        historyRevision += 1
                        layoutBoundaryDragActive = true
                    }
                },
                onRowBoundaryDrag = { boundaryIndex, deltaFraction ->
                    draftTemplateState = adjustMockRowBoundary(
                        templateState = draftTemplateState,
                        boundaryIndex = boundaryIndex,
                        deltaFraction = deltaFraction,
                    )
                },
                onColBoundaryDrag = { boundaryIndex, deltaFraction ->
                    draftTemplateState = adjustMockColumnBoundary(
                        templateState = draftTemplateState,
                        boundaryIndex = boundaryIndex,
                        deltaFraction = deltaFraction,
                    )
                },
                onBoundaryDragEnd = {
                    layoutBoundaryDragActive = false
                },
                onCellClick = { id ->
                    if (mode == MockMode.LAYOUT) {
                        val tapped = cells.firstOrNull { it.id == id }
                        val domainCellId = tapped?.domainCellId
                        if (domainCellId != null) {
                            layoutSelection = selectMockLayoutCell(
                                templateState = draftTemplateState,
                                current = layoutSelection,
                                tappedDomainCellId = domainCellId,
                            )
                        }
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
                        phraseSets = draftTemplateState.phraseSets,
                        onValueChange = { nextValue ->
                            val index = cells.indexOfFirst { it.id == selected.id }
                            if (index >= 0) {
                                cells[index] = cells[index].copy(value = nextValue)
                            }
                            selected.domainCellId?.let { cellId ->
                                commitTemplateChange(
                                    applyMockCellValue(
                                        templateState = draftTemplateState,
                                        domainCellId = cellId,
                                        nextValue = nextValue,
                                    )
                                )
                            }
                        },
                        onTypeChange = { nextType ->
                            val index = cells.indexOfFirst { it.id == selected.id }
                            if (index >= 0) {
                                cells[index] = cells[index].copy(type = nextType)
                            }
                            selected.domainCellId?.let { cellId ->
                                commitTemplateChange(
                                    applyMockCellType(
                                        templateState = draftTemplateState,
                                        domainCellId = cellId,
                                        nextType = nextType,
                                    )
                                )
                            }
                        },
                        onDatePatternChange = { pattern ->
                            selected.domainCellId?.let { cellId ->
                                commitTemplateChange(
                                    applyMockDatePattern(
                                        templateState = draftTemplateState,
                                        domainCellId = cellId,
                                        pattern = pattern,
                                    )
                                )
                            }
                        },
                        onApplyTimePolicy = {
                            selected.domainCellId?.let { cellId ->
                                commitTemplateChange(
                                    applyMockTimeFormatPolicy(
                                        templateState = draftTemplateState,
                                        domainCellId = cellId,
                                    )
                                )
                            }
                        },
                        onPhraseSetChange = { phraseSetId ->
                            selected.domainCellId?.let { cellId ->
                                commitTemplateChange(
                                    applyMockPhraseSet(
                                        templateState = draftTemplateState,
                                        domainCellId = cellId,
                                        phraseSetId = phraseSetId,
                                    )
                                )
                            }
                        },
                        onPhraseEveryChange = { every ->
                            selected.domainCellId?.let { cellId ->
                                commitTemplateChange(
                                    applyMockPhraseEvery(
                                        templateState = draftTemplateState,
                                        domainCellId = cellId,
                                        every = every,
                                    )
                                )
                            }
                        },
                        onCreatePhraseSet = { name ->
                            commitTemplateChange(
                                createMockPhraseSet(
                                    templateState = draftTemplateState,
                                    name = name,
                                )
                            )
                        },
                        onUpdatePhraseSet = { phraseSetId, transform ->
                            commitTemplateChange(
                                updateMockPhraseSet(
                                    templateState = draftTemplateState,
                                    phraseSetId = phraseSetId,
                                    transform = transform,
                                )
                            )
                        },
                        onDeletePhraseSet = { phraseSetId ->
                            commitTemplateChange(
                                deleteMockPhraseSet(
                                    templateState = draftTemplateState,
                                    phraseSetId = phraseSetId,
                                )
                            )
                        },
                        onClose = { selectedId = null },
                    )
                }
            } else {
                MockLayoutPanel(
                    selectedCount = layoutSelection.selectedCellIds.size,
                    rows = rows,
                    cols = cols,
                    mergedSelection = isMockLayoutSelectionMerged(draftTemplateState, layoutSelection),
                    onAddRow = {
                        commitTemplateChange(addMockLayoutRow(draftTemplateState, layoutSelection))
                    },
                    onAddCol = {
                        commitTemplateChange(addMockLayoutColumn(draftTemplateState, layoutSelection))
                    },
                    onMergeSelection = {
                        commitTemplateChange(
                            mergeOrUnmergeMockLayoutSelection(
                                templateState = draftTemplateState,
                                selection = layoutSelection,
                            )
                        )
                    },
                    onDeleteSelection = {
                        showLayoutDeleteSheet = true
                    },
                )
            }
        }
    }

    if (showLayoutDeleteSheet) {
        ModalBottomSheet(onDismissRequest = { showLayoutDeleteSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("구조 삭제", fontWeight = FontWeight.Bold)
                Text("선택 영역을 기준으로 삭제할 방향을 고르세요.", color = DDZColor.TextMuted)
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        commitTemplateChange(removeMockLayoutRows(draftTemplateState, layoutSelection))
                        layoutSelection = MockLayoutSelection()
                        showLayoutDeleteSheet = false
                    },
                ) { Text("행 삭제") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        commitTemplateChange(removeMockLayoutColumns(draftTemplateState, layoutSelection))
                        layoutSelection = MockLayoutSelection()
                        showLayoutDeleteSheet = false
                    },
                ) { Text("열 삭제") }
                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 18.dp),
                    onClick = { showLayoutDeleteSheet = false },
                ) { Text("취소") }
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
                        val selected = when (index) {
                            0 -> !darkTable && !transparentTable
                            1 -> darkTable && !transparentTable
                            else -> transparentTable
                        }
                        SegmentedButton(
                            selected = selected,
                            onClick = {
                                when (index) {
                                    0 -> {
                                        darkTable = false
                                        transparentTable = false
                                    }
                                    1 -> {
                                        darkTable = true
                                        transparentTable = false
                                    }
                                    else -> {
                                        transparentTable = true
                                        darkTable = false
                                    }
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, 3)
                        ) { Text(label) }
                    }
                }

                Text("글자 크기  " + (fontScale * 100).toInt() + "%", color = DDZColor.TextMuted)
                Slider(
                    value = fontScale,
                    onValueChange = { fontScale = it },
                    valueRange = 0.6f..1.6f,
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
                    onClick = { showAdvancedStyle = !showAdvancedStyle },
                ) {
                    Text(if (showAdvancedStyle) "고급 설정 접기" else "더보기")
                }

                if (showAdvancedStyle) {
                    Text("배경 투명도  " + ((bgAlpha / 255f) * 100).toInt() + "%", color = DDZColor.TextMuted)
                    Slider(
                        value = bgAlpha.toFloat(),
                        onValueChange = { bgAlpha = it.toInt().coerceIn(0, 255) },
                        valueRange = 0f..255f,
                        enabled = !transparentTable,
                    )

                    Text("글자 색", color = DDZColor.TextMuted)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("자동", "흰색", "검정").forEachIndexed { index, label ->
                            val selected = when (index) {
                                0 -> textColorMode == 0
                                1 -> textColorMode == 1 && manualTextColor == 0
                                else -> textColorMode == 1 && manualTextColor == 1
                            }
                            SegmentedButton(
                                selected = selected,
                                onClick = {
                                    when (index) {
                                        0 -> textColorMode = 0
                                        1 -> {
                                            textColorMode = 1
                                            manualTextColor = 0
                                        }
                                        else -> {
                                            textColorMode = 1
                                            manualTextColor = 1
                                        }
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index, 3)
                            ) { Text(label) }
                        }
                    }
                }

                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    onClick = {
                        commitStyleChange(
                            draftStyleState.copy(
                                bgStyle = when {
                                    transparentTable -> 2
                                    darkTable -> 0
                                    else -> 1
                                },
                                gridEnabled = gridEnabled,
                                valueScale = (fontScale * 100).toInt().coerceIn(60, 160),
                                textAlign = textAlignIndex.coerceIn(0, 2),
                                bgAlpha = bgAlpha.coerceIn(0, 255),
                                textColorMode = textColorMode.coerceIn(0, 1),
                                manualTextColor = manualTextColor.coerceIn(0, 1),
                            )
                        )
                        showStyle = false
                    },
                ) { Text("적용") }
            }
        }
    }

    if (showSaveRules) {
        MockSaveRulesSheet(
            cells = cells,
            rows = rows,
            cols = cols,
            draft = saveRulesDraft,
            onDraftChange = { saveRulesDraft = it },
            onApply = { applied ->
                saveRulesDraft = applied
                commitTemplateChange(applyMockSaveRulesDraft(draftTemplateState, applied))
                showSaveRules = false
            },
            onDismiss = { showSaveRules = false },
        )
    }

    if (showBackSaveDialog) {
        AlertDialog(
            onDismissRequest = { showBackSaveDialog = false },
            title = { Text("변경사항을 저장할까요?") },
            text = { Text("저장하지 않으면 이번 편집 내용은 모두 사라집니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        saveCurrentSession()
                        showBackSaveDialog = false
                        onBack()
                    },
                ) { Text("저장") }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            if (isUnsavedNewTemplate) {
                                onDiscardUnsavedNewTemplate()
                            }
                            showBackSaveDialog = false
                            onBack()
                        },
                    ) { Text("저장 안 함") }
                    TextButton(onClick = { showBackSaveDialog = false }) {
                        Text("취소")
                    }
                }
            },
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
    transparentTable: Boolean,
    gridEnabled: Boolean,
    bgAlpha: Int,
    fontScale: Float,
    textAlignIndex: Int,
    textColorMode: Int,
    manualTextColor: Int,
    layoutMode: Boolean,
    rowWeights: List<Float>?,
    colWeights: List<Float>?,
    onBoundaryDragStart: () -> Unit,
    onRowBoundaryDrag: (Int, Float) -> Unit,
    onColBoundaryDrag: (Int, Float) -> Unit,
    onBoundaryDragEnd: () -> Unit,
    onCellClick: (Int) -> Unit,
) {
    val background = if (darkTable) Color(0xFF1E2220) else Color(0xFFE9EFE7)
    val resolvedAlpha = (bgAlpha.coerceIn(0, 255) / 255f)
    val cellBackground = when {
        transparentTable -> Color.Transparent
        darkTable -> Color(0xFF202522).copy(alpha = resolvedAlpha)
        else -> Color.White.copy(alpha = resolvedAlpha)
    }
    val textColor = when {
        textColorMode == 1 && manualTextColor == 0 -> Color.White
        textColorMode == 1 && manualTextColor == 1 -> Color.Black
        darkTable -> Color.White
        else -> Color(0xFF202124)
    }
    val cellAlignment = when (textAlignIndex.coerceIn(0, 2)) {
        0 -> Alignment.CenterStart
        2 -> Alignment.CenterEnd
        else -> Alignment.Center
    }
    val resolvedRowWeights = TableLayoutCalculator.resolveWeights(rowWeights, rows)
    val resolvedColWeights = TableLayoutCalculator.resolveWeights(colWeights, cols)
    val tableHeight = (74.dp * rows.toFloat()).coerceIn(120.dp, 420.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(background)
            .padding(26.dp),
        contentAlignment = Alignment.Center,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(tableHeight)
                .border(2.dp, DDZColor.Primary, RoundedCornerShape(4.dp))
        ) {
            val density = LocalDensity.current
            val totalWidth = maxWidth
            val totalHeight = maxHeight
            val colWeightSum = resolvedColWeights.sum().coerceAtLeast(0.0001f)
            val rowWeightSum = resolvedRowWeights.sum().coerceAtLeast(0.0001f)
            val colSizes = resolvedColWeights.map { totalWidth * (it / colWeightSum) }
            val rowSizes = resolvedRowWeights.map { totalHeight * (it / rowWeightSum) }
            val totalWidthPx = with(density) { totalWidth.toPx().coerceAtLeast(1f) }
            val totalHeightPx = with(density) { totalHeight.toPx().coerceAtLeast(1f) }

            cells.filterNot { it.isCovered }.forEach { cell ->
                val startRow = cell.rowIndex.coerceIn(0, (rows - 1).coerceAtLeast(0))
                val startCol = cell.colIndex.coerceIn(0, (cols - 1).coerceAtLeast(0))
                val endRowExclusive = (startRow + cell.rowSpan.coerceAtLeast(1)).coerceAtMost(rows)
                val endColExclusive = (startCol + cell.colSpan.coerceAtLeast(1)).coerceAtMost(cols)
                val x = colSizes.take(startCol).fold(0.dp) { acc, value -> acc + value }
                val y = rowSizes.take(startRow).fold(0.dp) { acc, value -> acc + value }
                val width = colSizes.subList(startCol, endColExclusive).fold(0.dp) { acc, value -> acc + value }
                val height = rowSizes.subList(startRow, endRowExclusive).fold(0.dp) { acc, value -> acc + value }
                val isSelected = if (layoutMode) cell.id in selectedIds else selectedId == cell.id

                Box(
                    modifier = Modifier
                        .offset(x = x, y = y)
                        .size(width = width, height = height)
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
                    contentAlignment = cellAlignment,
                ) {
                    Text(
                        cell.value,
                        color = if (isSelected) DDZColor.PrimaryDark else textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = (14f * fontScale.coerceIn(0.6f, 1.6f)).sp,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }

            if (layoutMode) {
                for (boundaryIndex in 0 until (cols - 1).coerceAtLeast(0)) {
                    val x = colSizes.take(boundaryIndex + 1).fold(0.dp) { acc, value -> acc + value }
                    Box(
                        modifier = Modifier
                            .offset(x = x - 10.dp)
                            .width(20.dp)
                            .fillMaxHeight()
                            .pointerInput(boundaryIndex, totalWidthPx) {
                                detectDragGestures(
                                    onDragStart = { onBoundaryDragStart() },
                                    onDragEnd = onBoundaryDragEnd,
                                    onDragCancel = onBoundaryDragEnd,
                                ) { change, dragAmount ->
                                    change.consume()
                                    onColBoundaryDrag(boundaryIndex, dragAmount.x / totalWidthPx)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(DDZColor.Primary.copy(alpha = 0.45f))
                        )
                    }
                }

                for (boundaryIndex in 0 until (rows - 1).coerceAtLeast(0)) {
                    val y = rowSizes.take(boundaryIndex + 1).fold(0.dp) { acc, value -> acc + value }
                    Box(
                        modifier = Modifier
                            .offset(y = y - 10.dp)
                            .height(20.dp)
                            .fillMaxWidth()
                            .pointerInput(boundaryIndex, totalHeightPx) {
                                detectDragGestures(
                                    onDragStart = { onBoundaryDragStart() },
                                    onDragEnd = onBoundaryDragEnd,
                                    onDragCancel = onBoundaryDragEnd,
                                ) { change, dragAmount ->
                                    change.consume()
                                    onRowBoundaryDrag(boundaryIndex, dragAmount.y / totalHeightPx)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier
                                .height(2.dp)
                                .fillMaxWidth()
                                .background(DDZColor.Primary.copy(alpha = 0.45f))
                        )
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
    phraseSets: List<com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet>,
    onValueChange: (String) -> Unit,
    onTypeChange: (MockCellType) -> Unit,
    onDatePatternChange: (String) -> Unit,
    onApplyTimePolicy: () -> Unit,
    onPhraseSetChange: (String?) -> Unit,
    onPhraseEveryChange: (Int) -> Unit,
    onCreatePhraseSet: (String) -> Unit,
    onUpdatePhraseSet: (String, (com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet) -> com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet) -> Unit,
    onDeletePhraseSet: (String) -> Unit,
    onClose: () -> Unit,
) {
    var showTypePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPhrasePicker by remember { mutableStateOf(false) }
    var showCreatePhraseSet by remember { mutableStateOf(false) }
    var createPhraseSetName by remember { mutableStateOf("") }
    var editingPhraseSetId by remember { mutableStateOf<String?>(null) }

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
                val currentCounter = cell.value.toIntOrNull()?.coerceAtLeast(0) ?: 1
                Text("자동번호", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = { onValueChange((currentCounter - 1).coerceAtLeast(0).toString()) },
                    ) {
                        Text("−")
                    }
                    OutlinedTextField(
                        modifier = Modifier.weight(1f),
                        value = currentCounter.toString(),
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.isNotBlank()) {
                                onValueChange(digits)
                            }
                        },
                        label = { Text("시작 번호") },
                        singleLine = true,
                    )
                    OutlinedButton(
                        onClick = { onValueChange((currentCounter + 1).toString()) },
                    ) {
                        Text("+")
                    }
                }
                Text("촬영 성공 후 다음 번호로 증가합니다.", color = DDZColor.TextMuted)
            }

            MockCellType.DATE -> {
                Text(
                    text = when (cell.formatPattern.ifBlank { "yyyyMMdd" }) {
                        "yyMMdd" -> "261007"
                        "MMdd" -> "1007"
                        else -> "20261007"
                    },
                    fontWeight = FontWeight.Bold,
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDatePicker = true },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("날짜 형식")
                        Text((cell.formatPattern.ifBlank { "yyyyMMdd" }) + "  ›")
                    }
                }
            }

            MockCellType.TIME -> {
                Text("1251", fontWeight = FontWeight.Bold)
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onApplyTimePolicy,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("시간 형식")
                        Text("HHmm · 분 단위 고정")
                    }
                }
            }

            MockCellType.ROTATING_TEXT -> {
                val selectedSet = phraseSets.firstOrNull { it.id == cell.phraseSetId }
                Text(
                    selectedSet?.items?.firstOrNull().orEmpty().ifBlank { "문구 세트를 선택하세요" },
                    fontWeight = FontWeight.Bold,
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showPhrasePicker = true },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("문구 세트")
                        Text((selectedSet?.name ?: "선택 안 함") + "  ›")
                    }
                }
                if (selectedSet != null) {
                    val everyValue = (cell.everyOverride ?: selectedSet.defaultEvery).coerceAtLeast(1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("변경 주기", modifier = Modifier.weight(1f), color = DDZColor.TextMuted)
                        OutlinedButton(onClick = { onPhraseEveryChange((everyValue - 1).coerceAtLeast(1)) }) {
                            Text("−")
                        }
                        Text(everyValue.toString() + "장")
                        OutlinedButton(onClick = { onPhraseEveryChange(everyValue + 1) }) {
                            Text("+")
                        }
                    }
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
                                Text(if (type == cell.type) type.label + " ✓" else type.label)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    if (showDatePicker) {
        ModalBottomSheet(onDismissRequest = { showDatePicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("날짜 형식", fontWeight = FontWeight.Bold)
                listOf("yyyyMMdd", "yyMMdd", "MMdd").forEach { pattern ->
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onDatePatternChange(pattern)
                            showDatePicker = false
                        },
                    ) {
                        Text(
                            if (cell.formatPattern.ifBlank { "yyyyMMdd" } == pattern) pattern + " ✓" else pattern
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    if (showPhrasePicker) {
        ModalBottomSheet(onDismissRequest = { showPhrasePicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("문구 세트", fontWeight = FontWeight.Bold)

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        createPhraseSetName = ""
                        showCreatePhraseSet = true
                    },
                ) {
                    Text("+ 새 문구 세트")
                }

                if (phraseSets.isEmpty()) {
                    Text("등록된 문구 세트가 없습니다.", color = DDZColor.TextMuted)
                } else {
                    phraseSets.forEach { set ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onPhraseSetChange(set.id)
                                    showPhrasePicker = false
                                },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(set.name)
                                    Text(
                                        if (cell.phraseSetId == set.id) set.items.size.toString() + "개 ✓"
                                        else set.items.size.toString() + "개"
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    editingPhraseSetId = set.id
                                    showPhrasePicker = false
                                },
                            ) {
                                Text("편집")
                            }
                        }
                    }

                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onPhraseSetChange(null)
                            showPhrasePicker = false
                        },
                    ) {
                        Text("선택 해제")
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    if (showCreatePhraseSet) {
        AlertDialog(
            onDismissRequest = { showCreatePhraseSet = false },
            title = { Text("새 문구 세트") },
            text = {
                OutlinedTextField(
                    value = createPhraseSetName,
                    onValueChange = { createPhraseSetName = it },
                    singleLine = true,
                    label = { Text("세트 이름") },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = createPhraseSetName.trim().isNotBlank(),
                    onClick = {
                        onCreatePhraseSet(createPhraseSetName.trim())
                        showCreatePhraseSet = false
                    },
                ) {
                    Text("추가")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePhraseSet = false }) {
                    Text("취소")
                }
            },
        )
    }

    editingPhraseSetId
        ?.let { id -> phraseSets.firstOrNull { it.id == id } }
        ?.let { editingSet ->
            RotatingPhraseSetEditDialog(
                phraseSet = editingSet,
                onClose = { editingPhraseSetId = null },
                onUpdateSet = { transform ->
                    onUpdatePhraseSet(editingSet.id, transform)
                },
                onDeleteSet = { phraseSetId ->
                    onDeletePhraseSet(phraseSetId)
                    editingPhraseSetId = null
                },
            )
        }

}

@Composable
private fun MockLayoutPanel(
    selectedCount: Int,
    rows: Int,
    cols: Int,
    mergedSelection: Boolean,
    onAddRow: () -> Unit,
    onAddCol: () -> Unit,
    onMergeSelection: () -> Unit,
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
                enabled = selectedCount > 1 || mergedSelection,
                onClick = onMergeSelection,
            ) {
                Text(if (mergedSelection) "병합 해제" else "병합")
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
