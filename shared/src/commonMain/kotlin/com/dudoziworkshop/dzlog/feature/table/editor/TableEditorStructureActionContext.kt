package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.DeletedStructureSnapshot
import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotUiItem
import com.dudoziworkshop.dzlog.ui.table.section.PathSlotUiItem

data class StructureEditorContext(
    val currentTemplate: TableTemplateState,
    val currentFileNameSlots: List<FileNameSlotUiItem?>,
    val currentPathSlots: List<PathSlotUiItem?>,
    val fileNameSlotsDirtySinceStructureChange: Boolean,
    val pathSlotsDirtySinceStructureChange: Boolean,
    val deletedRowsStack: List<DeletedStructureSnapshot>,
    val deletedColsStack: List<DeletedStructureSnapshot>,
    val isStructureEditMode: Boolean,
    val structureSelectionRange: TableSelectionRange?,
    val selectedCellId: String?,
)

fun buildStructureEditorContext(
    currentTemplate: TableTemplateState,
    currentFileNameSlots: List<FileNameSlotUiItem?>,
    currentPathSlots: List<PathSlotUiItem?>,
    fileNameSlotsDirtySinceStructureChange: Boolean,
    pathSlotsDirtySinceStructureChange: Boolean,
    deletedRowsStack: List<DeletedStructureSnapshot>,
    deletedColsStack: List<DeletedStructureSnapshot>,
    isStructureEditMode: Boolean,
    structureSelectionRange: TableSelectionRange?,
    selectedCellId: String?,
): StructureEditorContext {
    return StructureEditorContext(
        currentTemplate = currentTemplate,
        currentFileNameSlots = currentFileNameSlots,
        currentPathSlots = currentPathSlots,
        fileNameSlotsDirtySinceStructureChange = fileNameSlotsDirtySinceStructureChange,
        pathSlotsDirtySinceStructureChange = pathSlotsDirtySinceStructureChange,
        deletedRowsStack = deletedRowsStack.toList(),
        deletedColsStack = deletedColsStack.toList(),
        isStructureEditMode = isStructureEditMode,
        structureSelectionRange = structureSelectionRange,
        selectedCellId = selectedCellId,
    )
}
