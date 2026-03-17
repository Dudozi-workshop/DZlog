package com.dudoziworkshop.dzlog.ui.table

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.feature.table.model.TablePlacementState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.feature.table.placement.TablePlacementPreviewDialog
import com.dudoziworkshop.dzlog.ui.table.rotating.RotatingPhraseSetEditDialog
import com.dudoziworkshop.dzlog.ui.table.rotating.RotatingPhraseTemplateDialog
import com.dudoziworkshop.dzlog.ui.table.rotating.RotatingPhraseUiState
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import java.util.UUID

@Composable
internal fun UnsavedChangesDialog(
    visible: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
) {
    if (!visible) return
    AlertDialog(
        containerColor = DDZColor.Surface,
        onDismissRequest = onCancel,
        title = { Text("저장되지 않은 변경사항", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary) },
        text = { Text("변경사항을 저장하시겠습니까?", style = DDZTypography.Body, color = DDZColor.TextPrimary) },
        confirmButton = {
            TextButton(onClick = onSave) { Text("저장", style = DDZTypography.ButtonText, color = DDZColor.Primary) }
            TextButton(onClick = onDiscard) { Text("저장안함", style = DDZTypography.ButtonText, color = DDZColor.Primary) }
            TextButton(onClick = onCancel) { Text("취소", style = DDZTypography.ButtonText, color = DDZColor.Primary) }
        },
        dismissButton = {}
    )
}

@Composable
internal fun PlacementDialogHost(
    show: Boolean,
    templateState: TableTemplateState,
    resolvedCells: List<ResolvedCell>,
    styleState: TableStyleState,
    placementState: TablePlacementState,
    onApplyPlacement: (TablePlacementState) -> Unit,
    onClose: () -> Unit,
) {
    if (!show) return
    TablePlacementPreviewDialog(
        templateState = templateState,
        resolvedCells = resolvedCells,
        wmBgStyle = styleState.bgStyle,
        wmBgAlpha = styleState.bgAlpha,
        wmValueScale = styleState.valueScale,
        wmTextColorMode = styleState.textColorMode,
        wmManualTextColor = styleState.manualTextColor,
        wmTextAlign = styleState.textAlign,
        wmGridEnabled = styleState.gridEnabled,
        placementState = placementState,
        onApplyPlacement = onApplyPlacement,
        onClose = onClose,
    )
}

@Composable
internal fun RotatingPhraseDialogsHost(
    rotatingUi: RotatingPhraseUiState,
    templateState: TableTemplateState,
    onRotatingUiChange: (RotatingPhraseUiState) -> Unit,
    updateTemplate: (TableTemplateState) -> Unit,
    closeTemplateDialog: () -> Unit,
) {
    if (rotatingUi.isTemplateDialogOpen) {
        val dialogCell = templateState.cells.firstOrNull { it.cellId == rotatingUi.templateDialogCellId }
        if (dialogCell == null || dialogCell.dataType != TableCellDataType.ROTATING_TEXT) {
            closeTemplateDialog()
        } else {
            RotatingPhraseTemplateDialog(
                cell = dialogCell,
                phraseSets = templateState.phraseSets,
                onDismiss = closeTemplateDialog,
                onRestore = {
                    rotatingUi.templateDialogRestore?.let(updateTemplate)
                    closeTemplateDialog()
                },
                onSelectSet = { phraseSetId ->
                    updateTemplate(templateState.updateCell(dialogCell.cellId) { current ->
                        if (phraseSetId == null) current.copy(phraseSetId = null, everyOverride = null)
                        else current.copy(phraseSetId = phraseSetId)
                    })
                },
                onEveryChange = { every ->
                    updateTemplate(templateState.updateCell(dialogCell.cellId) { current ->
                        current.copy(everyOverride = every.coerceAtLeast(1))
                    })
                },
                onIncreaseEvery = {
                    val selectedSet = templateState.phraseSets.firstOrNull { it.id == dialogCell.phraseSetId }
                    val currentEvery = dialogCell.everyOverride ?: selectedSet?.defaultEvery ?: 1
                    updateTemplate(templateState.updateCell(dialogCell.cellId) { current ->
                        current.copy(everyOverride = (currentEvery + 1).coerceAtLeast(1))
                    })
                },
                onDecreaseEvery = {
                    val selectedSet = templateState.phraseSets.firstOrNull { it.id == dialogCell.phraseSetId }
                    val currentEvery = dialogCell.everyOverride ?: selectedSet?.defaultEvery ?: 1
                    updateTemplate(templateState.updateCell(dialogCell.cellId) { current ->
                        current.copy(everyOverride = (currentEvery - 1).coerceAtLeast(1))
                    })
                },
                onRequestCreateSet = { onRotatingUiChange(rotatingUi.copy(isCreateSetDialogOpen = true, createSetName = "")) },
                onRequestDeleteSet = { phraseSetId -> onRotatingUiChange(rotatingUi.copy(pendingDeleteSetId = phraseSetId)) },
                onRequestEditSet = { phraseSetId -> onRotatingUiChange(rotatingUi.copy(isSetEditDialogOpen = true, editingSetId = phraseSetId)) },
            )
        }
    }

    if (rotatingUi.isSetEditDialogOpen) {
        val editingSet = templateState.phraseSets.firstOrNull { it.id == rotatingUi.editingSetId }
        if (editingSet == null) {
            onRotatingUiChange(rotatingUi.copy(isSetEditDialogOpen = false, editingSetId = null))
        } else {
            RotatingPhraseSetEditDialog(
                phraseSet = editingSet,
                onClose = { onRotatingUiChange(rotatingUi.copy(isSetEditDialogOpen = false, editingSetId = null)) },
                onUpdateSet = { transform ->
                    updateTemplate(templateState.copy(phraseSets = templateState.phraseSets.map { set -> if (set.id == editingSet.id) transform(set) else set }))
                },
                onDeleteSet = { deleteId ->
                    updateTemplate(
                        templateState.copy(
                            phraseSets = templateState.phraseSets.filterNot { it.id == deleteId },
                            cells = templateState.cells.map { cell -> if (cell.phraseSetId == deleteId) cell.copy(phraseSetId = null, everyOverride = null) else cell }
                        )
                    )
                    onRotatingUiChange(rotatingUi.copy(isSetEditDialogOpen = false, editingSetId = null))
                }
            )
        }
    }


    rotatingUi.pendingDeleteSetId?.let { deleteId ->
        val deleteTarget = templateState.phraseSets.firstOrNull { it.id == deleteId }
        if (deleteTarget != null) {
            AlertDialog(
                containerColor = DDZColor.Surface,
                onDismissRequest = { onRotatingUiChange(rotatingUi.copy(pendingDeleteSetId = null)) },
                title = { Text("세트 삭제", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary) },
                text = { Text("${deleteTarget.name} 세트를 삭제하시겠습니까?", style = DDZTypography.Body, color = DDZColor.TextPrimary) },
                confirmButton = {
                    TextButton(onClick = {
                        updateTemplate(templateState.copy(
                            phraseSets = templateState.phraseSets.filterNot { it.id == deleteId },
                            cells = templateState.cells.map { cell -> if (cell.phraseSetId == deleteId) cell.copy(phraseSetId = null, everyOverride = null) else cell }
                        ))
                        onRotatingUiChange(rotatingUi.copy(pendingDeleteSetId = null))
                    }) { Text("삭제", style = DDZTypography.ButtonText, color = DDZColor.Primary) }
                },
                dismissButton = {
                    TextButton(onClick = { onRotatingUiChange(rotatingUi.copy(pendingDeleteSetId = null)) }) {
                        Text("취소", style = DDZTypography.ButtonText, color = DDZColor.Primary)
                    }
                }
            )
        } else {
            onRotatingUiChange(rotatingUi.copy(pendingDeleteSetId = null))
        }
    }

    if (rotatingUi.isCreateSetDialogOpen) {
        AlertDialog(
            containerColor = DDZColor.Surface,
            onDismissRequest = { onRotatingUiChange(rotatingUi.copy(isCreateSetDialogOpen = false, createSetName = "")) },
            title = { Text("새 템플릿 추가", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary) },
            text = {
                OutlinedTextField(
                    value = rotatingUi.createSetName,
                    onValueChange = { onRotatingUiChange(rotatingUi.copy(createSetName = it)) },
                    singleLine = true,
                    label = { Text("세트 이름") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = rotatingUi.createSetName.trim()
                    if (name.isNotEmpty() && rotatingUi.templateDialogCellId != null) {
                        val created = RotatingPhraseSet(UUID.randomUUID().toString(), name, emptyList(), 1)
                        val updated = templateState.copy(
                            phraseSets = templateState.phraseSets + created,
                            cells = templateState.cells.map { cell ->
                                if (cell.cellId == rotatingUi.templateDialogCellId) cell.copy(phraseSetId = created.id, everyOverride = null) else cell
                            }
                        )
                        updateTemplate(updated)
                        onRotatingUiChange(rotatingUi.copy(isCreateSetDialogOpen = false, createSetName = ""))
                    }
                }) { Text("추가", style = DDZTypography.ButtonText, color = DDZColor.Primary) }
            },
            dismissButton = {
                TextButton(onClick = { onRotatingUiChange(rotatingUi.copy(isCreateSetDialogOpen = false, createSetName = "")) }) {
                    Text("취소", style = DDZTypography.ButtonText, color = DDZColor.Primary)
                }
            }
        )
    }
}

private fun TableTemplateState.updateCell(cellId: String, transform: (com.dudoziworkshop.dzlog.domain.model.TableCellState) -> com.dudoziworkshop.dzlog.domain.model.TableCellState): TableTemplateState {
    return copy(cells = cells.map { if (it.cellId == cellId) transform(it) else it })
}
