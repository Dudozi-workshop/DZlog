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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
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
import com.dudoziworkshop.dzlog.ui.common.DDZBottomNavigation
import com.dudoziworkshop.dzlog.ui.common.DDZBottomNavigationItem
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.common.DDZConfirmDialog
import com.dudoziworkshop.dzlog.ui.common.DDZContentDialog
import com.dudoziworkshop.dzlog.ui.common.DDZTextField
import com.dudoziworkshop.dzlog.ui.common.DDZQuickChoiceDialog
import com.dudoziworkshop.dzlog.ui.common.DDZSettingRow
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.common.DDZTopBarIconButton
import com.dudoziworkshop.dzlog.ui.table.rotating.RotatingPhraseSetEditDialog
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.launch

private enum class MockMode { EDIT, LAYOUT }
internal enum class TableEditorCellType(val label: String) {
    TEXT("텍스트"),
    NUMBER("숫자"),
    COUNTER("자동번호"),
    DATE("날짜"),
    TIME("시간"),
    ROTATING_TEXT("순환문구"),
}

internal data class TableEditorCellUiModel(
    val id: Int,
    val value: String,
    val type: TableEditorCellType = TableEditorCellType.TEXT,
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
fun TableEditorV2Screen(
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
    val saveCoordinator = remember { TableEditorV2SaveCoordinator() }
    val saveScope = rememberCoroutineScope()

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
        containerColor = DDZColor.Background,
        topBar = {
            DDZTopBar(
                title = "표 편집",
                onBack = { requestBack() },
                actions = {
                    DDZTopBarIconButton(
                        icon = Icons.Filled.Undo,
                        contentDescription = "실행 취소",
                        enabled = session.canUndo && !saveCoordinator.isSaving,
                        onClick = {
                            if (session.undo()) {
                                selectionState.clearAll()
                            }
                        },
                    )
                    DDZTopBarIconButton(
                        icon = Icons.Filled.Redo,
                        contentDescription = "다시 실행",
                        enabled = session.canRedo && !saveCoordinator.isSaving,
                        onClick = {
                            if (session.redo()) {
                                selectionState.clearAll()
                            }
                        },
                    )
                    DDZButton(
                        text = if (mode == MockMode.LAYOUT) "완료" else "저장",
                        enabled = !saveCoordinator.isSaving,
                        minHeight = 40.dp,
                        onClick = {
                            if (mode == MockMode.LAYOUT) {
                                mode = MockMode.EDIT
                                selectionState.clearLayoutSelection()
                            } else {
                                saveScope.launch { saveCoordinator.save(session, onSave) }
                            }
                        },
                    )
                },
            )
        },
        bottomBar = {
            MockBottomBar(
                active = when {
                    mode == MockMode.LAYOUT -> MockBottomTab.STRUCTURE
                    showStyle -> MockBottomTab.STYLE
                    showSaveRules -> MockBottomTab.SAVE
                    else -> MockBottomTab.CONTENT
                },
                onContent = {
                    mode = MockMode.EDIT
                    selectionState.clearLayoutSelection()
                    showStyle = false
                    showSaveRules = false
                },
                onLayout = {
                    selectionState.clearEditSelection()
                    showStyle = false
                    showSaveRules = false
                    mode = MockMode.LAYOUT
                },
                onStyle = {
                    mode = MockMode.EDIT
                    selectionState.clearLayoutSelection()
                    styleSheetDraft = draftStyleState
                    showAdvancedStyle = false
                    showSaveRules = false
                    showStyle = true
                },
                onSaveRules = {
                    mode = MockMode.EDIT
                    selectionState.clearLayoutSelection()
                    showStyle = false
                    saveRulesSheetDraft = saveRulesDraft
                    showSaveRules = true
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = if (mode == MockMode.EDIT)
                    "셀을 선택해 내용을 편집하세요."
                else
                    "구조를 바꿀 셀을 선택하세요.",
                color = DDZColor.TextMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
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
                    TableEditorCellUiModelEditor(
                        cell = selected,
                        phraseSets = draftTemplateState.phraseSets,
                        onValueChange = { nextValue ->
                            selected.domainCellId?.let { cellId ->
                                session.commitTemplateChange(
                                    applyTableEditorCellUiModelValue(
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
                                    applyTableEditorCellType(
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
        DDZConfirmDialog(
            title = "셀을 병합할까요?",
            message = "병합하면 왼쪽 위 셀의 값만 유지됩니다.",
            confirmText = "병합",
            onDismiss = { pendingMergeDecision = null },
            onConfirm = {
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
        )
    }

    if (showLayoutDeleteSheet) {
        DDZQuickChoiceDialog(
            onDismiss = { showLayoutDeleteSheet = false },
        ) {
            DDZButton(
                text = "행 삭제",
                leadingIcon = Icons.Filled.ViewStream,
                style = DDZButtonStyle.Destructive,
                modifier = Modifier.weight(1f),
                onClick = {
                    session.commitTemplateChange(
                        TableEditorV2StructureController.removeRows(draftTemplateState, layoutSelection)
                    )
                    selectionState.clearLayoutSelection()
                    showLayoutDeleteSheet = false
                },
            )
            DDZButton(
                text = "열 삭제",
                leadingIcon = Icons.Filled.ViewColumn,
                style = DDZButtonStyle.Destructive,
                modifier = Modifier.weight(1f),
                onClick = {
                    session.commitTemplateChange(
                        TableEditorV2StructureController.removeColumns(draftTemplateState, layoutSelection)
                    )
                    selectionState.clearLayoutSelection()
                    showLayoutDeleteSheet = false
                },
            )
        }
    }

    if (showStyle) {
        DDZBottomSheet(
            title = "표 스타일",
            onDismiss = {
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

                DDZButton(
                    text = if (showAdvancedStyle) "고급 설정 접기" else "더보기",
                    modifier = Modifier.fillMaxWidth(),
                    style = DDZButtonStyle.Secondary,
                    onClick = { showAdvancedStyle = !showAdvancedStyle },
                )

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

                DDZButton(
                    text = "적용",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        session.commitStyleChange(styleSheetDraft)
                        showStyle = false
                    },
                )
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

    if (saveCoordinator.errorMessage != null) {
        DDZConfirmDialog(
            title = "저장 실패",
            message = saveCoordinator.errorMessage.orEmpty(),
            confirmText = "확인",
            dismissText = "닫기",
            onConfirm = { saveCoordinator.clearError() },
            onDismiss = { saveCoordinator.clearError() },
        )
    }

    if (showBackSaveDialog) {
        DDZContentDialog(
            title = "변경사항을 저장할까요?",
            onDismiss = { showBackSaveDialog = false },
            content = {
                Text(
                    text = "저장하지 않으면 이번 편집 내용은 모두 사라집니다.",
                    color = DDZColor.TextSecondary,
                )
            },
            actions = {
                DDZButton(
                    text = "저장 안 함",
                    style = DDZButtonStyle.Text,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        if (isUnsavedNewTemplate) {
                            onDiscardUnsavedNewTemplate()
                        }
                        showBackSaveDialog = false
                        onBack()
                    },
                )
                DDZButton(
                    text = "취소",
                    style = DDZButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = { showBackSaveDialog = false },
                )
                DDZButton(
                    text = "저장",
                    enabled = !saveCoordinator.isSaving,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        saveScope.launch {
                            if (saveCoordinator.save(session, onSave)) {
                                showBackSaveDialog = false
                                onBack()
                            }
                        }
                    },
                )
            },
        )
    }
}

@Composable
private fun ColumnScope.MockTableCanvas(
    cells: List<TableEditorCellUiModel>,
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
    val background = DDZColor.Background
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
private fun TableEditorCellUiModelEditor(
    cell: TableEditorCellUiModel,
    phraseSets: List<com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet>,
    onValueChange: (String) -> Unit,
    onTypeChange: (TableEditorCellType) -> Unit,
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
            .background(DDZColor.Surface)
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
            TableEditorCellType.TEXT,
            TableEditorCellType.NUMBER -> {
                DDZTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = cell.value,
                    onValueChange = onValueChange,
                    label = "값",
                )
            }

            TableEditorCellType.COUNTER -> {
                val currentCounter = cell.value.toIntOrNull()?.coerceAtLeast(0) ?: 1
                Text("자동번호", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DDZButton(
                        text = "−",
                        style = DDZButtonStyle.Secondary,
                        minHeight = 40.dp,
                        onClick = { onValueChange((currentCounter - 1).coerceAtLeast(0).toString()) },
                    )
                    DDZTextField(
                        modifier = Modifier.weight(1f),
                        value = currentCounter.toString(),
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.isNotBlank()) {
                                onValueChange(digits)
                            }
                        },
                        label = "시작 번호",
                    )
                    DDZButton(
                        text = "+",
                        style = DDZButtonStyle.Secondary,
                        minHeight = 40.dp,
                        onClick = { onValueChange((currentCounter + 1).toString()) },
                    )
                }
                Text("촬영 성공 후 다음 번호로 증가합니다.", color = DDZColor.TextMuted)
            }

            TableEditorCellType.DATE -> {
                Text(
                    text = when (cell.formatPattern.ifBlank { "yyyyMMdd" }) {
                        "yyMMdd" -> "261007"
                        "MMdd" -> "1007"
                        else -> "20261007"
                    },
                    fontWeight = FontWeight.Bold,
                )
                DDZSettingRow(
                    label = "날짜 형식",
                    value = cell.formatPattern.ifBlank { "yyyyMMdd" },
                    onClick = { showDatePicker = true },
                )
            }

            TableEditorCellType.TIME -> {
                Text("1251", fontWeight = FontWeight.Bold)
                DDZSettingRow(
                    label = "시간 형식",
                    value = "HHmm · 분 단위 고정",
                    onClick = onApplyTimePolicy,
                )
            }

            TableEditorCellType.ROTATING_TEXT -> {
                val selectedSet = phraseSets.firstOrNull { it.id == cell.phraseSetId }
                Text(
                    selectedSet?.items?.firstOrNull().orEmpty().ifBlank { "문구 세트를 선택하세요" },
                    fontWeight = FontWeight.Bold,
                )
                DDZSettingRow(
                    label = "문구 세트",
                    value = selectedSet?.name ?: "선택 안 함",
                    onClick = { showPhrasePicker = true },
                )
                if (selectedSet != null) {
                    val everyValue = (cell.everyOverride ?: selectedSet.defaultEvery).coerceAtLeast(1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("변경 주기", modifier = Modifier.weight(1f), color = DDZColor.TextMuted)
                        DDZButton(
                            text = "−",
                            style = DDZButtonStyle.Secondary,
                            minHeight = 40.dp,
                            onClick = { onPhraseEveryChange((everyValue - 1).coerceAtLeast(1)) },
                        )
                        Text(everyValue.toString() + "장")
                        DDZButton(
                            text = "+",
                            style = DDZButtonStyle.Secondary,
                            minHeight = 40.dp,
                            onClick = { onPhraseEveryChange(everyValue + 1) },
                        )
                    }
                }
            }
        }

        DDZSettingRow(
            label = "셀 종류",
            value = cell.type.label,
            onClick = { showTypePicker = true },
        )
    }

    if (showTypePicker) {
        DDZBottomSheet(
            title = "셀에 무엇을 표시할까요?",
            onDismiss = { showTypePicker = false },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TableEditorCellType.entries.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { type ->
                            DDZButton(
                                text = if (type == cell.type) type.label + " ✓" else type.label,
                                modifier = Modifier.weight(1f),
                                style = DDZButtonStyle.Secondary,
                                onClick = {
                                    onTypeChange(type)
                                    showTypePicker = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    if (showDatePicker) {
        DDZBottomSheet(
            title = "날짜 형식",
            onDismiss = { showDatePicker = false },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf("yyyyMMdd", "yyMMdd", "MMdd").forEach { pattern ->
                    DDZButton(
                        text = if (cell.formatPattern.ifBlank { "yyyyMMdd" } == pattern) pattern + " ✓" else pattern,
                        modifier = Modifier.fillMaxWidth(),
                        style = DDZButtonStyle.Secondary,
                        onClick = {
                            onDatePatternChange(pattern)
                            showDatePicker = false
                        },
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    if (showPhrasePicker) {
        DDZBottomSheet(
            title = "문구 세트",
            onDismiss = { showPhrasePicker = false },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {

                DDZButton(
                    text = "+ 새 문구 세트",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        createPhraseSetName = ""
                        showCreatePhraseSet = true
                    },
                )

                if (phraseSets.isEmpty()) {
                    Text("등록된 문구 세트가 없습니다.", color = DDZColor.TextMuted)
                } else {
                    phraseSets.forEach { set ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DDZButton(
                                text = set.name + " · " + set.items.size + "개" +
                                    if (cell.phraseSetId == set.id) " ✓" else "",
                                modifier = Modifier.weight(1f),
                                style = DDZButtonStyle.Secondary,
                                onClick = {
                                    onPhraseSetChange(set.id)
                                    showPhrasePicker = false
                                },
                            )
                            DDZButton(
                                text = "편집",
                                style = DDZButtonStyle.Text,
                                minHeight = 40.dp,
                                onClick = {
                                    editingPhraseSetId = set.id
                                    showPhrasePicker = false
                                },
                            )
                        }
                    }

                    DDZButton(
                        text = "선택 해제",
                        modifier = Modifier.fillMaxWidth(),
                        style = DDZButtonStyle.Text,
                        onClick = {
                            onPhraseSetChange(null)
                            showPhrasePicker = false
                        },
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    if (showCreatePhraseSet) {
        DDZContentDialog(
            title = "새 문구 세트",
            onDismiss = { showCreatePhraseSet = false },
            content = {
                DDZTextField(
                    value = createPhraseSetName,
                    onValueChange = { createPhraseSetName = it },
                    label = "세트 이름",
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            actions = {
                DDZButton(
                    text = "취소",
                    style = DDZButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = { showCreatePhraseSet = false },
                )
                DDZButton(
                    text = "추가",
                    enabled = createPhraseSetName.trim().isNotBlank(),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onCreatePhraseSet(createPhraseSetName.trim())
                        showCreatePhraseSet = false
                    },
                )
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
            .background(DDZColor.Surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("구조", fontWeight = FontWeight.Bold, color = DDZColor.TextPrimary)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StructureActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.ViewStream,
                label = "행 추가",
                onClick = onAddRow,
            )
            StructureActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.ViewColumn,
                label = "열 추가",
                onClick = onAddCol,
            )
            StructureActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.GridView,
                label = if (mergedSelection) "병합 해제" else "병합",
                enabled = selectedCount > 1 || mergedSelection,
                onClick = onMergeSelection,
            )
            StructureActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Filled.Delete,
                label = "삭제",
                enabled = selectedCount > 0,
                danger = true,
                onClick = onDeleteSelection,
            )
        }

        Text(
            if (selectedCount == 0) "${rows}행 × ${cols}열 · 셀을 선택하면 병합/삭제가 활성화됩니다."
            else "${selectedCount}개 셀 선택됨",
            color = DDZColor.TextMuted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun StructureActionButton(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = when {
        !enabled -> DDZColor.IconMuted
        danger -> DDZColor.Destructive
        else -> DDZColor.TextPrimary
    }
    Column(
        modifier = modifier
            .background(
                color = if (enabled) DDZColor.Card.copy(alpha = 0.72f) else DDZColor.Card.copy(alpha = 0.36f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = label, tint = tint)
        Text(label, color = tint, fontSize = 12.sp, maxLines = 1)
    }
}

private enum class MockBottomTab { CONTENT, STRUCTURE, STYLE, SAVE }

@Composable
private fun MockBottomBar(
    active: MockBottomTab,
    onContent: () -> Unit,
    onLayout: () -> Unit,
    onStyle: () -> Unit,
    onSaveRules: () -> Unit,
) {
    DDZBottomNavigation(
        items = listOf(
            DDZBottomNavigationItem(
                label = "내용",
                icon = Icons.Filled.GridView,
                selected = active == MockBottomTab.CONTENT,
                onClick = onContent,
            ),
            DDZBottomNavigationItem(
                label = "구조",
                icon = Icons.Filled.ViewStream,
                selected = active == MockBottomTab.STRUCTURE,
                onClick = onLayout,
            ),
            DDZBottomNavigationItem(
                label = "스타일",
                icon = Icons.Filled.Palette,
                selected = active == MockBottomTab.STYLE,
                onClick = onStyle,
            ),
            DDZBottomNavigationItem(
                label = "저장설정",
                icon = Icons.Filled.Save,
                selected = active == MockBottomTab.SAVE,
                onClick = onSaveRules,
            ),
        ),
    )
}

