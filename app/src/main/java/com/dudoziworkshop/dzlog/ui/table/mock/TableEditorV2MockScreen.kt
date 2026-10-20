package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialDemoProgress
import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialLocation
import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialTarget
import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialReadOnly
import com.dudoziworkshop.dzlog.feature.help.ui.tutorialTarget
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextOverflow
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.ui.common.DDZCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.domain.naming.buildNamingSlotPreview
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import java.util.Date
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableMergeDecision
import com.dudoziworkshop.dzlog.feature.table.editor.TableMergeDecisionType
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator
import com.dudoziworkshop.dzlog.feature.table.policy.quickCellLinks
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.common.DDZConfirmDialog
import com.dudoziworkshop.dzlog.ui.common.DDZContentDialog
import com.dudoziworkshop.dzlog.ui.common.DDZQuickChoiceDialog
import com.dudoziworkshop.dzlog.ui.common.DDZUnsavedDecisionDialog
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import kotlinx.coroutines.delay
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
    val phraseIndex: Int = 0,
    val phraseCompletedShots: Int = 0,
    val rowIndex: Int = 0,
    val colIndex: Int = 0,
    val rowSpan: Int = 1,
    val colSpan: Int = 1,
    val isCovered: Boolean = false,
    val previewValue: String? = null,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    templateId: String? = null,
    templateName: String = "새 템플릿",
    templates: List<SavedTableTemplate> = emptyList(),
    hasPendingTemplateSelection: Boolean = false,
    onSwitchTemplate: (String) -> Unit = {},
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
    val styleGuide = LocalTutorialLocation.current?.takeIf { it.areaId == "table-detail" && it.sessionId == "style" }
    val guideProgress = LocalTutorialDemoProgress.current
    val draftStyleState = if (styleGuide == null) session.draftStyleState else session.draftStyleState.let { original ->
        when (styleGuide.pageId) {
            "style/background" -> original.copy(bgStyle = if (guideProgress < 0.5f) 1 else 0)
            "style/opacity" -> original.copy(bgStyle = 1, bgAlpha = (255 - 150 * guideProgress).toInt())
            "style/text" -> original.copy(valueScale = (80 + 60 * guideProgress).toInt())
            "style/alignment" -> original.copy(textAlign = (guideProgress * 3).toInt().coerceAtMost(2))
            "style/border" -> original.copy(gridEnabled = guideProgress >= 0.5f)
            else -> original
        }
    }
    val saveRulesDraft = session.saveRulesDraft

    val rows = draftTemplateState.rows
    val cols = draftTemplateState.cols
    val selectionState = remember { TableEditorV2SelectionState() }
    val selectedCellId = selectionState.selectedCellId
    val layoutSelection = selectionState.layoutSelection
    var pendingMergeDecision by remember { mutableStateOf<TableMergeDecision?>(null) }
    var mode by remember { mutableStateOf(MockMode.EDIT) }
    var showStyle by remember { mutableStateOf(false) }
    var showSaveRules by remember { mutableStateOf(openSaveSettingsInitially) }
    var saveDetailTrail by remember { mutableStateOf(emptyList<TableEditorSaveDetail>()) }
    val saveDetail = saveDetailTrail.lastOrNull()
    val readOnlyTour = LocalTutorialReadOnly.current
    val tourLocation = LocalTutorialLocation.current
    LaunchedEffect(tourLocation) {
        if (tourLocation != null) {
            val saving = tourLocation.areaId == "saving"
            mode = if (!saving && tourLocation.sessionId == "structure") MockMode.LAYOUT else MockMode.EDIT
            showStyle = !saving && tourLocation.sessionId == "style"
            showSaveRules = saving && tourLocation.sessionId != "rotation"
            saveDetailTrail = if (saving && tourLocation.sessionId != "rotation") listOf(when (tourLocation.sessionId) {
                "filename" -> TableEditorSaveDetail.FILE_NAME
                "folder" -> if (tourLocation.pageId == "advanced") TableEditorSaveDetail.SAVE_MODE else TableEditorSaveDetail.SAVE_PATH
                "counter" -> if (tourLocation.pageId.endsWith("/scope")) TableEditorSaveDetail.AUTO_NUMBER_ADVANCED else TableEditorSaveDetail.AUTO_NUMBER
                else -> TableEditorSaveDetail.FILE_NAME
            }) else emptyList()
            if (tourLocation.areaId == "table-detail" && tourLocation.sessionId == "structure") {
                val exampleCells = draftTemplateState.cells
                if (exampleCells.size >= 2) selectionState.selectLayoutRange(draftTemplateState, exampleCells[0].cellId, exampleCells[1].cellId)
            }
            if ((!saving && tourLocation.sessionId == "cells") || (saving && tourLocation.sessionId == "rotation")) {
                val typeName = if (saving) "ROTATING_TEXT" else when (tourLocation.pageId.substringAfter('/')) {
                    "text" -> "TEXT"; "number" -> "NUMBER"; "counter" -> "COUNTER"
                    "date" -> "DATE"; "time" -> "TIME"; "rotation" -> "ROTATING_TEXT"; else -> ""
                }
                (draftTemplateState.cells.firstOrNull { it.dataType.name == typeName }
                    ?: draftTemplateState.cells.firstOrNull())?.cellId?.let(selectionState::selectEditCell)
            }
        }
    }
    var showCounterResetDialog by remember { mutableStateOf(false) }
    var showBackSaveDialog by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var pendingSwitchId by remember { mutableStateOf<String?>(null) }
    var layoutBoundaryDragActive by remember { mutableStateOf(false) }
    val saveCoordinator = remember { TableEditorV2SaveCoordinator() }
    val saveScope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val imeVisible = WindowInsets.isImeVisible
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    var keyboardPreviewLocked by remember { mutableStateOf(false) }
    var inputExpanded by remember(selectedCellId) { mutableStateOf(false) }
    // Keep the scale through the closing animation too; release after the IME settles.
    LaunchedEffect(imeVisible, imeBottom) {
        if (imeVisible || imeBottom > 0) {
            keyboardPreviewLocked = true
        } else {
            delay(300)
            keyboardPreviewLocked = false
        }
    }

    val counterController = rememberTableEditorCounterOrchestrator(context, session, showSaveRules)
    val preview = tableEditorPreview(context, session)
    val namingPreview = preview.previewNaming.displayName to preview.previewNaming.relativePath
    val cells = remember(draftTemplateState, preview.plan.resolvedCells) {
        mockCellsFromTemplate(draftTemplateState, preview.plan.resolvedCells.associate { it.id to it.resolvedText })
    }

    val activeTab = when {
        mode == MockMode.LAYOUT -> MockBottomTab.STRUCTURE
        showStyle -> MockBottomTab.STYLE
        showSaveRules -> MockBottomTab.SAVE
        else -> MockBottomTab.CONTENT
    }
    LaunchedEffect(activeTab) {
        if (activeTab != MockBottomTab.CONTENT) inputExpanded = false
    }

    fun requestBack() {
        if (imeVisible) {
            keyboard?.hide()
            return
        }
        when (resolveTableEditorBackAction(
            isSaving = saveCoordinator.isSaving,
            hasDetail = saveDetail != null,
            activeTab = activeTab,
            hasAdvancedStyle = false,
            hasSelectedCell = selectedCellId != null,
            hasUnsavedChanges = session.isDirty || isUnsavedNewTemplate || hasPendingTemplateSelection,
        )) {
            TableEditorBackAction.IGNORE -> Unit
            TableEditorBackAction.CLOSE_DETAIL -> saveDetailTrail = saveDetailTrail.dropLast(1)
            TableEditorBackAction.CLOSE_ADVANCED_STYLE -> Unit
            TableEditorBackAction.CLOSE_PANEL -> {
                mode = MockMode.EDIT
                showStyle = false
                showSaveRules = false
                selectionState.clearAll()
            }
            TableEditorBackAction.CLOSE_CELL -> selectionState.clearEditSelection()
            TableEditorBackAction.CONFIRM_EXIT -> showBackSaveDialog = true
            TableEditorBackAction.EXIT -> onBack()
        }
    }

    BackHandler { requestBack() }


    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = DDZColor.Background,
        topBar = {
            DDZTopBar(
                title = "표 편집",
                onBack = { requestBack() },
                actions = {
                    DDZButton(
                        text = if (mode == MockMode.LAYOUT) "완료" else "저장",
                        modifier = Modifier.tutorialTarget("table.save"),
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
            if (!imeVisible) Column(Modifier.tutorialTarget("table.tabs")) {
                HorizontalDivider(color = DDZColor.BorderStrong)
                MockBottomBar(
                    active = activeTab,
                    onContent = {
                        mode = MockMode.EDIT
                        selectionState.reconcileEditSelection(draftTemplateState)
                        selectionState.clearLayoutSelection()
                        showStyle = false
                        showSaveRules = false
                        saveDetailTrail = emptyList()
                    },
                    onLayout = {
                        selectionState.reconcileEditSelection(draftTemplateState)
                        showStyle = false
                        showSaveRules = false
                        saveDetailTrail = emptyList()
                        mode = MockMode.LAYOUT
                    },
                    onStyle = {
                        mode = MockMode.EDIT
                        selectionState.reconcileEditSelection(draftTemplateState)
                        showSaveRules = false
                        saveDetailTrail = emptyList()
                        showStyle = true
                    },
                    onSaveRules = {
                        mode = MockMode.EDIT
                        selectionState.clearLayoutSelection()
                        showStyle = false
                        saveDetailTrail = emptyList()
                        showSaveRules = true
                    },
                )
            }
        }
    ) { padding ->
        BoxWithConstraints(
            Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
        ) {
            // Every normal tab owns the same band, including an unselected Content tab.
            // IME mode wraps the compact input instead of preserving an empty band.
            val panelHeight = structurePanelHeight(LocalDensity.current.fontScale).coerceAtMost(maxHeight * 0.55f)
            val expandedInputHeight = minOf(DDZLayout.QuickInput.ExpandedHeight,
                (maxHeight * 0.4f).coerceAtLeast(DDZLayout.QuickInput.RowHeight))
            val editorPanelModifier = Modifier.heightIn(
                max = if (imeVisible) maxHeight * 0.55f else panelHeight
            )
            Column(modifier = Modifier.fillMaxSize()) {
                DDZCard(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(12.dp),
                ) {
                    Column(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TableEditorTemplateSelector(
                                modifier = Modifier.weight(1f).tutorialTarget("table.template"), name = templateName,
                                enabled = !saveCoordinator.isSaving && templates.any { it.id != templateId },
                                onClick = { showTemplatePicker = true },
                            )
                            CompactHistoryButton(
                                icon = Icons.Filled.Undo, contentDescription = "실행 취소",
                                enabled = session.canUndo && !saveCoordinator.isSaving,
                                onClick = { if (session.undo()) selectionState.clearAll() },
                            )
                            CompactHistoryButton(
                                icon = Icons.Filled.Redo, contentDescription = "다시 실행",
                                enabled = session.canRedo && !saveCoordinator.isSaving,
                                onClick = { if (session.redo()) selectionState.clearAll() },
                            )
                        }
                        Column(Modifier.fillMaxSize().tutorialTarget("table.preview")) {
                        MockTableCanvas(
                            cells = cells,
                            rows = rows,
                            cols = cols,
                            selectedCellId = selectedCellId,
                            selectedIds = cells
                                .filter { it.domainCellId in layoutSelection.selectedCellIds }
                                .map { it.id }
                                .toSet(),
                            darkTable = draftStyleState.bgStyle == 0,
                            transparentTable = draftStyleState.bgStyle == 2,
                            gridEnabled = draftStyleState.gridEnabled,
                            bgAlpha = draftStyleState.bgAlpha,
                            fontScale = draftStyleState.valueScale / 100f,
                            textAlignIndex = draftStyleState.textAlign,
                            textColorMode = draftStyleState.textColorMode,
                            manualTextColor = draftStyleState.manualTextColor,
                            layoutMode = mode == MockMode.LAYOUT,
                            preserveInputScale = keyboardPreviewLocked || imeVisible || imeBottom > 0 || inputExpanded,
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
                                        templateState = session.draftTemplateState,
                                        boundaryIndex = boundaryIndex,
                                        deltaFraction = deltaFraction,
                                    )
                                )
                            },
                            onColBoundaryDrag = { boundaryIndex, deltaFraction ->
                                session.replaceTemplateDraftWithoutHistory(
                                    adjustMockColumnBoundary(
                                        templateState = session.draftTemplateState,
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
                        }

                    }
                }

                if (!imeVisible) Text(
                    text = when {
                        showStyle -> "스타일을 조절한 뒤 상단 저장을 눌러주세요."
                        showSaveRules -> "저장설정을 선택해 편집하세요."
                        mode == MockMode.EDIT -> "셀을 선택해 내용을 편집하세요."
                        else -> "표 바깥 ↔ · ↕ 를 드래그해 크기를 조절하세요."
                    },
                    color = DDZColor.TextMuted,
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 20.dp, vertical = 8.dp),
                    maxLines = 2,
                )

                TableEditorWorkPanel(
                    Modifier.fillMaxWidth().tutorialTarget("table.panel")
                        .then(if (imeVisible) Modifier else Modifier.height(panelHeight)),
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Box(Modifier.fillMaxWidth().weight(1f, fill = !imeVisible)) {
                            if (showSaveRules) {
                                TableEditorSaveSettingsPanel(
                                    draft = saveRulesDraft,
                                    fileNamePreview = namingPreview.first,
                                    pathPreview = namingPreview.second,
                                    counterPadding = session.draftCounterPadding,
                                    nextCounter = session.draftNextCounter ?: 1,
                                    onOpenDetail = { saveDetailTrail = saveDetailTrail + it },
                                    modifier = editorPanelModifier,
                                )
                            } else if (showStyle) {
                                TableEditorStylePanel(
                                    draft = draftStyleState,
                                    onDraftChange = session::commitStyleChange,
                                    modifier = editorPanelModifier,
                                )
                            } else if (mode == MockMode.EDIT) {
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
                                            inputExpanded = false
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
                                        onPhraseProgressChange = { index, completed ->
                                            selected.domainCellId?.let { cellId ->
                                                session.commitTemplateChange(draftTemplateState.copy(
                                                    cells = draftTemplateState.cells.map { cell ->
                                                        if (cell.cellId == cellId) cell.copy(
                                                            phraseIndex = index, phraseCompletedShots = completed
                                                        ) else cell
                                                    }
                                                ))
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
                                            val newId = java.util.UUID.randomUUID().toString()
                                            val updated = com.dudoziworkshop.dzlog.feature.table.policy.RotatingPhraseSetCreationPolicy
                                                .createAndSelect(
                                                    template = draftTemplateState,
                                                    targetCellId = selected.domainCellId.orEmpty(),
                                                    name = name,
                                                    newSetId = newId,
                                                )
                                            if (updated != null) {
                                                session.commitTemplateChange(updated.template)
                                                updated.selectedSetId
                                            } else null
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
                                        onClose = {
                                            keyboard?.hide()
                                            selectionState.clearEditSelection()
                                        },
                                        compactInput = imeVisible,
                                        inputExpanded = inputExpanded,
                                        expandedInputHeight = expandedInputHeight,
                                        onToggleInputExpanded = { inputExpanded = !inputExpanded },
                                        onInputFocus = { keyboardPreviewLocked = true },
                                        modifier = editorPanelModifier.tutorialTarget("table.cell-input"),
                                    )
                                }
                            } else {
                                MockLayoutPanel(
                                    modifier = editorPanelModifier,
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
                                    onDeleteRows = {
                                        session.commitTemplateChange(TableEditorV2StructureController.removeRows(session.draftTemplateState, layoutSelection))
                                        selectionState.clearLayoutSelection()
                                    },
                                    onDeleteColumns = {
                                        session.commitTemplateChange(TableEditorV2StructureController.removeColumns(session.draftTemplateState, layoutSelection))
                                        selectionState.clearLayoutSelection()
                                    },
                                    onEqualizeColumns = {
                                        session.commitTemplateChange(
                                            TableEditorV2StructureController.equalizeColumns(session.draftTemplateState)
                                        )
                                    },
                                    onEqualizeRows = {
                                        session.commitTemplateChange(
                                            TableEditorV2StructureController.equalizeRows(session.draftTemplateState)
                                        )
                                    },
                                )
                            }
                        }
                        if (!imeVisible && activeTab == MockBottomTab.CONTENT) {
                            val selected = cells.firstOrNull { it.domainCellId == selectedCellId && !it.isCovered }
                            selected?.domainCellId?.let { id ->
                                TableEditorCellInfoRow(
                                    cell = selected,
                                    links = quickCellLinks(session.finalTemplateForSave(), id),
                                    neighbours = editorCellNeighbours(cells, id),
                                    enabled = !saveCoordinator.isSaving,
                                    onMove = { nextId ->
                                        focusManager.clearFocus()
                                        selectionState.selectEditCell(nextId)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTemplatePicker) {
        TableEditorTemplatePicker(
            templates = templates,
            currentTemplateId = templateId,
            onDismiss = { showTemplatePicker = false },
            onSelect = { targetId ->
                showTemplatePicker = false
                if (targetId != templateId) {
                    if (session.isDirty || isUnsavedNewTemplate) pendingSwitchId = targetId
                    else onSwitchTemplate(targetId)
                }
            },
        )
    }

    if (pendingSwitchId != null) {
        DDZUnsavedDecisionDialog(
            title = "템플릿을 교체할까요?",
            message = "현재 변경사항을 저장한 뒤 교체할 수 있어요.",
            onCancel = { if (!saveCoordinator.isSaving) pendingSwitchId = null },
            onDiscard = {
                val targetId = pendingSwitchId
                if (targetId != null && !saveCoordinator.isSaving) {
                    if (isUnsavedNewTemplate) onDiscardUnsavedNewTemplate()
                    pendingSwitchId = null
                    onSwitchTemplate(targetId)
                }
            },
            onSave = {
                val targetId = pendingSwitchId
                if (targetId != null && !saveCoordinator.isSaving) {
                    saveScope.launch {
                        if (saveCoordinator.save(session, onSave)) {
                            pendingSwitchId = null
                            onSwitchTemplate(targetId)
                        }
                    }
                }
            },
            enabled = !saveCoordinator.isSaving,
            saveText = "저장하고 교체",
            discardText = "저장 안 하고 교체",
            verticalActions = true,
        )
    }

    if (pendingMergeDecision != null) {
        DDZConfirmDialog(
            title = "셀을 병합할까요?",
            message = "병합하면 왼쪽 위 셀의 값만 유지됩니다.",
            confirmText = "병합",
            onDismiss = { pendingMergeDecision = null },
            onConfirm = {
                pendingMergeDecision?.let { decision ->
                    val updated = TableEditorV2StructureController.applyMergeDecision(session.draftTemplateState, decision)
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

    saveDetail?.let { detail ->
        TableEditorSaveDetailScreen(
            detail = detail,
            draft = saveRulesDraft,
            fileNamePreview = namingPreview.first,
            pathPreview = namingPreview.second,
            saveMode = session.draftSaveMode,
            counterPadding = session.draftCounterPadding,
            nextCounter = session.draftNextCounter ?: 1,
            cells = cells,
            rows = rows,
            cols = cols,
            isCounterBusy = counterController.busy,
            counterStatus = counterController.status,
            onBack = { requestBack() },
            onOpenDetail = { saveDetailTrail = saveDetailTrail + it },
            onSaveModeChange = session::commitSaveModeChange,
            onCounterPaddingChange = session::commitCounterPaddingChange,
            onNextCounterChange = { next -> counterController.edit(session, next) },
            onSyncCounter = { saveScope.launch { counterController.sync(context, session) } },
            onResetCounter = { showCounterResetDialog = true },
            onManualPreview = { isFileName, index, value ->
                val source = if (isFileName) saveRulesDraft.fileNameItems else saveRulesDraft.pathItems
                val pendingItems = source.filterNotNull().mapIndexed { i, item ->
                    if (i == index) item.copy(value = value) else item
                }.let { it + List((source.size - it.size).coerceAtLeast(0)) { null } }
                val pendingRules = if (isFileName) saveRulesDraft.copy(fileNameItems = pendingItems)
                    else saveRulesDraft.copy(pathItems = pendingItems)
                val pendingTemplate = applyMockSaveRulesDraft(session.finalTemplateForSave(), pendingRules)
                buildNamingSlotPreview(
                    isFileName = isFileName,
                    slotIndex = index,
                    resolvedCells = preview.plan.resolvedCells,
                    slotDrafts = if (isFileName) pendingTemplate.fileNameSlotDrafts else pendingTemplate.pathSlotDrafts,
                    fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
                    counterDigits = session.draftCounterPadding,
                    usedCounter = preview.previewNaming.usedCounter,
                    now = Date(),
                    dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
                    timeFormat = NamingFormatDefaults.TIME_FORMAT_PREVIEW_COMPACT,
                )
            },
            onDraftChange = session::commitSaveRulesChange,
        )
    }

    if (showCounterResetDialog || (tourLocation?.areaId == "saving" && tourLocation.pageId == "counter/reset")) {
        DDZConfirmDialog(
            title = "자동번호를 초기화할까요?",
            message = "다음 번호가 ${formatMockCounter(1, session.draftCounterPadding)}로 변경됩니다. 저장된 사진의 번호는 변경되지 않습니다.",
            confirmText = "초기화",
            dismissText = "취소",
            onConfirm = {
                if (!readOnlyTour) counterController.reset(session)
                showCounterResetDialog = false
            },
            onDismiss = { showCounterResetDialog = false },
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

    // Present the real six-type selector in the full-screen passive tutorial layer.
    if (readOnlyTour && tourLocation?.areaId == "table-detail" && tourLocation.pageId == "cells/types") {
        TableEditorCellTypeSheet(
            selectedType = TableEditorCellType.TEXT,
            onSelect = {},
            onDismiss = {},
        )
    }

    if (readOnlyTour && tourLocation?.areaId == "table-detail" && tourLocation.pageId == "save/template") {
        TableEditorTemplatePicker(
            templates = emptyList(), currentTemplateId = null,
            onDismiss = {}, onSelect = {}, tutorialDemo = true,
        )
    }
    if (readOnlyTour && tourLocation?.areaId == "table-detail" && tourLocation.pageId == "save/template-confirm") {
        DDZUnsavedDecisionDialog(
            title = "템플릿을 교체할까요?",
            message = "현재 변경사항을 저장한 뒤 교체할 수 있어요.",
            onCancel = {}, onDiscard = {}, onSave = {},
            saveText = "저장하고 교체", discardText = "저장 안 하고 교체", verticalActions = true,
        )
    }

    if (tourLocation?.areaId == "table-detail" && tourLocation.pageId == "save/decision") {
        DDZUnsavedDecisionDialog(
            title = "변경사항을 저장할까요?",
            message = "저장하지 않으면 이번 편집 내용은 모두 사라집니다.",
            onCancel = {}, onDiscard = {}, onSave = {},
        )
    }

    if (showBackSaveDialog) {
        DDZUnsavedDecisionDialog(
            title = "변경사항을 저장할까요?",
            message = "저장하지 않으면 이번 편집 내용은 모두 사라집니다.",
            onCancel = { if (!saveCoordinator.isSaving) showBackSaveDialog = false },
            onDiscard = {
                if (!saveCoordinator.isSaving) {
                    if (isUnsavedNewTemplate) onDiscardUnsavedNewTemplate()
                    showBackSaveDialog = false
                    onBack()
                }
            },
            onSave = {
                if (!saveCoordinator.isSaving) {
                    saveScope.launch {
                        if (saveCoordinator.save(session, onSave)) {
                            showBackSaveDialog = false
                            onBack()
                        }
                    }
                }
            },
            enabled = !saveCoordinator.isSaving,
        )
    }
}



@Composable
private fun CompactHistoryButton(icon: ImageVector, contentDescription: String,
    enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(40.dp).tutorialTarget("table.history.$contentDescription")) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp),
            tint = if (enabled) DDZColor.TextPrimary else DDZColor.TextDisabled)
    }
}
