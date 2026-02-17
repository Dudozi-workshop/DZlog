package com.example.dzlog.ui.table.section

import androidx.compose.ui.focus.FocusRequester
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.table.ResolvePlan
import com.example.dzlog.ui.table.PathGroupAction

data class LayoutTabUiState(
    val savePathPreview: String,
    val filenamePreview: String,
    val counterModeLabel: String,
    val templateState: TableTemplateState,
    val plan: ResolvePlan,
    val selectedCellId: String?,
    val editingCellId: String?,
    val editingValue: String,
    val inlineFocusRequester: FocusRequester,
    val showCellSettingsPanel: Boolean,
    val selectedCell: TableCellState?,
    val hasGroup1: Boolean,
    val hasGroup2: Boolean,
    val isSavingTemplate: Boolean,
    val autoNextCounterValue: Int
)

data class LayoutTabActions(
    val onSelectCellId: (String?) -> Unit,
    val onShowCellSettingsPanel: (Boolean) -> Unit,
    val onStartInlineEditing: (cellId: String, initialText: String) -> Unit,
    val onOpenFormatDialog: (cellId: String, type: TableCellDataType) -> Unit,
    val onEditingValueChange: (String) -> Unit,
    val onCommitInline: () -> Unit,
    val onTryCommitInlineAndContinue: () -> Boolean,
    val onInlineFocusLostCommit: () -> Unit,
    val onAddRow: () -> Unit,
    val onRemoveRow: () -> Unit,
    val onAddCol: () -> Unit,
    val onRemoveCol: () -> Unit,
    val onReset: () -> Unit,
    val onSave: () -> Unit,
    val onDismissSettingsPanel: () -> Unit,
    val onSetFileNameIncludeForSelected: (Boolean) -> Unit,
    val onPathGroupActionForSelected: (PathGroupAction) -> Unit,
    val onSetDataTypeForSelected: (TableCellDataType) -> Unit,
    val onResetCounterSeedForSelected: () -> Unit,
    val onOpenRotatingTemplateDialogForSelected: (String) -> Unit
)
