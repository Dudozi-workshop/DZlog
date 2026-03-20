package com.dudoziworkshop.dzlog.feature.table.editor.handlers

import com.dudoziworkshop.dzlog.ui.table.section.PathSlotKind
import com.dudoziworkshop.dzlog.ui.table.section.PathSlotUiItem

data class PathSlotEditorUiState(
    val slots: List<PathSlotUiItem?>,
    val selectedSlotIndex: Int?,
    val isCellPickMode: Boolean,
    val showManualInputEditor: Boolean,
    val manualInputDraft: String,
)

data class PathSlotEditorUiResult(
    val nextSlots: List<PathSlotUiItem?>,
    val nextSelectedSlotIndex: Int?,
    val isCellPickMode: Boolean,
    val showManualInputEditor: Boolean,
    val manualInputDraft: String,
    val markDirty: Boolean,
)

object TableEditorPathSlotUiHandlers {
    fun selectSlot(
        state: PathSlotEditorUiState,
        slotIndex: Int,
    ): PathSlotEditorUiResult {
        return snapshot(
            state = state,
            nextSelectedSlotIndex = slotIndex,
            isCellPickMode = false,
            showManualInputEditor = false,
            manualInputDraft = "",
        )
    }

    fun fillEmptySlot(
        state: PathSlotEditorUiState,
        requestedSlotIndex: Int,
    ): PathSlotEditorUiResult {
        val firstEmptyIndex = state.slots.indexOfFirst { it == null }
        if (firstEmptyIndex < 0) {
            return snapshot(
                state = state,
                nextSelectedSlotIndex = requestedSlotIndex,
            )
        }

        val nextSlots = state.slots.toMutableList().apply {
            this[firstEmptyIndex] = PathSlotUiItem(
                kind = PathSlotKind.CELL,
                label = "셀",
            )
        }

        return snapshot(
            state = state,
            nextSlots = nextSlots,
            nextSelectedSlotIndex = firstEmptyIndex,
            isCellPickMode = false,
            showManualInputEditor = false,
            manualInputDraft = "",
            markDirty = true,
        )
    }

    fun moveSelectedLeft(
        state: PathSlotEditorUiState,
        moveSlot: (List<PathSlotUiItem?>, Int, Int) -> List<PathSlotUiItem?>,
    ): PathSlotEditorUiResult {
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
        state: PathSlotEditorUiState,
        moveSlot: (List<PathSlotUiItem?>, Int, Int) -> List<PathSlotUiItem?>,
    ): PathSlotEditorUiResult {
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
        state: PathSlotEditorUiState,
        removeSlotAt: (List<PathSlotUiItem?>, Int) -> List<PathSlotUiItem?>,
    ): PathSlotEditorUiResult {
        val selected = state.selectedSlotIndex
            ?: return snapshot(
                state = state,
                isCellPickMode = false,
                showManualInputEditor = false,
                manualInputDraft = "",
            )

        if (state.slots.getOrNull(selected) == null) {
            return snapshot(
                state = state,
                isCellPickMode = false,
                showManualInputEditor = false,
                manualInputDraft = "",
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
            manualInputDraft = "",
            markDirty = true,
        )
    }

    fun startCellPick(
        state: PathSlotEditorUiState,
    ): PathSlotEditorUiResult {
        if (state.selectedSlotIndex == null) return snapshot(state = state)
        return snapshot(
            state = state,
            isCellPickMode = true,
            showManualInputEditor = false,
        )
    }

    fun startManualInput(
        state: PathSlotEditorUiState,
    ): PathSlotEditorUiResult {
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
        state: PathSlotEditorUiState,
        draft: String,
    ): PathSlotEditorUiResult {
        return snapshot(
            state = state,
            manualInputDraft = draft,
        )
    }

    fun applyManualInput(
        state: PathSlotEditorUiState,
    ): PathSlotEditorUiResult {
        val selected = state.selectedSlotIndex ?: return snapshot(state = state)
        val trimmed = state.manualInputDraft.trim()
        if (trimmed.isEmpty()) return snapshot(state = state)

        val nextSlots = state.slots.toMutableList().apply {
            this[selected] = PathSlotUiItem(
                kind = PathSlotKind.MANUAL,
                label = trimmed,
                manualText = trimmed,
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

    fun bindSelectedSlotToCell(
        state: PathSlotEditorUiState,
        cellId: String,
        resolveCellLabel: (String) -> String,
    ): PathSlotEditorUiResult {
        val selected = state.selectedSlotIndex ?: return snapshot(state = state)
        val nextSlots = state.slots.toMutableList().apply {
            this[selected] = PathSlotUiItem(
                kind = PathSlotKind.CELL,
                label = resolveCellLabel(cellId),
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
        state: PathSlotEditorUiState,
        nextSlots: List<PathSlotUiItem?> = state.slots,
        nextSelectedSlotIndex: Int? = state.selectedSlotIndex,
        isCellPickMode: Boolean = state.isCellPickMode,
        showManualInputEditor: Boolean = state.showManualInputEditor,
        manualInputDraft: String = state.manualInputDraft,
        markDirty: Boolean = false,
    ): PathSlotEditorUiResult {
        return PathSlotEditorUiResult(
            nextSlots = nextSlots,
            nextSelectedSlotIndex = nextSelectedSlotIndex,
            isCellPickMode = isCellPickMode,
            showManualInputEditor = showManualInputEditor,
            manualInputDraft = manualInputDraft,
            markDirty = markDirty,
        )
    }
}
