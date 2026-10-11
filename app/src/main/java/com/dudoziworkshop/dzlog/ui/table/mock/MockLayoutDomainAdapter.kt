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
        current = TableSelectionResult(emptySet(), null, null),
        tappedCellId = tappedDomainCellId,
        additive = false,
    )
    val baseRange = result.range ?: return MockLayoutSelection()
    val expandedRange = TableStructureRangeActions.expandRangeToMergedBlocks(
        cells = templateState.cells,
        base = baseRange,
    )
    val root = TableStructureRangeActions.interactiveRootCellsInRange(
        cells = templateState.cells,
        range = expandedRange,
    ).firstOrNull() ?: return MockLayoutSelection()
    return MockLayoutSelection(
        selectedCellIds = setOf(root.cellId),
        range = expandedRange,
        lastSelectedCellId = root.cellId,
    )
}

internal fun selectMockLayoutRange(
    templateState: TableTemplateState,
    startDomainCellId: String,
    endDomainCellId: String,
): MockLayoutSelection {
    val result = TableSelectionResolver.selectByDrag(
        cells = templateState.cells,
        startCellId = startDomainCellId,
        endCellId = endDomainCellId,
    )
    val baseRange = result.range ?: return MockLayoutSelection()
    val expandedRange = TableStructureRangeActions.expandRangeToMergedBlocks(
        cells = templateState.cells,
        base = baseRange,
    )
    val rootIds = TableStructureRangeActions.interactiveRootCellsInRange(
        cells = templateState.cells,
        range = expandedRange,
    ).map { it.cellId }.toSet()
    return MockLayoutSelection(
        selectedCellIds = rootIds,
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
    collapseToTopLeft: Boolean = false,
): MockLayoutSelection {
    val normalizedRange = range ?: return MockLayoutSelection()
    val expanded = TableStructureRangeActions.expandRangeToMergedBlocks(
        cells = templateState.cells,
        base = normalizedRange,
    )
    val roots = TableStructureRangeActions.interactiveRootCellsInRange(
        cells = templateState.cells,
        range = expanded,
    )
    if (roots.isEmpty()) return MockLayoutSelection()
    if (roots.size == 1 || collapseToTopLeft) {
        val root = roots.minWith(compareBy({ it.rowIndex }, { it.colIndex }))
        val singleRange = if (root.rowSpan > 1 || root.colSpan > 1) {
            TableSelectionRange(
                minRow = root.rowIndex,
                maxRow = root.rowIndex + root.rowSpan - 1,
                minCol = root.colIndex,
                maxCol = root.colIndex + root.colSpan - 1,
            )
        } else {
            TableSelectionRange(root.rowIndex, root.rowIndex, root.colIndex, root.colIndex)
        }
        return MockLayoutSelection(
            selectedCellIds = setOf(root.cellId),
            range = singleRange,
            lastSelectedCellId = root.cellId,
        )
    }
    return MockLayoutSelection(
        selectedCellIds = roots.map { it.cellId }.toSet(),
        range = expanded,
        lastSelectedCellId = roots.last().cellId,
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
    if (!deltaFraction.isFinite() || boundaryIndex !in weights.indices) return weights
    // The terminal handle changes only the last cell; internal handles preserve the pair total.
    if (boundaryIndex == weights.lastIndex) {
        val next = (weights.last() + deltaFraction * weights.sum()).coerceIn(MOCK_MIN_WEIGHT, 6f)
        return if (next == weights.last()) weights else weights.toMutableList().also { it[boundaryIndex] = next }
    }
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
