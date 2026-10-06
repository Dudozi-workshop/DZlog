@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.dudoziworkshop.dzlog.ui.table

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.dudoziworkshop.dzlog.data.counter.COUNTER_DIGITS_DEFAULT
import com.dudoziworkshop.dzlog.data.counter.clampCounterDigits
import com.dudoziworkshop.dzlog.data.datastore.AppSettings
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.data.preferences.KEY_COUNTER_DIGITS
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.domain.counter.policy.CounterScopeSnapshot
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.naming.resolveFileNameScopeTokensFromDrafts
import com.dudoziworkshop.dzlog.domain.preview.PreviewInput
import com.dudoziworkshop.dzlog.domain.preview.buildPreview
import com.dudoziworkshop.dzlog.domain.preview.computeNextDelayMillis
import com.dudoziworkshop.dzlog.domain.preview.decideTickUnitFromTemplate
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.feature.counter.core.CounterFacade
import com.dudoziworkshop.dzlog.feature.counter.core.CounterRequestResolver
import com.dudoziworkshop.dzlog.feature.counter.table.TableCounterUiState
import com.dudoziworkshop.dzlog.feature.counter.table.applyCounterConflictDialogEffect
import com.dudoziworkshop.dzlog.feature.counter.table.buildFilenameScopeSignature
import com.dudoziworkshop.dzlog.feature.counter.table.syncCounterStateForScope
import com.dudoziworkshop.dzlog.feature.counter.table.updateCounterCellAndPolicy
import com.dudoziworkshop.dzlog.feature.counter.table.updateCounterUiConflictDialogState
import com.dudoziworkshop.dzlog.feature.counter.table.updateCounterUiScopeFlags
import com.dudoziworkshop.dzlog.feature.table.editor.InlineEditActionResult
import com.dudoziworkshop.dzlog.feature.table.editor.InlineEditSessionContext
import com.dudoziworkshop.dzlog.feature.table.editor.InlineEditSessionState
import com.dudoziworkshop.dzlog.feature.table.editor.StructureActionResult
import com.dudoziworkshop.dzlog.feature.table.editor.StructureAddOrRestoreInput
import com.dudoziworkshop.dzlog.feature.table.editor.StructureRemoveInput
import com.dudoziworkshop.dzlog.feature.table.editor.TableEditorInlineEditActions
import com.dudoziworkshop.dzlog.feature.table.editor.TableEditorStructureActions
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionRange
import com.dudoziworkshop.dzlog.feature.table.editor.TableStructureRangeActions
import com.dudoziworkshop.dzlog.feature.table.editor.TableUndoManager
import com.dudoziworkshop.dzlog.feature.table.editor.buildStructureEditorContext
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.DeletedStructureSnapshot
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.StructureRestoreAxis
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorExitCoordinator
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorInlineActionBindings
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorInlineActionCoordinator
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorSaveApplyInput
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorSaveCoordinator
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorSaveResultApplier
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorSelectedCounterResetCoordinator
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorSelectedCounterResetInput
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.FileNameSlotEditorUiResult
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.FileNameSlotEditorUiState
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.PathSlotEditorUiResult
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.PathSlotEditorUiState
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorBottomPanelModeChangeInput
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorBottomPanelModeChangeResolver
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorFileNameSlotActionBindings
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorInlineEditingActionBindings
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorPathSlotActionBindings
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorSelectedCellActionBinder
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorSelectedCellActionBindings
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorSelectionActionBindings
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorSelectionInlineEditingActionBinder
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorSlotActionBinder
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorSlotDraftHandlers
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorSlotListHandlers
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorTransientStateHandlers
import com.dudoziworkshop.dzlog.feature.table.editor.updateCell
import com.dudoziworkshop.dzlog.feature.table.model.TablePlacementState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.feature.table.policy.confirmCounterConflictDialog
import com.dudoziworkshop.dzlog.feature.table.policy.dismissCounterConflictDialog
import com.dudoziworkshop.dzlog.ui.common.dzScaffoldContent
import com.dudoziworkshop.dzlog.ui.table.editor.InlineEditState
import com.dudoziworkshop.dzlog.ui.table.editor.clearInlineEditing
import com.dudoziworkshop.dzlog.ui.table.editor.isEditing
import com.dudoziworkshop.dzlog.ui.table.format.TableFormatDialog
import com.dudoziworkshop.dzlog.ui.table.format.TableFormatDialogState
import com.dudoziworkshop.dzlog.ui.table.format.closeTableFormatDialog
import com.dudoziworkshop.dzlog.ui.table.rotating.RotatingPhraseUiState
import com.dudoziworkshop.dzlog.ui.table.section.BottomEditorPanelMode
import com.dudoziworkshop.dzlog.ui.table.section.FileNameFormatType
import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotKind
import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotUiItem
import com.dudoziworkshop.dzlog.ui.table.section.LayoutTabActions
import com.dudoziworkshop.dzlog.ui.table.section.LayoutTabContent
import com.dudoziworkshop.dzlog.ui.table.section.LayoutTabUiState
import com.dudoziworkshop.dzlog.ui.table.section.PathFormatType
import com.dudoziworkshop.dzlog.ui.table.section.PathSlotKind
import com.dudoziworkshop.dzlog.ui.table.section.PathSlotUiItem
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

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
// Patch 1: TableEditorScreen는 다음 단계 분해(dialogs/handlers/state coordinator)를 위한 경계 주석만 추가하고
// 실제 동작 흐름은 유지한다.
fun TableEditorScreen(
    templateState: TableTemplateState,
    templateName: String = "",
    onTemplateChange: (TableTemplateState) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showPlacementDialog by remember { mutableStateOf(false) }

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
    ): TableTemplateState = TableEditorSlotDraftHandlers.withUpdatedFileNameSlots(
        base = base,
        updatedSlots = updatedSlots,
        normalize = ::normalizeFileNameDraftSlots,
        toDomain = ::toFileNameDomainSlotDraft,
    )

    fun withUpdatedPathSlots(
        base: TableTemplateState,
        updatedSlots: List<PathSlotUiItem?>
    ): TableTemplateState = TableEditorSlotDraftHandlers.withUpdatedPathSlots(
        base = base,
        updatedSlots = updatedSlots,
        normalize = ::normalizePathDraftSlots,
        toDomain = ::toPathDomainSlotDraft,
    )

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
    var didInitializeDetailPanel by remember { mutableStateOf(false) }
    var structureSelectedCellIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var structureSelectionRange by remember { mutableStateOf<TableSelectionRange?>(null) }
    // ✅ 탭0: 셀 설정 패널 표시 여부
    var showCellSettingsPanel by remember { mutableStateOf(false) }
    // 모드형 하단 편집 패널 1차 구조 상태 (다음 단계 슬롯/직접 편집 확장 대비)
    var bottomPanelMode by remember { mutableStateOf(BottomEditorPanelMode.NONE) }
    fun isStructureEditMode(): Boolean = bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT
    var currentlySelectedFileNameSlot by remember { mutableStateOf<Int?>(null) }
    var currentlySelectedPathSlot by remember { mutableStateOf<Int?>(null) }
    // 정책 변경: FILENAME_EDIT 2단계에서는 템플릿의 실제 셀 연결과 분리된 "슬롯 UI 상태"를 별도로 유지한다.
    // 다음 단계에서 셀/카운터/직접입력 상세 선택 UI를 붙일 수 있게 구조화된 타입을 사용한다.
    val fileNameSlotItems = buildFileNameDraftSlots(currentTemplate)
    var isFileNameCellPickMode by remember { mutableStateOf(false) }
    var manualInputDraft by remember { mutableStateOf("") }
    var showManualInputEditor by remember { mutableStateOf(false) }

    fun removeFileNameSlotAt(slots: List<FileNameSlotUiItem?>, index: Int): List<FileNameSlotUiItem?> {
        return TableEditorSlotListHandlers.removeSlotAt(
            slots = slots,
            index = index,
            normalize = ::normalizeFileNameDraftSlots,
        )
    }

    fun moveFileNameSlot(slots: List<FileNameSlotUiItem?>, from: Int, to: Int): List<FileNameSlotUiItem?> {
        return TableEditorSlotListHandlers.moveSlot(
            slots = slots,
            from = from,
            to = to,
            normalize = ::normalizeFileNameDraftSlots,
        )
    }

    fun clearFileNameEditorTransientState(clearDraft: Boolean) {
        TableEditorTransientStateHandlers.clearFileNameEditorTransientState(
            clearDraft = clearDraft,
            setIsFileNameCellPickMode = { isFileNameCellPickMode = it },
            setShowManualInputEditor = { showManualInputEditor = it },
            setManualInputDraft = { manualInputDraft = it },
        )
    }

    fun currentFileNameSlotEditorState(): FileNameSlotEditorUiState {
        return FileNameSlotEditorUiState(
            slots = fileNameSlotItems,
            selectedSlotIndex = currentlySelectedFileNameSlot,
            isCellPickMode = isFileNameCellPickMode,
            showManualInputEditor = showManualInputEditor,
            manualInputDraft = manualInputDraft,
        )
    }

    fun applyFileNameSlotUiResult(result: FileNameSlotEditorUiResult) {
        if (result.nextSlots != fileNameSlotItems) {
            updateFileNameSlotDraft(
                base = currentTemplate,
                updated = result.nextSlots,
                markDirty = result.markDirty,
            )
        }
        currentlySelectedFileNameSlot = result.nextSelectedSlotIndex
        isFileNameCellPickMode = result.isCellPickMode
        showManualInputEditor = result.showManualInputEditor
        manualInputDraft = result.manualInputDraft
    }

    val pathSlotItems = buildPathDraftSlots(currentTemplate)
    var isPathCellPickMode by remember { mutableStateOf(false) }
    var showPathManualInputEditor by remember { mutableStateOf(false) }
    var pathManualInputDraft by remember { mutableStateOf("") }

    fun removePathSlotAt(slots: List<PathSlotUiItem?>, index: Int): List<PathSlotUiItem?> {
        return TableEditorSlotListHandlers.removeSlotAt(
            slots = slots,
            index = index,
            normalize = ::normalizePathDraftSlots,
        )
    }

    fun movePathSlot(slots: List<PathSlotUiItem?>, from: Int, to: Int): List<PathSlotUiItem?> {
        return TableEditorSlotListHandlers.moveSlot(
            slots = slots,
            from = from,
            to = to,
            normalize = ::normalizePathDraftSlots,
        )
    }

    fun removeCellRefsFromFileNameSlots(
        slots: List<FileNameSlotUiItem?>,
        deletedCellIds: Set<String>
    ): List<FileNameSlotUiItem?> {
        return TableEditorSlotListHandlers.removeCellRefs(
            slots = slots,
            deletedCellIds = deletedCellIds,
            slotCellId = { it.cellId },
            normalize = ::normalizeFileNameDraftSlots,
        )
    }

    fun removeCellRefsFromPathSlots(
        slots: List<PathSlotUiItem?>,
        deletedCellIds: Set<String>
    ): List<PathSlotUiItem?> {
        return TableEditorSlotListHandlers.removeCellRefs(
            slots = slots,
            deletedCellIds = deletedCellIds,
            slotCellId = { it.cellId },
            normalize = ::normalizePathDraftSlots,
        )
    }


    fun clearPathEditorTransientState(clearDraft: Boolean) {
        TableEditorTransientStateHandlers.clearPathEditorTransientState(
            clearDraft = clearDraft,
            setIsPathCellPickMode = { isPathCellPickMode = it },
            setShowPathManualInputEditor = { showPathManualInputEditor = it },
            setPathManualInputDraft = { pathManualInputDraft = it },
        )
    }


    fun currentPathSlotEditorState(): PathSlotEditorUiState {
        return PathSlotEditorUiState(
            slots = pathSlotItems,
            selectedSlotIndex = currentlySelectedPathSlot,
            isCellPickMode = isPathCellPickMode,
            showManualInputEditor = showPathManualInputEditor,
            manualInputDraft = pathManualInputDraft,
        )
    }

    fun applyPathSlotUiResult(result: PathSlotEditorUiResult) {
        if (result.nextSlots != pathSlotItems) {
            updatePathSlotDraft(
                base = currentTemplate,
                updated = result.nextSlots,
                markDirty = result.markDirty,
            )
        }
        currentlySelectedPathSlot = result.nextSelectedSlotIndex
        isPathCellPickMode = result.isCellPickMode
        showPathManualInputEditor = result.showManualInputEditor
        pathManualInputDraft = result.manualInputDraft
    }

    // 탭1 스크롤 (분리)

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
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
        if (filenameChanged) {
            lastFilenameScopeSignature = filenameScopeSignature
        }
        // 키가 바뀔 때만 Effect가 재실행되므로 tick은 항상 1회 증가시킨다.
        scopeInputTick += 1
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

    var inlineEdit by remember { mutableStateOf(InlineEditState()) }
    // 주요 정책: inline 세션은 첫 onValueChange 시점의 undo snapshot push 여부를 기준으로 관리한다.
    var inlineEditSessionState by remember { mutableStateOf(InlineEditSessionState()) }
    // 선택된 셀의 세션 시작 전 원본 스냅샷이다. 즉시 반영 이후에도 undo/세션 동기화 보조 정보로만 유지한다.
    var editSessionOriginalCellState by remember { mutableStateOf<TableCellState?>(null) }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var showStructureDeleteSheet by remember { mutableStateOf(false) }
    var pendingMergeRange by remember { mutableStateOf<TableSelectionRange?>(null) }
    val deletedRowsStack = remember { mutableStateListOf<DeletedStructureSnapshot>() }
    val deletedColsStack = remember { mutableStateListOf<DeletedStructureSnapshot>() }

    fun clearInlineEditingState() {
        inlineEdit = clearInlineEditing(inlineEdit)
        inlineEditSessionState = InlineEditSessionState()
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

    fun currentInlineEditSessionContext(): InlineEditSessionContext {
        return InlineEditSessionContext(
            currentTemplate = currentTemplate,
            inlineEdit = inlineEdit,
            selectedCellId = selectedCellId,
            inlineSessionState = inlineEditSessionState,
            editSessionOriginalCellState = editSessionOriginalCellState,
            autoNextCounterValue = counterUi.autoNextCounterValue,
            lowCounterWarningLatchedInSession = counterUi.lowCounterWarningLatchedInSession,
            updateCell = ::updateCell,
        )
    }

    val hasGroup1 = currentTemplate.cells.any { it.groupLevel == GroupLevel.G1 }
    val hasGroup2 = currentTemplate.cells.any { it.groupLevel == GroupLevel.G2 }
    val resolvedByCellId = remember(plan.resolvedCells) {
        plan.resolvedCells.associate { it.id to it.resolvedText }
    }

    fun resolveSelectedCellLabel(targetCellId: String): String {
        return currentTemplate.cells.firstOrNull { it.cellId == targetCellId }?.let { cell ->
            resolvedByCellId[cell.cellId]?.takeIf { it.isNotBlank() }
                ?: "셀(${cell.rowIndex + 1},${cell.colIndex + 1})"
        } ?: "셀"
    }

    val fileNameSlotActionBindings = TableEditorFileNameSlotActionBindings(
        currentState = ::currentFileNameSlotEditorState,
        applyResult = ::applyFileNameSlotUiResult,
        moveSlot = ::moveFileNameSlot,
        removeSlotAt = ::removeFileNameSlotAt,
        resolveCellLabel = ::resolveSelectedCellLabel,
    )

    val pathSlotActionBindings = TableEditorPathSlotActionBindings(
        currentState = ::currentPathSlotEditorState,
        applyResult = ::applyPathSlotUiResult,
        moveSlot = ::movePathSlot,
        removeSlotAt = ::removePathSlotAt,
        resolveCellLabel = ::resolveSelectedCellLabel,
    )

    var watermarkUi by remember { mutableStateOf(TablePlacementState()) }
    var initialPlacementSnapshot by remember { mutableStateOf(TablePlacementState()) }
    var tableStyleUi by remember { mutableStateOf(TableStyleState()) }
    var initialStyleSnapshot by remember { mutableStateOf(TableStyleState()) }
    val undoManager = remember { TableUndoManager<TableEditorUndoSnapshot>() }
    var undoRevision by remember { mutableIntStateOf(0) }

    fun currentUndoSnapshot(): TableEditorUndoSnapshot {
        return buildTableEditorUndoSnapshot(
            templateState = currentTemplate,
            styleState = tableStyleUi,
            selectedCellId = selectedCellId,
            structureSelectedCellIds = structureSelectedCellIds,
            structureSelectionRange = structureSelectionRange,
        )
    }

    fun applyUndo() {
        val restored = undoTableEditorSnapshot(
            undoManager = undoManager,
            currentSnapshot = currentUndoSnapshot(),
        ) ?: return
        editableTemplateState = restored.templateState
        tableStyleUi = restored.styleState
        inlineEditSessionState = InlineEditSessionState()
        selectedCellId = restored.selectedCellId
        structureSelectedCellIds = restored.structureSelectedCellIds
        structureSelectionRange = restored.structureSelectionRange
        undoRevision += 1
    }

    fun applyRedo() {
        val restored = redoTableEditorSnapshot(
            undoManager = undoManager,
            currentSnapshot = currentUndoSnapshot(),
        ) ?: return
        editableTemplateState = restored.templateState
        tableStyleUi = restored.styleState
        inlineEditSessionState = InlineEditSessionState()
        selectedCellId = restored.selectedCellId
        structureSelectedCellIds = restored.structureSelectedCellIds
        structureSelectionRange = restored.structureSelectionRange
        undoRevision += 1
    }

    fun applyTemplateWithUndo(nextTemplate: TableTemplateState) {
        if (nextTemplate == currentTemplate) return
        val pushed = pushUndoSnapshotBeforeChange(
            undoManager = undoManager,
            currentSnapshot = currentUndoSnapshot(),
            nextTemplate = nextTemplate,
        )
        if (pushed) {
            undoRevision += 1
        }
        updateTemplateDraft(nextTemplate)
    }

    fun syncStructureSelection(range: TableSelectionRange?) {
        structureSelectionRange = range
        if (range == null) {
            structureSelectedCellIds = emptySet()
            return
        }
        structureSelectedCellIds = TableStructureRangeActions.interactiveRootCellsInRange(
            cells = currentTemplate.cells,
            range = range,
        )
            .map { it.cellId }
            .toSet()
    }

    fun applyCommittedInlineCounterUpdate(
        templateState: TableTemplateState,
        cellId: String,
        seed: Int,
        lowCounterWarningLatchedInSession: Boolean,
        counterUiState: TableCounterUiState,
    ): TableCounterUiState {
        var appliedCounterUi = counterUiState
        updateCounterCellAndPolicy(
            templateState = templateState,
            cellId = cellId,
            seed = seed,
            preserveManual = true,
            persistToCounterPolicy = true,
            counterRequest = counterRequest,
            counterFacade = counterFacade,
            counterUi = appliedCounterUi,
            onTemplateChange = ::updateTemplateDraft,
            setCounterUi = { appliedCounterUi = it },
            updateCell = ::updateCell,
            scope = scope,
            lowCounterWarningLatchedInSession = lowCounterWarningLatchedInSession,
        )
        return appliedCounterUi
    }

    fun applyInlineAppliedState(applied: com.dudoziworkshop.dzlog.feature.table.editor.TableEditorInlineEditAppliedState) {
        inlineEdit = applied.nextInlineEdit
        inlineEditSessionState = applied.nextInlineSessionState
        editSessionOriginalCellState = applied.nextEditSessionOriginalCellState
        selectedCellId = applied.nextSelectedCellId
        counterUi = applied.nextCounterUi
    }

    val inlineActionBindings = TableEditorInlineActionBindings(
        currentSessionContext = ::currentInlineEditSessionContext,
        currentTemplate = { currentTemplate },
        currentCounterUi = { counterUi },
        applyTemplateWithUndo = ::applyTemplateWithUndo,
        applyTemplateDirectly = ::updateTemplateDraft,
        updateCounterConflictUi = ::updateCounterUiConflictDialogState,
        applyCommittedCounter = ::applyCommittedInlineCounterUpdate,
        reflectAppliedState = ::applyInlineAppliedState,
    )

    fun runInlineAction(action: (InlineEditSessionContext) -> InlineEditActionResult): InlineEditActionResult {
        return TableEditorInlineActionCoordinator.runAction(inlineActionBindings, action)
    }

    fun runInlineCommitAction(): InlineEditActionResult =
        TableEditorInlineActionCoordinator.runCommitAction(inlineActionBindings)

    fun commitInlineEditIfNeeded() {
        runInlineCommitAction()
    }

    val selectedCell = currentTemplate.cells.firstOrNull { it.cellId == selectedCellId }

    val selectedCellActionBindings = TableEditorSelectedCellActionBindings(
        currentTemplate = { currentTemplate },
        currentSelectedCell = { selectedCell },
        updateTemplateDraft = ::updateTemplateDraft,
        showCellSettingsPanel = { showCellSettingsPanel = it },
        openRotatingPhraseTemplateDialog = ::openRotatingPhraseTemplateDialog,
    )

    val selectionActionBindings = TableEditorSelectionActionBindings(
        currentTemplate = { currentTemplate },
        currentBottomPanelMode = { bottomPanelMode },
        isStructureEditMode = ::isStructureEditMode,
        runInlineAction = ::runInlineAction,
        setStructureSelectedCellIds = { structureSelectedCellIds = it },
        setStructureSelectionRange = { structureSelectionRange = it },
        setSelectedCellId = { selectedCellId = it },
    )

    val inlineEditingActionBindings = TableEditorInlineEditingActionBindings(
        currentInlineEdit = { inlineEdit },
        setInlineEdit = { inlineEdit = it },
        currentFormatDialog = { formatDialog },
        setFormatDialog = { formatDialog = it },
        runInlineAction = ::runInlineAction,
        runInlineCommitAction = ::runInlineCommitAction,
        commitInlineEditIfNeeded = ::commitInlineEditIfNeeded,
        setShowCellSettingsPanel = { showCellSettingsPanel = it },
    )

    fun firstCellIdOrNull(): String? {
        return currentTemplate.cells
            .minWithOrNull(compareBy<TableCellState> { it.rowIndex }.thenBy { it.colIndex })
            ?.cellId
    }

    fun ensureSelectedCellForCellEdit() {
        if (selectedCellId != null) return
        selectedCellId = firstCellIdOrNull()
    }

    LaunchedEffect(currentTemplate.cells, didInitializeDetailPanel) {
        if (!didInitializeDetailPanel && currentTemplate.cells.isNotEmpty()) {
            // 현재 화면 구조에서 "표 상세설정 패널 진입" 시점은 화면 첫 로드와 동일하므로,
            // 최초 1회만 CELL_EDIT + 첫 셀 자동 선택을 적용한다.
            ensureSelectedCellForCellEdit()
            bottomPanelMode = BottomEditorPanelMode.CELL_EDIT
            didInitializeDetailPanel = true
        }
    }

    LaunchedEffect(currentTemplate.cells, selectedCellId) {
        val hasSelectedCell = selectedCellId != null && currentTemplate.cells.any { it.cellId == selectedCellId }
        if (!hasSelectedCell) {
            selectedCellId = null
        }
    }

    LaunchedEffect(bottomPanelMode, selectedCellId, currentTemplate.cells) {
        runInlineAction { context ->
            TableEditorInlineEditActions.syncSessionSnapshot(
                context = context,
                shouldTrackSessionSnapshot = bottomPanelMode == BottomEditorPanelMode.CELL_EDIT,
            )
        }
    }

    fun applyStructureActionResult(result: StructureActionResult) {
        applyTemplateWithUndo(result.nextTemplate)
        fileNameSlotsDirtySinceStructureChange = result.nextFileNameSlotsDirtySinceStructureChange
        pathSlotsDirtySinceStructureChange = result.nextPathSlotsDirtySinceStructureChange
        deletedRowsStack.clear()
        deletedRowsStack.addAll(result.nextDeletedRowsStack)
        deletedColsStack.clear()
        deletedColsStack.addAll(result.nextDeletedColsStack)
        structureSelectionRange = result.nextStructureSelectionRange
        structureSelectedCellIds = result.nextStructureSelectedCellIds
        selectedCellId = result.nextSelectedCellId
    }

    fun currentStructureEditorContext() = buildStructureEditorContext(
        currentTemplate = currentTemplate,
        currentFileNameSlots = fileNameSlotItems,
        currentPathSlots = pathSlotItems,
        fileNameSlotsDirtySinceStructureChange = fileNameSlotsDirtySinceStructureChange,
        pathSlotsDirtySinceStructureChange = pathSlotsDirtySinceStructureChange,
        deletedRowsStack = deletedRowsStack,
        deletedColsStack = deletedColsStack,
        isStructureEditMode = isStructureEditMode(),
        structureSelectionRange = structureSelectionRange,
        selectedCellId = selectedCellId,
    )

    fun applyStructureAdd(axis: StructureRestoreAxis) {
        val result = TableEditorStructureActions.addBlank(
            StructureAddOrRestoreInput(
                axis = axis,
                editor = currentStructureEditorContext(),
                sanitizeTemplate = ::sanitizePathGroupAfterStructureChange,
                applyFileNameSlots = ::withUpdatedFileNameSlots,
                applyPathSlots = ::withUpdatedPathSlots,
            )
        )
        applyStructureActionResult(result)
    }

    fun applyStructureRemove(axis: StructureRestoreAxis) {
        val result = TableEditorStructureActions.remove(
            StructureRemoveInput(
                axis = axis,
                editor = currentStructureEditorContext(),
                sanitizeTemplate = ::sanitizePathGroupAfterStructureChange,
                applyFileNameSlots = ::withUpdatedFileNameSlots,
                applyPathSlots = ::withUpdatedPathSlots,
                removeCellRefsFromFileNameSlots = ::removeCellRefsFromFileNameSlots,
                removeCellRefsFromPathSlots = ::removeCellRefsFromPathSlots,
            ),
        )
        applyStructureActionResult(result)
    }

    fun applyStyleWithUndo(nextStyle: TableStyleState) {
        if (nextStyle == tableStyleUi) return
        val pushed = pushUndoSnapshotBeforeChange(
            undoManager = undoManager,
            currentSnapshot = currentUndoSnapshot(),
            nextStyle = nextStyle,
        )
        if (pushed) {
            undoRevision += 1
        }
        tableStyleUi = nextStyle
    }

    fun applyTableStyleMutation(mutate: (TableStyleState) -> TableStyleState) {
        applyStyleWithUndo(mutate(tableStyleUi))
    }

    fun updateWatermarkWidthRatio(width: Int) {
        watermarkUi = watermarkUi.copy(wmWidthRatio = width.coerceIn(10, 100))
    }

    fun resetEditorToInitialSnapshot() {
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
    }

    LaunchedEffect(Unit) {
        loadTableEditorBootstrapState(context).onSuccess { loaded ->
            watermarkUi = loaded.placement
            initialPlacementSnapshot = loaded.placement
            tableStyleUi = loaded.style
            initialStyleSnapshot = loaded.style
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
                    text = """
                        중복된 카운터가 발생할 수 있습니다. 계속 진행하시겠습니까?

                        입력값: ${counterUi.counterConflictDialogState.pendingCounterCommitValue}
                        현재 스트림 next: ${counterUi.counterConflictDialogState.pendingCounterStreamNextValue}
                    """.trimIndent(),
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
        onClose = { formatDialog = closeTableFormatDialog() },
        dateFormatOptions = dateFormatOptions
    )


    RotatingPhraseDialogsHost(
        rotatingUi = rotatingUi,
        templateState = currentTemplate,
        onRotatingUiChange = { rotatingUi = it },
        updateTemplate = { updateTemplateDraft(it) },
        closeTemplateDialog = ::closeRotatingPhraseTemplateDialog,
    )


    // 주요 정책: 패널 모드 변경 전에도 inline commit을 우선 보장한다.
    fun reflectBottomPanelModeChangedState(
        changed: com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorBottomPanelModeChangedState,
    ) {
        if (!changed.shouldApply) return
        if (changed.shouldClearFileNameTransientState) {
            clearFileNameEditorTransientState(clearDraft = true)
        }
        if (changed.shouldClearPathTransientState) {
            clearPathEditorTransientState(clearDraft = true)
        }
        bottomPanelMode = changed.nextBottomPanelMode
        changed.nextStructureSelectedCellIds?.let { structureSelectedCellIds = it }
        if (changed.nextStructureSelectionRangeCleared) {
            structureSelectionRange = null
            selectedCellId = changed.nextSelectedCellId
        }
    }

    fun requestBottomPanelModeChange(nextMode: BottomEditorPanelMode) {
        if (nextMode == BottomEditorPanelMode.CELL_EDIT) {
            ensureSelectedCellForCellEdit()
        }
        if (bottomPanelMode == nextMode) return
        val inlineCommitResult = runInlineCommitAction()
        val changedState = TableEditorBottomPanelModeChangeResolver.resolve(
            TableEditorBottomPanelModeChangeInput(
                currentMode = bottomPanelMode,
                nextMode = nextMode,
                inlineCommitWasBlocked = inlineCommitResult.wasBlocked,
                wasStructureMode = isStructureEditMode(),
                currentSelectedCellId = selectedCellId,
            )
        )
        reflectBottomPanelModeChangedState(changedState)
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
                watermarkUi != initialPlacementSnapshot ||
                inlineEdit.isEditing() ||
                manualInputDraft.isNotBlank() ||
                pathManualInputDraft.isNotBlank()
        }
    }
    val isUndoAvailable by remember(undoRevision) {
        derivedStateOf { undoManager.canUndo() }
    }
    val isRedoAvailable by remember(undoRevision) {
        derivedStateOf { undoManager.canRedo() }
    }

    // Save/Reset/Back 동작은 editor 내부 local state를 기준으로 유지하되, 구현만 별도 helper로 분리한다.
    fun applySaveAppliedState(applied: com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorSaveAppliedState) {
        applied.nextPlacement?.let { watermarkUi = it }
        applied.nextInitialTemplateSnapshot?.let { initialTemplateSnapshot = it }
        applied.nextInitialStyleSnapshot?.let { initialStyleSnapshot = it }
        applied.nextInitialPlacementSnapshot?.let { initialPlacementSnapshot = it }
        if (applied.shouldClearUndo) {
            undoManager.clear()
        }
        undoRevision = applied.nextUndoRevision
        isSavingTemplate = applied.nextIsSavingTemplate
        scope.launch {
            snackbarHostState.showSnackbar(applied.toastMessage)
        }
        if (applied.shouldNotifyTemplateChange) {
            onTemplateChange(initialTemplateSnapshot)
        }
        if (applied.shouldExitAfterSave) {
            onBack()
        }
    }

    fun saveTemplate(exitAfterSave: Boolean = false) {
        val inlineResult = runInlineCommitAction()
        if (inlineResult.wasBlocked) return
        isSavingTemplate = true
        val savePayload = editableTemplateState
        val stylePayload = tableStyleUi
        scope.launch {
            val result = TableEditorSaveCoordinator.persist(
                context = context,
                templatePayload = savePayload,
                stylePayload = stylePayload,
                placementPayload = watermarkUi,
                rollbackTemplate = initialTemplateSnapshot,
            )
            val appliedState = TableEditorSaveResultApplier.apply(
                TableEditorSaveApplyInput(
                    result = result,
                    savePayload = savePayload,
                    stylePayload = stylePayload,
                    exitAfterSave = exitAfterSave,
                    currentUndoRevision = undoRevision,
                )
            )
            applySaveAppliedState(appliedState)
        }
    }

    fun requestNavigateBack() {
        when (TableEditorExitCoordinator.onBackPressed(hasUnsavedChanges)) {
            TableEditorExitCoordinator.Effect.OpenUnsavedChangesDialog -> showUnsavedChangesDialog = true
            TableEditorExitCoordinator.Effect.ExitNow -> {
                undoManager.clear()
                undoRevision += 1
                onBack()
            }
        }
    }

    fun requestCloseBottomPanelToNone() {
        requestCloseBottomPanel(
            inlineEdit = inlineEdit,
            onCommitInlineEdit = ::commitInlineEditIfNeeded,
            onClosed = {
                closeBottomPanelUiState(
                    clearFileNameEditorTransientState = ::clearFileNameEditorTransientState,
                    clearPathEditorTransientState = ::clearPathEditorTransientState,
                    setBottomPanelMode = { bottomPanelMode = it },
                    setShowCellSettingsPanel = { showCellSettingsPanel = it },
                    clearSelectedFileNameSlot = { currentlySelectedFileNameSlot = null },
                    clearSelectedPathSlot = { currentlySelectedPathSlot = null },
                )
            },
        )
    }

    fun dismissUnsavedChangesDialog() {
        if (showUnsavedChangesDialog) {
            showUnsavedChangesDialog = false
        }
    }

    TableEditorUnsavedChangesHost(
        showUnsavedChangesDialog = showUnsavedChangesDialog,
        onRequestNavigateBack = ::requestNavigateBack,
        onSaveAndExit = {
            dismissUnsavedChangesDialog()
            saveTemplate(exitAfterSave = true)
        },
        onDiscardAndExit = {
            dismissUnsavedChangesDialog()
            undoManager.clear()
            undoRevision += 1
            onBack()
        },
        onCancel = ::dismissUnsavedChangesDialog,
    )

    fun performMergeSelection(range: TableSelectionRange) {
        val expanded = TableStructureRangeActions.expandRangeToMergedBlocks(currentTemplate.cells, range)
        if (expanded != range) return
        val mergedTemplate = TableStructureRangeActions.mergeSelection(currentTemplate, range)
        applyTemplateWithUndo(mergedTemplate)
        val mergedRoot = mergedTemplate.cells.firstOrNull {
            it.rowIndex == range.minRow && it.colIndex == range.minCol
        }
        structureSelectionRange = null
        structureSelectedCellIds = mergedRoot?.let { setOf(it.cellId) } ?: emptySet()
    }

    if (pendingMergeRange != null) {
        AlertDialog(
            containerColor = DDZColor.Surface,
            onDismissRequest = { pendingMergeRange = null },
            title = {
                Text(
                    "셀을 병합할까요?",
                    style = DDZTypography.CardTitle,
                    color = DDZColor.TextPrimary,
                )
            },
            text = {
                Text(
                    "병합하면 왼쪽 위 셀의 값만 유지됩니다.",
                    style = DDZTypography.Body,
                    color = DDZColor.TextPrimary,
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingMergeRange = null }) {
                    Text("취소")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingMergeRange?.let(::performMergeSelection)
                        pendingMergeRange = null
                    }
                ) {
                    Text("병합")
                }
            },
        )
    }

    if (showStructureDeleteSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStructureDeleteSheet = false },
            containerColor = DDZColor.Surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    "삭제",
                    style = DDZTypography.CardTitle,
                    color = DDZColor.TextPrimary,
                )
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    onClick = {
                        showStructureDeleteSheet = false
                        applyStructureRemove(StructureRestoreAxis.ROW)
                    },
                ) {
                    Text("행 삭제")
                }
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    onClick = {
                        showStructureDeleteSheet = false
                        applyStructureRemove(StructureRestoreAxis.COL)
                    },
                ) {
                    Text("열 삭제")
                }
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showStructureDeleteSheet = false },
                ) {
                    Text("취소")
                }
            }
        }
    }

    fun resetSelectedCounterSeed() {
        val result = TableEditorSelectedCounterResetCoordinator.resetToAutoNext(
            TableEditorSelectedCounterResetInput(
                selectedCell = selectedCell,
                templateState = currentTemplate,
                counterRequest = counterRequest,
                counterFacade = counterFacade,
                counterUi = counterUi,
                inlineEdit = inlineEdit,
                onTemplateChange = ::updateTemplateDraft,
                setCounterUi = { counterUi = it },
                updateCell = ::updateCell,
                scope = scope,
            )
        )
        inlineEdit = result.nextInlineEdit
        result.toastMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun buildLayoutTabActions(): LayoutTabActions = LayoutTabActions(
        onSelectCellId = { cellId ->
            TableEditorSelectionInlineEditingActionBinder.requestSelectCell(selectionActionBindings, cellId)
        },
        onSelectStructureRange = { startId, endId ->
            TableEditorSelectionInlineEditingActionBinder.selectStructureRange(selectionActionBindings, startId, endId)
        },
        onChangeBottomPanelMode = ::requestBottomPanelModeChange,
        onCloseBottomPanel = ::requestCloseBottomPanelToNone,
        onShowCellSettingsPanel = { showCellSettingsPanel = it },
        onSelectFileNameSlot = { slotIndex ->
            TableEditorSlotActionBinder.selectFileNameSlot(fileNameSlotActionBindings, slotIndex)
        },
        onFillEmptyFileNameSlot = { slotIndex ->
            TableEditorSlotActionBinder.fillEmptyFileNameSlot(fileNameSlotActionBindings, slotIndex)
        },
        onMoveSelectedFileNameSlotLeft = {
            TableEditorSlotActionBinder.moveSelectedFileNameSlotLeft(fileNameSlotActionBindings)
        },
        onMoveSelectedFileNameSlotRight = {
            TableEditorSlotActionBinder.moveSelectedFileNameSlotRight(fileNameSlotActionBindings)
        },
        onDeleteSelectedFileNameSlot = {
            TableEditorSlotActionBinder.deleteSelectedFileNameSlot(fileNameSlotActionBindings)
        },
        onStartFileNameCellPick = {
            TableEditorSlotActionBinder.startFileNameCellPick(fileNameSlotActionBindings)
        },
        onStartManualInputEditor = {
            TableEditorSlotActionBinder.startFileNameManualInput(fileNameSlotActionBindings)
        },
        onManualInputDraftChange = { draft ->
            TableEditorSlotActionBinder.updateFileNameManualInputDraft(fileNameSlotActionBindings, draft)
        },
        onApplyManualInput = {
            TableEditorSlotActionBinder.applyFileNameManualInput(fileNameSlotActionBindings)
        },
        onBindSelectedSlotToCell = { cellId ->
            TableEditorSlotActionBinder.bindSelectedFileNameSlotToCell(fileNameSlotActionBindings, cellId)
        },
        onSelectPathSlot = { slotIndex ->
            TableEditorSlotActionBinder.selectPathSlot(pathSlotActionBindings, slotIndex)
        },
        onFillEmptyPathSlot = { slotIndex ->
            TableEditorSlotActionBinder.fillEmptyPathSlot(pathSlotActionBindings, slotIndex)
        },
        onMoveSelectedPathSlotLeft = {
            TableEditorSlotActionBinder.moveSelectedPathSlotLeft(pathSlotActionBindings)
        },
        onMoveSelectedPathSlotRight = {
            TableEditorSlotActionBinder.moveSelectedPathSlotRight(pathSlotActionBindings)
        },
        onDeleteSelectedPathSlot = {
            TableEditorSlotActionBinder.deleteSelectedPathSlot(pathSlotActionBindings)
        },
        onStartPathCellPick = {
            TableEditorSlotActionBinder.startPathCellPick(pathSlotActionBindings)
        },
        onStartPathManualInputEditor = {
            TableEditorSlotActionBinder.startPathManualInput(pathSlotActionBindings)
        },
        onPathManualInputDraftChange = { draft ->
            TableEditorSlotActionBinder.updatePathManualInputDraft(pathSlotActionBindings, draft)
        },
        onApplyPathManualInput = {
            TableEditorSlotActionBinder.applyPathManualInput(pathSlotActionBindings)
        },
        onBindSelectedPathSlotToCell = { cellId ->
            TableEditorSlotActionBinder.bindSelectedPathSlotToCell(pathSlotActionBindings, cellId)
        },
        onStartInlineEditing = { cellId, value ->
            TableEditorSelectionInlineEditingActionBinder.startCellInlineEditing(inlineEditingActionBindings, cellId, value)
        },
        onOpenFormatDialog = { cellId, type ->
            TableEditorSelectionInlineEditingActionBinder.openCellFormatDialog(inlineEditingActionBindings, cellId, type)
        },
        onEditingValueChange = { nextValue ->
            TableEditorSelectionInlineEditingActionBinder.applyInlineEditingValue(inlineEditingActionBindings, nextValue)
        },
        onTryCommitInlineAndContinue = {
            TableEditorSelectionInlineEditingActionBinder.tryCommitInlineAndContinue(inlineEditingActionBindings)
        },
        onAddRow = { applyStructureAdd(StructureRestoreAxis.ROW) },
        onAddCol = { applyStructureAdd(StructureRestoreAxis.COL) },
        onMergeSelection = merge@{
            val singleSelectedId = structureSelectedCellIds.singleOrNull()
            val singleSelectedCell = singleSelectedId?.let { id ->
                currentTemplate.cells.firstOrNull { it.cellId == id }
            }
            if (singleSelectedCell != null &&
                (singleSelectedCell.rowSpan > 1 || singleSelectedCell.colSpan > 1)
            ) {
                val unmerged = TableStructureRangeActions.unmergeRoot(
                    templateState = currentTemplate,
                    rootCellId = singleSelectedCell.cellId,
                )
                applyTemplateWithUndo(unmerged)
                structureSelectionRange = TableSelectionRange(
                    minRow = singleSelectedCell.rowIndex,
                    maxRow = singleSelectedCell.rowIndex,
                    minCol = singleSelectedCell.colIndex,
                    maxCol = singleSelectedCell.colIndex,
                )
                structureSelectedCellIds = setOf(singleSelectedCell.cellId)
                return@merge
            }

            val range = structureSelectionRange ?: return@merge
            val expanded = TableStructureRangeActions.expandRangeToMergedBlocks(currentTemplate.cells, range)
            if (expanded != range || range.rowCount * range.colCount < 2) return@merge
            val populatedCount = currentTemplate.cells.count { cell ->
                range.contains(cell.rowIndex, cell.colIndex) &&
                    resolvedByCellId[cell.cellId].orEmpty().isNotBlank()
            }
            if (populatedCount > 1) {
                pendingMergeRange = range
            } else {
                performMergeSelection(range)
            }
        },
        onDeleteSelection = delete@{
            if (structureSelectionRange == null) return@delete
            showStructureDeleteSheet = true
        },
        onUndo = ::applyUndo,
        onReset = ::resetEditorToInitialSnapshot,
        onSave = { saveTemplate(exitAfterSave = false) },
        onDismissSettingsPanel = {
            TableEditorSelectionInlineEditingActionBinder.dismissCellSettingsPanel(inlineEditingActionBindings)
        },
        onToggleFileNameForSelected = { cellId, enabled ->
            TableEditorSlotActionBinder.toggleFileNameForSelectedCell(fileNameSlotActionBindings, cellId, enabled)
        },
        onReorderFileNameSlots = { fromIndex, toIndex ->
            TableEditorSlotActionBinder.reorderFileNameSlots(fileNameSlotActionBindings, fromIndex, toIndex)
        },
        onPathGroupActionForSelected = { action ->
            TableEditorSelectedCellActionBinder.applyPathGroupActionForSelected(selectedCellActionBindings, action)
        },
        onSetDataTypeForSelected = { type ->
            TableEditorSelectedCellActionBinder.setDataTypeForSelected(selectedCellActionBindings, type)
        },
        onSetCounterScopeModeForSelected = { mode ->
            TableEditorSelectedCellActionBinder.setCounterScopeModeForSelected(selectedCellActionBindings, mode)
        },
        onResetCounterSeedForSelected = ::resetSelectedCounterSeed,
        onSetBgStyle = { bgStyle ->
            applyTableStyleMutation {
                it.copy(
                    bgStyle = bgStyle.coerceIn(0, 2),
                    textColorMode = 0,
                )
            }
        },
        onSetBgAlpha = { alpha -> applyTableStyleMutation { it.copy(bgAlpha = alpha.coerceIn(0, 255)) } },
        onSetGridEnabled = { enabled -> applyTableStyleMutation { it.copy(gridEnabled = enabled) } },
        onSetTextColorMode = { mode -> applyTableStyleMutation { it.copy(textColorMode = mode.coerceIn(0, 1)) } },
        onSetManualTextColor = { color -> applyTableStyleMutation { it.copy(manualTextColor = color.coerceIn(0, 1)) } },
        onSetValueScale = { scale -> applyTableStyleMutation { it.copy(valueScale = scale.coerceIn(60, 160)) } },
        onSetTextAlign = { align -> applyTableStyleMutation { it.copy(textAlign = align.coerceIn(0, 2)) } },
        onSetWmWidthRatio = ::updateWatermarkWidthRatio,
        onCommitRowWeights = { weights ->
            applyTemplateWithUndo(currentTemplate.copy(rowWeights = weights))
        },
        onCommitColumnWeights = { weights ->
            applyTemplateWithUndo(currentTemplate.copy(colWeights = weights))
        },
        onOpenRotatingTemplateDialogForSelected = { cellId ->
            TableEditorSelectedCellActionBinder.openRotatingTemplateDialogForSelected(selectedCellActionBindings, cellId)
        },
        onOpenPlacementDialog = { showPlacementDialog = true }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
        containerColor = DDZColor.Background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier,
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            if (bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT) "레이아웃 편집" else "표 편집",
                            style = DDZTypography.ScreenTitle,
                            color = DDZColor.Primary
                        )
                        if (bottomPanelMode != BottomEditorPanelMode.STRUCTURE_EDIT && templateName.isNotBlank()) {
                            Text(
                                text = templateName,
                                style = DDZTypography.Caption,
                                color = DDZColor.TextMuted,
                                maxLines = 1,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (bottomPanelMode == BottomEditorPanelMode.STRUCTURE_EDIT) {
                                requestBottomPanelModeChange(BottomEditorPanelMode.NONE)
                            } else {
                                requestNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = DDZColor.Primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = ::applyUndo,
                        enabled = isUndoAvailable,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Undo,
                            contentDescription = "실행 취소",
                        )
                    }
                    IconButton(
                        onClick = ::applyRedo,
                        enabled = isRedoAvailable,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Redo,
                            contentDescription = "다시 실행",
                        )
                    }
                    IconButton(
                        onClick = { saveTemplate(exitAfterSave = false) },
                        enabled = hasUnsavedChanges && !isSavingTemplate,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = "저장",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DDZColor.Background,
                    navigationIconContentColor = DDZColor.Primary,
                    titleContentColor = DDZColor.Primary,
                    actionIconContentColor = DDZColor.Primary,
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .dzScaffoldContent()
                .padding(innerPadding)
        ) {
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
                        actions = buildLayoutTabActions()
                    )

            PlacementDialogHost(
                show = showPlacementDialog,
                templateState = currentTemplate,
                resolvedCells = plan.resolvedCells,
                styleState = tableStyleUi,
                placementState = watermarkUi,
                onApplyPlacement = { applied ->
                    watermarkUi = applied.copy(
                        wmOffsetXRatio = applied.wmOffsetXRatio.coerceIn(0, 100),
                        wmOffsetYRatio = applied.wmOffsetYRatio.coerceIn(0, 100),
                        wmWidthRatio = applied.wmWidthRatio.coerceIn(10, 100),
                        wmHeightRatio = applied.wmWidthRatio.coerceIn(10, 100),
                        rotationCwDeg = if (applied.rotationCwDeg == 90) 90 else 0,
                        captureAspect = applied.captureAspect,
                        keepAspectRatio = true,
                    )
                                showPlacementDialog = false
                },
                onClose = { showPlacementDialog = false },
            )
        }

        }

    }
}
