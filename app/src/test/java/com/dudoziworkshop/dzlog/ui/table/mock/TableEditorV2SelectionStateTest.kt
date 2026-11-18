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

    @Test fun initialSelectionUsesFirstRowMajorVisibleRoot() {
        val shuffled = template.copy(cells = template.cells.reversed())
        val state = TableEditorV2SelectionState()
        state.selectFirstEditableCellOnEntry(shuffled)
        assertEquals("left", state.selectedCellId)
    }

    @Test fun initialSelectionSkipsCoveredMergedCells() {
        val coveredFirst = template.copy(cells = listOf(
            template.cells[0].copy(colSpan = 2),
            template.cells[1],
        ).reversed())
        val state = TableEditorV2SelectionState()
        state.selectFirstEditableCellOnEntry(coveredFirst)
        assertEquals("left", state.selectedCellId)
    }

    @Test fun explicitCloseAndTabReturnDoNotReinitializeSelection() {
        val state = TableEditorV2SelectionState()
        state.selectFirstEditableCellOnEntry(template)
        state.selectEditCell("right")
        state.selectFirstEditableCellOnEntry(template)
        assertEquals("right", state.selectedCellId)
        state.clearEditSelection()
        state.selectFirstEditableCellOnEntry(template)
        assertNull(state.selectedCellId)
        state.restoreEditSelection(template)
        assertNull(state.selectedCellId)
    }

    @Test fun emptyTemplateLeavesSelectionEmpty() {
        val state = TableEditorV2SelectionState()
        state.selectFirstEditableCellOnEntry(template.copy(cells = emptyList()))
        assertNull(state.selectedCellId)
    }

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

    @Test fun deletedContentSelectionDoesNotFallBackToStructureCell() {
        val state = TableEditorV2SelectionState()
        state.selectEditCell("right")
        state.restoreLayoutSelection(template)
        state.selectLayoutCell(template, "left")
        val reduced = template.copy(cells = template.cells.take(1))
        state.restoreEditSelection(reduced)
        assertNull(state.selectedCellId)
        state.restoreLayoutSelection(reduced)
        assertEquals("left", state.selectedCellId)
    }

    @Test fun explicitlyClearedContentSelectionStaysClearedOnTabReturn() {
        val state = TableEditorV2SelectionState()
        state.selectEditCell("left")
        state.clearEditSelection()
        state.restoreLayoutSelection(template)
        state.selectLayoutCell(template, "right")
        state.restoreEditSelection(template)
        assertNull(state.selectedCellId)
    }

    @Test fun selectedCellSurvivesStyleTabAndValidStructure() {
        val state = TableEditorV2SelectionState()
        state.selectEditCell("left")
        state.reconcileEditSelection(template) // switch to Style
        assertEquals("left", state.selectedCellId)
        state.reconcileEditSelection(template)
        assertEquals("left", state.selectedCellId)
    }

    @Test fun coveredContentCellIsNotRestoredAfterMerge() {
        val merged = template.copy(cells = listOf(
            template.cells[0].copy(colSpan = 2),
            template.cells[1],
        ))
        val state = TableEditorV2SelectionState()
        state.selectEditCell("right")
        // The covered cell still exists in the stored template but cannot be edited.
        state.restoreEditSelection(merged)
        assertNull(state.selectedCellId)

        // A root remains selectable; never replace the covered cell with a
        // different root without an explicit selection.
        state.selectEditCell("left")
        state.restoreEditSelection(merged)
        assertEquals("left", state.selectedCellId)
    }

    @Test fun coveredContentCellIsClearedWhenReconcilingStyleOrSaveTabs() {
        val merged = template.copy(cells = listOf(
            template.cells[0].copy(colSpan = 2),
            template.cells[1],
        ))
        val state = TableEditorV2SelectionState()
        state.selectEditCell("right")
        state.reconcileEditSelection(merged)
        assertNull(state.selectedCellId)
        state.restoreEditSelection(merged)
        assertNull(state.selectedCellId)
    }

    @Test fun removedSelectedCellIsClearedSafely() {
        val state = TableEditorV2SelectionState()
        state.selectEditCell("right")
        state.reconcileEditSelection(template.copy(cells = template.cells.take(1)))
        assertNull(state.selectedCellId)
    }
}
