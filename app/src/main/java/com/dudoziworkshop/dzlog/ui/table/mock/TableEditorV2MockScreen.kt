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
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableMergeDecision
import com.dudoziworkshop.dzlog.feature.table.editor.TableMergeDecisionType
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.common.DDZConfirmDialog
import com.dudoziworkshop.dzlog.ui.common.DDZContentDialog
import com.dudoziworkshop.dzlog.ui.common.DDZQuickChoiceDialog
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.common.DDZTopBarIconButton
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
    saveMode: SaveMode,
    counterPadding: Int,
    styleState: TableStyleState,
    isUnsavedNewTemplate: Boolean = false,
    openSaveSettingsInitially: Boolean = false,
    onSave: suspend (
        TableTemplateState,
        TableStyleState,
        Boolean,
        Boolean,
        SaveMode,
        Int,
        Int?,
        Boolean,
    ) -> Boolean,
    onDiscardUnsavedNewTemplate: () -> Unit = {},
    onBack: () -> Unit,
) {
    val session = remember {
        TableEditorV2SessionState(
            initialTemplateState = templateState,
            initialStyleState = styleState,
            includePathInCounterScope = includePathInCounterScope,
            includeFilenameInCounterScope = includeFilenameInCounterScope,
            initialSaveMode = saveMode,
            initialCounterPadding = counterPadding,
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
    var showSaveRules by remember { mutableStateOf(openSaveSettingsInitially) }
    var saveRulesSheetDraft by remember {
        mutableStateOf<MockSaveRulesDraft?>(
            if (openSaveSettingsInitially) saveRulesDraft else null,
        )
    }
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
        TableEditorStyleSheet(
            draft = styleSheetDraft,
            showAdvanced = showAdvancedStyle,
            onDraftChange = { styleSheetDraft = it },
            onAdvancedChange = { showAdvancedStyle = it },
            onApply = { applied ->
                session.commitStyleChange(applied)
                showStyle = false
            },
            onDismiss = {
                styleSheetDraft = draftStyleState
                showStyle = false
            },
        )
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
