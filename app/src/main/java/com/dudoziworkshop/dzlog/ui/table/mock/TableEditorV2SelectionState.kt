package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionRange

internal class TableEditorV2SelectionState {
    var selectedCellId by mutableStateOf<String?>(null)
        private set

    var layoutSelection by mutableStateOf(MockLayoutSelection())
        private set

    // Selection in Content must not be overwritten by selection in Structure.
    private var lastEditCellId: String? = null

    fun selectEditCell(cellId: String) {
        selectedCellId = cellId
        lastEditCellId = cellId
    }

    fun restoreEditSelection(templateState: TableTemplateState) {
        val valid = templateState.cells.map { it.cellId }.toSet()
        val restored = lastEditCellId?.takeIf { it in valid }
            ?: selectedCellId?.takeIf { it in valid }
        selectedCellId = restored
        lastEditCellId = restored
    }

    fun restoreLayoutSelection(templateState: TableTemplateState) {
        val valid = templateState.cells.map { it.cellId }.toSet()
        val stored = layoutSelection
        if (stored.selectedCellIds.isNotEmpty()) {
            layoutSelection = if (stored.range != null &&
                stored.selectedCellIds.all { it in valid }) {
                normalizeMockLayoutSelection(templateState, stored.range)
            } else MockLayoutSelection()
        }
        syncFocusedCellFromLayout()
    }

    fun selectLayoutCell(
        templateState: TableTemplateState,
        tappedDomainCellId: String,
    ) {
        layoutSelection = selectMockLayoutCell(
            templateState = templateState,
            current = layoutSelection,
            tappedDomainCellId = tappedDomainCellId,
        )
        syncFocusedCellFromLayout()
    }

    fun selectLayoutRange(
        templateState: TableTemplateState,
        startDomainCellId: String,
        endDomainCellId: String,
    ) {
        layoutSelection = selectMockLayoutRange(
            templateState = templateState,
            startDomainCellId = startDomainCellId,
            endDomainCellId = endDomainCellId,
        )
        syncFocusedCellFromLayout()
    }

    fun normalizeLayoutSelection(
        templateState: TableTemplateState,
        range: TableSelectionRange?,
        collapseToTopLeft: Boolean,
    ) {
        layoutSelection = normalizeMockLayoutSelection(
            templateState = templateState,
            range = range,
            collapseToTopLeft = collapseToTopLeft,
        )
        syncFocusedCellFromLayout()
    }

    private fun syncFocusedCellFromLayout() {
        selectedCellId = layoutSelection.lastSelectedCellId
            ?.takeIf { it in layoutSelection.selectedCellIds }
            ?: layoutSelection.selectedCellIds.firstOrNull()
    }

    fun reconcileEditSelection(templateState: TableTemplateState) {
        val valid = templateState.cells.map { it.cellId }.toSet()
        if (selectedCellId !in valid) selectedCellId = null
        if (lastEditCellId !in valid) lastEditCellId = null
    }

    fun clearEditSelection() {
        selectedCellId = null
        lastEditCellId = null
    }

    fun clearLayoutSelection() {
        layoutSelection = MockLayoutSelection()
    }

    fun clearAll() {
        clearEditSelection()
        clearLayoutSelection()
    }
}
