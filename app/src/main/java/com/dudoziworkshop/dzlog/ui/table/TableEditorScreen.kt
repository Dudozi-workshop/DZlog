@file:Suppress("UNUSED_VALUE", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.dudoziworkshop.dzlog.ui.table

import android.content.pm.ApplicationInfo
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
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
import com.dudoziworkshop.dzlog.domain.model.CounterScopeMode
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.TimeFormatOptions
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
import com.dudoziworkshop.dzlog.feature.counter.table.restoreCounterCellToAutoNext
import com.dudoziworkshop.dzlog.feature.counter.table.syncCounterStateForScope
import com.dudoziworkshop.dzlog.feature.counter.table.updateCounterCellAndPolicy
import com.dudoziworkshop.dzlog.feature.counter.table.updateCounterUiConflictDialogState
import com.dudoziworkshop.dzlog.feature.counter.table.updateCounterUiScopeFlags
import com.dudoziworkshop.dzlog.feature.table.editor.TableHandleOverlay
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionRange
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionResolver
import com.dudoziworkshop.dzlog.feature.table.editor.StructureActionResult
import com.dudoziworkshop.dzlog.feature.table.editor.TableEditorStructureActions
import com.dudoziworkshop.dzlog.feature.table.editor.TableEditorStructureAddOrRestoreActionInput
import com.dudoziworkshop.dzlog.feature.table.editor.TableEditorStructureRemoveActionInput
import com.dudoziworkshop.dzlog.feature.table.editor.TableUndoManager
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorExitCoordinator
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.DeletedStructureSnapshot
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorSaveCoordinator
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorSaveResult
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.StructureRestoreAxis
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorStructureDeletionDebugInfo
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorStructureRestoreDebugInfo
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.FileNameSlotEditorUiResult
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.FileNameSlotEditorUiState
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorFileNameSlotUiHandlers
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.PathSlotEditorUiResult
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.PathSlotEditorUiState
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorPathSlotUiHandlers
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorModeTransitionHandlers
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorSlotDraftHandlers
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorSlotListHandlers
import com.dudoziworkshop.dzlog.feature.table.editor.handlers.TableEditorTransientStateHandlers
import com.dudoziworkshop.dzlog.feature.table.editor.resetColumnWeights
import com.dudoziworkshop.dzlog.feature.table.editor.resetRowWeights
import com.dudoziworkshop.dzlog.feature.table.editor.updateCell
import com.dudoziworkshop.dzlog.feature.table.model.TablePlacementState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.feature.table.placement.resolveRatioLockedSizeFromHeight
import com.dudoziworkshop.dzlog.feature.table.placement.resolveRatioLockedSizeFromWidth
import com.dudoziworkshop.dzlog.feature.table.policy.confirmCounterConflictDialog
import com.dudoziworkshop.dzlog.feature.table.policy.dismissCounterConflictDialog
import com.dudoziworkshop.dzlog.ui.common.dzScaffoldContent
import com.dudoziworkshop.dzlog.ui.table.editor.InlineEditState
import com.dudoziworkshop.dzlog.ui.table.editor.clearInlineEditing
import com.dudoziworkshop.dzlog.ui.table.editor.isEditing
import com.dudoziworkshop.dzlog.ui.table.editor.shouldBlockTabSwitchAfterCommit
import com.dudoziworkshop.dzlog.ui.table.editor.startInlineEditing
import com.dudoziworkshop.dzlog.ui.table.debug.TableEditorDebugOverlay
import com.dudoziworkshop.dzlog.ui.table.debug.TableEditorDebugOverlaySource
import com.dudoziworkshop.dzlog.ui.table.debug.buildTableEditorDebugOverlayState
import com.dudoziworkshop.dzlog.ui.table.format.TableFormatDialog
import com.dudoziworkshop.dzlog.ui.table.format.TableFormatDialogState
import com.dudoziworkshop.dzlog.ui.table.format.close
import com.dudoziworkshop.dzlog.ui.table.format.open
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
import com.dudoziworkshop.dzlog.ui.table.editor.commitInlineEditIfNeeded as commitInlineEdit

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
    onTemplateChange: (TableTemplateState) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDebugBuild = remember(context) {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

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
    var debugOverlayVisible by rememberSaveable { mutableStateOf(false) }
    var debugLastAction by rememberSaveable { mutableStateOf("init") }
    var latestStructureDeletionDebugInfo by remember { mutableStateOf<TableEditorStructureDeletionDebugInfo?>(null) }
    var latestStructureRestoreDebugInfo by remember { mutableStateOf<TableEditorStructureRestoreDebugInfo?>(null) }

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
    var initialPlacementSnapshot by remember { mutableStateOf(TablePlacementState()) }
    var isWmRatioLocked by rememberSaveable { mutableStateOf(false) }
    var tableStyleUi by remember { mutableStateOf(TableStyleState()) }
    var initialStyleSnapshot by remember { mutableStateOf(TableStyleState()) }
    val undoManager = remember { TableUndoManager<TableEditorUndoSnapshot>() }
    var undoRevision by remember { mutableIntStateOf(0) }
    var rowWeightsDragBaseTemplate by remember { mutableStateOf<TableTemplateState?>(null) }
    var colWeightsDragBaseTemplate by remember { mutableStateOf<TableTemplateState?>(null) }

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
        rowWeightsDragBaseTemplate = null
        colWeightsDragBaseTemplate = null
        val restored = undoTableEditorSnapshot(
            undoManager = undoManager,
            currentSnapshot = currentUndoSnapshot(),
        ) ?: return
        editableTemplateState = restored.templateState
        tableStyleUi = restored.styleState
        selectedCellId = restored.selectedCellId
        structureSelectedCellIds = restored.structureSelectedCellIds
        structureSelectionRange = restored.structureSelectionRange
        undoRevision += 1
    }

    fun applyTemplateDragCommitWithUndo(baseTemplate: TableTemplateState, nextTemplate: TableTemplateState) {
        val pushed = pushTemplateDragCommitUndoSnapshot(
            undoManager = undoManager,
            currentSnapshot = currentUndoSnapshot(),
            baseTemplate = baseTemplate,
            nextTemplate = nextTemplate,
        )
        if (!pushed) return
        updateTemplateDraft(nextTemplate)
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

    fun applyStructureActionResult(result: StructureActionResult) {
        result.deletedSnapshotToPush?.let { snapshot ->
            when (result.deletedSnapshotAxis) {
                StructureRestoreAxis.ROW -> deletedRowsStack.add(snapshot)
                StructureRestoreAxis.COL -> deletedColsStack.add(snapshot)
                null -> Unit
            }
        }
        when (result.consumedDeletedStackAxis) {
            StructureRestoreAxis.ROW -> if (deletedRowsStack.isNotEmpty()) deletedRowsStack.removeAt(deletedRowsStack.lastIndex)
            StructureRestoreAxis.COL -> if (deletedColsStack.isNotEmpty()) deletedColsStack.removeAt(deletedColsStack.lastIndex)
            null -> Unit
        }
        applyTemplateWithUndo(result.nextTemplate)
        fileNameSlotsDirtySinceStructureChange = result.nextFileNameSlotsDirtySinceStructureChange
        pathSlotsDirtySinceStructureChange = result.nextPathSlotsDirtySinceStructureChange
        structureSelectionRange = result.nextStructureSelectionRange
        structureSelectedCellIds = result.nextStructureSelectedCellIds
        selectedCellId = result.nextSelectedCellId
        debugLastAction = result.actionLabel
        latestStructureDeletionDebugInfo = result.deletionDebugInfo
        latestStructureRestoreDebugInfo = result.restoreDebugInfo
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


    RotatingPhraseDialogsHost(
        rotatingUi = rotatingUi,
        templateState = currentTemplate,
        onRotatingUiChange = { rotatingUi = it },
        updateTemplate = { updateTemplateDraft(it) },
        closeTemplateDialog = ::closeRotatingPhraseTemplateDialog,
    )


    // 주요 정책: 셀 선택 전에는 inline 값을 항상 먼저 commit 시도해 유실을 막는다.
    fun requestSelectCell(cellId: String?) {
        if (selectedCellId == cellId && !isStructureEditMode()) return
        if (inlineEdit.isEditing()) {
            commitInlineEditIfNeeded()
            if (shouldBlockTabSwitchAfterCommit(inlineEdit)) return
        }
        snapshotCellForEditSession(cellId)
        selectedCellId = cellId
        if (isStructureEditMode()) {
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

        val wasStructureMode = isStructureEditMode()
        val effects = TableEditorModeTransitionHandlers.resolveEffects(
            nextMode = nextMode,
            wasStructureMode = wasStructureMode,
        )
        if (effects.clearFileNameTransientState) {
            clearFileNameEditorTransientState(clearDraft = true)
        }
        if (effects.clearPathTransientState) {
            clearPathEditorTransientState(clearDraft = true)
        }
        bottomPanelMode = nextMode
        if (effects.shouldResetStructureSelection) {
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
                watermarkUi != initialPlacementSnapshot ||
                inlineEdit.isEditing() ||
                manualInputDraft.isNotBlank() ||
                pathManualInputDraft.isNotBlank()
        }
    }
    val isUndoAvailable by remember(undoRevision) {
        derivedStateOf { undoManager.canUndo() }
    }

    val debugOverlayState = buildTableEditorDebugOverlayState(
        TableEditorDebugOverlaySource(
            lastAction = debugLastAction,
            currentMode = if (isStructureEditMode()) "STRUCTURE_EDIT" else "NORMAL",
            bottomPanelMode = bottomPanelMode,
            rows = currentTemplate.rows,
            cols = currentTemplate.cols,
            selectedCellId = selectedCellId,
            selectedCell = selectedCell,
            selectionRange = structureSelectionRange,
            deletedRowsStackSize = deletedRowsStack.size,
            deletedColsStackSize = deletedColsStack.size,
            latestDeletionDebugInfo = latestStructureDeletionDebugInfo,
            latestRestoreDebugInfo = latestStructureRestoreDebugInfo,
            fileNameSlotsDirty = fileNameSlotsDirtySinceStructureChange,
            pathSlotsDirty = pathSlotsDirtySinceStructureChange,
            undoSummary = "canUndo=${isUndoAvailable}, revision=${undoRevision}",
        )
    )

    // Save/Reset/Back 동작은 editor 내부 local state를 기준으로 유지하되, 구현만 별도 helper로 분리한다.
    fun saveTemplate(exitAfterSave: Boolean = false) {
        commitInlineEditIfNeeded()
        if (shouldBlockTabSwitchAfterCommit(inlineEdit)) return
        isSavingTemplate = true
        val savePayload = editableTemplateState
        val stylePayload = tableStyleUi
        scope.launch {
            when (val result = TableEditorSaveCoordinator.persist(
                context = context,
                templatePayload = savePayload,
                stylePayload = stylePayload,
                placementPayload = watermarkUi,
                rollbackTemplate = initialTemplateSnapshot,
            )) {
                is TableEditorSaveResult.Failure -> {
                    Toast.makeText(
                        context,
                        result.message,
                        if (result.isLongToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT,
                    ).show()
                    isSavingTemplate = false
                    return@launch
                }

                is TableEditorSaveResult.Success -> {
                    watermarkUi = result.savedPlacement
                    initialTemplateSnapshot = savePayload
                    initialStyleSnapshot = stylePayload
                    initialPlacementSnapshot = result.savedPlacement
                    rowWeightsDragBaseTemplate = null
                    colWeightsDragBaseTemplate = null
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

    TableEditorUnsavedChangesHost(
        showUnsavedChangesDialog = showUnsavedChangesDialog,
        onRequestNavigateBack = ::requestNavigateBack,
        onSaveAndExit = {
            showUnsavedChangesDialog = false
            saveTemplate(exitAfterSave = true)
        },
        onDiscardAndExit = {
            showUnsavedChangesDialog = false
            undoManager.clear()
            undoRevision += 1
            onBack()
        },
        onCancel = { showUnsavedChangesDialog = false },
    )

    Box(modifier = Modifier.fillMaxSize()) {
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
                            isWmRatioLocked = isWmRatioLocked,
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
                                if (!isStructureEditMode()) return@LayoutTabActions
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
                                applyFileNameSlotUiResult(
                                    TableEditorFileNameSlotUiHandlers.selectSlot(
                                        state = currentFileNameSlotEditorState(),
                                        slotIndex = slotIndex,
                                    )
                                )
                            },
                            onFillEmptyFileNameSlot = { slotIndex ->
                                applyFileNameSlotUiResult(
                                    TableEditorFileNameSlotUiHandlers.fillEmptySlot(
                                        state = currentFileNameSlotEditorState(),
                                        requestedSlotIndex = slotIndex,
                                    )
                                )
                            },
                            onMoveSelectedFileNameSlotLeft = {
                                applyFileNameSlotUiResult(
                                    TableEditorFileNameSlotUiHandlers.moveSelectedLeft(
                                        state = currentFileNameSlotEditorState(),
                                        moveSlot = ::moveFileNameSlot,
                                    )
                                )
                            },
                            onMoveSelectedFileNameSlotRight = {
                                applyFileNameSlotUiResult(
                                    TableEditorFileNameSlotUiHandlers.moveSelectedRight(
                                        state = currentFileNameSlotEditorState(),
                                        moveSlot = ::moveFileNameSlot,
                                    )
                                )
                            },
                            onDeleteSelectedFileNameSlot = {
                                applyFileNameSlotUiResult(
                                    TableEditorFileNameSlotUiHandlers.deleteSelectedSlot(
                                        state = currentFileNameSlotEditorState(),
                                        removeSlotAt = ::removeFileNameSlotAt,
                                    )
                                )
                            },
                            onStartFileNameCellPick = {
                                applyFileNameSlotUiResult(
                                    TableEditorFileNameSlotUiHandlers.startCellPick(
                                        state = currentFileNameSlotEditorState(),
                                    )
                                )
                            },
                            onStartManualInputEditor = {
                                applyFileNameSlotUiResult(
                                    TableEditorFileNameSlotUiHandlers.startManualInput(
                                        state = currentFileNameSlotEditorState(),
                                    )
                                )
                            },
                            onManualInputDraftChange = {
                                applyFileNameSlotUiResult(
                                    TableEditorFileNameSlotUiHandlers.updateManualInputDraft(
                                        state = currentFileNameSlotEditorState(),
                                        draft = it,
                                    )
                                )
                            },
                            onApplyManualInput = {
                                applyFileNameSlotUiResult(
                                    TableEditorFileNameSlotUiHandlers.applyManualInput(
                                        state = currentFileNameSlotEditorState(),
                                    )
                                )
                            },
                            onBindSelectedSlotToCell = { cellId ->
                                applyFileNameSlotUiResult(
                                    TableEditorFileNameSlotUiHandlers.bindSelectedSlotToCell(
                                        state = currentFileNameSlotEditorState(),
                                        cellId = cellId,
                                        resolveCellLabel = { targetCellId ->
                                            currentTemplate.cells.firstOrNull { it.cellId == targetCellId }?.let { cell ->
                                                resolvedByCellId[cell.cellId]?.takeIf { it.isNotBlank() }
                                                    ?: "셀(${cell.rowIndex + 1},${cell.colIndex + 1})"
                                            } ?: "셀"
                                        },
                                    )
                                )
                            },
                            onSelectPathSlot = { slotIndex ->
                                applyPathSlotUiResult(
                                    TableEditorPathSlotUiHandlers.selectSlot(
                                        state = currentPathSlotEditorState(),
                                        slotIndex = slotIndex,
                                    )
                                )
                            },
                            onFillEmptyPathSlot = { slotIndex ->
                                applyPathSlotUiResult(
                                    TableEditorPathSlotUiHandlers.fillEmptySlot(
                                        state = currentPathSlotEditorState(),
                                        requestedSlotIndex = slotIndex,
                                    )
                                )
                            },
                            onMoveSelectedPathSlotLeft = {
                                applyPathSlotUiResult(
                                    TableEditorPathSlotUiHandlers.moveSelectedLeft(
                                        state = currentPathSlotEditorState(),
                                        moveSlot = ::movePathSlot,
                                    )
                                )
                            },
                            onMoveSelectedPathSlotRight = {
                                applyPathSlotUiResult(
                                    TableEditorPathSlotUiHandlers.moveSelectedRight(
                                        state = currentPathSlotEditorState(),
                                        moveSlot = ::movePathSlot,
                                    )
                                )
                            },
                            onDeleteSelectedPathSlot = {
                                applyPathSlotUiResult(
                                    TableEditorPathSlotUiHandlers.deleteSelectedSlot(
                                        state = currentPathSlotEditorState(),
                                        removeSlotAt = ::removePathSlotAt,
                                    )
                                )
                            },
                            onStartPathCellPick = {
                                applyPathSlotUiResult(
                                    TableEditorPathSlotUiHandlers.startCellPick(
                                        state = currentPathSlotEditorState(),
                                    )
                                )
                            },
                            onStartPathManualInputEditor = {
                                applyPathSlotUiResult(
                                    TableEditorPathSlotUiHandlers.startManualInput(
                                        state = currentPathSlotEditorState(),
                                    )
                                )
                            },
                            onPathManualInputDraftChange = {
                                applyPathSlotUiResult(
                                    TableEditorPathSlotUiHandlers.updateManualInputDraft(
                                        state = currentPathSlotEditorState(),
                                        draft = it,
                                    )
                                )
                            },
                            onApplyPathManualInput = {
                                applyPathSlotUiResult(
                                    TableEditorPathSlotUiHandlers.applyManualInput(
                                        state = currentPathSlotEditorState(),
                                    )
                                )
                            },
                            onBindSelectedPathSlotToCell = { cellId ->
                                applyPathSlotUiResult(
                                    TableEditorPathSlotUiHandlers.bindSelectedSlotToCell(
                                        state = currentPathSlotEditorState(),
                                        cellId = cellId,
                                        resolveCellLabel = { targetCellId ->
                                            currentTemplate.cells.firstOrNull { it.cellId == targetCellId }?.let { cell ->
                                                resolvedByCellId[cell.cellId]?.takeIf { it.isNotBlank() }
                                                    ?: "셀(${cell.rowIndex + 1},${cell.colIndex + 1})"
                                            } ?: "셀"
                                        },
                                    )
                                )
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
                                val result = TableEditorStructureActions.addOrRestore(
                                    TableEditorStructureAddOrRestoreActionInput(
                                        axis = StructureRestoreAxis.ROW,
                                        isStructureEditMode = isStructureEditMode(),
                                        structureSelectionRange = structureSelectionRange,
                                        selectedCellId = selectedCellId,
                                        currentTemplate = currentTemplate,
                                        deletedRowsStack = deletedRowsStack,
                                        deletedColsStack = deletedColsStack,
                                        currentFileNameSlots = fileNameSlotItems,
                                        currentPathSlots = pathSlotItems,
                                        fileNameSlotsDirtySinceStructureChange = fileNameSlotsDirtySinceStructureChange,
                                        pathSlotsDirtySinceStructureChange = pathSlotsDirtySinceStructureChange,
                                        sanitizeTemplate = ::sanitizePathGroupAfterStructureChange,
                                        applyFileNameSlots = ::withUpdatedFileNameSlots,
                                        applyPathSlots = ::withUpdatedPathSlots,
                                    )
                                )
                                applyStructureActionResult(result)
                            },
                            onRemoveRow = {
                                val result = TableEditorStructureActions.remove(
                                    TableEditorStructureRemoveActionInput(
                                        axis = StructureRestoreAxis.ROW,
                                        isStructureEditMode = isStructureEditMode(),
                                        structureSelectionRange = structureSelectionRange,
                                        selectedCellId = selectedCellId,
                                        currentTemplate = currentTemplate,
                                        currentFileNameSlots = fileNameSlotItems,
                                        currentPathSlots = pathSlotItems,
                                        fileNameSlotsDirtySinceStructureChange = fileNameSlotsDirtySinceStructureChange,
                                        pathSlotsDirtySinceStructureChange = pathSlotsDirtySinceStructureChange,
                                        sanitizeTemplate = ::sanitizePathGroupAfterStructureChange,
                                        applyFileNameSlots = ::withUpdatedFileNameSlots,
                                        applyPathSlots = ::withUpdatedPathSlots,
                                        removeCellRefsFromFileNameSlots = ::removeCellRefsFromFileNameSlots,
                                        removeCellRefsFromPathSlots = ::removeCellRefsFromPathSlots,
                                    ),
                                )
                                applyStructureActionResult(result)
                            },
                            onAddCol = {
                                val result = TableEditorStructureActions.addOrRestore(
                                    TableEditorStructureAddOrRestoreActionInput(
                                        axis = StructureRestoreAxis.COL,
                                        isStructureEditMode = isStructureEditMode(),
                                        structureSelectionRange = structureSelectionRange,
                                        selectedCellId = selectedCellId,
                                        currentTemplate = currentTemplate,
                                        deletedRowsStack = deletedRowsStack,
                                        deletedColsStack = deletedColsStack,
                                        currentFileNameSlots = fileNameSlotItems,
                                        currentPathSlots = pathSlotItems,
                                        fileNameSlotsDirtySinceStructureChange = fileNameSlotsDirtySinceStructureChange,
                                        pathSlotsDirtySinceStructureChange = pathSlotsDirtySinceStructureChange,
                                        sanitizeTemplate = ::sanitizePathGroupAfterStructureChange,
                                        applyFileNameSlots = ::withUpdatedFileNameSlots,
                                        applyPathSlots = ::withUpdatedPathSlots,
                                    )
                                )
                                applyStructureActionResult(result)
                            },
                            onRemoveCol = {
                                val result = TableEditorStructureActions.remove(
                                    TableEditorStructureRemoveActionInput(
                                        axis = StructureRestoreAxis.COL,
                                        isStructureEditMode = isStructureEditMode(),
                                        structureSelectionRange = structureSelectionRange,
                                        selectedCellId = selectedCellId,
                                        currentTemplate = currentTemplate,
                                        currentFileNameSlots = fileNameSlotItems,
                                        currentPathSlots = pathSlotItems,
                                        fileNameSlotsDirtySinceStructureChange = fileNameSlotsDirtySinceStructureChange,
                                        pathSlotsDirtySinceStructureChange = pathSlotsDirtySinceStructureChange,
                                        sanitizeTemplate = ::sanitizePathGroupAfterStructureChange,
                                        applyFileNameSlots = ::withUpdatedFileNameSlots,
                                        applyPathSlots = ::withUpdatedPathSlots,
                                        removeCellRefsFromFileNameSlots = ::removeCellRefsFromFileNameSlots,
                                        removeCellRefsFromPathSlots = ::removeCellRefsFromPathSlots,
                                    ),
                                )
                                applyStructureActionResult(result)
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
                                debugLastAction = "undo"
                                latestStructureRestoreDebugInfo = null
                                latestStructureDeletionDebugInfo = null
                                applyUndo()
                            },
                            onReset = {
                                debugLastAction = "reset"
                                latestStructureRestoreDebugInfo = null
                                latestStructureDeletionDebugInfo = null
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
                                rowWeightsDragBaseTemplate = null
                                colWeightsDragBaseTemplate = null
                                undoManager.clear()
                                undoRevision += 1
                            },
                            onSave = { saveTemplate(exitAfterSave = false) },
                            onDismissSettingsPanel = {
                                commitInlineEditIfNeeded()
                                showCellSettingsPanel = false
                            },
                            onToggleFileNameForSelected = { cellId, enabled ->
                                applyFileNameSlotUiResult(
                                    TableEditorFileNameSlotUiHandlers.toggleSelectedCell(
                                        state = currentFileNameSlotEditorState(),
                                        cellId = cellId,
                                        enabled = enabled,
                                    )
                                )
                            },
                            onReorderFileNameSlots = { fromIndex, toIndex ->
                                applyFileNameSlotUiResult(
                                    TableEditorFileNameSlotUiHandlers.reorderSlots(
                                        state = currentFileNameSlotEditorState(),
                                        fromIndex = fromIndex,
                                        toIndex = toIndex,
                                    )
                                )
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
                            onSetWmRatioLocked = { locked ->
                                isWmRatioLocked = locked
                            },
                            onSetWmWidthRatio = { width ->
                                val normalized = width.coerceIn(10, 100)
                                if (!isWmRatioLocked) {
                                    watermarkUi = watermarkUi.copy(wmWidthRatio = normalized)
                                } else {
                                    val ratioLocked = resolveRatioLockedSizeFromWidth(
                                        baseWidthRatio = watermarkUi.wmWidthRatio,
                                        baseHeightRatio = watermarkUi.wmHeightRatio,
                                        requestedWidthRatio = normalized,
                                    )
                                    watermarkUi = watermarkUi.copy(
                                        wmWidthRatio = ratioLocked.widthRatio,
                                        wmHeightRatio = ratioLocked.heightRatio,
                                    )
                                }
                            },
                            onSetWmHeightRatio = { height ->
                                val normalized = height.coerceIn(10, 100)
                                if (!isWmRatioLocked) {
                                    watermarkUi = watermarkUi.copy(wmHeightRatio = normalized)
                                } else {
                                    val ratioLocked = resolveRatioLockedSizeFromHeight(
                                        baseWidthRatio = watermarkUi.wmWidthRatio,
                                        baseHeightRatio = watermarkUi.wmHeightRatio,
                                        requestedHeightRatio = normalized,
                                    )
                                    watermarkUi = watermarkUi.copy(
                                        wmWidthRatio = ratioLocked.widthRatio,
                                        wmHeightRatio = ratioLocked.heightRatio,
                                    )
                                }
                            },
                            onStartRowWeightsDrag = {
                                rowWeightsDragBaseTemplate = editableTemplateState
                            },
                            onStartColumnWeightsDrag = {
                                colWeightsDragBaseTemplate = editableTemplateState
                            },
                            onFinishRowWeightsDrag = {
                                rowWeightsDragBaseTemplate = null
                            },
                            onFinishColumnWeightsDrag = {
                                colWeightsDragBaseTemplate = null
                            },
                            onCommitRowWeightsDragEnd = { nextWeights ->
                                val baseTemplate = rowWeightsDragBaseTemplate ?: editableTemplateState
                                rowWeightsDragBaseTemplate = null
                                val nextTemplate = TableHandleOverlay.applyRowWeightDragEnd(baseTemplate, nextWeights)
                                applyTemplateDragCommitWithUndo(baseTemplate, nextTemplate)
                            },
                            onCommitColumnWeightsDragEnd = { nextWeights ->
                                val baseTemplate = colWeightsDragBaseTemplate ?: editableTemplateState
                                colWeightsDragBaseTemplate = null
                                val nextTemplate = TableHandleOverlay.applyColumnWeightDragEnd(baseTemplate, nextWeights)
                                applyTemplateDragCommitWithUndo(baseTemplate, nextTemplate)
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
                            onOpenPlacementDialog = { showPlacementDialog = true }
                        )
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
                        wmHeightRatio = applied.wmHeightRatio.coerceIn(10, 100),
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

        if (isDebugBuild) {
            TableEditorDebugOverlay(
                visible = debugOverlayVisible,
                state = debugOverlayState,
                onToggle = { debugOverlayVisible = !debugOverlayVisible },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 12.dp)
            )
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
