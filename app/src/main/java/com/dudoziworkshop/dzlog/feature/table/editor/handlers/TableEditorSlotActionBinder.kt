package com.dudoziworkshop.dzlog.feature.table.editor.handlers

import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotUiItem
import com.dudoziworkshop.dzlog.ui.table.section.PathSlotUiItem

data class TableEditorFileNameSlotActionBindings(
    val currentState: () -> FileNameSlotEditorUiState,
    val applyResult: (FileNameSlotEditorUiResult) -> Unit,
    val moveSlot: (List<FileNameSlotUiItem?>, Int, Int) -> List<FileNameSlotUiItem?>,
    val removeSlotAt: (List<FileNameSlotUiItem?>, Int) -> List<FileNameSlotUiItem?>,
    val resolveCellLabel: (String) -> String,
)

data class TableEditorPathSlotActionBindings(
    val currentState: () -> PathSlotEditorUiState,
    val applyResult: (PathSlotEditorUiResult) -> Unit,
    val moveSlot: (List<PathSlotUiItem?>, Int, Int) -> List<PathSlotUiItem?>,
    val removeSlotAt: (List<PathSlotUiItem?>, Int) -> List<PathSlotUiItem?>,
    val resolveCellLabel: (String) -> String,
)

object TableEditorSlotActionBinder {

    fun selectFileNameSlot(bindings: TableEditorFileNameSlotActionBindings, slotIndex: Int) {
        bindings.applyResult(
            TableEditorFileNameSlotUiHandlers.selectSlot(
                state = bindings.currentState(),
                slotIndex = slotIndex,
            )
        )
    }

    fun fillEmptyFileNameSlot(bindings: TableEditorFileNameSlotActionBindings, slotIndex: Int) {
        bindings.applyResult(
            TableEditorFileNameSlotUiHandlers.fillEmptySlot(
                state = bindings.currentState(),
                requestedSlotIndex = slotIndex,
            )
        )
    }

    fun moveSelectedFileNameSlotLeft(bindings: TableEditorFileNameSlotActionBindings) {
        bindings.applyResult(
            TableEditorFileNameSlotUiHandlers.moveSelectedLeft(
                state = bindings.currentState(),
                moveSlot = bindings.moveSlot,
            )
        )
    }

    fun moveSelectedFileNameSlotRight(bindings: TableEditorFileNameSlotActionBindings) {
        bindings.applyResult(
            TableEditorFileNameSlotUiHandlers.moveSelectedRight(
                state = bindings.currentState(),
                moveSlot = bindings.moveSlot,
            )
        )
    }

    fun deleteSelectedFileNameSlot(bindings: TableEditorFileNameSlotActionBindings) {
        bindings.applyResult(
            TableEditorFileNameSlotUiHandlers.deleteSelectedSlot(
                state = bindings.currentState(),
                removeSlotAt = bindings.removeSlotAt,
            )
        )
    }

    fun startFileNameCellPick(bindings: TableEditorFileNameSlotActionBindings) {
        bindings.applyResult(TableEditorFileNameSlotUiHandlers.startCellPick(bindings.currentState()))
    }

    fun startFileNameManualInput(bindings: TableEditorFileNameSlotActionBindings) {
        bindings.applyResult(TableEditorFileNameSlotUiHandlers.startManualInput(bindings.currentState()))
    }

    fun updateFileNameManualInputDraft(bindings: TableEditorFileNameSlotActionBindings, draft: String) {
        bindings.applyResult(
            TableEditorFileNameSlotUiHandlers.updateManualInputDraft(
                state = bindings.currentState(),
                draft = draft,
            )
        )
    }

    fun applyFileNameManualInput(bindings: TableEditorFileNameSlotActionBindings) {
        bindings.applyResult(TableEditorFileNameSlotUiHandlers.applyManualInput(bindings.currentState()))
    }

    fun bindSelectedFileNameSlotToCell(bindings: TableEditorFileNameSlotActionBindings, cellId: String) {
        bindings.applyResult(
            TableEditorFileNameSlotUiHandlers.bindSelectedSlotToCell(
                state = bindings.currentState(),
                cellId = cellId,
                resolveCellLabel = bindings.resolveCellLabel,
            )
        )
    }

    fun toggleFileNameForSelectedCell(
        bindings: TableEditorFileNameSlotActionBindings,
        cellId: String,
        enabled: Boolean,
    ) {
        bindings.applyResult(
            TableEditorFileNameSlotUiHandlers.toggleSelectedCell(
                state = bindings.currentState(),
                cellId = cellId,
                enabled = enabled,
            )
        )
    }

    fun reorderFileNameSlots(
        bindings: TableEditorFileNameSlotActionBindings,
        fromIndex: Int,
        toIndex: Int,
    ) {
        bindings.applyResult(
            TableEditorFileNameSlotUiHandlers.reorderSlots(
                state = bindings.currentState(),
                fromIndex = fromIndex,
                toIndex = toIndex,
            )
        )
    }

    fun selectPathSlot(bindings: TableEditorPathSlotActionBindings, slotIndex: Int) {
        bindings.applyResult(
            TableEditorPathSlotUiHandlers.selectSlot(
                state = bindings.currentState(),
                slotIndex = slotIndex,
            )
        )
    }

    fun fillEmptyPathSlot(bindings: TableEditorPathSlotActionBindings, slotIndex: Int) {
        bindings.applyResult(
            TableEditorPathSlotUiHandlers.fillEmptySlot(
                state = bindings.currentState(),
                requestedSlotIndex = slotIndex,
            )
        )
    }

    fun moveSelectedPathSlotLeft(bindings: TableEditorPathSlotActionBindings) {
        bindings.applyResult(
            TableEditorPathSlotUiHandlers.moveSelectedLeft(
                state = bindings.currentState(),
                moveSlot = bindings.moveSlot,
            )
        )
    }

    fun moveSelectedPathSlotRight(bindings: TableEditorPathSlotActionBindings) {
        bindings.applyResult(
            TableEditorPathSlotUiHandlers.moveSelectedRight(
                state = bindings.currentState(),
                moveSlot = bindings.moveSlot,
            )
        )
    }

    fun deleteSelectedPathSlot(bindings: TableEditorPathSlotActionBindings) {
        bindings.applyResult(
            TableEditorPathSlotUiHandlers.deleteSelectedSlot(
                state = bindings.currentState(),
                removeSlotAt = bindings.removeSlotAt,
            )
        )
    }

    fun startPathCellPick(bindings: TableEditorPathSlotActionBindings) {
        bindings.applyResult(TableEditorPathSlotUiHandlers.startCellPick(bindings.currentState()))
    }

    fun startPathManualInput(bindings: TableEditorPathSlotActionBindings) {
        bindings.applyResult(TableEditorPathSlotUiHandlers.startManualInput(bindings.currentState()))
    }

    fun updatePathManualInputDraft(bindings: TableEditorPathSlotActionBindings, draft: String) {
        bindings.applyResult(
            TableEditorPathSlotUiHandlers.updateManualInputDraft(
                state = bindings.currentState(),
                draft = draft,
            )
        )
    }

    fun applyPathManualInput(bindings: TableEditorPathSlotActionBindings) {
        bindings.applyResult(TableEditorPathSlotUiHandlers.applyManualInput(bindings.currentState()))
    }

    fun bindSelectedPathSlotToCell(bindings: TableEditorPathSlotActionBindings, cellId: String) {
        bindings.applyResult(
            TableEditorPathSlotUiHandlers.bindSelectedSlotToCell(
                state = bindings.currentState(),
                cellId = cellId,
                resolveCellLabel = bindings.resolveCellLabel,
            )
        )
    }
}
