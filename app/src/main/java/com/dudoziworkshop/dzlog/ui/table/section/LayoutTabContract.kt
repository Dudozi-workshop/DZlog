package com.dudoziworkshop.dzlog.ui.table.section

import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.CounterScopeMode
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.table.ResolvePlan
import com.dudoziworkshop.dzlog.ui.table.PathGroupAction
import java.util.Date

enum class BottomEditorPanelMode {
    NONE,
    STRUCTURE_EDIT,
    STYLE_EDIT,
    CELL_EDIT,
    FILENAME_EDIT,
    PATH_EDIT
}

enum class FileNameSlotKind {
    CELL,
    FORMAT,
    MANUAL
}

enum class FileNameFormatType {
    DATE,
    TIME,
    COUNTER,
    ROTATING_TEXT
}

data class FileNameSlotUiItem(
    val kind: FileNameSlotKind,
    val label: String,
    val cellId: String? = null,
    val manualText: String? = null,
    val formatType: FileNameFormatType? = null
)

enum class PathSlotKind {
    CELL,
    FORMAT,
    MANUAL
}

enum class PathFormatType {
    DATE,
    TIME,
    ROTATING_TEXT
}

data class PathSlotUiItem(
    val kind: PathSlotKind,
    val label: String,
    val cellId: String? = null,
    val manualText: String? = null,
    val formatType: PathFormatType? = null
)

data class LayoutTabUiState(
    val savePathPreview: String,
    val filenamePreview: String,
    val counterModeLabel: String,
    val templateState: TableTemplateState,
    val plan: ResolvePlan,
    val resolvedByCellId: Map<String, String>,
    val previewNow: Date,
    val previewCounterDigits: Int,
    val scopeNextCounter: Int,
    // 순환문구 진행 커서(카운터 seed와 분리된 상태)
    val phraseProgressCursor: Int,
    val dateFormat: String,
    val timeFormat: String,
    val selectedCellId: String?,
    val editingCellId: String?,
    val editingValue: String,
    val bottomPanelMode: BottomEditorPanelMode,
    val currentlySelectedFileNameSlot: Int?,
    val currentlySelectedPathSlot: Int?,
    val fileNameSlotItems: List<FileNameSlotUiItem?>,
    val isFileNameCellPickMode: Boolean,
    val manualInputDraft: String,
    val showManualInputEditor: Boolean,
    val pathSlotItems: List<PathSlotUiItem?>,
    val isPathCellPickMode: Boolean,
    val showPathManualInputEditor: Boolean,
    val pathManualInputDraft: String,
    val showCellSettingsPanel: Boolean,
    val selectedCell: TableCellState?,
    val hasGroup1: Boolean,
    val hasGroup2: Boolean,
    val isSavingTemplate: Boolean,
    val autoNextCounterValue: Int,
    val phraseSets: List<RotatingPhraseSet>,
    val captureAspect: CaptureAspect,
    val wmWidthRatio: Int,
    val wmHeightRatio: Int,
    val isWmRatioLocked: Boolean,
    val wmBgStyle: Int,
    val wmBgAlpha: Int,
    val wmGridEnabled: Boolean,
    val wmTextColorMode: Int,
    val wmManualTextColor: Int,
    val wmValueScale: Int,
    val wmTextAlign: Int,
    val structureSelectedCellIds: Set<String>,
    val isUndoAvailable: Boolean,
)

data class LayoutTabActions(
    val onSelectCellId: (String?) -> Unit,
    val onSelectStructureRange: (startCellId: String, endCellId: String) -> Unit,
    val onChangeBottomPanelMode: (BottomEditorPanelMode) -> Unit,
    val onCloseBottomPanel: () -> Unit,
    val onShowCellSettingsPanel: (Boolean) -> Unit,
    val onSelectFileNameSlot: (Int) -> Unit,
    val onFillEmptyFileNameSlot: (Int) -> Unit,
    val onMoveSelectedFileNameSlotLeft: () -> Unit,
    val onMoveSelectedFileNameSlotRight: () -> Unit,
    val onDeleteSelectedFileNameSlot: () -> Unit,
    val onStartFileNameCellPick: () -> Unit,
    val onStartManualInputEditor: () -> Unit,
    val onManualInputDraftChange: (String) -> Unit,
    val onApplyManualInput: () -> Unit,
    val onBindSelectedSlotToCell: (String) -> Unit,
    val onSelectPathSlot: (Int) -> Unit,
    val onFillEmptyPathSlot: (Int) -> Unit,
    val onMoveSelectedPathSlotLeft: () -> Unit,
    val onMoveSelectedPathSlotRight: () -> Unit,
    val onDeleteSelectedPathSlot: () -> Unit,
    val onStartPathCellPick: () -> Unit,
    val onStartPathManualInputEditor: () -> Unit,
    val onPathManualInputDraftChange: (String) -> Unit,
    val onApplyPathManualInput: () -> Unit,
    val onBindSelectedPathSlotToCell: (String) -> Unit,
    val onStartInlineEditing: (cellId: String, initialText: String) -> Unit,
    val onOpenFormatDialog: (cellId: String, type: TableCellDataType) -> Unit,
    val onEditingValueChange: (String) -> Unit,
    val onCommitInline: () -> Unit,
    val onTryCommitInlineAndContinue: () -> Boolean,
    val onAddRow: () -> Unit,
    val onRemoveRow: () -> Unit,
    val onAddCol: () -> Unit,
    val onRemoveCol: () -> Unit,
    val onResetRowWeights: () -> Unit,
    val onResetColumnWeights: () -> Unit,
    val onResetAllWeights: () -> Unit,
    val onUndo: () -> Unit,
    val onReset: () -> Unit,
    val onSave: () -> Unit,
    val onDismissSettingsPanel: () -> Unit,
    val onToggleFileNameForSelected: (cellId: String, enabled: Boolean) -> Unit,
    val onReorderFileNameSlots: (fromIndex: Int, toIndex: Int) -> Unit,
    val onPathGroupActionForSelected: (PathGroupAction) -> Unit,
    val onSetDataTypeForSelected: (TableCellDataType) -> Unit,
    val onSetCounterScopeModeForSelected: (CounterScopeMode) -> Unit,
    val onResetCounterSeedForSelected: () -> Unit,
    val onSetBgStyle: (Int) -> Unit,
    val onSetGridEnabled: (Boolean) -> Unit,
    val onSetTextColorMode: (Int) -> Unit,
    val onSetManualTextColor: (Int) -> Unit,
    val onSetValueScale: (Int) -> Unit,
    val onSetTextAlign: (Int) -> Unit,
    val onSetWmRatioLocked: (Boolean) -> Unit,
    val onSetWmWidthRatio: (Int) -> Unit,
    val onSetWmHeightRatio: (Int) -> Unit,
    val onCommitRowWeightsDragEnd: (List<Float>) -> Unit,
    val onCommitColumnWeightsDragEnd: (List<Float>) -> Unit,
    val onOpenRotatingTemplateDialogForSelected: (String) -> Unit,
    val onSaveSelectedCell: () -> Unit,
    val onRevertSelectedCell: () -> Unit,
    val onOpenPreview: () -> Unit
)
