package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

enum class TableMergeDecisionType {
    NONE,
    MERGE,
    CONFIRM_MERGE,
    UNMERGE,
}

data class TableMergeDecision(
    val type: TableMergeDecisionType,
    val range: TableSelectionRange? = null,
    val rootCellId: String? = null,
)

object TableStructureRangeActions {

    fun resolveMergeDecision(
        templateState: TableTemplateState,
        selectionRange: TableSelectionRange?,
        populatedCellIds: Set<String>,
    ): TableMergeDecision {
        val range = selectionRange ?: return TableMergeDecision(TableMergeDecisionType.NONE)
        val selectedRoots = interactiveRootCellsInRange(templateState.cells, range)
        val singleRoot = selectedRoots.singleOrNull()
        if (
            singleRoot != null &&
            (singleRoot.rowSpan > 1 || singleRoot.colSpan > 1) &&
            range.minRow == singleRoot.rowIndex &&
            range.maxRow == singleRoot.rowIndex + singleRoot.rowSpan - 1 &&
            range.minCol == singleRoot.colIndex &&
            range.maxCol == singleRoot.colIndex + singleRoot.colSpan - 1
        ) {
            return TableMergeDecision(
                type = TableMergeDecisionType.UNMERGE,
                range = range,
                rootCellId = singleRoot.cellId,
            )
        }

        val expanded = expandRangeToMergedBlocks(templateState.cells, range)
        if (expanded != range || range.rowCount * range.colCount < 2) {
            return TableMergeDecision(TableMergeDecisionType.NONE)
        }

        val populatedCount = templateState.cells.count { cell ->
            range.contains(cell.rowIndex, cell.colIndex) && cell.cellId in populatedCellIds
        }
        return TableMergeDecision(
            type = if (populatedCount > 1) {
                TableMergeDecisionType.CONFIRM_MERGE
            } else {
                TableMergeDecisionType.MERGE
            },
            range = range,
        )
    }

    fun applyMergeDecision(
        templateState: TableTemplateState,
        decision: TableMergeDecision,
    ): TableTemplateState {
        return when (decision.type) {
            TableMergeDecisionType.MERGE,
            TableMergeDecisionType.CONFIRM_MERGE -> mergeSelection(
                templateState = templateState,
                selectionRange = decision.range,
            )
            TableMergeDecisionType.UNMERGE -> {
                val rootCellId = decision.rootCellId ?: return templateState
                unmergeRoot(templateState, rootCellId)
            }
            TableMergeDecisionType.NONE -> templateState
        }
    }

    data class StructureResolvedCell(
        val cellId: String,
        val rowIndex: Int,
        val colIndex: Int,
        val rootRowIndex: Int,
        val rootColIndex: Int,
        val rootRowSpan: Int,
        val rootColSpan: Int,
        val isCovered: Boolean,
    )

    fun rootCells(cells: List<TableCellState>): List<TableCellState> =
        resolveStructure(cells).rootCells

    fun interactiveRootCellsInRange(
        cells: List<TableCellState>,
        range: TableSelectionRange,
    ): List<TableCellState> {
        val resolution = resolveStructure(cells)
        val expanded = expandRangeToMergedBlocks(resolution, range)
        return resolution.rootCoverages
            .filter { rangesIntersect(expanded, it.range) }
            .map { it.root }
    }

    fun mergeSelection(
        templateState: TableTemplateState,
        selectionRange: TableSelectionRange?,
    ): TableTemplateState {
        val range = selectionRange ?: return templateState
        val baseCells = templateState.cells
        val resolution = resolveStructure(baseCells)
        val expanded = expandRangeToMergedBlocks(resolution, range)
        if (expanded != range) return templateState
        if (!isMergeableRectangle(baseCells, resolution, range)) return templateState

        val root = baseCells.firstOrNull { it.rowIndex == range.minRow && it.colIndex == range.minCol }
            ?: return templateState
        val nextCells = baseCells.map { cell ->
            when {
                cell.cellId == root.cellId -> cell.copy(
                    rowSpan = range.rowCount,
                    colSpan = range.colCount,
                )
                range.contains(cell.rowIndex, cell.colIndex) -> cell.copy(
                    rawText = "",
                    typedValue = CellValue.Auto,
                    timeFormatOptions = null,
                    rowSpan = 1,
                    colSpan = 1,
                    dataType = TableCellDataType.TEXT,
                    phraseSetId = null,
                    everyOverride = null,
                    formatPattern = "",
                    counterScopeMode = null,
                )
                else -> cell
            }
        }
        val nextResolution = resolveStructure(nextCells)
        return templateState.copy(cells = sanitizeCoveredCells(nextCells, nextResolution))
    }

    fun unmergeRoot(
        templateState: TableTemplateState,
        rootCellId: String,
    ): TableTemplateState {
        val root = templateState.cells.firstOrNull { it.cellId == rootCellId } ?: return templateState
        if (root.rowSpan <= 1 && root.colSpan <= 1) return templateState
        return templateState.copy(
            cells = templateState.cells.map { cell ->
                if (cell.cellId == rootCellId) {
                    cell.copy(rowSpan = 1, colSpan = 1)
                } else {
                    cell
                }
            }
        )
    }

    fun deleteSelectionWithAbsorb(
        templateState: TableTemplateState,
        selectionRange: TableSelectionRange?,
    ): TableTemplateState {
        val range = selectionRange ?: return templateState
        val baseCells = templateState.cells
        val resolution = resolveStructure(baseCells)
        val expanded = expandRangeToMergedBlocks(resolution, range)
        val direction = resolveAbsorbDirection(expanded, templateState.rows, templateState.cols) ?: return templateState

        val nextCells = absorbByDirection(
            cells = baseCells,
            resolution = resolution,
            range = expanded,
            direction = direction,
        )
        val nextResolution = resolveStructure(nextCells)
        return templateState.copy(cells = sanitizeCoveredCells(nextCells, nextResolution))
    }

    fun resolveRootCell(cells: List<TableCellState>, cell: TableCellState): TableCellState {
        val resolution = resolveStructure(cells)
        return resolveRootCell(resolution, cell)
    }

    fun isCoveredCell(cells: List<TableCellState>, row: Int, col: Int): Boolean {
        val resolution = resolveStructure(cells)
        return isCoveredCell(resolution, row, col)
    }

    fun expandRangeToMergedBlocks(
        cells: List<TableCellState>,
        base: TableSelectionRange,
    ): TableSelectionRange {
        return expandRangeToMergedBlocks(resolveStructure(cells), base)
    }

    fun resolveStructureCells(cells: List<TableCellState>): List<StructureResolvedCell> {
        val resolution = resolveStructure(cells)
        return cells.map { cell ->
            val root = resolveRootCell(resolution, cell)
            StructureResolvedCell(
                cellId = cell.cellId,
                rowIndex = cell.rowIndex,
                colIndex = cell.colIndex,
                rootRowIndex = root.rowIndex,
                rootColIndex = root.colIndex,
                rootRowSpan = root.rowSpan,
                rootColSpan = root.colSpan,
                isCovered = isCoveredCell(resolution, cell.rowIndex, cell.colIndex),
            )
        }
    }

    private fun expandRangeToMergedBlocks(
        resolution: StructureResolution,
        base: TableSelectionRange,
    ): TableSelectionRange {
        var current = base
        var changed: Boolean
        do {
            changed = false
            resolution.mergedRootCoverages.forEach { coverage ->
                val intersects = rangesIntersect(current, coverage.range)
                if (intersects && !containsRange(current, coverage.range)) {
                    current = TableSelectionRange(
                        minRow = minOf(current.minRow, coverage.range.minRow),
                        maxRow = maxOf(current.maxRow, coverage.range.maxRow),
                        minCol = minOf(current.minCol, coverage.range.minCol),
                        maxCol = maxOf(current.maxCol, coverage.range.maxCol),
                    )
                    changed = true
                }
            }
        } while (changed)
        return current
    }

    private fun containsRange(outer: TableSelectionRange, inner: TableSelectionRange): Boolean =
        outer.minRow <= inner.minRow &&
            outer.maxRow >= inner.maxRow &&
            outer.minCol <= inner.minCol &&
            outer.maxCol >= inner.maxCol

    private fun rangesIntersect(a: TableSelectionRange, b: TableSelectionRange): Boolean {
        return !(a.maxRow < b.minRow || a.minRow > b.maxRow || a.maxCol < b.minCol || a.minCol > b.maxCol)
    }

    private fun isMergeableRectangle(
        cells: List<TableCellState>,
        resolution: StructureResolution,
        range: TableSelectionRange,
    ): Boolean {
        val expectedCount = range.rowCount * range.colCount
        val inRange = cells.filter { range.contains(it.rowIndex, it.colIndex) }
        if (inRange.size != expectedCount) return false
        return inRange.none { isCoveredCell(resolution, it.rowIndex, it.colIndex) }
    }

    private fun isCoveredCell(resolution: StructureResolution, row: Int, col: Int): Boolean =
        (row to col) in resolution.coveredCoordinates

    private fun resolveRootCell(resolution: StructureResolution, cell: TableCellState): TableCellState =
        resolution.rootByCoordinate[cell.rowIndex to cell.colIndex] ?: cell

    private fun sanitizeCoveredCells(
        cells: List<TableCellState>,
        resolution: StructureResolution,
    ): List<TableCellState> {
        return cells.map { cell ->
            if (isCoveredCell(resolution, cell.rowIndex, cell.colIndex)) {
                cell.copy(rowSpan = 1, colSpan = 1)
            } else {
                cell
            }
        }
    }

    private enum class AbsorbDirection { LEFT, TOP, RIGHT, BOTTOM }

    private fun resolveAbsorbDirection(
        range: TableSelectionRange,
        rows: Int,
        cols: Int,
    ): AbsorbDirection? {
        if (range.minCol > 0) return AbsorbDirection.LEFT
        if (range.minRow > 0) return AbsorbDirection.TOP
        if (range.maxCol < cols - 1) return AbsorbDirection.RIGHT
        if (range.maxRow < rows - 1) return AbsorbDirection.BOTTOM
        return null
    }

    private fun absorbByDirection(
        cells: List<TableCellState>,
        resolution: StructureResolution,
        range: TableSelectionRange,
        direction: AbsorbDirection,
    ): List<TableCellState> {
        val absorber = findAbsorberRoot(resolution, range, direction) ?: return cells
        val targetRange = when (direction) {
            AbsorbDirection.LEFT -> TableSelectionRange(
                minRow = minOf(absorber.rowIndex, range.minRow),
                maxRow = maxOf(absorber.rowIndex + absorber.rowSpan - 1, range.maxRow),
                minCol = absorber.colIndex,
                maxCol = range.maxCol,
            )
            AbsorbDirection.TOP -> TableSelectionRange(
                minRow = absorber.rowIndex,
                maxRow = range.maxRow,
                minCol = minOf(absorber.colIndex, range.minCol),
                maxCol = maxOf(absorber.colIndex + absorber.colSpan - 1, range.maxCol),
            )
            AbsorbDirection.RIGHT -> TableSelectionRange(
                minRow = minOf(absorber.rowIndex, range.minRow),
                maxRow = maxOf(absorber.rowIndex + absorber.rowSpan - 1, range.maxRow),
                minCol = range.minCol,
                maxCol = absorber.colIndex + absorber.colSpan - 1,
            )
            AbsorbDirection.BOTTOM -> TableSelectionRange(
                minRow = range.minRow,
                maxRow = absorber.rowIndex + absorber.rowSpan - 1,
                minCol = minOf(absorber.colIndex, range.minCol),
                maxCol = maxOf(absorber.colIndex + absorber.colSpan - 1, range.maxCol),
            )
        }

        return cells.map { cell ->
            when {
                cell.cellId == absorber.cellId -> cell.copy(
                    rowIndex = targetRange.minRow,
                    colIndex = targetRange.minCol,
                    rowSpan = targetRange.rowCount,
                    colSpan = targetRange.colCount,
                )
                range.contains(cell.rowIndex, cell.colIndex) -> cell.copy(rowSpan = 1, colSpan = 1)
                else -> cell
            }
        }
    }

    private fun findAbsorberRoot(
        resolution: StructureResolution,
        range: TableSelectionRange,
        direction: AbsorbDirection,
    ): TableCellState? {
        val roots = resolution.rootCells
        val candidates = when (direction) {
            AbsorbDirection.LEFT -> roots.filter {
                val maxCol = it.colIndex + it.colSpan - 1
                maxCol == range.minCol - 1 &&
                    rangesOverlap(it.rowIndex, it.rowIndex + it.rowSpan - 1, range.minRow, range.maxRow)
            }
            AbsorbDirection.TOP -> roots.filter {
                val maxRow = it.rowIndex + it.rowSpan - 1
                maxRow == range.minRow - 1 &&
                    rangesOverlap(it.colIndex, it.colIndex + it.colSpan - 1, range.minCol, range.maxCol)
            }
            AbsorbDirection.RIGHT -> roots.filter {
                it.colIndex == range.maxCol + 1 &&
                    rangesOverlap(it.rowIndex, it.rowIndex + it.rowSpan - 1, range.minRow, range.maxRow)
            }
            AbsorbDirection.BOTTOM -> roots.filter {
                it.rowIndex == range.maxRow + 1 &&
                    rangesOverlap(it.colIndex, it.colIndex + it.colSpan - 1, range.minCol, range.maxCol)
            }
        }
        return candidates
            .sortedWith(compareBy<TableCellState> { it.rowIndex }.thenBy { it.colIndex })
            .firstOrNull()
    }

    private fun rangesOverlap(aStart: Int, aEnd: Int, bStart: Int, bEnd: Int): Boolean {
        return !(aEnd < bStart || bEnd < aStart)
    }

    private data class RootCoverage(
        val root: TableCellState,
        val range: TableSelectionRange,
    )

    private data class StructureResolution(
        val rootCells: List<TableCellState>,
        val rootCoverages: List<RootCoverage>,
        val mergedRootCoverages: List<RootCoverage>,
        val coveredCoordinates: Set<Pair<Int, Int>>,
        val rootByCoordinate: Map<Pair<Int, Int>, TableCellState>,
    )

    private fun resolveStructure(cells: List<TableCellState>): StructureResolution {
        val sortedCells = cells.sortedWith(compareBy<TableCellState> { it.rowIndex }.thenBy { it.colIndex })

        val roots = mutableListOf<TableCellState>()
        val rootCoverages = mutableListOf<RootCoverage>()
        val covered = mutableSetOf<Pair<Int, Int>>()
        val rootByCoordinate = mutableMapOf<Pair<Int, Int>, TableCellState>()

        sortedCells.forEach { rawCandidate ->
            val candidate = rawCandidate.copy(
                rowSpan = rawCandidate.rowSpan.coerceAtLeast(1),
                colSpan = rawCandidate.colSpan.coerceAtLeast(1),
            )
            val origin = candidate.rowIndex to candidate.colIndex
            if (origin in covered) return@forEach

            roots += candidate
            val range = TableSelectionRange(
                minRow = candidate.rowIndex,
                maxRow = candidate.rowIndex + candidate.rowSpan - 1,
                minCol = candidate.colIndex,
                maxCol = candidate.colIndex + candidate.colSpan - 1,
            )
            rootCoverages += RootCoverage(candidate, range)

            for (row in range.minRow..range.maxRow) {
                for (col in range.minCol..range.maxCol) {
                    val coordinate = row to col
                    rootByCoordinate.putIfAbsent(coordinate, candidate)
                    if (coordinate != origin) {
                        covered += coordinate
                    }
                }
            }
        }

        val merged = rootCoverages.filter { it.root.rowSpan > 1 || it.root.colSpan > 1 }
        return StructureResolution(
            rootCells = roots,
            rootCoverages = rootCoverages,
            mergedRootCoverages = merged,
            coveredCoordinates = covered,
            rootByCoordinate = rootByCoordinate,
        )
    }
}
