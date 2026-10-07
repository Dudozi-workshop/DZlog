package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableMergeDecision
import com.dudoziworkshop.dzlog.feature.table.editor.TableMergeDecisionType
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator
import com.dudoziworkshop.dzlog.ui.table.rotating.RotatingPhraseSetEditDialog
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.launch

private enum class MockMode { EDIT, LAYOUT }
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
    onSave: suspend (TableTemplateState, TableStyleState, Boolean, Boolean) -> Boolean,
    onDiscardUnsavedNewTemplate: () -> Unit = {},
    onBack: () -> Unit,
) {
    val session = remember {
        TableEditorV2SessionState(
            initialTemplateState = templateState,
            initialStyleState = styleState,
            includePathInCounterScope = includePathInCounterScope,
            includeFilenameInCounterScope = includeFilenameInCounterScope,
        )
    }
    val draftTemplateState = session.draftTemplateState
    val draftStyleState = session.draftStyleState
    val saveRulesDraft = session.saveRulesDraft

    val cells = remember(draftTemplateState) {
        mockCellsFromTemplate(draftTemplateState)
    }
    val rows = draftTemplateState.rows
    val cols = draftTemplateState.cols
    val selectionState = remember { TableEditorV2SelectionState() }
    val selectedCellId = selectionState.selectedCellId
    val layoutSelection = selectionState.layoutSelection
    var showLayoutDeleteSheet by remember { mutableStateOf(false) }
    var pendingMergeDecision by remember { mutableStateOf<TableMergeDecision?>(null) }
    var mode by remember { mutableStateOf(MockMode.EDIT) }
    var showStyle by remember { mutableStateOf(false) }
    var showSaveRules by remember { mutableStateOf(false) }
    var saveRulesSheetDraft by remember { mutableStateOf<MockSaveRulesDraft?>(null) }
    var showBackSaveDialog by remember { mutableStateOf(false) }
    var styleSheetDraft by remember { mutableStateOf(draftStyleState) }
    var showAdvancedStyle by remember { mutableStateOf(false) }
    var layoutBoundaryDragActive by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveErrorMessage by remember { mutableStateOf<String?>(null) }
    val saveScope = rememberCoroutineScope()

    suspend fun saveCurrentSession(): Boolean {
        if (isSaving) return false
        isSaving = true
        saveErrorMessage = null
        val finalTemplate = session.finalTemplateForSave()
        val success = runCatching {
            onSave(
                finalTemplate,
                draftStyleState,
                saveRulesDraft.includePathInScope,
                saveRulesDraft.includeFilenameInScope,
            )
        }.getOrDefault(false)
        if (success) {
            session.markSaved(finalTemplate)
        } else {
            saveErrorMessage = "저장하지 못했습니다. 변경사항은 유지됩니다."
        }
        isSaving = false
        return success
    }

    fun requestBack() {
        if (mode == MockMode.LAYOUT) {
            mode = MockMode.EDIT
            selectionState.clearLayoutSelection()
            return
        }
        if (session.isDirty || isUnsavedNewTemplate) {
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
                            selectionState.clearLayoutSelection()
                        }) { Text("완료") }
                    } else {
                        IconButton(
                            enabled = session.canUndo && session.historyRevision >= 0,
                            onClick = {
                                if (session.undo()) {
                                    selectionState.clearEditSelection()
                                    selectionState.clearLayoutSelection()
                                }
                            },
                        ) {
                            Icon(Icons.Filled.Undo, contentDescription = "실행 취소")
                        }
                        IconButton(
                            enabled = session.canRedo && session.historyRevision >= 0,
                            onClick = {
                                if (session.redo()) {
                                    selectionState.clearEditSelection()
                                    selectionState.clearLayoutSelection()
                                }
                            },
                        ) {
                            Icon(Icons.Filled.Redo, contentDescription = "다시 실행")
                        }
                        IconButton(
                            enabled = !isSaving,
                            onClick = {
                                saveScope.launch { saveCurrentSession() }
                            },
                        ) {
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
                        selectionState.clearEditSelection()
                        mode = MockMode.LAYOUT
                    },
                    onStyle = {
                        styleSheetDraft = draftStyleState
                        showAdvancedStyle = false
                        showStyle = true
                    },
                    onSaveRules = {
                        saveRulesSheetDraft = saveRulesDraft
                        showSaveRules = true
                    },
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
                selectedCellId = selectedCellId,
                selectedIds = cells
                    .filter { it.domainCellId in layoutSelection.selectedCellIds }
                    .map { it.id }
                    .toSet(),
                darkTable = (if (showStyle) styleSheetDraft else draftStyleState).bgStyle == 0,
                transparentTable = (if (showStyle) styleSheetDraft else draftStyleState).bgStyle == 2,
                gridEnabled = (if (showStyle) styleSheetDraft else draftStyleState).gridEnabled,
                bgAlpha = (if (showStyle) styleSheetDraft else draftStyleState).bgAlpha,
                fontScale = (if (showStyle) styleSheetDraft else draftStyleState).valueScale / 100f,
                textAlignIndex = (if (showStyle) styleSheetDraft else draftStyleState).textAlign,
                textColorMode = (if (showStyle) styleSheetDraft else draftStyleState).textColorMode,
                manualTextColor = (if (showStyle) styleSheetDraft else draftStyleState).manualTextColor,
                layoutMode = mode == MockMode.LAYOUT,
                rowWeights = draftTemplateState.rowWeights,
                colWeights = draftTemplateState.colWeights,
                onBoundaryDragStart = {
                    if (!layoutBoundaryDragActive) {
                        session.beginContinuousTemplateChange()
                        layoutBoundaryDragActive = true
                    }
                },
                onRowBoundaryDrag = { boundaryIndex, deltaFraction ->
                    session.replaceTemplateDraftWithoutHistory(
                        adjustMockRowBoundary(
                            templateState = draftTemplateState,
                            boundaryIndex = boundaryIndex,
                            deltaFraction = deltaFraction,
                        )
                    )
                },
                onColBoundaryDrag = { boundaryIndex, deltaFraction ->
                    session.replaceTemplateDraftWithoutHistory(
                        adjustMockColumnBoundary(
                            templateState = draftTemplateState,
                            boundaryIndex = boundaryIndex,
                            deltaFraction = deltaFraction,
                        )
                    )
                },
                onBoundaryDragEnd = {
                    layoutBoundaryDragActive = false
                },
                onCellClick = { domainCellId ->
                    if (mode == MockMode.LAYOUT) {
                        selectionState.selectLayoutCell(
                            templateState = draftTemplateState,
                            tappedDomainCellId = domainCellId,
                        )
                    } else {
                        selectionState.selectEditCell(domainCellId)
                    }
                },
                onCellRangeDrag = { startId, endId ->
                    selectionState.selectLayoutRange(
                        templateState = draftTemplateState,
                        startDomainCellId = startId,
                        endDomainCellId = endId,
                    )
                },
                onClearLayoutSelection = {
                    selectionState.clearLayoutSelection()
                },
            )

            Spacer(Modifier.height(12.dp))

            if (mode == MockMode.EDIT) {
                val selected = selectedCellId?.let { id ->
                    cells.firstOrNull { it.domainCellId == id }
                }
                if (selected != null) {
                    MockCellEditor(
                        cell = selected,
                        phraseSets = draftTemplateState.phraseSets,
                        onValueChange = { nextValue ->
                            selected.domainCellId?.let { cellId ->
                                session.commitTemplateChange(
                                    applyMockCellValue(
                                        templateState = draftTemplateState,
                                        domainCellId = cellId,
                                        nextValue = nextValue,
                                    )
                                )
                            }
                        },
                        onTypeChange = { nextType ->
                            selected.domainCellId?.let { cellId ->
                                session.commitTemplateChange(
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
                                session.commitTemplateChange(
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
                                session.commitTemplateChange(
                                    applyMockTimeFormatPolicy(
                                        templateState = draftTemplateState,
                                        domainCellId = cellId,
                                    )
                                )
                            }
                        },
                        onPhraseSetChange = { phraseSetId ->
                            selected.domainCellId?.let { cellId ->
                                session.commitTemplateChange(
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
                                session.commitTemplateChange(
                                    applyMockPhraseEvery(
                                        templateState = draftTemplateState,
                                        domainCellId = cellId,
                                        every = every,
                                    )
                                )
                            }
                        },
                        onCreatePhraseSet = { name ->
                            session.commitTemplateChange(
                                createMockPhraseSet(
                                    templateState = draftTemplateState,
                                    name = name,
                                )
                            )
                        },
                        onUpdatePhraseSet = { phraseSetId, transform ->
                            session.commitTemplateChange(
                                updateMockPhraseSet(
                                    templateState = draftTemplateState,
                                    phraseSetId = phraseSetId,
                                    transform = transform,
                                )
                            )
                        },
                        onDeletePhraseSet = { phraseSetId ->
                            session.commitTemplateChange(
                                deleteMockPhraseSet(
                                    templateState = draftTemplateState,
                                    phraseSetId = phraseSetId,
                                )
                            )
                        },
                        onClose = { selectionState.clearEditSelection() },
                    )
                }
            } else {
                MockLayoutPanel(
                    selectedCount = layoutSelection.selectedCellIds.size,
                    rows = rows,
                    cols = cols,
                    mergedSelection = isMockLayoutSelectionMerged(draftTemplateState, layoutSelection),
                    onAddRow = {
                        session.commitTemplateChange(
                            TableEditorV2StructureController.addRow(draftTemplateState, layoutSelection)
                        )
                        selectionState.clearLayoutSelection()
                    },
                    onAddCol = {
                        session.commitTemplateChange(
                            TableEditorV2StructureController.addColumn(draftTemplateState, layoutSelection)
                        )
                        selectionState.clearLayoutSelection()
                    },
                    onMergeSelection = {
                        val decision = TableEditorV2StructureController.resolveMergeDecision(
                            templateState = draftTemplateState,
                            selection = layoutSelection,
                        )
                        when (decision.type) {
                            TableMergeDecisionType.CONFIRM_MERGE -> {
                                pendingMergeDecision = decision
                            }
                            TableMergeDecisionType.MERGE,
                            TableMergeDecisionType.UNMERGE -> {
                                val updated = TableEditorV2StructureController.applyMergeDecision(draftTemplateState, decision)
                                session.commitTemplateChange(updated)
                                selectionState.normalizeLayoutSelection(
                                    templateState = updated,
                                    range = decision.range,
                                    collapseToTopLeft = decision.type == TableMergeDecisionType.UNMERGE,
                                )
                            }
                            TableMergeDecisionType.NONE -> Unit
                        }
                    },
                    onDeleteSelection = {
                        showLayoutDeleteSheet = true
                    },
                )
            }
        }
    }

    if (pendingMergeDecision != null) {
        AlertDialog(
            onDismissRequest = { pendingMergeDecision = null },
            title = { Text("셀을 병합할까요?") },
            text = { Text("병합하면 왼쪽 위 셀의 값만 유지됩니다.") },
            dismissButton = {
                TextButton(onClick = { pendingMergeDecision = null }) {
                    Text("취소")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingMergeDecision?.let { decision ->
                            val updated = TableEditorV2StructureController.applyMergeDecision(draftTemplateState, decision)
                            session.commitTemplateChange(updated)
                            selectionState.normalizeLayoutSelection(
                                templateState = updated,
                                range = decision.range,
                                collapseToTopLeft = false,
                            )
                        }
                        pendingMergeDecision = null
                    },
                ) { Text("병합") }
            },
        )
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
                        session.commitTemplateChange(
                            TableEditorV2StructureController.removeRows(draftTemplateState, layoutSelection)
                        )
                        selectionState.clearLayoutSelection()
                        showLayoutDeleteSheet = false
                    },
                ) { Text("행 삭제") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        session.commitTemplateChange(
                            TableEditorV2StructureController.removeColumns(draftTemplateState, layoutSelection)
                        )
                        selectionState.clearLayoutSelection()
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
        ModalBottomSheet(
            onDismissRequest = {
                styleSheetDraft = draftStyleState
                showStyle = false
            },
        ) {
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
                            0 -> styleSheetDraft.bgStyle == 1
                            1 -> styleSheetDraft.bgStyle == 0
                            else -> styleSheetDraft.bgStyle == 2
                        }
                        SegmentedButton(
                            selected = selected,
                            onClick = {
                                styleSheetDraft = styleSheetDraft.copy(
                                    bgStyle = when (index) {
                                        0 -> 1
                                        1 -> 0
                                        else -> 2
                                    }
                                )
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, 3)
                        ) { Text(label) }
                    }
                }

                Text("글자 크기  " + styleSheetDraft.valueScale + "%", color = DDZColor.TextMuted)
                Slider(
                    value = styleSheetDraft.valueScale / 100f,
                    onValueChange = { value ->
                        styleSheetDraft = styleSheetDraft.copy(
                            valueScale = (value * 100).toInt().coerceIn(60, 160)
                        )
                    },
                    valueRange = 0.6f..1.6f,
                )

                Text("정렬", color = DDZColor.TextMuted)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("왼쪽", "가운데", "오른쪽").forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = styleSheetDraft.textAlign == index,
                            onClick = { styleSheetDraft = styleSheetDraft.copy(textAlign = index) },
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
                    Switch(
                        checked = styleSheetDraft.gridEnabled,
                        onCheckedChange = { styleSheetDraft = styleSheetDraft.copy(gridEnabled = it) },
                    )
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showAdvancedStyle = !showAdvancedStyle },
                ) {
                    Text(if (showAdvancedStyle) "고급 설정 접기" else "더보기")
                }

                if (showAdvancedStyle) {
                    Text(
                        "배경 투명도  " + ((styleSheetDraft.bgAlpha / 255f) * 100).toInt() + "%",
                        color = DDZColor.TextMuted,
                    )
                    Slider(
                        value = styleSheetDraft.bgAlpha.toFloat(),
                        onValueChange = { value ->
                            styleSheetDraft = styleSheetDraft.copy(
                                bgAlpha = value.toInt().coerceIn(0, 255)
                            )
                        },
                        valueRange = 0f..255f,
                        enabled = styleSheetDraft.bgStyle != 2,
                    )

                    Text("글자 색", color = DDZColor.TextMuted)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf("자동", "흰색", "검정").forEachIndexed { index, label ->
                            val selected = when (index) {
                                0 -> styleSheetDraft.textColorMode == 0
                                1 -> styleSheetDraft.textColorMode == 1 && styleSheetDraft.manualTextColor == 0
                                else -> styleSheetDraft.textColorMode == 1 && styleSheetDraft.manualTextColor == 1
                            }
                            SegmentedButton(
                                selected = selected,
                                onClick = {
                                    styleSheetDraft = when (index) {
                                        0 -> styleSheetDraft.copy(textColorMode = 0)
                                        1 -> styleSheetDraft.copy(
                                            textColorMode = 1,
                                            manualTextColor = 0,
                                        )
                                        else -> styleSheetDraft.copy(
                                            textColorMode = 1,
                                            manualTextColor = 1,
                                        )
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
                        session.commitStyleChange(styleSheetDraft)
                        showStyle = false
                    },
                ) { Text("적용") }
            }
        }
    }

    if (showSaveRules) {
        val sheetDraft = saveRulesSheetDraft ?: saveRulesDraft
        MockSaveRulesSheet(
            cells = cells,
            rows = rows,
            cols = cols,
            draft = sheetDraft,
            onDraftChange = { saveRulesSheetDraft = it },
            onApply = { applied ->
                session.commitSaveRulesChange(applied)
                saveRulesSheetDraft = null
                showSaveRules = false
            },
            onDismiss = {
                saveRulesSheetDraft = null
                showSaveRules = false
            },
        )
    }

    if (saveErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { saveErrorMessage = null },
            title = { Text("저장 실패") },
            text = { Text(saveErrorMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { saveErrorMessage = null }) {
                    Text("확인")
                }
            },
        )
    }

    if (showBackSaveDialog) {
        AlertDialog(
            onDismissRequest = { showBackSaveDialog = false },
            title = { Text("변경사항을 저장할까요?") },
            text = { Text("저장하지 않으면 이번 편집 내용은 모두 사라집니다.") },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        saveScope.launch {
                            if (saveCurrentSession()) {
                                showBackSaveDialog = false
                                onBack()
                            }
                        }
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
    selectedCellId: String?,
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
    onCellClick: (String) -> Unit,
    onCellRangeDrag: (String, String) -> Unit,
    onClearLayoutSelection: () -> Unit,
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
    val baseTableHeight = (74.dp * rows.toFloat()).coerceIn(120.dp, 420.dp)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        val previousWorkingWidth = (maxWidth - 52.dp).coerceAtLeast(1.dp)
        val expandedWorkingWidth = (maxWidth - 16.dp).coerceAtLeast(1.dp)
        val editorScale = (expandedWorkingWidth / previousWorkingWidth).coerceAtLeast(1f)
        val stableTableHeight = (baseTableHeight * editorScale)
            .coerceAtMost((maxHeight - 16.dp).coerceAtLeast(120.dp))

        BoxWithConstraints(
            modifier = Modifier
                .width(expandedWorkingWidth)
                .height(stableTableHeight)
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
            val rootHitRects = cells.filterNot { it.isCovered }.map { cell ->
                val startRow = cell.rowIndex.coerceIn(0, (rows - 1).coerceAtLeast(0))
                val startCol = cell.colIndex.coerceIn(0, (cols - 1).coerceAtLeast(0))
                val endRowExclusive = (startRow + cell.rowSpan.coerceAtLeast(1)).coerceAtMost(rows)
                val endColExclusive = (startCol + cell.colSpan.coerceAtLeast(1)).coerceAtMost(cols)
                val x = colSizes.take(startCol).fold(0.dp) { acc, value -> acc + value }
                val y = rowSizes.take(startRow).fold(0.dp) { acc, value -> acc + value }
                val width = colSizes.subList(startCol, endColExclusive).fold(0.dp) { acc, value -> acc + value }
                val height = rowSizes.subList(startRow, endRowExclusive).fold(0.dp) { acc, value -> acc + value }
                cell to Rect(
                    left = with(density) { x.toPx() },
                    top = with(density) { y.toPx() },
                    right = with(density) { (x + width).toPx() },
                    bottom = with(density) { (y + height).toPx() },
                )
            }

            cells.filterNot { it.isCovered }.forEach { cell ->
                val startRow = cell.rowIndex.coerceIn(0, (rows - 1).coerceAtLeast(0))
                val startCol = cell.colIndex.coerceIn(0, (cols - 1).coerceAtLeast(0))
                val endRowExclusive = (startRow + cell.rowSpan.coerceAtLeast(1)).coerceAtMost(rows)
                val endColExclusive = (startCol + cell.colSpan.coerceAtLeast(1)).coerceAtMost(cols)
                val x = colSizes.take(startCol).fold(0.dp) { acc, value -> acc + value }
                val y = rowSizes.take(startRow).fold(0.dp) { acc, value -> acc + value }
                val width = colSizes.subList(startCol, endColExclusive).fold(0.dp) { acc, value -> acc + value }
                val height = rowSizes.subList(startRow, endRowExclusive).fold(0.dp) { acc, value -> acc + value }
                val isSelected = if (layoutMode) {
                    cell.id in selectedIds
                } else {
                    selectedCellId != null && cell.domainCellId == selectedCellId
                }

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
                        .clickable(enabled = !layoutMode && cell.domainCellId != null) {
                            cell.domainCellId?.let(onCellClick)
                        },
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
                fun hitDomainCellId(position: Offset): String? =
                    rootHitRects.firstOrNull { (_, rect) -> rect.contains(position) }
                        ?.first
                        ?.domainCellId

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(rootHitRects) {
                            detectTapGestures { position ->
                                val domainId = hitDomainCellId(position)
                                if (domainId == null) {
                                    onClearLayoutSelection()
                                } else {
                                    onCellClick(domainId)
                                }
                            }
                        }
                        .pointerInput(rootHitRects) {
                            var startDomainId: String? = null
                            detectDragGestures(
                                onDragStart = { position ->
                                    startDomainId = hitDomainCellId(position)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val startId = startDomainId ?: return@detectDragGestures
                                    val endId = hitDomainCellId(change.position) ?: return@detectDragGestures
                                    onCellRangeDrag(startId, endId)
                                },
                                onDragEnd = { startDomainId = null },
                                onDragCancel = { startDomainId = null },
                            )
                        }
                )

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
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(18.dp)
                                .background(DDZColor.Primary.copy(alpha = 0.65f))
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
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Box(
                            Modifier
                                .height(3.dp)
                                .width(18.dp)
                                .background(DDZColor.Primary.copy(alpha = 0.65f))
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
