package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionRange
import com.dudoziworkshop.dzlog.feature.table.editor.TableStructureRangeActions

class TableEditorV2SelectionState {
    var selectedCellId by mutableStateOf<String?>(null)
        private set

    var layoutSelection by mutableStateOf(MockLayoutSelection())
        private set

    private var editCellId: String? = null

    fun selectFirstEditableCellOnEntry(template: TableTemplateState) {
        TableStructureRangeActions.rootCells(template.cells)
            .minWithOrNull(compareBy({ it.rowIndex }, { it.colIndex }))
            ?.cellId?.let(::selectEditCell)
    }

    fun selectEditCell(cellId: String) {
        selectedCellId = cellId
        editCellId = cellId
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
        val roots = TableStructureRangeActions.rootCells(templateState.cells)
        editCellId = editCellId?.takeIf { id -> roots.any { it.cellId == id } }
        selectedCellId = editCellId
    }

    fun clearEditSelection() {
        selectedCellId = null
        editCellId = null
    }

    fun clearLayoutSelection() {
        layoutSelection = MockLayoutSelection()
    }

    fun clearAll() {
        clearEditSelection()
        clearLayoutSelection()
    }
}
