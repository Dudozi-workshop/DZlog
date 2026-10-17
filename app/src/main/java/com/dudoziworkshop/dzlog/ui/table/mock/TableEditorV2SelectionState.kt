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

    fun selectEditCell(cellId: String) {
        selectedCellId = cellId
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
        selectedCellId = tappedDomainCellId.takeIf { it in layoutSelection.selectedCellIds }
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
        selectedCellId = startDomainCellId.takeIf { it in layoutSelection.selectedCellIds }
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
    }

    fun reconcileEditSelection(templateState: TableTemplateState) {
        if (selectedCellId != null && templateState.cells.none { it.cellId == selectedCellId }) {
            selectedCellId = null
        }
    }

    fun clearEditSelection() {
        selectedCellId = null
    }

    fun clearLayoutSelection() {
        layoutSelection = MockLayoutSelection()
    }

    fun clearAll() {
        clearEditSelection()
        clearLayoutSelection()
    }
}
