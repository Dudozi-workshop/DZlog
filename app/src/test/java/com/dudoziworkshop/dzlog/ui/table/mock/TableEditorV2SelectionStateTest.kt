package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TableEditorV2SelectionStateTest {
    private val template = TableTemplateState(
        rows = 1, cols = 2,
        cells = listOf(
            TableCellState(rowIndex = 0, colIndex = 0, cellId = "left"),
            TableCellState(rowIndex = 0, colIndex = 1, cellId = "right"),
        ),
    )

    @Test fun structureCellSelectionCarriesIntoContentWhenNoEarlierEditSelection() {
        val state = TableEditorV2SelectionState()
        state.restoreLayoutSelection(template)
        state.selectLayoutCell(template, "right")
        assertEquals("right", state.selectedCellId)
        state.restoreEditSelection(template)
        assertEquals("right", state.selectedCellId)
    }

    @Test fun switchingTabsRestoresSeparateSelections() {
        val state = TableEditorV2SelectionState()
        state.selectEditCell("left")
        state.restoreLayoutSelection(template)
        state.selectLayoutCell(template, "right")
        state.restoreEditSelection(template)
        assertEquals("left", state.selectedCellId)
        state.restoreLayoutSelection(template)
        assertEquals("right", state.selectedCellId)
        assertEquals(setOf("right"), state.layoutSelection.selectedCellIds)
    }

    @Test fun unavailableSelectionsAreClearedWithoutChangingValidContentSelection() {
        val state = TableEditorV2SelectionState()
        state.selectEditCell("left")
        state.restoreLayoutSelection(template)
        state.selectLayoutCell(template, "right")
        val reduced = template.copy(cells = template.cells.take(1))
        state.restoreLayoutSelection(reduced)
        assertNull(state.selectedCellId)
        assertEquals(emptySet<String>(), state.layoutSelection.selectedCellIds)
        state.restoreEditSelection(reduced)
        assertEquals("left", state.selectedCellId)
    }

    @Test fun selectedCellSurvivesStyleTabAndValidStructure() {
        val state = TableEditorV2SelectionState()
        state.selectEditCell("left")
        state.reconcileEditSelection(template) // switch to Style
        assertEquals("left", state.selectedCellId)
        state.reconcileEditSelection(template)
        assertEquals("left", state.selectedCellId)
    }

    @Test fun removedSelectedCellIsClearedSafely() {
        val state = TableEditorV2SelectionState()
        state.selectEditCell("right")
        state.reconcileEditSelection(template.copy(cells = template.cells.take(1)))
        assertNull(state.selectedCellId)
    }
}
