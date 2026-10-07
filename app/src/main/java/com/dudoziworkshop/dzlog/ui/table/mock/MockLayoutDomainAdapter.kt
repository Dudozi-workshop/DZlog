package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionRange
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionResolver
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionResult
import com.dudoziworkshop.dzlog.feature.table.editor.TableStructureRangeActions
import com.dudoziworkshop.dzlog.feature.table.editor.addColumnBySelection
import com.dudoziworkshop.dzlog.feature.table.editor.addRowBySelection
import com.dudoziworkshop.dzlog.feature.table.editor.removeColumnBySelection
import com.dudoziworkshop.dzlog.feature.table.editor.removeRowBySelection

internal data class MockLayoutSelection(
    val selectedCellIds: Set<String> = emptySet(),
    val range: TableSelectionRange? = null,
    val lastSelectedCellId: String? = null,
)

internal fun selectMockLayoutCell(
    templateState: TableTemplateState,
    current: MockLayoutSelection,
    tappedDomainCellId: String,
): MockLayoutSelection {
    val result = TableSelectionResolver.selectByTap(
        cells = templateState.cells,
        current = TableSelectionResult(
            selectedCellIds = current.selectedCellIds,
            range = current.range,
            lastSelectedCellId = current.lastSelectedCellId,
        ),
        tappedCellId = tappedDomainCellId,
        additive = current.selectedCellIds.isNotEmpty(),
    )
    return result.toMockLayoutSelection()
}

internal fun addMockLayoutRow(
    templateState: TableTemplateState,
    selection: MockLayoutSelection,
): TableTemplateState =
    addRowBySelection(templateState, selection.range)

internal fun addMockLayoutColumn(
    templateState: TableTemplateState,
    selection: MockLayoutSelection,
): TableTemplateState =
    addColumnBySelection(templateState, selection.range)

internal fun removeMockLayoutRows(
    templateState: TableTemplateState,
    selection: MockLayoutSelection,
): TableTemplateState =
    removeRowBySelection(templateState, selection.range)

internal fun removeMockLayoutColumns(
    templateState: TableTemplateState,
    selection: MockLayoutSelection,
): TableTemplateState =
    removeColumnBySelection(templateState, selection.range)

internal fun mergeOrUnmergeMockLayoutSelection(
    templateState: TableTemplateState,
    selection: MockLayoutSelection,
): TableTemplateState {
    val range = selection.range ?: return templateState
    val selectedRoots = TableStructureRangeActions.interactiveRootCellsInRange(
        cells = templateState.cells,
        range = range,
    )
    val singleRoot = selectedRoots.singleOrNull()
    if (
        singleRoot != null &&
        (singleRoot.rowSpan > 1 || singleRoot.colSpan > 1) &&
        range.minRow == singleRoot.rowIndex &&
        range.maxRow == singleRoot.rowIndex + singleRoot.rowSpan - 1 &&
        range.minCol == singleRoot.colIndex &&
        range.maxCol == singleRoot.colIndex + singleRoot.colSpan - 1
    ) {
        return TableStructureRangeActions.unmergeRoot(
            templateState = templateState,
            rootCellId = singleRoot.cellId,
        )
    }
    return TableStructureRangeActions.mergeSelection(
        templateState = templateState,
        selectionRange = range,
    )
}

internal fun isMockLayoutSelectionMerged(
    templateState: TableTemplateState,
    selection: MockLayoutSelection,
): Boolean {
    val range = selection.range ?: return false
    val selectedRoots = TableStructureRangeActions.interactiveRootCellsInRange(
        cells = templateState.cells,
        range = range,
    )
    val root = selectedRoots.singleOrNull() ?: return false
    return (root.rowSpan > 1 || root.colSpan > 1) &&
        range.minRow == root.rowIndex &&
        range.maxRow == root.rowIndex + root.rowSpan - 1 &&
        range.minCol == root.colIndex &&
        range.maxCol == root.colIndex + root.colSpan - 1
}

private fun TableSelectionResult.toMockLayoutSelection(): MockLayoutSelection =
    MockLayoutSelection(
        selectedCellIds = selectedCellIds,
        range = range,
        lastSelectedCellId = lastSelectedCellId,
    )
