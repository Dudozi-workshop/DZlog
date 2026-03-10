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
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureContext
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureNamingPolicy
import com.dudoziworkshop.dzlog.domain.counter.policy.CounterScopeSnapshot
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.CounterScopeMode
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.RotatingCounterMode
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.TimeFormatOptions
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.preview.computeNextDelayMillis
import com.dudoziworkshop.dzlog.domain.preview.PreviewPipelineInput
import com.dudoziworkshop.dzlog.domain.preview.buildPreviewPipeline
import com.dudoziworkshop.dzlog.domain.preview.decideTickUnitFromTemplate
import com.dudoziworkshop.dzlog.domain.table.TableResolver
import com.dudoziworkshop.dzlog.feature.table.policy.confirmCounterConflictDialog
import com.dudoziworkshop.dzlog.feature.table.policy.dismissCounterConflictDialog
import com.dudoziworkshop.dzlog.feature.table.policy.saveTableTemplate
import com.dudoziworkshop.dzlog.ui.table.counter.TableCounterUiState
import com.dudoziworkshop.dzlog.ui.table.counter.applyCounterConflictDialogEffect
import com.dudoziworkshop.dzlog.ui.table.counter.buildTableCounterStreamContext
import com.dudoziworkshop.dzlog.ui.table.counter.buildTableScopedCounterStream
import com.dudoziworkshop.dzlog.ui.table.counter.restoreCounterCellToAutoNext
import com.dudoziworkshop.dzlog.ui.table.counter.syncCounterStateForScope
import com.dudoziworkshop.dzlog.ui.table.counter.updateCounterCellAndPolicy
import com.dudoziworkshop.dzlog.ui.table.counter.updateCounterUiConflictDialogState
import com.dudoziworkshop.dzlog.ui.table.counter.updateCounterUiScopeFlags
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
import com.dudoziworkshop.dzlog.ui.table.section.TableEditorTabs
import com.dudoziworkshop.dzlog.ui.table.template.addColumn
import com.dudoziworkshop.dzlog.ui.table.template.addRow
import com.dudoziworkshop.dzlog.ui.table.template.removeColumn
import com.dudoziworkshop.dzlog.ui.table.template.removeRow
import com.dudoziworkshop.dzlog.ui.table.template.updateCell
import com.dudoziworkshop.dzlog.ui.table.watermark.TableWatermarkUiState
import com.dudoziworkshop.dzlog.ui.table.watermark.applyAnchorOffsetDragChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyBgAlphaChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyBgStyleChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyCaptureAspectChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyGridEnabledChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyHeightRatioChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyManualTextColorChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyTextAlignChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyTextColorModeChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyValueScaleChange
import com.dudoziworkshop.dzlog.ui.table.watermark.applyWidthRatioChange
import com.dudoziworkshop.dzlog.ui.table.watermark.loadTableWatermarkUiState
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
    // 정책 보강: 저장된 draft 슬롯 payload가 있으면 우선 복원하고,
    // 레거시 템플릿(셀 id만 저장된 경우)은 기존 fallback으로 UI 슬롯을 재구성한다.
    val fromDraftPayload = normalizeFileNameDraftSlots(template.fileNameSlotDrafts.map(::toFileNameUiSlotDraft))
    if (fromDraftPayload.any { it != null }) return fromDraftPayload

    return normalizeFileNameDraftSlots(
        template.fileNameSlots.map { cellId ->
            cellId?.let { key ->
                val cell = template.cells.firstOrNull { it.cellId == key }
                val fallbackLabel = "셀"
                val label = cell?.let { "셀(${it.rowIndex + 1},${it.colIndex + 1})" } ?: fallbackLabel
                FileNameSlotUiItem(
                    kind = FileNameSlotKind.CELL,
                    label = label,
                    cellId = key
                )
            }
        }
    )
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

    val initialTemplateSnapshot = remember { templateState }
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
        val cellSlots = normalized.map { it?.cellId }
        return base.copy(
            fileNameSlots = cellSlots,
            fileNameSlotDrafts = normalized.map(::toFileNameDomainSlotDraft)
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

    var selectedCellId by remember { mutableStateOf(currentTemplate.cells.firstOrNull()?.cellId) }
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
    var showFileNameFormatOptions by remember { mutableStateOf(false) }
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
        showFileNameFormatOptions = false
        showManualInputEditor = false
        if (clearDraft) {
            manualInputDraft = ""
        }
    }

    val pathSlotItems = buildPathDraftSlots(currentTemplate)
    var isPathCellPickMode by remember { mutableStateOf(false) }
    var showPathFormatOptions by remember { mutableStateOf(false) }
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
        showPathFormatOptions = false
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

    val dateFormatOptions = listOf(NamingFormatDefaults.DATE_FORMAT_DEFAULT, "yyyy_MM_dd", "yyyyMMdd")
    // TIME 형식은 Step3부터 토글 UI(12/24, 초, 구분자)로 설정한다.

    // NOTE: formatPattern 기반이 아니라 현재는 고정값. (기존 코드 유지)
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

    var counterUi by remember { mutableStateOf(TableCounterUiState()) }
    var resumeTick by remember { mutableIntStateOf(0) }
    var scopeInputTick by remember { mutableIntStateOf(0) }
    var scopeKeySnapshot by remember { mutableStateOf<String?>(null) }
    var lastFilenameScopeSignature by remember { mutableStateOf<String?>(null) }
    val hasTemplateCells = currentTemplate.cells.isNotEmpty()

    // 파일명 scope 사용 여부는 counterUi 기준으로 판정해,
    // 실제 counter scope 계산 기준과 signature 기준이 어긋나지 않게 맞춘다.
    val filenameScopeSignature = remember(currentTemplate.fileNameSlots, counterUi.includeFilenameInCounterScope) {
        if (!counterUi.includeFilenameInCounterScope) {
            "filename-scope-disabled"
        } else {
            currentTemplate.fileNameSlots.joinToString(separator = "|") { slot -> slot ?: "_" }
        }
    }

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
        filenameScopeSignature,
    ) {
        // 정책: 스코프 입력(셀/슬롯/문구세트)이 바뀌면 즉시 next counter 재동기화를 강제한다.
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
    ) {
        buildPreviewPipeline(
            input = PreviewPipelineInput(
                templateState = currentTemplate,
                captureNow = previewNow,
                counterDigits = previewCounterDigits,
                dateFormat = dateFormat,
                timeFormat = timeFormat,
                fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
                includePathInCounterScope = counterUi.includePathInCounterScope,
                includeFilenameInCounterScope = counterUi.includeFilenameInCounterScope,
                scopeNextCounter = counterUi.scopeNextCounter,
                phraseProgressCursor = phraseProgressCounter,
            ),
            tableResolver = tableResolver,
        )
    }
    val plan = previewPipeline.plan

    val isManualCounterModeDisplay by remember(
        counterUi.isManualCounterMode,
        counterUi.preserveManualCounterSeed,
        counterUi.scopeNextCounter,
        counterUi.autoNextCounterValue
    ) {
        derivedStateOf {
            counterUi.isManualCounterMode ||
                (counterUi.preserveManualCounterSeed && counterUi.scopeNextCounter > counterUi.autoNextCounterValue)
        }
    }

    val scopeValues = previewPipeline.scopeValues

    val counterStreamContext by remember(
        plan.resolvedCells,
        counterUi.scopeNextCounter,
        isManualCounterModeDisplay,
        scopeValues.dateScopeValues,
        scopeValues.timeScopeValues,
        scopeValues.phraseScopeValues,
    ) {
        derivedStateOf {
            buildTableCounterStreamContext(
                resolvedCells = plan.resolvedCells,
                fileNameSlots = currentTemplate.fileNameSlots,
                includeFilenameInCounterScope = counterUi.includeFilenameInCounterScope,
                dateScopeValues = scopeValues.dateScopeValues,
                timeScopeValues = scopeValues.timeScopeValues,
                phraseScopeValues = scopeValues.phraseScopeValues,
                scopeNextCounter = counterUi.scopeNextCounter,
                isManualCounterModeDisplay = isManualCounterModeDisplay,
            )
        }
    }

    val scopedCounterStream by remember(
        counterStreamContext,
        counterUi.includePathInCounterScope,
        counterUi.includeFilenameInCounterScope
    ) {
        derivedStateOf {
            buildTableScopedCounterStream(
                counterStreamContext = counterStreamContext,
                includePathInCounterScope = counterUi.includePathInCounterScope,
                includeFilenameInCounterScope = counterUi.includeFilenameInCounterScope,
            )
        }
    }

    LaunchedEffect(resumeTick) {
        scopeKeySnapshot = scopedCounterStream.scopeParts.scopeKey
    }
    val activeScopeKey = scopeKeySnapshot ?: scopedCounterStream.scopeParts.scopeKey

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
    ) {
        // 빈 템플릿은 카운터 재동기화 입력이 없으므로 SSOT 경로에서 조기 종료한다.
        if (!hasTemplateCells) return@LaunchedEffect

        val isFilenameScopeSignatureChanged =
            counterUi.includeFilenameInCounterScope &&
                (lastFilenameScopeSignature != null) &&
                (lastFilenameScopeSignature != filenameScopeSignature)

        val isExternalResync = (resumeTick != lastProcessedResumeTick) || (scopeInputTick != lastProcessedScopeInputTick)
        val syncResult = syncCounterStateForScope(
            context = context,
            templateState = currentTemplate,
            counterUi = counterUi,
            counterStreamContext = counterStreamContext,
            scopedCounterStream = scopedCounterStream,
            previewCounterDigits = previewCounterDigits,
            saveMode = tableSaveMode,
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
            updateCell = ::updateCell
        )
        val committedCellId = inlineEdit.editingCellId
        inlineEdit = result.nextInlineState
        result.openedCounterConflict?.let {
            counterUi = updateCounterUiConflictDialogState(counterUi, it)
        }
        result.committedCounterSeed?.let { seed ->
            val cellId = committedCellId ?: return@let
            updateCounterCellAndPolicy(
                context = context,
                templateState = currentTemplate,
                cellId = cellId,
                seed = seed,
                preserveManual = true,
                forcePolicyUpdate = false,
                scopedCounterStream = scopedCounterStream,
                previewCounterDigits = previewCounterDigits,
                saveMode = tableSaveMode,
                counterUi = counterUi,
                onTemplateChange = ::updateTemplateDraft,
                setCounterUi = { counterUi = it },
                updateCell = ::updateCell,
                scope = scope
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

    if (selectedCellId == null && currentTemplate.cells.isNotEmpty()) {
        selectedCellId = currentTemplate.cells.first().cellId
    }

    val selectedCell = currentTemplate.cells.firstOrNull { it.cellId == selectedCellId }

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

    var watermarkUi by remember { mutableStateOf(TableWatermarkUiState()) }

    LaunchedEffect(Unit) {
        runCatching {
            watermarkUi = loadTableWatermarkUiState(context)
        }
    }

    val namingPreview = remember(
        previewPipeline.namingPreview,
        plan.resolvedCells,
        counterStreamContext.nextCounter,
    ) {
        // 상단 프리뷰는 공용 pipeline 결과를 기본으로 사용하되,
        // 수동 카운터/충돌 조정으로 nextCounter가 달라진 경우에만 카운터만 교정한다.
        if (counterStreamContext.nextCounter == counterUi.scopeNextCounter) {
            previewPipeline.namingPreview
        } else {
            CaptureNamingPolicy.buildForCaptureWithCounter(
                captureContext = CaptureContext(
                    resolvedCells = plan.resolvedCells,
                    fileNameSlots = currentTemplate.fileNameSlots,
                    fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
                    counterDigits = previewCounterDigits,
                    dateFormat = NamingFormatDefaults.DATE_FORMAT_DEFAULT,
                    timeFormat = NamingFormatDefaults.TIME_FORMAT_CAPTURE_DEFAULT,
                    includePathInCounterScope = counterUi.includePathInCounterScope,
                    includeFilenameInCounterScope = counterUi.includeFilenameInCounterScope,
                    dateScopeValues = scopeValues.dateScopeValues,
                    timeScopeValues = scopeValues.timeScopeValues,
                    phraseScopeValues = scopeValues.phraseScopeValues,
                ),
                usedCounter = counterStreamContext.nextCounter,
            )
        }
    }
    val savePathPreview = namingPreview.relativePath
    val filenamePreview = namingPreview.displayName

    fun handleCounterConflictDialogEffect(effect: com.dudoziworkshop.dzlog.feature.table.policy.TableCounterConflictDialogEffect) {
        applyCounterConflictDialogEffect(
            effect = effect,
            context = context,
            templateState = currentTemplate,
            scopedCounterStream = scopedCounterStream,
            previewCounterDigits = previewCounterDigits,
            saveMode = tableSaveMode,
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
        if (selectedCellId == cellId) return
        if (inlineEdit.isEditing()) {
            commitInlineEditIfNeeded()
            if (shouldBlockTabSwitchAfterCommit(inlineEdit)) return
        }
        snapshotCellForEditSession(cellId)
        selectedCellId = cellId
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
        bottomPanelMode = nextMode
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
        inlineEdit,
        manualInputDraft,
        pathManualInputDraft
    ) {
        derivedStateOf {
            currentTemplate != initialTemplateSnapshot ||
                inlineEdit.isEditing() ||
                manualInputDraft.isNotBlank() ||
                pathManualInputDraft.isNotBlank()
        }
    }

    fun saveTemplateAndExit() {
        commitInlineEditIfNeeded()
        if (shouldBlockTabSwitchAfterCommit(inlineEdit)) return
        isSavingTemplate = true
        val savePayload = editableTemplateState
        scope.launch {
            saveTableTemplate(context, savePayload)
                .onFailure {
                    Toast.makeText(
                        context,
                        "저장 실패: ${it.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    isSavingTemplate = false
                }
                .onSuccess {
                    Toast.makeText(context, "저장됨", Toast.LENGTH_SHORT).show()
                    onTemplateChange(savePayload)
                    onBack()
                }
        }
    }

    fun requestNavigateBack() {
        if (hasUnsavedChanges) {
            showUnsavedChangesDialog = true
        } else {
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
                        saveTemplateAndExit()
                    }) {
                        Text("저장", style = DDZTypography.ButtonText, color = DDZColor.Primary)
                    }
                    TextButton(onClick = {
                        showUnsavedChangesDialog = false
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
            TableEditorTabs(
                selectedTabIndex = selectedTabIndex,
                onTabSelected = { requestTabSwitch(it) }
            )

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
                            showFileNameFormatOptions = showFileNameFormatOptions,
                            manualInputDraft = manualInputDraft,
                            showManualInputEditor = showManualInputEditor,
                            pathSlotItems = pathSlotItems,
                            isPathCellPickMode = isPathCellPickMode,
                            showPathFormatOptions = showPathFormatOptions,
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
                            wmBgStyle = watermarkUi.wmBgStyle
                        ),
                        actions = LayoutTabActions(
                            onSelectCellId = ::requestSelectCell,
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
                                    showFileNameFormatOptions = false
                                    showManualInputEditor = false
                                }
                            },
                            onMoveSelectedFileNameSlotLeft = {
                                isFileNameCellPickMode = false
                                showFileNameFormatOptions = false
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
                                showFileNameFormatOptions = false
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
                                showFileNameFormatOptions = false
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
                                    showFileNameFormatOptions = false
                                    showManualInputEditor = false
                                }
                            },
                            onToggleFileNameFormatOptions = {
                                if (currentlySelectedFileNameSlot != null) {
                                    isFileNameCellPickMode = false
                                    showManualInputEditor = false
                                    showFileNameFormatOptions = !showFileNameFormatOptions
                                }
                            },
                            onApplyFileNameFormatType = { formatType ->
                                val selected = currentlySelectedFileNameSlot
                                if (selected != null) {
                                    val normalized = normalizeFileNameDraftSlots(fileNameSlotItems)
                                    val next = normalized.toMutableList().apply {
                                        this[selected] = FileNameSlotUiItem(
                                            kind = FileNameSlotKind.FORMAT,
                                            label = when (formatType) {
                                                FileNameFormatType.DATE -> "날짜"
                                                FileNameFormatType.TIME -> "시간"
                                                FileNameFormatType.COUNTER -> "카운터"
                                                FileNameFormatType.ROTATING_TEXT -> "순환문구"
                                            },
                                            formatType = formatType
                                        )
                                    }
                                    updateFileNameSlotDraft(currentTemplate, next)
                                    isFileNameCellPickMode = false
                                    showFileNameFormatOptions = false
                                    showManualInputEditor = false
                                }
                            },
                            onStartManualInputEditor = {
                                val selected = currentlySelectedFileNameSlot
                                if (selected != null) {
                                    val current = normalizeFileNameDraftSlots(fileNameSlotItems).getOrNull(selected)
                                    manualInputDraft = current?.manualText ?: current?.label.orEmpty()
                                    isFileNameCellPickMode = false
                                    showFileNameFormatOptions = false
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
                                    showFileNameFormatOptions = false
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
                                    showPathFormatOptions = false
                                    showPathManualInputEditor = false
                                }
                            },
                            onTogglePathFormatOptions = {
                                if (currentlySelectedPathSlot != null) {
                                    isPathCellPickMode = false
                                    showPathManualInputEditor = false
                                    showPathFormatOptions = !showPathFormatOptions
                                }
                            },
                            onApplyPathFormatType = { formatType ->
                                val selected = currentlySelectedPathSlot
                                if (selected != null) {
                                    val normalized = normalizePathDraftSlots(pathSlotItems)
                                    val next = normalized.toMutableList().apply {
                                        this[selected] = PathSlotUiItem(
                                            kind = PathSlotKind.FORMAT,
                                            label = when (formatType) {
                                                PathFormatType.DATE -> "날짜"
                                                PathFormatType.TIME -> "시간"
                                                PathFormatType.ROTATING_TEXT -> "순환문구"
                                            },
                                            formatType = formatType
                                        )
                                    }
                                    updatePathSlotDraft(currentTemplate, next)
                                    clearPathEditorTransientState(clearDraft = true)
                                }
                            },
                            onStartPathManualInputEditor = {
                                val selected = currentlySelectedPathSlot
                                if (selected != null) {
                                    val current = normalizePathDraftSlots(pathSlotItems).getOrNull(selected)
                                    pathManualInputDraft = current?.manualText ?: current?.label.orEmpty()
                                    isPathCellPickMode = false
                                    showPathFormatOptions = false
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
                                    sanitizePathGroupAfterStructureChange(addRow(currentTemplate))
                                }
                                updateTemplateDraft(nextTemplate)
                                fileNameSlotsDirtySinceStructureChange = false
                                pathSlotsDirtySinceStructureChange = false
                            },
                            onRemoveRow = {
                                val lastRowIndex = currentTemplate.rows - 1
                                val lastRowCells = currentTemplate.cells
                                    .filter { it.rowIndex == lastRowIndex }
                                    .sortedBy { it.colIndex }

                                val deletedCellIds = lastRowCells.map { it.cellId }.toSet()
                                if (lastRowCells.isNotEmpty()) {
                                    deletedRowsStack.add(
                                        DeletedStructureSnapshot(
                                            cells = lastRowCells,
                                            fileNameSlotsSnapshot = fileNameSlotItems,
                                            pathSlotsSnapshot = pathSlotItems
                                        )
                                    )
                                }

                                // 정책: 구조 삭제 + 슬롯 정리를 하나의 템플릿으로 순차 가공 후 단일 update로 반영한다.
                                var nextTemplate = sanitizePathGroupAfterStructureChange(removeRow(currentTemplate))
                                nextTemplate = withUpdatedFileNameSlots(
                                    nextTemplate,
                                    removeCellRefsFromFileNameSlots(fileNameSlotItems, deletedCellIds)
                                )
                                nextTemplate = withUpdatedPathSlots(
                                    nextTemplate,
                                    removeCellRefsFromPathSlots(pathSlotItems, deletedCellIds)
                                )
                                updateTemplateDraft(nextTemplate)
                                fileNameSlotsDirtySinceStructureChange = false
                                pathSlotsDirtySinceStructureChange = false

                                if (nextTemplate.cells.none { it.cellId == selectedCellId }) {
                                    selectedCellId = nextTemplate.cells.firstOrNull()?.cellId
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
                                    sanitizePathGroupAfterStructureChange(addColumn(currentTemplate))
                                }
                                updateTemplateDraft(nextTemplate)
                                fileNameSlotsDirtySinceStructureChange = false
                                pathSlotsDirtySinceStructureChange = false
                            },
                            onRemoveCol = {
                                val lastColIndex = currentTemplate.cols - 1
                                val lastColCells = currentTemplate.cells
                                    .filter { it.colIndex == lastColIndex }
                                    .sortedBy { it.rowIndex }

                                val deletedCellIds = lastColCells.map { it.cellId }.toSet()
                                if (lastColCells.isNotEmpty()) {
                                    deletedColsStack.add(
                                        DeletedStructureSnapshot(
                                            cells = lastColCells,
                                            fileNameSlotsSnapshot = fileNameSlotItems,
                                            pathSlotsSnapshot = pathSlotItems
                                        )
                                    )
                                }

                                // 정책: 구조 삭제 + 슬롯 정리를 하나의 템플릿으로 순차 가공 후 단일 update로 반영한다.
                                var nextTemplate = sanitizePathGroupAfterStructureChange(removeColumn(currentTemplate))
                                nextTemplate = withUpdatedFileNameSlots(
                                    nextTemplate,
                                    removeCellRefsFromFileNameSlots(fileNameSlotItems, deletedCellIds)
                                )
                                nextTemplate = withUpdatedPathSlots(
                                    nextTemplate,
                                    removeCellRefsFromPathSlots(pathSlotItems, deletedCellIds)
                                )
                                updateTemplateDraft(nextTemplate)
                                fileNameSlotsDirtySinceStructureChange = false
                                pathSlotsDirtySinceStructureChange = false

                                if (nextTemplate.cells.none { it.cellId == selectedCellId }) {
                                    selectedCellId = nextTemplate.cells.firstOrNull()?.cellId
                                }
                            },
                            onReset = {
                                // 정책 변경: 초기화는 기본 템플릿이 아니라 "화면 진입 시점(initialTemplateSnapshot)" 복원이다.
                                editableTemplateState = initialTemplateSnapshot
                                selectedCellId = initialTemplateSnapshot.cells.firstOrNull()?.cellId
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
                            },
                            onSave = { saveTemplateAndExit() },
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
                                    // 정책 보정: legacy fileNameSlots가 아니라 현재 UI 슬롯 순서를 기준으로 재정렬한다.
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
                            onSetRotatingCounterModeForSelected = { mode ->
                                selectedCell?.let { cell ->
                                    if (cell.dataType != TableCellDataType.ROTATING_TEXT) return@let
                                    val updated = updateCell(currentTemplate, cell.cellId) { c ->
                                        c.copy(rotatingCounterMode = mode)
                                    }
                                    updateTemplateDraft(updated)
                                }
                            },
                            onResetCounterSeedForSelected = {
                                selectedCell?.let { cell ->
                                    if (cell.dataType != TableCellDataType.COUNTER) return@let
                                    val syncedCounterText = counterUi.autoNextCounterValue.toString()
                                    restoreCounterCellToAutoNext(
                                        context = context,
                                        templateState = currentTemplate,
                                        cellId = cell.cellId,
                                        scopedCounterStream = scopedCounterStream,
                                        previewCounterDigits = previewCounterDigits,
                                        saveMode = tableSaveMode,
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
                            onOpenRotatingTemplateDialogForSelected = { cellId ->
                                val cell = currentTemplate.cells.firstOrNull { it.cellId == cellId }
                                if (cell != null && cell.dataType == TableCellDataType.ROTATING_TEXT) {
                                    openRotatingPhraseTemplateDialog(cell.cellId)
                                } else {
                                    showCellSettingsPanel = true
                                }
                            },
                            onSaveSelectedCell = ::requestSaveSelectedCell,
                            onRevertSelectedCell = ::requestRevertSelectedCell
                        )
                    )
                }

                1 -> {
                    // ==========================
                    // 탭1: 표 미리보기
                    // ==========================
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
                        wmBgStyle = watermarkUi.wmBgStyle,
                        wmBgAlpha = watermarkUi.wmBgAlpha,
                        wmValueScale = watermarkUi.wmValueScale,
                        wmTextColorMode = watermarkUi.wmTextColorMode,
                        wmManualTextColor = watermarkUi.wmManualTextColor,
                        wmTextAlign = watermarkUi.wmTextAlign,
                        wmGridEnabled = watermarkUi.wmGridEnabled,
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
                            scope.launch {
                                watermarkUi = applyBgStyleChange(context, bgStyle, watermarkUi)
                            }
                        },
                        onBgAlphaChange = { alpha ->
                            scope.launch {
                                watermarkUi = applyBgAlphaChange(context, alpha, watermarkUi)
                            }
                        },
                        onValueScaleChange = { scale ->
                            scope.launch {
                                watermarkUi = applyValueScaleChange(context, scale, watermarkUi)
                            }
                        },
                        onTextColorModeChange = { mode ->
                            scope.launch {
                                watermarkUi = applyTextColorModeChange(context, mode, watermarkUi)
                            }
                        },
                        onManualTextColorChange = { color ->
                            scope.launch {
                                watermarkUi = applyManualTextColorChange(context, color, watermarkUi)
                            }
                        },
                        onTextAlignChange = { align ->
                            scope.launch {
                                watermarkUi = applyTextAlignChange(context, align, watermarkUi)
                            }
                        },
                        onGridEnabledChange = { enabled ->
                            scope.launch {
                                watermarkUi = applyGridEnabledChange(context, enabled, watermarkUi)
                            }
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
            rotatingCounterMode = null
        )

        TableCellDataType.NUMBER -> this.copy(
            dataType = newType,
            typedValue = CellValue.Number(this.rawText),
            timeFormatOptions = null,
            counterScopeMode = null,
            rotatingCounterMode = null
        )

        TableCellDataType.DATE -> this.copy(
            dataType = newType,
            typedValue = CellValue.Auto,
            timeFormatOptions = null,
            counterScopeMode = CounterScopeMode.EXCLUDE,
            rotatingCounterMode = null
        )

        TableCellDataType.TIME -> this.copy(
            dataType = newType,
            typedValue = CellValue.Auto,
            timeFormatOptions = this.timeFormatOptions ?: TimeFormatOptions(),
            counterScopeMode = CounterScopeMode.EXCLUDE,
            rotatingCounterMode = null
        )

        TableCellDataType.COUNTER -> this.copy(
            dataType = newType,
            typedValue = (this.typedValue as? CellValue.CounterSeed) ?: CellValue.CounterSeed(1),
            timeFormatOptions = null,
            counterScopeMode = null,
            rotatingCounterMode = null
        )

        TableCellDataType.ROTATING_TEXT -> this.copy(
            dataType = newType,
            typedValue = CellValue.Auto,
            timeFormatOptions = null,
            counterScopeMode = null,
            rotatingCounterMode = RotatingCounterMode.GLOBAL
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
