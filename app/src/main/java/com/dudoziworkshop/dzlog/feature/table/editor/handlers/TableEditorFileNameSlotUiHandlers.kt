package com.dudoziworkshop.dzlog.feature.table.editor.handlers

import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotKind
import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotUiItem

data class FileNameSlotEditorUiState(
    val slots: List<FileNameSlotUiItem?>,
    val selectedSlotIndex: Int?,
    val isCellPickMode: Boolean,
    val showManualInputEditor: Boolean,
    val manualInputDraft: String,
)

data class FileNameSlotEditorUiResult(
    val nextSlots: List<FileNameSlotUiItem?>,
    val nextSelectedSlotIndex: Int?,
    val isCellPickMode: Boolean,
    val showManualInputEditor: Boolean,
    val manualInputDraft: String,
    val markDirty: Boolean,
)

object TableEditorFileNameSlotUiHandlers {
    fun selectSlot(
        state: FileNameSlotEditorUiState,
        slotIndex: Int,
    ): FileNameSlotEditorUiResult {
        return snapshot(
            state = state,
            nextSelectedSlotIndex = slotIndex,
            isCellPickMode = false,
            showManualInputEditor = false,
            manualInputDraft = "",
        )
    }

    fun fillEmptySlot(
        state: FileNameSlotEditorUiState,
        requestedSlotIndex: Int,
    ): FileNameSlotEditorUiResult {
        val firstEmptyIndex = state.slots.indexOfFirst { it == null }
        if (firstEmptyIndex < 0) {
            return snapshot(
                state = state,
                nextSelectedSlotIndex = requestedSlotIndex,
            )
        }

        val nextSlots = state.slots.toMutableList().apply {
            this[firstEmptyIndex] = FileNameSlotUiItem(
                kind = FileNameSlotKind.CELL,
                label = "셀",
            )
        }

        return snapshot(
            state = state,
            nextSlots = nextSlots,
            nextSelectedSlotIndex = firstEmptyIndex,
            isCellPickMode = false,
            showManualInputEditor = false,
            markDirty = true,
        )
    }

    fun moveSelectedLeft(
        state: FileNameSlotEditorUiState,
        moveSlot: (List<FileNameSlotUiItem?>, Int, Int) -> List<FileNameSlotUiItem?>,
    ): FileNameSlotEditorUiResult {
        val selected = state.selectedSlotIndex
            ?: return snapshot(
                state = state,
                isCellPickMode = false,
                showManualInputEditor = false,
            )

        val target = selected - 1
        if (target < 0) {
            return snapshot(
                state = state,
                isCellPickMode = false,
                showManualInputEditor = false,
            )
        }

        if (state.slots.getOrNull(selected) == null || state.slots.getOrNull(target) == null) {
            return snapshot(
                state = state,
                isCellPickMode = false,
                showManualInputEditor = false,
            )
        }

        return snapshot(
            state = state,
            nextSlots = moveSlot(state.slots, selected, target),
            nextSelectedSlotIndex = target,
            isCellPickMode = false,
            showManualInputEditor = false,
            markDirty = true,
        )
    }

    fun moveSelectedRight(
        state: FileNameSlotEditorUiState,
        moveSlot: (List<FileNameSlotUiItem?>, Int, Int) -> List<FileNameSlotUiItem?>,
    ): FileNameSlotEditorUiResult {
        val selected = state.selectedSlotIndex
            ?: return snapshot(
                state = state,
                isCellPickMode = false,
                showManualInputEditor = false,
            )

        val target = selected + 1
        if (target >= state.slots.size) {
            return snapshot(
                state = state,
                isCellPickMode = false,
                showManualInputEditor = false,
            )
        }

        if (state.slots.getOrNull(selected) == null || state.slots.getOrNull(target) == null) {
            return snapshot(
                state = state,
                isCellPickMode = false,
                showManualInputEditor = false,
            )
        }

        return snapshot(
            state = state,
            nextSlots = moveSlot(state.slots, selected, target),
            nextSelectedSlotIndex = target,
            isCellPickMode = false,
            showManualInputEditor = false,
            markDirty = true,
        )
    }

    fun deleteSelectedSlot(
        state: FileNameSlotEditorUiState,
        removeSlotAt: (List<FileNameSlotUiItem?>, Int) -> List<FileNameSlotUiItem?>,
    ): FileNameSlotEditorUiResult {
        val selected = state.selectedSlotIndex
            ?: return snapshot(
                state = state,
                isCellPickMode = false,
                showManualInputEditor = false,
            )

        if (state.slots.getOrNull(selected) == null) {
            return snapshot(
                state = state,
                isCellPickMode = false,
                showManualInputEditor = false,
            )
        }

        val nextSlots = removeSlotAt(state.slots, selected)
        val nextFilledIndex = nextSlots.indexOfFirst { it != null }.takeIf { it >= 0 }

        return snapshot(
            state = state,
            nextSlots = nextSlots,
            nextSelectedSlotIndex = nextFilledIndex,
            isCellPickMode = false,
            showManualInputEditor = false,
            markDirty = true,
        )
    }

    fun startCellPick(
        state: FileNameSlotEditorUiState,
    ): FileNameSlotEditorUiResult {
        if (state.selectedSlotIndex == null) return snapshot(state = state)
        return snapshot(
            state = state,
            isCellPickMode = true,
            showManualInputEditor = false,
        )
    }

    fun startManualInput(
        state: FileNameSlotEditorUiState,
    ): FileNameSlotEditorUiResult {
        val selected = state.selectedSlotIndex ?: return snapshot(state = state)
        val current = state.slots.getOrNull(selected)
        return snapshot(
            state = state,
            isCellPickMode = false,
            showManualInputEditor = true,
            manualInputDraft = current?.manualText ?: current?.label.orEmpty(),
        )
    }

    fun updateManualInputDraft(
        state: FileNameSlotEditorUiState,
        draft: String,
    ): FileNameSlotEditorUiResult {
        return snapshot(
            state = state,
            manualInputDraft = draft,
        )
    }

    fun applyManualInput(
        state: FileNameSlotEditorUiState,
    ): FileNameSlotEditorUiResult {
        val selected = state.selectedSlotIndex ?: return snapshot(state = state)
        val trimmed = state.manualInputDraft.trim()
        if (trimmed.isEmpty()) return snapshot(state = state)

        val nextSlots = state.slots.toMutableList().apply {
            this[selected] = FileNameSlotUiItem(
                kind = FileNameSlotKind.MANUAL,
                label = trimmed,
                manualText = trimmed,
            )
        }

        return snapshot(
            state = state,
            nextSlots = nextSlots,
            isCellPickMode = false,
            showManualInputEditor = false,
            markDirty = true,
        )
    }

    fun bindSelectedSlotToCell(
        state: FileNameSlotEditorUiState,
        cellId: String,
        cellLabel: String,
    ): FileNameSlotEditorUiResult {
        val selected = state.selectedSlotIndex ?: return snapshot(state = state)
        val nextSlots = state.slots.toMutableList().apply {
            this[selected] = FileNameSlotUiItem(
                kind = FileNameSlotKind.CELL,
                label = cellLabel,
                cellId = cellId,
            )
        }

        return snapshot(
            state = state,
            nextSlots = nextSlots,
            isCellPickMode = false,
            showManualInputEditor = false,
            manualInputDraft = "",
            markDirty = true,
        )
    }

    private fun snapshot(
        state: FileNameSlotEditorUiState,
        nextSlots: List<FileNameSlotUiItem?> = state.slots,
        nextSelectedSlotIndex: Int? = state.selectedSlotIndex,
        isCellPickMode: Boolean = state.isCellPickMode,
        showManualInputEditor: Boolean = state.showManualInputEditor,
        manualInputDraft: String = state.manualInputDraft,
        markDirty: Boolean = false,
    ): FileNameSlotEditorUiResult {
        return FileNameSlotEditorUiResult(
            nextSlots = nextSlots,
            nextSelectedSlotIndex = nextSelectedSlotIndex,
            isCellPickMode = isCellPickMode,
            showManualInputEditor = showManualInputEditor,
            manualInputDraft = manualInputDraft,
            markDirty = markDirty,
        )
    }
}