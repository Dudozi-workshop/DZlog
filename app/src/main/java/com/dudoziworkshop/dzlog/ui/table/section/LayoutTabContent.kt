package com.dudoziworkshop.dzlog.ui.table.section

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.ui.common.DDZSectionHeader
import com.dudoziworkshop.dzlog.ui.table.BottomFixedActionBar
import com.dudoziworkshop.dzlog.ui.table.CellSettingsBottomPanel
import com.dudoziworkshop.dzlog.ui.table.CompactPathHeader
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private val BottomBarReserveHeight = 140.dp

@Composable
fun LayoutTabContent(
    uiState: LayoutTabUiState,
    actions: LayoutTabActions,
    modifier: Modifier = Modifier
) {
    val isInlineEditing = uiState.editingCellId != null

    Box(modifier = modifier.fillMaxSize()) {
        val contentColumnModifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
                .let { baseModifier ->
                    if (isInlineEditing) {
                        baseModifier.imePadding()
                    } else {
                        baseModifier.padding(bottom = BottomBarReserveHeight)
                    }
                }

        Column(
            modifier = contentColumnModifier
        ) {
            Column(modifier = Modifier.weight(1f)) {
                CompactPathHeader(
                    savePath = uiState.savePathPreview,
                    fileName = uiState.filenamePreview,
                    fileNameRightLabel = uiState.counterModeLabel
                )

                Spacer(Modifier.height(10.dp))
                DDZSectionHeader(title = "셀 구성")
                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .background(DDZColor.Card, RoundedCornerShape(14.dp))
                        .padding(10.dp)
                ) {
                    TableGridSection(
                        templateState = uiState.templateState,
                        displayTextProvider = { cellId ->
                            uiState.plan.resolvedCells.firstOrNull { it.id == cellId }?.resolvedText.orEmpty()
                        },
                        selectedCellId = uiState.selectedCellId,
                        editingCellId = uiState.editingCellId,
                        onSelectCell = { id ->
                            if (uiState.editingCellId != null && uiState.editingCellId != id) {
                                if (!actions.onTryCommitInlineAndContinue()) return@TableGridSection
                            }
                            actions.onSelectCellId(id)
                            actions.onShowCellSettingsPanel(true)
                        },
                        onDoubleClickCell = { cell ->
                            if (uiState.editingCellId != null && uiState.editingCellId != cell.cellId) {
                                if (!actions.onTryCommitInlineAndContinue()) return@TableGridSection
                                actions.onSelectCellId(cell.cellId)
                                actions.onShowCellSettingsPanel(true)
                                return@TableGridSection
                            }

                            actions.onSelectCellId(cell.cellId)
                            val canInline =
                                (cell.dataType == TableCellDataType.TEXT ||
                                    cell.dataType == TableCellDataType.NUMBER ||
                                    cell.dataType == TableCellDataType.COUNTER)

                            if (canInline) {
                                actions.onShowCellSettingsPanel(false)
                                actions.onStartInlineEditing(cell.cellId, cell.toEditableText())
                            } else {
                                if (cell.dataType == TableCellDataType.DATE || cell.dataType == TableCellDataType.TIME) {
                                    actions.onOpenFormatDialog(cell.cellId, cell.dataType)
                                } else if (cell.dataType == TableCellDataType.ROTATING_TEXT) {
                                    actions.onOpenRotatingTemplateDialogForSelected(cell.cellId)
                                } else {
                                    actions.onShowCellSettingsPanel(true)
                                }
                            }
                        },
                        editingValue = uiState.editingValue,
                        onEditingValueChange = actions.onEditingValueChange,
                        onCommitInline = actions.onCommitInline,
                        inlineFocusRequester = uiState.inlineFocusRequester,
                        onInlineFocusLostCommit = actions.onInlineFocusLostCommit
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "셀을 눌러 편집을 시작하세요.",
                        style = DDZTypography.Caption,
                        color = DDZColor.TextMuted
                    )
                }
            }
        }

        BottomFixedActionBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
            rows = uiState.templateState.rows,
            cols = uiState.templateState.cols,
            isSaving = uiState.isSavingTemplate,
            onAddRow = actions.onAddRow,
            onRemoveRow = actions.onRemoveRow,
            onAddCol = actions.onAddCol,
            onRemoveCol = actions.onRemoveCol,
            onReset = actions.onReset,
            onSave = actions.onSave
        )

        if (uiState.showCellSettingsPanel && uiState.selectedCell != null && uiState.editingCellId == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DDZColor.PrimaryDark.copy(alpha = 0.4f))
                    .clickable { actions.onDismissSettingsPanel() }
            )

            CellSettingsBottomPanel(
                modifier = Modifier.align(Alignment.BottomCenter),
                cell = uiState.selectedCell,
                hasGroup1 = uiState.hasGroup1,
                hasGroup2 = uiState.hasGroup2,
                templateState = uiState.templateState,
                onToggleFileNameForCell = actions.onToggleFileNameForSelected,
                onReorderFileNameSlots = actions.onReorderFileNameSlots,
                onPathGroupAction = actions.onPathGroupActionForSelected,
                onSetDataType = actions.onSetDataTypeForSelected,
                onSetCounterScopeMode = actions.onSetCounterScopeModeForSelected,
                onResetCounterSeed = actions.onResetCounterSeedForSelected,
                autoNextCounterValue = uiState.autoNextCounterValue,
                onOpenRotatingTemplateDialog = {
                    uiState.selectedCell?.let { actions.onOpenRotatingTemplateDialogForSelected(it.cellId) }
                },
                onSetRotatingCounterMode = actions.onSetRotatingCounterModeForSelected,
                onOpenFormatDialog = actions.onOpenFormatDialog,
                previewNow = uiState.previewNow,
                previewCounterDigits = uiState.previewCounterDigits,
                scopeNextCounter = uiState.scopeNextCounter,
                dateFormat = uiState.dateFormat,
                timeFormat = uiState.timeFormat,
                phraseSets = uiState.phraseSets
            )
        }
    }
}
