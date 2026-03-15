@file:Suppress("UNUSED_VALUE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.dudoziworkshop.dzlog.ui.table

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dudoziworkshop.dzlog.ui.common.dzScaffoldContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.counter.COUNTER_DIGITS_DEFAULT
import com.dudoziworkshop.dzlog.data.counter.clampCounterDigits
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.data.preferences.KEY_COUNTER_DIGITS
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.domain.counter.policy.CounterScopeSnapshot
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.CounterScopeMode
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.TimeFormatOptions
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.naming.resolveFileNameScopeTokensFromDrafts
import com.dudoziworkshop.dzlog.domain.preview.computeNextDelayMillis
import com.dudoziworkshop.dzlog.domain.preview.PreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildPreview
import com.dudoziworkshop.dzlog.domain.preview.decideTickUnitFromTemplate
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.feature.table.policy.confirmCounterConflictDialog
import com.dudoziworkshop.dzlog.feature.table.policy.dismissCounterConflictDialog
import com.dudoziworkshop.dzlog.feature.table.policy.saveTableTemplate
import com.dudoziworkshop.dzlog.feature.counter.core.CounterFacade
import com.dudoziworkshop.dzlog.feature.counter.core.CounterRequestResolver
import com.dudoziworkshop.dzlog.feature.counter.table.TableCounterUiState
import com.dudoziworkshop.dzlog.feature.counter.table.applyCounterConflictDialogEffect
import com.dudoziworkshop.dzlog.feature.counter.table.buildFilenameScopeSignature
import com.dudoziworkshop.dzlog.feature.counter.table.restoreCounterCellToAutoNext
import com.dudoziworkshop.dzlog.feature.counter.table.syncCounterStateForScope
import com.dudoziworkshop.dzlog.feature.counter.table.updateCounterCellAndPolicy
import com.dudoziworkshop.dzlog.feature.counter.table.updateCounterUiConflictDialogState
import com.dudoziworkshop.dzlog.feature.counter.table.updateCounterUiScopeFlags
import com.dudoziworkshop.dzlog.ui.table.editor.InlineEditState
import com.dudoziworkshop.dzlog.ui.table.editor.clearInlineEditing
import com.dudoziworkshop.dzlog.ui.table.editor.isEditing
import com.dudoziworkshop.dzlog.ui.table.editor.shouldBlockTabSwitchAfterCommit
import com.dudoziworkshop.dzlog.ui.table.editor.startInlineEditing
import com.dudoziworkshop.dzlog.ui.table.format.TableFormatDialog
import com.dudoziworkshop.dzlog.ui.table.format.TableFormatDialogState
import com.dudoziworkshop.dzlog.ui.table.format.close
import com.dudoziworkshop.dzlog.ui.table.format.open
import com.dudoziworkshop.dzlog.ui.table.rotating.RotatingPhraseSetEditDialog
import com.dudoziworkshop.dzlog.ui.table.rotating.RotatingPhraseTemplateDialog
import com.dudoziworkshop.dzlog.ui.table.rotating.RotatingPhraseUiState
import com.dudoziworkshop.dzlog.ui.table.section.BottomEditorPanelMode
import com.dudoziworkshop.dzlog.ui.table.section.FileNameFormatType
import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotKind
import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotUiItem
import com.dudoziworkshop.dzlog.ui.table.section.PathFormatType
import com.dudoziworkshop.dzlog.ui.table.section.PathSlotKind
import com.dudoziworkshop.dzlog.ui.table.section.PathSlotUiItem
import com.dudoziworkshop.dzlog.ui.table.section.LayoutTabActions
import com.dudoziworkshop.dzlog.ui.table.section.LayoutTabContent
import com.dudoziworkshop.dzlog.ui.table.section.LayoutTabUiState
import com.dudoziworkshop.dzlog.ui.table.section.PreviewTabContent
import com.dudoziworkshop.dzlog.feature.table.editor.TableHandleOverlay
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionRange
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionResolver
import com.dudoziworkshop.dzlog.feature.table.editor.TableUndoManager
import com.dudoziworkshop.dzlog.feature.table.editor.addColumn
import com.dudoziworkshop.dzlog.feature.table.editor.addColumnBySelection
import com.dudoziworkshop.dzlog.feature.table.editor.addRow
import com.dudoziworkshop.dzlog.feature.table.editor.addRowBySelection
import com.dudoziworkshop.dzlog.feature.table.editor.removeColumn
import com.dudoziworkshop.dzlog.feature.table.editor.removeColumnBySelection
import com.dudoziworkshop.dzlog.feature.table.editor.removeRow
import com.dudoziworkshop.dzlog.feature.table.editor.removeRowBySelection
import com.dudoziworkshop.dzlog.feature.table.editor.resetColumnWeights
import com.dudoziworkshop.dzlog.feature.table.editor.resetRowWeights
import com.dudoziworkshop.dzlog.feature.table.editor.updateCell
import com.dudoziworkshop.dzlog.feature.table.model.TablePlacementState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.feature.table.placement.applyAnchorOffsetDragChange
import com.dudoziworkshop.dzlog.feature.table.placement.applyCaptureAspectChange
import com.dudoziworkshop.dzlog.feature.table.placement.applyHeightRatioChange
import com.dudoziworkshop.dzlog.feature.table.placement.applyWidthRatioChange
import com.dudoziworkshop.dzlog.feature.table.placement.loadTablePlacementState
import com.dudoziworkshop.dzlog.feature.table.state.loadTableStyleState
import com.dudoziworkshop.dzlog.feature.table.state.persistTableStyleState
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.awaitCancellation
import java.util.Date
import java.util.UUID
import com.dudoziworkshop.dzlog.ui.table.editor.commitInlineEditIfNeeded as commitInlineEdit

private data class DeletedStructureSnapshot(
    val cells: List<TableCellState>,
    val fileNameSlotsSnapshot: List<FileNameSlotUiItem?>,
    val pathSlotsSnapshot: List<PathSlotUiItem?>
)

private data class TableEditorUndoSnapshot(
    val templateState: TableTemplateState,
    val styleState: TableStyleState,
    val selectedCellId: String?,
    val structureSelectedCellIds: Set<String>,
    val structureSelectionRange: TableSelectionRange?,
)

private fun normalizeFileNameDraftSlots(slots: List<FileNameSlotUiItem?>): List<FileNameSlotUiItem?> {
    return List(3) { index -> slots.getOrNull(index) }
}

private fun normalizePathDraftSlots(slots: List<PathSlotUiItem?>): List<PathSlotUiItem?> {
    return List(2) { index -> slots.getOrNull(index) }
}

private fun toFileNameUiSlotDraft(slot: com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft?): FileNameSlotUiItem? {
    if (slot == null) return null
    val kind = runCatching { FileNameSlotKind.valueOf(slot.kind) }.getOrNull() ?: return null
    val formatType = slot.formatType?.let { runCatching { FileNameFormatType.valueOf(it) }.getOrNull() }
    return FileNameSlotUiItem(
        kind = kind,
        label = slot.label,
        cellId = slot.cellId,
        manualText = slot.manualText,
        formatType = formatType
    )
}

private fun toPathUiSlotDraft(slot: com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft?): PathSlotUiItem? {
    if (slot == null) return null
    val kind = runCatching { PathSlotKind.valueOf(slot.kind) }.getOrNull() ?: return null
    val formatType = slot.formatType?.let { runCatching { PathFormatType.valueOf(it) }.getOrNull() }
    return PathSlotUiItem(
        kind = kind,
        label = slot.label,
        cellId = slot.cellId,
        manualText = slot.manualText,
        formatType = formatType
    )
}

private fun toFileNameDomainSlotDraft(slot: FileNameSlotUiItem?): com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft? {
    if (slot == null) return null
    return com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft(
        kind = slot.kind.name,
        label = slot.label,
        cellId = slot.cellId,
        manualText = slot.manualText,
        formatType = slot.formatType?.name
    )
}

private fun toPathDomainSlotDraft(slot: PathSlotUiItem?): com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft? {
    if (slot == null) return null
    return com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft(
        kind = slot.kind.name,
        label = slot.label,
        cellId = slot.cellId,
        manualText = slot.manualText,
        formatType = slot.formatType?.name
    )
}

private fun buildFileNameDraftSlots(template: TableTemplateState): List<FileNameSlotUiItem?> {
    return normalizeFileNameDraftSlots(template.fileNameSlotDrafts.map(::toFileNameUiSlotDraft))
}

private fun buildPathDraftSlots(template: TableTemplateState): List<PathSlotUiItem?> {
    return normalizePathDraftSlots(template.pathSlotDrafts.map(::toPathUiSlotDraft))
}

@Composable
fun TableEditorScreen(
    templateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // ✅ 탭 상태
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    var initialTemplateSnapshot by remember { mutableStateOf(templateState) }
    var editableTemplateState by remember { mutableStateOf(templateState) }
    val currentTemplate = editableTemplateState

    fun updateTemplateDraft(updated: TableTemplateState) {
        // 정책 변경: TableEditor의 편집 SSOT는 editableTemplateState 단일 상태다.
        editableTemplateState = updated
    }

    var fileNameSlotsDirtySinceStructureChange by remember { mutableStateOf(false) }
    var pathSlotsDirtySinceStructureChange by remember { mutableStateOf(false) }

    fun withUpdatedFileNameSlots(
        base: TableTemplateState,
        updatedSlots: List<FileNameSlotUiItem?>
    ): TableTemplateState {
        val normalized = normalizeFileNameDraftSlots(updatedSlots)
        val domainDrafts = normalized.map(::toFileNameDomainSlotDraft)
        return base.copy(
            fileNameSlotDrafts = domainDrafts
        )
    }

    fun withUpdatedPathSlots(
        base: TableTemplateState,
        updatedSlots: List<PathSlotUiItem?>
    ): TableTemplateState {
        val normalized = normalizePathDraftSlots(updatedSlots)
        return base.copy(
            pathSlotDrafts = normalized.map(::toPathDomainSlotDraft)
        )
    }

    fun updateFileNameSlotDraft(
        base: TableTemplateState,
        updated: List<FileNameSlotUiItem?>,
        markDirty: Boolean = true
    ) {
        updateTemplateDraft(withUpdatedFileNameSlots(base, updated))
        if (markDirty) {
            fileNameSlotsDirtySinceStructureChange = true
        }
    }

    fun updatePathSlotDraft(
        base: TableTemplateState,
        updated: List<PathSlotUiItem?>,
        markDirty: Boolean = true
    ) {
        updateTemplateDraft(withUpdatedPathSlots(base, updated))
        if (markDirty) {
            pathSlotsDirtySinceStructureChange = true
        }
    }

    var selectedCellId by remember { mutableStateOf<String?>(null) }
    var structureSelectedCellIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var structureSelectionRange by remember { mutableStateOf<TableSelectionRange?>(null) }
    // ✅ 탭0: 셀 설정 패널 표시 여부
    var showCellSettingsPanel by remember { mutableStateOf(false) }
    // 모드형 하단 편집 패널 1차 구조 상태 (다음 단계 슬롯/직접 편집 확장 대비)
    var bottomPanelMode by remember { mutableStateOf(BottomEditorPanelMode.NONE) }
    var currentlySelectedFileNameSlot by remember { mutableStateOf<Int?>(null) }
    var currentlySelectedPathSlot by remember { mutableStateOf<Int?>(null) }
    // 정책 변경: FILENAME_EDIT 2단계에서는 템플릿의 실제 셀 연결과 분리된 "슬롯 UI 상태"를 별도로 유지한다.
    // 다음 단계에서 셀/카운터/직접입력 상세 선택 UI를 붙일 수 있게 구조화된 타입을 사용한다.
    val fileNameSlotItems = buildFileNameDraftSlots(currentTemplate)
    var isFileNameCellPickMode by remember { mutableStateOf(false) }
    var manualInputDraft by remember { mutableStateOf("") }
    var showManualInputEditor by remember { mutableStateOf(false) }

    fun removeFileNameSlotAt(slots: List<FileNameSlotUiItem?>, index: Int): List<FileNameSlotUiItem?> {
        val compacted = slots.filterIndexed { i, item -> i != index && item != null }
        return normalizeFileNameDraftSlots(compacted)
    }

    fun moveFileNameSlot(slots: List<FileNameSlotUiItem?>, from: Int, to: Int): List<FileNameSlotUiItem?> {
        val mutable = slots.toMutableList()
        val temp = mutable[from]
        mutable[from] = mutable[to]
        mutable[to] = temp
        return normalizeFileNameDraftSlots(mutable)
    }

    fun clearFileNameEditorTransientState(clearDraft: Boolean) {
        // 정책 보강: 파일명 편집의 보조 UI 상태는 슬롯/모드 전환 시 잔존하지 않게 정리한다.
        isFileNameCellPickMode = false
        showManualInputEditor = false
        if (clearDraft) {
            manualInputDraft = ""
        }
    }

    val pathSlotItems = buildPathDraftSlots(currentTemplate)
    var isPathCellPickMode by remember { mutableStateOf(false) }
    var showPathManualInputEditor by remember { mutableStateOf(false) }
    var pathManualInputDraft by remember { mutableStateOf("") }

    fun removePathSlotAt(slots: List<PathSlotUiItem?>, index: Int): List<PathSlotUiItem?> {
        val compacted = slots.filterIndexed { i, item -> i != index && item != null }
        return normalizePathDraftSlots(compacted)
    }

    fun movePathSlot(slots: List<PathSlotUiItem?>, from: Int, to: Int): List<PathSlotUiItem?> {
        val mutable = slots.toMutableList()
        val temp = mutable[from]
        mutable[from] = mutable[to]
        mutable[to] = temp
        return normalizePathDraftSlots(mutable)
    }

    fun removeCellRefsFromFileNameSlots(
        slots: List<FileNameSlotUiItem?>,
        deletedCellIds: Set<String>
    ): List<FileNameSlotUiItem?> {
        val filtered = slots.filter { slot ->
            slot != null && (slot.cellId == null || slot.cellId !in deletedCellIds)
        }
        return normalizeFileNameDraftSlots(filtered)
    }

    fun removeCellRefsFromPathSlots(
        slots: List<PathSlotUiItem?>,
        deletedCellIds: Set<String>
    ): List<PathSlotUiItem?> {
        val filtered = slots.filter { slot ->
            slot != null && (slot.cellId == null || slot.cellId !in deletedCellIds)
        }
        return normalizePathDraftSlots(filtered)
    }

    fun clearPathEditorTransientState(clearDraft: Boolean) {
        // 정책 보강: 저장경로 편집의 보조 UI 상태(셀대기/서식/직접입력)는 모드/슬롯 전환에서 분리 정리한다.
        isPathCellPickMode = false
        showPathManualInputEditor = false
        if (clearDraft) {
            pathManualInputDraft = ""
        }
    }

    // 탭1 스크롤 (분리)
    val previewTabScrollState = rememberScrollState()

    val scope = rememberCoroutineScope()
    var isSavingTemplate by remember { mutableStateOf(false) }

    var formatDialog by remember { mutableStateOf(TableFormatDialogState()) }

    var rotatingUi by remember { mutableStateOf(RotatingPhraseUiState()) }

    fun openRotatingPhraseTemplateDialog(targetCellId: String) {
        rotatingUi = rotatingUi.copy(
            isTemplateDialogOpen = true,
            templateDialogCellId = targetCellId,
            templateDialogRestore = currentTemplate,
            isCreateSetDialogOpen = false,
            createSetName = "",
            pendingDeleteSetId = null,
            isSetEditDialogOpen = false,
            editingSetId = null
        )
        showCellSettingsPanel = false
    }

    fun closeRotatingPhraseTemplateDialog() {
        rotatingUi = RotatingPhraseUiState()
    }

    val dateFormatOptions = listOf("yyyyMMdd", "yyMMdd", "MMdd")
    // 주요 정책: TIME 형식은 naming 경로에서 HHmm만 사용한다.

    // 주요 정책: 테이블/홈/카메라/실저장이 같은 naming 기본값을 공유한다.
    val dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT
    val timeFormat = NamingFormatDefaults.TIME_FORMAT_CAPTURE_DEFAULT

    var previewCounterDigits by remember { mutableIntStateOf(COUNTER_DIGITS_DEFAULT) }

    val settings by AppSettingsStore.flow(context).collectAsState(
        initial = AppSettings(
            saveMode = SaveMode.BOTH,
            continuousPreviewMode = ContinuousPreviewMode.OFF,
            photoQualityMode = PhotoQualityMode.BALANCED,
            counterPadding = previewCounterDigits,
            includePathInCounterScope = true,
            includeFilenameInCounterScope = true,
            toastEnabled = true,
            hapticEnabled = true,
            captureHapticEnabled = true,
            blankWarningEnabled = true,
        )
    )
    val tableSaveMode = settings.saveMode

    val initialCounterSeed = (currentTemplate.cells.firstOrNull { it.dataType == TableCellDataType.COUNTER }?.typedValue as? CellValue.CounterSeed)?.start
        ?.coerceAtLeast(1) ?: 1
    var counterUi by remember {
        mutableStateOf(
            TableCounterUiState(
                scopeNextCounter = initialCounterSeed,
                autoNextCounterValue = initialCounterSeed,
                // 초기 진입 시에는 facade.read 동기화 전으로 간주한다.
                isScopeCounterSynced = false,
            )
        )
    }
    val counterFacade = remember(context, previewCounterDigits) {
        CounterFacade(
            context = context,
            counterDigits = previewCounterDigits,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
        )
    }

    var resumeTick by remember { mutableIntStateOf(0) }
    var scopeInputTick by remember { mutableIntStateOf(0) }
    var scopeKeySnapshot by remember { mutableStateOf<String?>(null) }
    var lastFilenameScopeSignature by remember { mutableStateOf<String?>(null) }
    var lastPathScopeSignature by remember { mutableStateOf<String?>(null) }
    val hasTemplateCells = currentTemplate.cells.isNotEmpty()

    var previewNow by remember { mutableStateOf(Date()) }
    // 정책 변경: 프리뷰에서도 문구 순환 커서를 파일 카운터와 분리한다.
    var phraseProgressCounter by remember { mutableIntStateOf(1) }

    LaunchedEffect(currentTemplate.cells, lifecycleOwner) {
        val unit = decideTickUnitFromTemplate(currentTemplate.cells)
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                val delayMs = computeNextDelayMillis(unit)
                delay(delayMs)
                previewNow = Date()
            }
        }
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            resumeTick += 1
            awaitCancellation()
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

    LaunchedEffect(settings.includePathInCounterScope, settings.includeFilenameInCounterScope) {
        counterUi = updateCounterUiScopeFlags(
            counterUi = counterUi,
            includePathInCounterScope = settings.includePathInCounterScope,
            includeFilenameInCounterScope = settings.includeFilenameInCounterScope,
        )
    }

    LaunchedEffect(
        currentTemplate.cells,
        currentTemplate.phraseSets,
    ) {
        // 정책: 스코프 입력(셀/문구세트)이 바뀌면 즉시 next counter 재동기화를 강제한다.
        scopeInputTick += 1
    }

    val tableResolver = remember { TableResolver() }

    // 프리뷰 계산 경로를 Camera/Home과 동일하게 공용 pipeline으로 통일한다.
    val previewPipeline = remember(
        currentTemplate,
        previewNow,
        previewCounterDigits,
        counterUi.scopeNextCounter,
        phraseProgressCounter,
        dateFormat,
        timeFormat,
        counterUi.includePathInCounterScope,
        counterUi.includeFilenameInCounterScope,
        settings.saveMode,
    ) {
        buildPreview(
            input = PreviewInput(
                templateState = currentTemplate,
                captureNow = previewNow,
                counterDigits = previewCounterDigits,
                dateFormat = dateFormat,
                timeFormat = timeFormat,
                fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
                includePathInCounterScope = counterUi.includePathInCounterScope,
                includeFilenameInCounterScope = counterUi.includeFilenameInCounterScope,
                saveMode = settings.saveMode,
                scopeNextCounter = counterUi.scopeNextCounter,
                phraseProgressCursor = phraseProgressCounter,
            ),
            tableResolver = tableResolver,
        )
    }
    val plan = previewPipeline.plan

    // 주요 정책(파일명 축): 카운터 분리 기준은 fileNameSlotDrafts의 "최종 해석 token 목록"이다.
    // draft 원문 비교가 아니라, 실제 scope에 반영되는 결과값(순서 포함)으로 signature를 만든다.
    val filenameScopeTokens = remember(
        plan.resolvedCells,
        currentTemplate.fileNameSlotDrafts,
        previewNow,
        dateFormat,
        timeFormat,
        counterUi.includeFilenameInCounterScope,
        settings.saveMode,
    ) {
        if (!counterUi.includeFilenameInCounterScope) {
            emptyList()
        } else {
            resolveFileNameScopeTokensFromDrafts(
                fileNameSlotDrafts = currentTemplate.fileNameSlotDrafts,
                resolvedCells = plan.resolvedCells,
                now = previewNow,
                dateFormat = dateFormat,
                timeFormat = timeFormat,
            )
        }
    }
    val filenameScopeSignature = remember(
        counterUi.includeFilenameInCounterScope,
        filenameScopeTokens,
    ) {
        buildFilenameScopeSignature(
            includeFilenameInCounterScope = counterUi.includeFilenameInCounterScope,
            filenameScopeTokens = filenameScopeTokens,
        )
    }

    // 주요 정책(path 축): 카운터 재동기화 기준은 draft 원문이 아니라 "최종 해석된 relativePath"다.
    // includePathInCounterScope=false 이면 path 변경이 스트림에 영향을 주지 않는다.
    val pathScopeSignature = remember(
        previewPipeline.previewNaming.relativePath,
        counterUi.includePathInCounterScope
    ) {
        if (!counterUi.includePathInCounterScope) {
            "path-scope-disabled"
        } else {
            previewPipeline.previewNaming.relativePath
        }
    }

    LaunchedEffect(filenameScopeSignature, pathScopeSignature) {
        val filenameChanged = filenameScopeSignature != lastFilenameScopeSignature
        val pathChanged = pathScopeSignature != lastPathScopeSignature
        if (filenameChanged) {
            lastFilenameScopeSignature = filenameScopeSignature
        }
        if (pathChanged) {
            lastPathScopeSignature = pathScopeSignature
        }
        if (filenameChanged || pathChanged) {
            scopeInputTick += 1
        }
    }

    val isManualCounterModeDisplay by remember(
        counterUi.scopeNextCounter,
        counterUi.autoNextCounterValue
    ) {
        derivedStateOf {
            // 정책: 수동 표시(표 상세/촬영)는 manual override 존재 여부가 아니라
            // "현재 표시값 vs media auto-next" 차이로 판정한다.
            counterUi.scopeNextCounter != counterUi.autoNextCounterValue
        }
    }


    val counterScope by remember(previewPipeline.previewNaming.counterScope) {
        derivedStateOf { previewPipeline.previewNaming.counterScope }
    }
    val scanPrefix by remember(previewPipeline.previewNaming.scanPrefix) {
        derivedStateOf { previewPipeline.previewNaming.scanPrefix }
    }

    val counterRequest by remember(
        counterScope,
        scanPrefix,
        tableSaveMode,
        counterUi.includePathInCounterScope,
        counterUi.includeFilenameInCounterScope,
    ) {
        derivedStateOf {
            // 표 상세 카운터 입력은 resolver에서만 정규화한다.
            CounterRequestResolver.fromTable(
                saveMode = tableSaveMode,
                relativePathKey = counterScope.relativePathKey,
                prefix = counterScope.streamPrefix,
                scanPrefix = scanPrefix,
                includePathInScope = counterUi.includePathInCounterScope,
                includeFilenameInScope = counterUi.includeFilenameInCounterScope,
                tableTemplateId = null,
            )
        }
    }

    fun buildTableCounterRequestKey(request: com.dudoziworkshop.dzlog.feature.counter.core.CounterRequest): String {
        return listOf(
            request.relativePathKey,
            request.prefix,
            request.includePathInScope,
            request.includeFilenameInScope,
        ).joinToString("|")
    }


    fun hideTrailingCounterToken(displayName: String): String {
        // 초기 동기화 전에는 기본 seed(1) 플리커를 피하기 위해 파일명 말미 카운터 토큰을 숨긴다.
        // 수동 입력 후보값/첫 readback 완료 이후에는 원본 displayName을 그대로 사용한다.
        return displayName.replace(Regex("_[0-9]+$"), "")
    }

    LaunchedEffect(resumeTick, counterRequest) {
        scopeKeySnapshot = buildTableCounterRequestKey(counterRequest)
    }
    val activeScopeKey = scopeKeySnapshot ?: buildTableCounterRequestKey(counterRequest)

    // ✅ 스트림 변경 감지용 (스트림이 바뀌면 seed를 "새 스트림 next"로 강제 동기화)


    var lastScopeSnapshot by remember { mutableStateOf<CounterScopeSnapshot?>(null) }
    var lastProcessedResumeTick by remember { mutableIntStateOf(-1) }
    var lastProcessedScopeInputTick by remember { mutableIntStateOf(-1) }


    LaunchedEffect(
        activeScopeKey,
        previewCounterDigits,
        tableSaveMode,
        resumeTick,
        scopeInputTick,
        counterRequest,
        counterFacade,
    ) {
        // 빈 템플릿은 카운터 재동기화 입력이 없으므로 SSOT 경로에서 조기 종료한다.
        if (!hasTemplateCells) return@LaunchedEffect

        val isFilenameScopeSignatureChanged =
            counterUi.includeFilenameInCounterScope &&
                (lastFilenameScopeSignature != null) &&
                (lastFilenameScopeSignature != filenameScopeSignature)

        val isExternalResync = (resumeTick != lastProcessedResumeTick) || (scopeInputTick != lastProcessedScopeInputTick)
        val syncResult = syncCounterStateForScope(
            templateState = currentTemplate,
            counterUi = counterUi,
            counterScope = counterScope,
            counterRequest = counterRequest,
            counterFacade = counterFacade,
            isManualCounterModeDisplay = isManualCounterModeDisplay,
            lastScopeSnapshot = lastScopeSnapshot,
            filenameScopeSignature = filenameScopeSignature,
            isFilenameScopeSignatureChanged = isFilenameScopeSignatureChanged,
            isExternalResync = isExternalResync,
            updateCell = ::updateCell
        )
        counterUi = syncResult.counterUi
        lastScopeSnapshot = syncResult.nextScopeSnapshot
        lastFilenameScopeSignature = syncResult.nextFilenameScopeSignature
        lastProcessedResumeTick = resumeTick
        lastProcessedScopeInputTick = scopeInputTick
        syncResult.updatedTemplateState?.let(::updateTemplateDraft)
    }

    // 주요 정책 변경: inlineEdit 상태는 더 이상 "표 내부 입력창"이 아니라 CELL_EDIT 패널 draft/commit 상태로만 사용한다.
    var inlineEdit by remember { mutableStateOf(InlineEditState()) }
    // 주요 정책: 되돌리기 기준은 "셀 선택 시점"의 TableCellState 전체 snapshot이다.
    var editSessionOriginalCellState by remember { mutableStateOf<TableCellState?>(null) }
    var editSessionSnapshotCellId by remember { mutableStateOf<String?>(null) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    val deletedRowsStack = remember { mutableStateListOf<DeletedStructureSnapshot>() }
    val deletedColsStack = remember { mutableStateListOf<DeletedStructureSnapshot>() }

    fun clearInlineEditingState() {
        inlineEdit = clearInlineEditing(inlineEdit)
    }

    fun sanitizePathGroupAfterStructureChange(state: TableTemplateState): TableTemplateState {
        val hasG1 = state.cells.any { it.groupLevel == GroupLevel.G1 }
        if (hasG1) return state

        val nextCells = state.cells.map { cellState ->
            if (cellState.groupLevel == GroupLevel.G2) {
                cellState.copy(groupLevel = GroupLevel.NONE)
            } else {
                cellState
            }
        }
        return state.copy(cells = nextCells)
    }

    fun commitInlineEditIfNeeded() {
        val result = commitInlineEdit(
            inlineState = inlineEdit,
            templateState = currentTemplate,
            autoNextCounterValue = counterUi.autoNextCounterValue,
            lowCounterWarningLatchedInSession = counterUi.lowCounterWarningLatchedInSession,
            updateCell = ::updateCell
        )
        counterUi = counterUi.copy(
            lowCounterWarningLatchedInSession = result.lowCounterWarningLatchedInSession,
        )
        val committedCellId = inlineEdit.editingCellId
        inlineEdit = result.nextInlineState
        result.openedCounterConflict?.let {
            counterUi = updateCounterUiConflictDialogState(counterUi, it)
        }
        result.committedCounterSeed?.let { seed ->
            val cellId = committedCellId ?: return@let
            updateCounterCellAndPolicy(
                templateState = currentTemplate,
                cellId = cellId,
                seed = seed,
                preserveManual = true,
                // 정책 변경: 표 상세에서 사용자 확정(저장/반영)은 전역 manual override 저장으로 반영한다.
                persistToCounterPolicy = true,
                counterRequest = counterRequest,
                counterFacade = counterFacade,
                counterUi = counterUi,
                onTemplateChange = ::updateTemplateDraft,
                setCounterUi = { counterUi = it },
                updateCell = ::updateCell,
                scope = scope,
                lowCounterWarningLatchedInSession = result.lowCounterWarningLatchedInSession,
            )
        } ?: result.updatedTemplateState?.let(::updateTemplateDraft)
    }


    fun snapshotCellForEditSession(cellId: String?) {
        if (cellId == null) {
            editSessionOriginalCellState = null
            editSessionSnapshotCellId = null
            return
        }
        if (editSessionSnapshotCellId == cellId) return
        editSessionOriginalCellState = currentTemplate.cells.firstOrNull { it.cellId == cellId }?.copy()
        editSessionSnapshotCellId = cellId
    }

    val selectedCell = currentTemplate.cells.firstOrNull { it.cellId == selectedCellId }

    LaunchedEffect(currentTemplate.cells, selectedCellId) {
        val hasSelectedCell = selectedCellId != null && currentTemplate.cells.any { it.cellId == selectedCellId }
        if (!hasSelectedCell) {
            selectedCellId = null
        }
    }

    LaunchedEffect(bottomPanelMode, selectedCellId, currentTemplate.cells) {
        // 주요 정책: 되돌리기 snapshot은 CELL_EDIT 진입 상태에서만 selectedCell 기준으로 1회 저장한다.
        if (bottomPanelMode == BottomEditorPanelMode.CELL_EDIT) {
            snapshotCellForEditSession(selectedCellId)
        } else {
            editSessionOriginalCellState = null
            editSessionSnapshotCellId = null
        }
    }
    val hasGroup1 = currentTemplate.cells.any { it.groupLevel == GroupLevel.G1 }
    val hasGroup2 = currentTemplate.cells.any { it.groupLevel == GroupLevel.G2 }
    val resolvedByCellId = remember(plan.resolvedCells) {
        plan.resolvedCells.associate { it.id to it.resolvedText }
    }

    var watermarkUi by remember { mutableStateOf(TablePlacementState()) }
    var tableStyleUi by remember { mutableStateOf(TableStyleState()) }
    var initialStyleSnapshot by remember { mutableStateOf(TableStyleState()) }
    val undoManager = remember { TableUndoManager<TableEditorUndoSnapshot>() }
    var undoRevision by remember { mutableIntStateOf(0) }

    fun currentUndoSnapshot(): TableEditorUndoSnapshot {
        return TableEditorUndoSnapshot(
            templateState = currentTemplate,
            styleState = tableStyleUi,
            selectedCellId = selectedCellId,
            structureSelectedCellIds = structureSelectedCellIds,
            structureSelectionRange = structureSelectionRange,
        )
    }

    fun pushUndoSnapshotBeforeChange(
        nextTemplate: TableTemplateState = currentTemplate,
        nextStyle: TableStyleState = tableStyleUi,
    ) {
        val currentSnapshot = currentUndoSnapshot()
        val nextSnapshot = currentSnapshot.copy(
            templateState = nextTemplate,
            styleState = nextStyle,
        )
        if (currentSnapshot == nextSnapshot) return
        undoManager.pushSnapshotBeforeAction(currentSnapshot)
        undoRevision += 1
    }

    fun applyUndo() {
        val restored = undoManager.undo(currentUndoSnapshot())
        if (restored == currentUndoSnapshot()) return
        editableTemplateState = restored.templateState
        tableStyleUi = restored.styleState
        selectedCellId = restored.selectedCellId
        structureSelectedCellIds = restored.structureSelectedCellIds
        structureSelectionRange = restored.structureSelectionRange
        undoRevision += 1
    }

    fun applyTemplateWithUndo(nextTemplate: TableTemplateState) {
        if (nextTemplate == currentTemplate) return
        pushUndoSnapshotBeforeChange(nextTemplate = nextTemplate)
        updateTemplateDraft(nextTemplate)
    }

    fun applyStyleWithUndo(nextStyle: TableStyleState) {
        if (nextStyle == tableStyleUi) return
        pushUndoSnapshotBeforeChange(nextStyle = nextStyle)
        tableStyleUi = nextStyle
    }

    LaunchedEffect(Unit) {
        runCatching {
            watermarkUi = loadTablePlacementState(context)
            val loadedStyle = loadTableStyleState(context)
            tableStyleUi = loadedStyle
            initialStyleSnapshot = loadedStyle
        }
    }

    // 주요 정책: 표 상세설정 상단 프리뷰는 실제 촬영/저장과 동일한 previewPipeline 결과를 그대로 사용한다.
    val rawFilenamePreview = previewPipeline.previewNaming.displayName
    val filenamePreview = if (counterUi.isScopeCounterSynced || counterUi.manualSeedOverride != null) {
        rawFilenamePreview
    } else {
        hideTrailingCounterToken(rawFilenamePreview)
    }
    val savePathPreview = previewPipeline.previewNaming.relativePath

    fun handleCounterConflictDialogEffect(effect: com.dudoziworkshop.dzlog.feature.table.policy.TableCounterConflictDialogEffect) {
        applyCounterConflictDialogEffect(
            effect = effect,
            templateState = currentTemplate,
            counterRequest = counterRequest,
            counterFacade = counterFacade,
            counterUi = counterUi,
            onTemplateChange = ::updateTemplateDraft,
            setCounterUi = { counterUi = it },
            updateCell = ::updateCell,
            scope = scope
        )
    }

    if (counterUi.counterConflictDialogState.isVisible) {
        AlertDialog(
            containerColor = DDZColor.Surface,
            onDismissRequest = {
                val (nextState, effect) = dismissCounterConflictDialog(counterUi.counterConflictDialogState)
                counterUi = updateCounterUiConflictDialogState(counterUi, nextState)
                handleCounterConflictDialogEffect(effect)
                clearInlineEditingState()
            },
            title = { Text("카운터 충돌 경고", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary) },
            text = {
                Text(
                    text = "중복된 카운터가 발생할 수 있습니다. 계속 진행하시겠습니까?\n\n" +
                        "입력값: ${counterUi.counterConflictDialogState.pendingCounterCommitValue}\n" +
                        "현재 스트림 next: ${counterUi.counterConflictDialogState.pendingCounterStreamNextValue}",
                    style = DDZTypography.Body,
                    color = DDZColor.TextPrimary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val (nextState, effect) = confirmCounterConflictDialog(counterUi.counterConflictDialogState)
                    counterUi = updateCounterUiConflictDialogState(counterUi, nextState)
                    handleCounterConflictDialogEffect(effect)
                    clearInlineEditingState()
                }) { Text("진행", style = DDZTypography.ButtonText, color = DDZColor.Primary) }
            },
            dismissButton = {
                TextButton(onClick = {
                    val (nextState, effect) = dismissCounterConflictDialog(counterUi.counterConflictDialogState)
                    counterUi = updateCounterUiConflictDialogState(counterUi, nextState)
                    handleCounterConflictDialogEffect(effect)
                    clearInlineEditingState()
                }) {
                    Text("취소", style = DDZTypography.ButtonText, color = DDZColor.Primary)
                }
            }
        )
    }

    TableFormatDialog(
        state = formatDialog,
        templateState = currentTemplate,
        onTemplateChange = ::updateTemplateDraft,
        onClose = { formatDialog = formatDialog.close() },
        dateFormatOptions = dateFormatOptions
    )

    if (rotatingUi.isTemplateDialogOpen) {
        val dialogCell = currentTemplate.cells.firstOrNull { it.cellId == rotatingUi.templateDialogCellId }
        if (dialogCell == null || dialogCell.dataType != TableCellDataType.ROTATING_TEXT) {
            closeRotatingPhraseTemplateDialog()
        } else {
            RotatingPhraseTemplateDialog(
                cell = dialogCell,
                phraseSets = currentTemplate.phraseSets,
                onDismiss = { closeRotatingPhraseTemplateDialog() },
                onRestore = {
                    rotatingUi.templateDialogRestore?.let { updateTemplateDraft(it) }
                    closeRotatingPhraseTemplateDialog()
                },
                onSelectSet = { phraseSetId ->
                    val updated = updateCell(currentTemplate, dialogCell.cellId) { current ->
                        if (phraseSetId == null) {
                            current.copy(phraseSetId = null, everyOverride = null)
                        } else {
                            current.copy(phraseSetId = phraseSetId)
                        }
                    }
                    updateTemplateDraft(updated)
                },
                onEveryChange = { every ->
                    val updated = updateCell(currentTemplate, dialogCell.cellId) { current ->
                        current.copy(everyOverride = every.coerceAtLeast(1))
                    }
                    updateTemplateDraft(updated)
                },
                onIncreaseEvery = {
                    val selectedSet = currentTemplate.phraseSets.firstOrNull { it.id == dialogCell.phraseSetId }
                    val currentEvery = dialogCell.everyOverride ?: selectedSet?.defaultEvery ?: 1
                    val nextEvery = (currentEvery + 1).coerceAtLeast(1)
                    val updated = updateCell(currentTemplate, dialogCell.cellId) { current ->
                        current.copy(everyOverride = nextEvery)
                    }
                    updateTemplateDraft(updated)
                },
                onDecreaseEvery = {
                    val selectedSet = currentTemplate.phraseSets.firstOrNull { it.id == dialogCell.phraseSetId }
                    val currentEvery = dialogCell.everyOverride ?: selectedSet?.defaultEvery ?: 1
                    val nextEvery = (currentEvery - 1).coerceAtLeast(1)
                    val updated = updateCell(currentTemplate, dialogCell.cellId) { current ->
                        current.copy(everyOverride = nextEvery)
                    }
                    updateTemplateDraft(updated)
                },
                onRequestCreateSet = {
                    rotatingUi = rotatingUi.copy(isCreateSetDialogOpen = true, createSetName = "")
                },
                onRequestDeleteSet = { phraseSetId ->
                    rotatingUi = rotatingUi.copy(pendingDeleteSetId = phraseSetId)
                },
                onRequestEditSet = { phraseSetId ->
                    rotatingUi = rotatingUi.copy(isSetEditDialogOpen = true, editingSetId = phraseSetId)
                }
            )
        }
    }

    if (rotatingUi.isSetEditDialogOpen) {
        val editingSet = currentTemplate.phraseSets.firstOrNull { it.id == rotatingUi.editingSetId }
        if (editingSet == null) {
            rotatingUi = rotatingUi.copy(isSetEditDialogOpen = false, editingSetId = null)
        } else {
            RotatingPhraseSetEditDialog(
                phraseSet = editingSet,
                onClose = {
                    rotatingUi = rotatingUi.copy(isSetEditDialogOpen = false, editingSetId = null)
                },
                onUpdateSet = { transform ->
                    val updated = currentTemplate.copy(
                        phraseSets = currentTemplate.phraseSets.map { set ->
                            if (set.id == editingSet.id) transform(set) else set
                        }
                    )
                    updateTemplateDraft(updated)
                },
                onDeleteSet = { deleteId ->
                    val updated = currentTemplate.copy(
                        phraseSets = currentTemplate.phraseSets.filterNot { it.id == deleteId },
                        cells = currentTemplate.cells.map { cell ->
                            if (cell.phraseSetId == deleteId) {
                                cell.copy(phraseSetId = null, everyOverride = null)
                            } else {
                                cell
                            }
                        }
                    )
                    updateTemplateDraft(updated)
                    rotatingUi = rotatingUi.copy(isSetEditDialogOpen = false, editingSetId = null)
                }
            )
        }
    }

    if (rotatingUi.isCreateSetDialogOpen) {
        AlertDialog(
            containerColor = DDZColor.Surface,
            onDismissRequest = { rotatingUi = rotatingUi.copy(isCreateSetDialogOpen = false, createSetName = "") },
            title = { Text("새 템플릿 추가", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary) },
            text = {
                OutlinedTextField(
                    value = rotatingUi.createSetName,
                    onValueChange = { rotatingUi = rotatingUi.copy(createSetName = it) },
                    singleLine = true,
                    label = { Text("세트 이름") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = rotatingUi.createSetName.trim()
                    if (name.isNotEmpty() && rotatingUi.templateDialogCellId != null) {
                        val created = RotatingPhraseSet(
                            id = UUID.randomUUID().toString(),
                            name = name,
                            items = emptyList(),
                            defaultEvery = 1
                        )
                        val updatedTemplate = currentTemplate.copy(
                            phraseSets = currentTemplate.phraseSets + created,
                            cells = currentTemplate.cells.map { cell ->
                                if (cell.cellId == rotatingUi.templateDialogCellId) {
                                    cell.copy(phraseSetId = created.id, everyOverride = null)
                                } else {
                                    cell
                                }
                            }
                        )
                        updateTemplateDraft(updatedTemplate)
                    }
                    rotatingUi = rotatingUi.copy(isCreateSetDialogOpen = false, createSetName = "")
                }) {
                    Text("추가", style = DDZTypography.ButtonText, color = DDZColor.Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { rotatingUi = rotatingUi.copy(isCreateSetDialogOpen = false, createSetName = "") }) {
                    Text("취소", style = DDZTypography.ButtonText, color = DDZColor.Primary)
                }
            }
        )
    }

    rotatingUi.pendingDeleteSetId?.let { deleteId ->
        val deleteTarget = currentTemplate.phraseSets.firstOrNull { it.id == deleteId }
        if (deleteTarget != null) {
            AlertDialog(
                containerColor = DDZColor.Surface,
                onDismissRequest = { rotatingUi = rotatingUi.copy(pendingDeleteSetId = null) },
                title = { Text("세트 삭제", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary) },
                text = {
                    Text(
                        "${deleteTarget.name} 세트를 삭제하시겠습니까?",
                        style = DDZTypography.Body,
                        color = DDZColor.TextPrimary
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val updated = currentTemplate.copy(
                            phraseSets = currentTemplate.phraseSets.filterNot { it.id == deleteId },
                            cells = currentTemplate.cells.map { cell ->
                                if (cell.phraseSetId == deleteId) {
                                    cell.copy(phraseSetId = null, everyOverride = null)
                                } else {
                                    cell
                                }
                            }
                        )
                        updateTemplateDraft(updated)
                        rotatingUi = rotatingUi.copy(pendingDeleteSetId = null)
                    }) {
                        Text("삭제", style = DDZTypography.ButtonText, color = DDZColor.Primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { rotatingUi = rotatingUi.copy(pendingDeleteSetId = null) }) {
                        Text("취소", style = DDZTypography.ButtonText, color = DDZColor.Primary)
                    }
                }
            )
        } else {
            rotatingUi = rotatingUi.copy(pendingDeleteSetId = null)
        }
    }

    // ✅ 탭 전환: 편집 중이면 먼저 commit (중복 다이얼로그 등으로 commit이 보류되면 탭 전환 막기)
    fun requestTabSwitch(targetIndex: Int) {
        if (selectedTabIndex == targetIndex) return

        if (inlineEdit.isEditing()) {
            commitInlineEditIfNeeded()
            // commit이 보류되면(=editingCellId가 유지됨) 전환 막음
            if (shouldBlockTabSwitchAfterCommit(inlineEdit)) return
        }
        selectedTabIndex = targetIndex
    }

    // 주요 정책: 셀 선택 전에는 inline 값을 항상 먼저 commit 시도해 유실을 막는다.
    fun requestSelectCell(cellId: String?) {
        if (selectedCellId == cellId && bottomPanelMode != BottomEditorPanelMode.STRUCTURE_EDIT) return
        if (inlineEdit.isEditing()) {
            commitInlineEditIfNeeded()
            if (shouldBlockTabSwitchAfterCommit(inlineEdit)) return
        }
        snapshotCellForEditSession(cellId)
        selectedCellId = cellId
        if (bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT) {
            val selected = cellId?.let { setOf(it) } ?: emptySet()
            structureSelectedCellIds = selected
            structureSelectionRange = TableSelectionResolver.rangeFromSelection(currentTemplate.cells, selected)
        }
    }

    // 주요 정책: 패널 모드 변경 전에도 inline commit을 우선 보장한다.
    fun requestBottomPanelModeChange(nextMode: BottomEditorPanelMode) {
        if (bottomPanelMode == nextMode) return
        if (inlineEdit.isEditing()) {
            commitInlineEditIfNeeded()
            if (shouldBlockTabSwitchAfterCommit(inlineEdit)) return
        }

        // 정책 보강: 각 편집 모드를 벗어날 때 해당 임시 상태를 분리 정리한다.
        if (nextMode != BottomEditorPanelMode.FILENAME_EDIT) {
            clearFileNameEditorTransientState(clearDraft = true)
        }
        if (nextMode != BottomEditorPanelMode.PATH_EDIT) {
            clearPathEditorTransientState(clearDraft = true)
        }
        val wasStructureMode = bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT
        bottomPanelMode = nextMode
        if (nextMode == BottomEditorPanelMode.STRUCTURE_EDIT || wasStructureMode) {
            structureSelectedCellIds = emptySet()
            structureSelectionRange = null
            selectedCellId = null
        }
    }

    fun requestSaveSelectedCell() {
        commitInlineEditIfNeeded()
        if (shouldBlockTabSwitchAfterCommit(inlineEdit)) return
    }

    fun requestRevertSelectedCell() {
        val snapshot = editSessionOriginalCellState ?: return
        val selectedId = selectedCellId ?: return
        if (snapshot.cellId != selectedId) return
        val updated = updateCell(currentTemplate, snapshot.cellId) { snapshot }
        updateTemplateDraft(updated)
        clearInlineEditingState()
    }

    val hasUnsavedChanges by remember(
        currentTemplate,
        initialTemplateSnapshot,
        tableStyleUi,
        initialStyleSnapshot,
        inlineEdit,
        manualInputDraft,
        pathManualInputDraft
    ) {
        derivedStateOf {
            currentTemplate != initialTemplateSnapshot ||
                tableStyleUi != initialStyleSnapshot ||
                inlineEdit.isEditing() ||
                manualInputDraft.isNotBlank() ||
                pathManualInputDraft.isNotBlank()
        }
    }
    val isUndoAvailable by remember(undoRevision) {
        derivedStateOf { undoManager.canUndo() }
    }

    fun saveTemplate(exitAfterSave: Boolean = false) {
        commitInlineEditIfNeeded()
        if (shouldBlockTabSwitchAfterCommit(inlineEdit)) return
        isSavingTemplate = true
        val savePayload = editableTemplateState
        val stylePayload = tableStyleUi
        scope.launch {
            val templateSaveResult = saveTableTemplate(context, savePayload)
            if (templateSaveResult.isFailure) {
                Toast.makeText(
                    context,
                    "저장 실패: ${templateSaveResult.exceptionOrNull()?.message}",
                    Toast.LENGTH_SHORT
                ).show()
                isSavingTemplate = false
                return@launch
            }

            val styleSaveResult = runCatching { persistTableStyleState(context, stylePayload) }
            if (styleSaveResult.isFailure) {
                // 정책 보강: 스타일 저장 실패 시 템플릿 저장 롤백을 시도해 부분 성공 방치를 줄인다.
                val rollbackResult = saveTableTemplate(context, initialTemplateSnapshot)
                val rollbackSuffix = if (rollbackResult.isFailure) {
                    " (롤백 실패: ${rollbackResult.exceptionOrNull()?.message})"
                } else {
                    ""
                }
                Toast.makeText(
                    context,
                    "서식 저장 실패로 저장을 취소했습니다: ${styleSaveResult.exceptionOrNull()?.message}${rollbackSuffix}",
                    Toast.LENGTH_LONG
                ).show()
                isSavingTemplate = false
                return@launch
            }

            initialTemplateSnapshot = savePayload
            initialStyleSnapshot = stylePayload
            undoManager.clear()
            undoRevision += 1
            Toast.makeText(context, "저장됨", Toast.LENGTH_SHORT).show()
            onTemplateChange(savePayload)
            isSavingTemplate = false
            if (exitAfterSave) {
                onBack()
            }
        }
    }

    fun requestNavigateBack() {
        if (hasUnsavedChanges) {
            showUnsavedChangesDialog = true
        } else {
            undoManager.clear()
            undoRevision += 1
            onBack()
        }
    }

    fun requestCloseBottomPanelToNone() {
        // 정책 보강: X 닫기에서도 탭 전환과 동일하게 inline commit을 우선 시도해 값 유실을 막는다.
        if (inlineEdit.isEditing()) {
            commitInlineEditIfNeeded()
            if (shouldBlockTabSwitchAfterCommit(inlineEdit)) return
        }

        bottomPanelMode = BottomEditorPanelMode.NONE
        showCellSettingsPanel = false

        currentlySelectedFileNameSlot = null
        currentlySelectedPathSlot = null
        clearFileNameEditorTransientState(clearDraft = true)
        clearPathEditorTransientState(clearDraft = true)
    }

    BackHandler { requestNavigateBack() }

    if (showUnsavedChangesDialog) {
        AlertDialog(
            containerColor = DDZColor.Surface,
            onDismissRequest = { showUnsavedChangesDialog = false },
            title = {
                Text("저장되지 않은 변경사항", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary)
            },
            text = {
                Text("변경사항을 저장하시겠습니까?", style = DDZTypography.Body, color = DDZColor.TextPrimary)
            },
            confirmButton = {
                // 주요 정책 변경: 채워진 버튼 3등분 UI를 제거하고 일반 팝업 액션처럼 텍스트 버튼 3개를 통일 적용한다.
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = {
                        showUnsavedChangesDialog = false
                        saveTemplate(exitAfterSave = true)
                    }) {
                        Text("저장", style = DDZTypography.ButtonText, color = DDZColor.Primary)
                    }
                    TextButton(onClick = {
                        showUnsavedChangesDialog = false
                        undoManager.clear()
                        undoRevision += 1
                        onBack()
                    }) {
                        Text("저장안함", style = DDZTypography.ButtonText, color = DDZColor.Primary)
                    }
                    TextButton(onClick = { showUnsavedChangesDialog = false }) {
                        Text("취소", style = DDZTypography.ButtonText, color = DDZColor.Primary)
                    }
                }
            },
            dismissButton = {}
        )
    }

    Scaffold(
        containerColor = DDZColor.Background,
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier,
                title = {
                    Text(
                        "표 상세설정",
                        style = DDZTypography.ScreenTitle,
                        color = DDZColor.Primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { requestNavigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = DDZColor.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DDZColor.Background,
                    navigationIconContentColor = DDZColor.Primary,
                    titleContentColor = DDZColor.Primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .dzScaffoldContent()
                .padding(innerPadding)
        ) {
            when (selectedTabIndex) {
                0 -> {
                    LayoutTabContent(
                        uiState = LayoutTabUiState(
                            savePathPreview = savePathPreview,
                            filenamePreview = filenamePreview,
                            counterModeLabel = if (isManualCounterModeDisplay) "수동" else "자동",
                            templateState = currentTemplate,
                            plan = plan,
                            resolvedByCellId = resolvedByCellId,
                            previewNow = previewNow,
                            previewCounterDigits = previewCounterDigits,
                            scopeNextCounter = counterUi.scopeNextCounter,
                            phraseProgressCursor = phraseProgressCounter,
                            dateFormat = dateFormat,
                            timeFormat = timeFormat,
                            selectedCellId = selectedCellId,
                            editingCellId = inlineEdit.editingCellId,
                            editingValue = inlineEdit.editingValue,
                            bottomPanelMode = bottomPanelMode,
                            currentlySelectedFileNameSlot = currentlySelectedFileNameSlot,
                            currentlySelectedPathSlot = currentlySelectedPathSlot,
                            fileNameSlotItems = fileNameSlotItems,
                            isFileNameCellPickMode = isFileNameCellPickMode,
                            manualInputDraft = manualInputDraft,
                            showManualInputEditor = showManualInputEditor,
                            pathSlotItems = pathSlotItems,
                            isPathCellPickMode = isPathCellPickMode,
                            showPathManualInputEditor = showPathManualInputEditor,
                            pathManualInputDraft = pathManualInputDraft,
                            showCellSettingsPanel = showCellSettingsPanel,
                            selectedCell = selectedCell,
                            hasGroup1 = hasGroup1,
                            hasGroup2 = hasGroup2,
                            isSavingTemplate = isSavingTemplate,
                            autoNextCounterValue = counterUi.autoNextCounterValue,
                            phraseSets = currentTemplate.phraseSets,
                            captureAspect = watermarkUi.captureAspect,
                            wmWidthRatio = watermarkUi.wmWidthRatio,
                            wmHeightRatio = watermarkUi.wmHeightRatio,
                            wmBgStyle = tableStyleUi.bgStyle,
                            wmBgAlpha = tableStyleUi.bgAlpha,
                            wmGridEnabled = tableStyleUi.gridEnabled,
                            wmTextColorMode = tableStyleUi.textColorMode,
                            wmManualTextColor = tableStyleUi.manualTextColor,
                            wmValueScale = tableStyleUi.valueScale,
                            wmTextAlign = tableStyleUi.textAlign,
                            structureSelectedCellIds = structureSelectedCellIds,
                            isUndoAvailable = isUndoAvailable,
                        ),
                        actions = LayoutTabActions(
                            onSelectCellId = ::requestSelectCell,
                            onSelectStructureRange = { startId, endId ->
                                if (bottomPanelMode != BottomEditorPanelMode.STRUCTURE_EDIT) return@LayoutTabActions
                                val result = TableSelectionResolver.selectByDrag(currentTemplate.cells, startId, endId)
                                if (result.range != null) {
                                    structureSelectedCellIds = result.selectedCellIds
                                    structureSelectionRange = result.range
                                    selectedCellId = result.lastSelectedCellId
                                }
                            },
                            onChangeBottomPanelMode = ::requestBottomPanelModeChange,
                            onCloseBottomPanel = ::requestCloseBottomPanelToNone,
                            onShowCellSettingsPanel = { showCellSettingsPanel = it },
                            onSelectFileNameSlot = { slotIndex ->
                                currentlySelectedFileNameSlot = slotIndex
                                // 정책 보강: 슬롯 전환 시 이전 슬롯 보조 UI 상태를 모두 정리한다.
                                clearFileNameEditorTransientState(clearDraft = true)
                            },
                            onFillEmptyFileNameSlot = { slotIndex ->
                                val normalized = normalizeFileNameDraftSlots(fileNameSlotItems)
                                val firstEmptyIndex = normalized.indexOfFirst { it == null }
                                if (firstEmptyIndex < 0) {
                                    currentlySelectedFileNameSlot = slotIndex
                                } else {
                                    // 정책 변경: 어떤 '+'를 눌러도 항상 가장 앞의 빈 슬롯부터 채워
                                    // 파일명 슬롯 상태를 [A, B, C, null...] 형태로 연속 유지한다.
                                    val next = normalized.toMutableList().apply {
                                        this[firstEmptyIndex] = FileNameSlotUiItem(
                                            kind = FileNameSlotKind.CELL,
                                            label = "셀"
                                        )
                                    }
                                    updateFileNameSlotDraft(currentTemplate, next)
                                    currentlySelectedFileNameSlot = firstEmptyIndex
                                    isFileNameCellPickMode = false
                                    showManualInputEditor = false
                                }
                            },
                            onMoveSelectedFileNameSlotLeft = {
                                isFileNameCellPickMode = false
                                showManualInputEditor = false
                                val selected = currentlySelectedFileNameSlot
                                if (selected != null) {
                                    val normalized = normalizeFileNameDraftSlots(fileNameSlotItems)
                                    val target = selected - 1
                                    if (target >= 0 && normalized[selected] != null && normalized[target] != null) {
                                        updateFileNameSlotDraft(currentTemplate, moveFileNameSlot(normalized, selected, target))
                                        currentlySelectedFileNameSlot = target
                                    }
                                }
                            },
                            onMoveSelectedFileNameSlotRight = {
                                isFileNameCellPickMode = false
                                showManualInputEditor = false
                                val selected = currentlySelectedFileNameSlot
                                if (selected != null) {
                                    val normalized = normalizeFileNameDraftSlots(fileNameSlotItems)
                                    val target = selected + 1
                                    if (target < normalized.size && normalized[selected] != null && normalized[target] != null) {
                                        updateFileNameSlotDraft(currentTemplate, moveFileNameSlot(normalized, selected, target))
                                        currentlySelectedFileNameSlot = target
                                    }
                                }
                            },
                            onDeleteSelectedFileNameSlot = {
                                isFileNameCellPickMode = false
                                showManualInputEditor = false
                                val selected = currentlySelectedFileNameSlot
                                if (selected != null) {
                                    val normalized = normalizeFileNameDraftSlots(fileNameSlotItems)
                                    if (normalized.getOrNull(selected) != null) {
                                        val next = removeFileNameSlotAt(normalized, selected)
                                        updateFileNameSlotDraft(currentTemplate, next)
                                        val nextFilledIndex = next.indexOfFirst { it != null }.takeIf { it >= 0 }
                                        currentlySelectedFileNameSlot = nextFilledIndex
                                    }
                                }
                            },
                            onStartFileNameCellPick = {
                                if (currentlySelectedFileNameSlot != null) {
                                    isFileNameCellPickMode = true
                                    showManualInputEditor = false
                                }
                            },
                            onStartManualInputEditor = {
                                val selected = currentlySelectedFileNameSlot
                                if (selected != null) {
                                    val current = normalizeFileNameDraftSlots(fileNameSlotItems).getOrNull(selected)
                                    manualInputDraft = current?.manualText ?: current?.label.orEmpty()
                                    isFileNameCellPickMode = false
                                    showManualInputEditor = true
                                }
                            },
                            onManualInputDraftChange = {
                                manualInputDraft = it
                            },
                            onApplyManualInput = {
                                val selected = currentlySelectedFileNameSlot
                                val trimmed = manualInputDraft.trim()
                                if (selected != null && trimmed.isNotEmpty()) {
                                    val normalized = normalizeFileNameDraftSlots(fileNameSlotItems)
                                    val next = normalized.toMutableList().apply {
                                        this[selected] = FileNameSlotUiItem(
                                            kind = FileNameSlotKind.MANUAL,
                                            label = trimmed,
                                            manualText = trimmed
                                        )
                                    }
                                    updateFileNameSlotDraft(currentTemplate, next)
                                    showManualInputEditor = false
                                    isFileNameCellPickMode = false
                                }
                            },
                            onBindSelectedSlotToCell = { cellId ->
                                val selected = currentlySelectedFileNameSlot
                                if (selected != null) {
                                    val cellLabel = currentTemplate.cells.firstOrNull { it.cellId == cellId }?.let { cell ->
                                        resolvedByCellId[cell.cellId]?.takeIf { it.isNotBlank() }
                                            ?: "셀(${cell.rowIndex + 1},${cell.colIndex + 1})"
                                    } ?: "셀"
                                    val normalized = normalizeFileNameDraftSlots(fileNameSlotItems)
                                    val next = normalized.toMutableList().apply {
                                        this[selected] = FileNameSlotUiItem(
                                            kind = FileNameSlotKind.CELL,
                                            label = cellLabel,
                                            cellId = cellId
                                        )
                                    }
                                    updateFileNameSlotDraft(currentTemplate, next)
                                    clearFileNameEditorTransientState(clearDraft = true)
                                }
                            },
                            onSelectPathSlot = { slotIndex ->
                                currentlySelectedPathSlot = slotIndex
                                clearPathEditorTransientState(clearDraft = true)
                            },
                            onFillEmptyPathSlot = { slotIndex ->
                                val normalized = normalizePathDraftSlots(pathSlotItems)
                                val firstEmptyIndex = normalized.indexOfFirst { it == null }
                                if (firstEmptyIndex < 0) {
                                    currentlySelectedPathSlot = slotIndex
                                } else {
                                    val next = normalized.toMutableList().apply {
                                        this[firstEmptyIndex] = PathSlotUiItem(
                                            kind = PathSlotKind.CELL,
                                            label = "셀"
                                        )
                                    }
                                    updatePathSlotDraft(currentTemplate, next)
                                    currentlySelectedPathSlot = firstEmptyIndex
                                    clearPathEditorTransientState(clearDraft = true)
                                }
                            },
                            onMoveSelectedPathSlotLeft = {
                                clearPathEditorTransientState(clearDraft = false)
                                val selected = currentlySelectedPathSlot
                                if (selected != null) {
                                    val normalized = normalizePathDraftSlots(pathSlotItems)
                                    val target = selected - 1
                                    if (target >= 0 && normalized[selected] != null && normalized[target] != null) {
                                        updatePathSlotDraft(currentTemplate, movePathSlot(normalized, selected, target))
                                        currentlySelectedPathSlot = target
                                    }
                                }
                            },
                            onMoveSelectedPathSlotRight = {
                                clearPathEditorTransientState(clearDraft = false)
                                val selected = currentlySelectedPathSlot
                                if (selected != null) {
                                    val normalized = normalizePathDraftSlots(pathSlotItems)
                                    val target = selected + 1
                                    if (target < normalized.size && normalized[selected] != null && normalized[target] != null) {
                                        updatePathSlotDraft(currentTemplate, movePathSlot(normalized, selected, target))
                                        currentlySelectedPathSlot = target
                                    }
                                }
                            },
                            onDeleteSelectedPathSlot = {
                                clearPathEditorTransientState(clearDraft = true)
                                val selected = currentlySelectedPathSlot
                                if (selected != null) {
                                    val normalized = normalizePathDraftSlots(pathSlotItems)
                                    if (normalized.getOrNull(selected) != null) {
                                        val next = removePathSlotAt(normalized, selected)
                                        updatePathSlotDraft(currentTemplate, next)
                                        val nextFilledIndex = next.indexOfFirst { it != null }.takeIf { it >= 0 }
                                        currentlySelectedPathSlot = nextFilledIndex
                                    }
                                }
                            },
                            onStartPathCellPick = {
                                if (currentlySelectedPathSlot != null) {
                                    isPathCellPickMode = true
                                    showPathManualInputEditor = false
                                }
                            },
                            onStartPathManualInputEditor = {
                                val selected = currentlySelectedPathSlot
                                if (selected != null) {
                                    val current = normalizePathDraftSlots(pathSlotItems).getOrNull(selected)
                                    pathManualInputDraft = current?.manualText ?: current?.label.orEmpty()
                                    isPathCellPickMode = false
                                    showPathManualInputEditor = true
                                }
                            },
                            onPathManualInputDraftChange = {
                                pathManualInputDraft = it
                            },
                            onApplyPathManualInput = {
                                val selected = currentlySelectedPathSlot
                                val trimmed = pathManualInputDraft.trim()
                                if (selected != null && trimmed.isNotEmpty()) {
                                    val normalized = normalizePathDraftSlots(pathSlotItems)
                                    val next = normalized.toMutableList().apply {
                                        this[selected] = PathSlotUiItem(
                                            kind = PathSlotKind.MANUAL,
                                            label = trimmed,
                                            manualText = trimmed
                                        )
                                    }
                                    updatePathSlotDraft(currentTemplate, next)
                                    clearPathEditorTransientState(clearDraft = true)
                                }
                            },
                            onBindSelectedPathSlotToCell = { cellId ->
                                val selected = currentlySelectedPathSlot
                                if (selected != null) {
                                    val cellLabel = currentTemplate.cells.firstOrNull { it.cellId == cellId }?.let { cell ->
                                        resolvedByCellId[cell.cellId]?.takeIf { it.isNotBlank() }
                                            ?: "셀(${cell.rowIndex + 1},${cell.colIndex + 1})"
                                    } ?: "셀"
                                    val normalized = normalizePathDraftSlots(pathSlotItems)
                                    val next = normalized.toMutableList().apply {
                                        this[selected] = PathSlotUiItem(
                                            kind = PathSlotKind.CELL,
                                            label = cellLabel,
                                            cellId = cellId
                                        )
                                    }
                                    updatePathSlotDraft(currentTemplate, next)
                                    clearPathEditorTransientState(clearDraft = true)
                                }
                            },
                            onStartInlineEditing = { cellId, value ->
                                inlineEdit = startInlineEditing(inlineEdit, cellId, value)
                            },
                            onOpenFormatDialog = { cellId, type ->
                                formatDialog = formatDialog.open(cellId, type)
                            },
                            onEditingValueChange = { inlineEdit = inlineEdit.copy(editingValue = it) },
                            onCommitInline = ::commitInlineEditIfNeeded,
                            onTryCommitInlineAndContinue = {
                                commitInlineEditIfNeeded()
                                !inlineEdit.isEditing()
                            },
                            onAddRow = {
                                val restored = deletedRowsStack.lastOrNull()
                                val nextTemplate = if (restored != null) {
                                    deletedRowsStack.removeAt(deletedRowsStack.lastIndex)
                                    val newRowIndex = currentTemplate.rows
                                    val restoredReindexed = restored.cells.map { it.copy(rowIndex = newRowIndex) }
                                    val baseRowWeights = currentTemplate.rowWeights
                                        ?: List(currentTemplate.rows.coerceAtLeast(1)) { 1f }
                                    var next = sanitizePathGroupAfterStructureChange(
                                        currentTemplate.copy(
                                            rows = currentTemplate.rows + 1,
                                            cells = currentTemplate.cells + restoredReindexed,
                                            rowWeights = baseRowWeights + 1f
                                        )
                                    )

                                    // 정책: 삭제 이후 슬롯 수정이 없으면 삭제 직전 슬롯 snapshot까지 함께 복원한다.
                                    if (!fileNameSlotsDirtySinceStructureChange) {
                                        next = withUpdatedFileNameSlots(next, restored.fileNameSlotsSnapshot)
                                    }
                                    if (!pathSlotsDirtySinceStructureChange) {
                                        next = withUpdatedPathSlots(next, restored.pathSlotsSnapshot)
                                    }
                                    next
                                } else {
                                    sanitizePathGroupAfterStructureChange(if (bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT) addRowBySelection(currentTemplate, structureSelectionRange) else addRow(currentTemplate))
                                }
                                applyTemplateWithUndo(nextTemplate)
                                fileNameSlotsDirtySinceStructureChange = false
                                pathSlotsDirtySinceStructureChange = false
                                if (bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT) {
                                    val maxRow = (nextTemplate.rows - 1).coerceAtLeast(0)
                                    val maxCol = (nextTemplate.cols - 1).coerceAtLeast(0)
                                    val range = structureSelectionRange
                                    if (range != null) {
                                        val bounded = TableSelectionRange(
                                            minRow = range.minRow.coerceIn(0, maxRow),
                                            maxRow = range.maxRow.coerceIn(0, maxRow),
                                            minCol = range.minCol.coerceIn(0, maxCol),
                                            maxCol = range.maxCol.coerceIn(0, maxCol),
                                        )
                                        structureSelectionRange = bounded
                                        structureSelectedCellIds = nextTemplate.cells.filter { bounded.contains(it.rowIndex, it.colIndex) }.map { it.cellId }.toSet()
                                    }
                                }
                            },
                            onRemoveRow = {
                                val deletedRowRange = if (bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT) {
                                    structureSelectionRange?.let { it.minRow..it.maxRow }
                                } else {
                                    val lastRowIndex = currentTemplate.rows - 1
                                    lastRowIndex..lastRowIndex
                                }
                                val deletedRowCells = currentTemplate.cells
                                    .filter { cell -> deletedRowRange?.contains(cell.rowIndex) == true }
                                    .sortedWith(compareBy({ it.rowIndex }, { it.colIndex }))

                                val deletedCellIds = deletedRowCells.map { it.cellId }.toSet()
                                if (deletedRowCells.isNotEmpty()) {
                                    deletedRowsStack.add(
                                        DeletedStructureSnapshot(
                                            cells = deletedRowCells,
                                            fileNameSlotsSnapshot = fileNameSlotItems,
                                            pathSlotsSnapshot = pathSlotItems
                                        )
                                    )
                                }

                                // 정책: 구조 삭제 + 슬롯 정리를 하나의 템플릿으로 순차 가공 후 단일 update로 반영한다.
                                var nextTemplate = sanitizePathGroupAfterStructureChange(if (bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT) removeRowBySelection(currentTemplate, structureSelectionRange) else removeRow(currentTemplate))
                                nextTemplate = withUpdatedFileNameSlots(
                                    nextTemplate,
                                    removeCellRefsFromFileNameSlots(fileNameSlotItems, deletedCellIds)
                                )
                                nextTemplate = withUpdatedPathSlots(
                                    nextTemplate,
                                    removeCellRefsFromPathSlots(pathSlotItems, deletedCellIds)
                                )
                                applyTemplateWithUndo(nextTemplate)
                                fileNameSlotsDirtySinceStructureChange = false
                                pathSlotsDirtySinceStructureChange = false
                                if (bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT) {
                                    structureSelectedCellIds = emptySet()
                                    structureSelectionRange = null
                                    selectedCellId = null
                                }

                                if (bottomPanelMode != BottomEditorPanelMode.STRUCTURE_EDIT && nextTemplate.cells.none { it.cellId == selectedCellId }) {
                                    selectedCellId = null
                                }
                            },
                            onAddCol = {
                                val restored = deletedColsStack.lastOrNull()
                                val nextTemplate = if (restored != null) {
                                    deletedColsStack.removeAt(deletedColsStack.lastIndex)
                                    val newColIndex = currentTemplate.cols
                                    val restoredReindexed = restored.cells.map { it.copy(colIndex = newColIndex) }
                                    val baseColWeights = currentTemplate.colWeights
                                        ?: List(currentTemplate.cols.coerceAtLeast(1)) { 1f }
                                    var next = sanitizePathGroupAfterStructureChange(
                                        currentTemplate.copy(
                                            cols = currentTemplate.cols + 1,
                                            cells = currentTemplate.cells + restoredReindexed,
                                            colWeights = baseColWeights + 1f
                                        )
                                    )

                                    // 정책: 삭제 이후 슬롯 수정이 없으면 삭제 직전 슬롯 snapshot까지 함께 복원한다.
                                    if (!fileNameSlotsDirtySinceStructureChange) {
                                        next = withUpdatedFileNameSlots(next, restored.fileNameSlotsSnapshot)
                                    }
                                    if (!pathSlotsDirtySinceStructureChange) {
                                        next = withUpdatedPathSlots(next, restored.pathSlotsSnapshot)
                                    }
                                    next
                                } else {
                                    sanitizePathGroupAfterStructureChange(if (bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT) addColumnBySelection(currentTemplate, structureSelectionRange) else addColumn(currentTemplate))
                                }
                                applyTemplateWithUndo(nextTemplate)
                                fileNameSlotsDirtySinceStructureChange = false
                                pathSlotsDirtySinceStructureChange = false
                                if (bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT) {
                                    val maxRow = (nextTemplate.rows - 1).coerceAtLeast(0)
                                    val maxCol = (nextTemplate.cols - 1).coerceAtLeast(0)
                                    val range = structureSelectionRange
                                    if (range != null) {
                                        val bounded = TableSelectionRange(
                                            minRow = range.minRow.coerceIn(0, maxRow),
                                            maxRow = range.maxRow.coerceIn(0, maxRow),
                                            minCol = range.minCol.coerceIn(0, maxCol),
                                            maxCol = range.maxCol.coerceIn(0, maxCol),
                                        )
                                        structureSelectionRange = bounded
                                        structureSelectedCellIds = nextTemplate.cells.filter { bounded.contains(it.rowIndex, it.colIndex) }.map { it.cellId }.toSet()
                                    }
                                }
                            },
                            onRemoveCol = {
                                val deletedColRange = if (bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT) {
                                    structureSelectionRange?.let { it.minCol..it.maxCol }
                                } else {
                                    val lastColIndex = currentTemplate.cols - 1
                                    lastColIndex..lastColIndex
                                }
                                val deletedColCells = currentTemplate.cells
                                    .filter { cell -> deletedColRange?.contains(cell.colIndex) == true }
                                    .sortedWith(compareBy({ it.colIndex }, { it.rowIndex }))

                                val deletedCellIds = deletedColCells.map { it.cellId }.toSet()
                                if (deletedColCells.isNotEmpty()) {
                                    deletedColsStack.add(
                                        DeletedStructureSnapshot(
                                            cells = deletedColCells,
                                            fileNameSlotsSnapshot = fileNameSlotItems,
                                            pathSlotsSnapshot = pathSlotItems
                                        )
                                    )
                                }

                                // 정책: 구조 삭제 + 슬롯 정리를 하나의 템플릿으로 순차 가공 후 단일 update로 반영한다.
                                var nextTemplate = sanitizePathGroupAfterStructureChange(if (bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT) removeColumnBySelection(currentTemplate, structureSelectionRange) else removeColumn(currentTemplate))
                                nextTemplate = withUpdatedFileNameSlots(
                                    nextTemplate,
                                    removeCellRefsFromFileNameSlots(fileNameSlotItems, deletedCellIds)
                                )
                                nextTemplate = withUpdatedPathSlots(
                                    nextTemplate,
                                    removeCellRefsFromPathSlots(pathSlotItems, deletedCellIds)
                                )
                                applyTemplateWithUndo(nextTemplate)
                                fileNameSlotsDirtySinceStructureChange = false
                                pathSlotsDirtySinceStructureChange = false
                                if (bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT) {
                                    structureSelectedCellIds = emptySet()
                                    structureSelectionRange = null
                                    selectedCellId = null
                                }

                                if (bottomPanelMode != BottomEditorPanelMode.STRUCTURE_EDIT && nextTemplate.cells.none { it.cellId == selectedCellId }) {
                                    selectedCellId = null
                                }
                            },
                            onResetRowWeights = {
                                applyTemplateWithUndo(resetRowWeights(currentTemplate))
                            },
                            onResetColumnWeights = {
                                applyTemplateWithUndo(resetColumnWeights(currentTemplate))
                            },
                            onResetAllWeights = {
                                applyTemplateWithUndo(resetColumnWeights(resetRowWeights(currentTemplate)))
                            },
                            onUndo = {
                                applyUndo()
                            },
                            onReset = {
                                // 정책 변경: 초기화는 기본 템플릿이 아니라 "화면 진입 시점(initialTemplateSnapshot)" 복원이다.
                                editableTemplateState = initialTemplateSnapshot
                                tableStyleUi = initialStyleSnapshot
                                selectedCellId = null
                                structureSelectedCellIds = emptySet()
                                structureSelectionRange = null
                                showCellSettingsPanel = false
                                bottomPanelMode = BottomEditorPanelMode.NONE
                                currentlySelectedFileNameSlot = null
                                currentlySelectedPathSlot = null
                                clearFileNameEditorTransientState(clearDraft = true)
                                clearPathEditorTransientState(clearDraft = true)
                                clearInlineEditingState()
                                editSessionOriginalCellState = null
                                deletedRowsStack.clear()
                                deletedColsStack.clear()
                                fileNameSlotsDirtySinceStructureChange = false
                                pathSlotsDirtySinceStructureChange = false
                                undoManager.clear()
                                undoRevision += 1
                            },
                            onSave = { saveTemplate(exitAfterSave = false) },
                            onDismissSettingsPanel = {
                                commitInlineEditIfNeeded()
                                showCellSettingsPanel = false
                            },
                            onToggleFileNameForSelected = { cellId, enabled ->
                                // 정책 보정: SSOT(fileNameSlotDrafts 우선) 기준으로 current UI 슬롯을 직접 갱신한다.
                                val currentUiSlots = normalizeFileNameDraftSlots(fileNameSlotItems)
                                val nextUiSlots = if (enabled) {
                                    if (currentUiSlots.any { it?.cellId == cellId }) {
                                        currentUiSlots
                                    } else {
                                        val firstEmpty = currentUiSlots.indexOfFirst { it == null }
                                        if (firstEmpty < 0) {
                                            currentUiSlots
                                        } else {
                                            currentUiSlots.toMutableList().apply {
                                                this[firstEmpty] = FileNameSlotUiItem(
                                                    kind = FileNameSlotKind.CELL,
                                                    label = "셀",
                                                    cellId = cellId
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    normalizeFileNameDraftSlots(
                                        currentUiSlots.filter { slot -> slot != null && slot.cellId != cellId }
                                    )
                                }

                                if (nextUiSlots != currentUiSlots) {
                                    updateTemplateDraft(withUpdatedFileNameSlots(currentTemplate, nextUiSlots))
                                    fileNameSlotsDirtySinceStructureChange = true
                                }
                            },
                            onReorderFileNameSlots = { fromIndex, toIndex ->
                                if (fromIndex != toIndex && fromIndex in 0..2 && toIndex in 0..2) {
                                    // 정책 보정: fileName draft가 아니라 현재 UI 슬롯 순서를 기준으로 재정렬한다.
                                    val currentUiSlots = normalizeFileNameDraftSlots(fileNameSlotItems)
                                    val reordered = currentUiSlots.toMutableList().apply {
                                        val temp = this[fromIndex]
                                        this[fromIndex] = this[toIndex]
                                        this[toIndex] = temp
                                    }
                                    val nextUiSlots = normalizeFileNameDraftSlots(reordered)

                                    if (nextUiSlots != currentUiSlots) {
                                        updateTemplateDraft(withUpdatedFileNameSlots(currentTemplate, nextUiSlots))
                                        fileNameSlotsDirtySinceStructureChange = true
                                    }
                                }
                            },
                            onPathGroupActionForSelected = { action ->
                                selectedCell?.let { cell ->
                                    val updated = applyPathGroupAction(
                                        state = currentTemplate,
                                        targetCellId = cell.cellId,
                                        action = action
                                    )
                                    updateTemplateDraft(updated)
                                }
                            },
                            onSetDataTypeForSelected = { type ->
                                selectedCell?.let { cell ->
                                    val updated = updateCell(currentTemplate, cell.cellId) { c ->
                                        c.withDataType(type)
                                    }
                                    updateTemplateDraft(updated)
                                }
                            },
                            onSetCounterScopeModeForSelected = { mode ->
                                selectedCell?.let { cell ->
                                    if (cell.dataType != TableCellDataType.DATE && cell.dataType != TableCellDataType.TIME) return@let
                                    val updated = updateCell(currentTemplate, cell.cellId) { c ->
                                        c.copy(counterScopeMode = mode)
                                    }
                                    updateTemplateDraft(updated)
                                }
                            },
                            onResetCounterSeedForSelected = {
                                selectedCell?.let { cell ->
                                    if (cell.dataType != TableCellDataType.COUNTER) return@let
                                    val syncedCounterText = counterUi.autoNextCounterValue.toString()
                                    restoreCounterCellToAutoNext(
                                        templateState = currentTemplate,
                                        cellId = cell.cellId,
                                        counterRequest = counterRequest,
                                        counterFacade = counterFacade,
                                        counterUi = counterUi,
                                        onTemplateChange = ::updateTemplateDraft,
                                        setCounterUi = { counterUi = it },
                                        updateCell = ::updateCell,
                                        scope = scope
                                    )
                                    // 주요 정책: COUNTER 동기화 직후 편집 draft가 남아 UI를 덮지 않도록 즉시 동기화한다.
                                    if (inlineEdit.editingCellId == cell.cellId) {
                                        inlineEdit = inlineEdit.copy(editingValue = syncedCounterText)
                                    }
                                    Toast.makeText(context, "카운터를 자동 기준으로 초기화했습니다.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onSetBgStyle = { bgStyle ->
                                applyStyleWithUndo(tableStyleUi.copy(bgStyle = bgStyle.coerceIn(0, 2)))
                            },
                            onSetGridEnabled = { enabled ->
                                applyStyleWithUndo(tableStyleUi.copy(gridEnabled = enabled))
                            },
                            onSetTextColorMode = { mode ->
                                applyStyleWithUndo(tableStyleUi.copy(textColorMode = mode.coerceIn(0, 1)))
                            },
                            onSetManualTextColor = { color ->
                                applyStyleWithUndo(tableStyleUi.copy(manualTextColor = color.coerceIn(0, 1)))
                            },
                            onSetValueScale = { scale ->
                                applyStyleWithUndo(tableStyleUi.copy(valueScale = scale.coerceIn(60, 160)))
                            },
                            onSetTextAlign = { align ->
                                applyStyleWithUndo(tableStyleUi.copy(textAlign = align.coerceIn(0, 2)))
                            },
                            onCommitRowWeightsDragEnd = { nextWeights ->
                                applyTemplateWithUndo(TableHandleOverlay.applyRowWeightDragEnd(currentTemplate, nextWeights))
                            },
                            onCommitColumnWeightsDragEnd = { nextWeights ->
                                applyTemplateWithUndo(TableHandleOverlay.applyColumnWeightDragEnd(currentTemplate, nextWeights))
                            },
                            onOpenRotatingTemplateDialogForSelected = { cellId ->
                                val cell = currentTemplate.cells.firstOrNull { it.cellId == cellId }
                                if (cell != null && cell.dataType == TableCellDataType.ROTATING_TEXT) {
                                    openRotatingPhraseTemplateDialog(cell.cellId)
                                } else {
                                    showCellSettingsPanel = true
                                }
                            },
                            onSaveSelectedCell = ::requestSaveSelectedCell,
                            onRevertSelectedCell = ::requestRevertSelectedCell,
                            onOpenPreview = { requestTabSwitch(1) }
                        )
                    )
                }

                1 -> {
                    // 탭 제거 후 미리보기는 셀 구성 우측 버튼으로 진입한다.
                    TextButton(onClick = { requestTabSwitch(0) }) { Text("셀 구성으로") }
                    PreviewTabContent(
                        scrollState = previewTabScrollState,
                        captureAspect = watermarkUi.captureAspect,
                        templateState = currentTemplate,
                        resolvedCells = plan.resolvedCells,
                        wmAnchor = watermarkUi.wmAnchor,
                        wmOffsetXRatio = watermarkUi.wmOffsetXRatio,
                        wmOffsetYRatio = watermarkUi.wmOffsetYRatio,
                        wmWidthRatio = watermarkUi.wmWidthRatio,
                        wmHeightRatio = watermarkUi.wmHeightRatio,
                        wmBgStyle = tableStyleUi.bgStyle,
                        wmBgAlpha = tableStyleUi.bgAlpha,
                        wmValueScale = tableStyleUi.valueScale,
                        wmTextColorMode = tableStyleUi.textColorMode,
                        wmManualTextColor = tableStyleUi.manualTextColor,
                        wmTextAlign = tableStyleUi.textAlign,
                        wmGridEnabled = tableStyleUi.gridEnabled,
                        onCaptureAspectChange = { aspect ->
                            scope.launch {
                                watermarkUi = applyCaptureAspectChange(context, aspect, watermarkUi)
                            }
                        },
                        onWatermarkDragPreview = { _, _ ->
                            // 드래그 중에는 로컬 프리뷰만 갱신하고 상위 상태/SSOT 갱신은 하지 않음
                        },
                        onWatermarkDragCommit = { offsetX, offsetY ->
                            scope.launch {
                                watermarkUi = applyAnchorOffsetDragChange(context, offsetX, offsetY, watermarkUi)
                            }
                        },
                        onRowColWeightsChange = { updated -> updateTemplateDraft(updated) },
                        onWidthRatioChange = { width ->
                            scope.launch {
                                watermarkUi = applyWidthRatioChange(context, width, watermarkUi)
                            }
                        },
                        onHeightRatioChange = { height ->
                            scope.launch {
                                watermarkUi = applyHeightRatioChange(context, height, watermarkUi)
                            }
                        },
                        onBgStyleChange = { bgStyle ->
                            tableStyleUi = tableStyleUi.copy(bgStyle = bgStyle.coerceIn(0, 2))
                        },
                        onBgAlphaChange = { alpha ->
                            tableStyleUi = tableStyleUi.copy(bgAlpha = alpha.coerceIn(0, 255))
                        },
                        onValueScaleChange = { scale ->
                            tableStyleUi = tableStyleUi.copy(valueScale = scale.coerceIn(60, 160))
                        },
                        onTextColorModeChange = { mode ->
                            tableStyleUi = tableStyleUi.copy(textColorMode = mode.coerceIn(0, 1))
                        },
                        onManualTextColorChange = { color ->
                            tableStyleUi = tableStyleUi.copy(manualTextColor = color.coerceIn(0, 1))
                        },
                        onTextAlignChange = { align ->
                            tableStyleUi = tableStyleUi.copy(textAlign = align.coerceIn(0, 2))
                        },
                        onGridEnabledChange = { enabled ->
                            tableStyleUi = tableStyleUi.copy(gridEnabled = enabled)
                        }
                    )
                }
            }
        }
    }
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
    val next = when (newType) {
        TableCellDataType.TEXT -> this.copy(
            dataType = newType,
            typedValue = CellValue.Text(this.rawText),
            timeFormatOptions = null,
            counterScopeMode = null,
        )

        TableCellDataType.NUMBER -> this.copy(
            dataType = newType,
            typedValue = CellValue.Number(this.rawText),
            timeFormatOptions = null,
            counterScopeMode = null,
        )

        TableCellDataType.DATE -> this.copy(
            dataType = newType,
            typedValue = CellValue.Auto,
            timeFormatOptions = null,
            counterScopeMode = CounterScopeMode.EXCLUDE,
        )

        TableCellDataType.TIME -> this.copy(
            dataType = newType,
            typedValue = CellValue.Auto,
            timeFormatOptions = this.timeFormatOptions ?: TimeFormatOptions(),
            counterScopeMode = CounterScopeMode.EXCLUDE,
        )

        TableCellDataType.COUNTER -> this.copy(
            dataType = newType,
            typedValue = (this.typedValue as? CellValue.CounterSeed) ?: CellValue.CounterSeed(1),
            timeFormatOptions = null,
            counterScopeMode = null,
        )

        TableCellDataType.ROTATING_TEXT -> this.copy(
            dataType = newType,
            typedValue = CellValue.Auto,
            timeFormatOptions = null,
            counterScopeMode = null,
        )
    }

    return if (newType == TableCellDataType.ROTATING_TEXT) {
        next
    } else {
        next.copy(
            phraseSetId = null,
            everyOverride = null
        )
    }
}
