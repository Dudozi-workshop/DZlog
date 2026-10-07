package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionRange
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionResolver
import com.dudoziworkshop.dzlog.feature.table.editor.StructureDraftRemoveInput
import com.dudoziworkshop.dzlog.feature.table.editor.TableEditorStructureActions
import com.dudoziworkshop.dzlog.feature.table.editor.TableMergeDecision
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionResult
import com.dudoziworkshop.dzlog.feature.table.editor.TableStructureRangeActions
import com.dudoziworkshop.dzlog.feature.table.editor.addColumnBySelection
import com.dudoziworkshop.dzlog.feature.table.editor.addRowBySelection
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.StructureRestoreAxis
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator

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
    val baseRange = result.range ?: return result.toMockLayoutSelection()
    val expandedRange = TableStructureRangeActions.expandRangeToMergedBlocks(
        cells = templateState.cells,
        base = baseRange,
    )
    val expandedIds = templateState.cells
        .filter { cell -> expandedRange.contains(cell.rowIndex, cell.colIndex) }
        .map { it.cellId }
        .toSet()
    return MockLayoutSelection(
        selectedCellIds = expandedIds,
        range = expandedRange,
        lastSelectedCellId = result.lastSelectedCellId,
    )
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
    TableEditorStructureActions.removeDraft(
        StructureDraftRemoveInput(
            axis = StructureRestoreAxis.ROW,
            currentTemplate = templateState,
            selectionRange = selection.range,
        )
    )

internal fun removeMockLayoutColumns(
    templateState: TableTemplateState,
    selection: MockLayoutSelection,
): TableTemplateState =
    TableEditorStructureActions.removeDraft(
        StructureDraftRemoveInput(
            axis = StructureRestoreAxis.COL,
            currentTemplate = templateState,
            selectionRange = selection.range,
        )
    )

internal fun resolveMockLayoutMergeDecision(
    templateState: TableTemplateState,
    selection: MockLayoutSelection,
    populatedCellIds: Set<String>,
): TableMergeDecision =
    TableStructureRangeActions.resolveMergeDecision(
        templateState = templateState,
        selectionRange = selection.range,
        populatedCellIds = populatedCellIds,
    )

internal fun applyMockLayoutMergeDecision(
    templateState: TableTemplateState,
    decision: TableMergeDecision,
): TableTemplateState =
    TableStructureRangeActions.applyMergeDecision(templateState, decision)

internal fun mergeOrUnmergeMockLayoutSelection(
    templateState: TableTemplateState,
    selection: MockLayoutSelection,
): TableTemplateState =
    applyMockLayoutMergeDecision(
        templateState = templateState,
        decision = resolveMockLayoutMergeDecision(
            templateState = templateState,
            selection = selection,
            populatedCellIds = emptySet(),
        ),
    )

internal fun normalizeMockLayoutSelection(
    templateState: TableTemplateState,
    range: TableSelectionRange?,
): MockLayoutSelection {
    val normalizedRange = range ?: return MockLayoutSelection()
    val expanded = TableStructureRangeActions.expandRangeToMergedBlocks(
        cells = templateState.cells,
        base = normalizedRange,
    )
    val ids = templateState.cells
        .filter { cell -> expanded.contains(cell.rowIndex, cell.colIndex) }
        .map { it.cellId }
        .toSet()
    val rootId = TableStructureRangeActions.interactiveRootCellsInRange(
        cells = templateState.cells,
        range = expanded,
    ).firstOrNull()?.cellId
    return MockLayoutSelection(
        selectedCellIds = ids,
        range = expanded,
        lastSelectedCellId = rootId,
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


private const val MOCK_MIN_WEIGHT = 0.20f

internal fun adjustMockRowBoundary(
    templateState: TableTemplateState,
    boundaryIndex: Int,
    deltaFraction: Float,
): TableTemplateState {
    val weights = TableLayoutCalculator.resolveWeights(templateState.rowWeights, templateState.rows)
    return templateState.copy(
        rowWeights = adjustBoundaryWeights(weights, boundaryIndex, deltaFraction),
    )
}

internal fun adjustMockColumnBoundary(
    templateState: TableTemplateState,
    boundaryIndex: Int,
    deltaFraction: Float,
): TableTemplateState {
    val weights = TableLayoutCalculator.resolveWeights(templateState.colWeights, templateState.cols)
    return templateState.copy(
        colWeights = adjustBoundaryWeights(weights, boundaryIndex, deltaFraction),
    )
}

private fun adjustBoundaryWeights(
    weights: List<Float>,
    boundaryIndex: Int,
    deltaFraction: Float,
): List<Float> {
    if (weights.size < 2 || boundaryIndex !in 0 until weights.lastIndex) return weights
    val total = weights.sum().coerceAtLeast(0.0001f)
    val deltaWeight = deltaFraction * total
    val left = weights[boundaryIndex]
    val right = weights[boundaryIndex + 1]
    val clampedDelta = deltaWeight.coerceIn(
        MOCK_MIN_WEIGHT - left,
        right - MOCK_MIN_WEIGHT,
    )
    if (clampedDelta == 0f) return weights
    return weights.toMutableList().also {
        it[boundaryIndex] = left + clampedDelta
        it[boundaryIndex + 1] = right - clampedDelta
    }
}
