package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.data.template.newBlankTableTemplateState
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.feature.table.editor.TableMergeDecisionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockLayoutDomainAdapterTest {

    @Test
    fun `add row and column mutate real template structure`() {
        val base = newBlankTableTemplateState(rows = 2, cols = 2)
        val first = base.cells.first()
        val selection = selectMockLayoutCell(
            templateState = base,
            current = MockLayoutSelection(),
            tappedDomainCellId = first.cellId,
        )

        val rowAdded = addMockLayoutRow(base, selection)
        assertEquals(3, rowAdded.rows)

        val colAdded = addMockLayoutColumn(rowAdded, selection)
        assertEquals(3, colAdded.cols)
    }

    @Test
    fun `merge and unmerge selection use production range actions`() {
        val base = newBlankTableTemplateState(rows = 2, cols = 2)
        val topLeft = base.cells.first { it.rowIndex == 0 && it.colIndex == 0 }
        val bottomRight = base.cells.first { it.rowIndex == 1 && it.colIndex == 1 }

        val selection = selectMockLayoutRange(
            templateState = base,
            startDomainCellId = topLeft.cellId,
            endDomainCellId = bottomRight.cellId,
        )

        val merged = mergeOrUnmergeMockLayoutSelection(base, selection)
        val root = merged.cells.first { it.cellId == topLeft.cellId }
        assertEquals(2, root.rowSpan)
        assertEquals(2, root.colSpan)

        val mergedSelection = selectMockLayoutCell(
            templateState = merged,
            current = MockLayoutSelection(),
            tappedDomainCellId = topLeft.cellId,
        )
        val unmerged = mergeOrUnmergeMockLayoutSelection(merged, mergedSelection)
        val unmergedRoot = unmerged.cells.first { it.cellId == topLeft.cellId }
        assertEquals(1, unmergedRoot.rowSpan)
        assertEquals(1, unmergedRoot.colSpan)
    }

    @Test
    fun `row deletion clears selection targets through structure mutation`() {
        val base = newBlankTableTemplateState(rows = 3, cols = 2)
        val middle = base.cells.first { it.rowIndex == 1 && it.colIndex == 0 }
        val selection = selectMockLayoutCell(
            templateState = base,
            current = MockLayoutSelection(),
            tappedDomainCellId = middle.cellId,
        )

        val next = removeMockLayoutRows(base, selection)

        assertEquals(2, next.rows)
        assertTrue(next.cells.none { it.cellId == middle.cellId })
    }
    @Test
    fun `boundary drag preserves total weight and changes neighbors`() {
        val base = newBlankTableTemplateState(rows = 2, cols = 2)

        val rowAdjusted = adjustMockRowBoundary(
            templateState = base,
            boundaryIndex = 0,
            deltaFraction = 0.10f,
        )
        val rowWeights = rowAdjusted.rowWeights ?: error("row weights missing")
        assertEquals(2f, rowWeights.sum(), 0.0001f)
        assertTrue(rowWeights[0] > rowWeights[1])

        val colAdjusted = adjustMockColumnBoundary(
            templateState = base,
            boundaryIndex = 0,
            deltaFraction = -0.10f,
        )
        val colWeights = colAdjusted.colWeights ?: error("column weights missing")
        assertEquals(2f, colWeights.sum(), 0.0001f)
        assertTrue(colWeights[0] < colWeights[1])
    }


    @Test
    fun `mock canvas geometry marks merged covered cells`() {
        val base = newBlankTableTemplateState(rows = 2, cols = 2)
        val topLeft = base.cells.first { it.rowIndex == 0 && it.colIndex == 0 }
        val bottomRight = base.cells.first { it.rowIndex == 1 && it.colIndex == 1 }

        val selection = selectMockLayoutRange(
            templateState = base,
            startDomainCellId = topLeft.cellId,
            endDomainCellId = bottomRight.cellId,
        )
        val merged = mergeOrUnmergeMockLayoutSelection(base, selection)
        val mockCells = mockCellsFromTemplate(merged)

        val root = mockCells.first { it.domainCellId == topLeft.cellId }
        assertEquals(2, root.rowSpan)
        assertEquals(2, root.colSpan)
        assertTrue(mockCells.count { it.isCovered } == 3)
    }


    @Test
    fun `tapping merged root selects full merged block`() {
        val base = newBlankTableTemplateState(rows = 2, cols = 2)
        val topLeft = base.cells.first { it.rowIndex == 0 && it.colIndex == 0 }
        val bottomRight = base.cells.first { it.rowIndex == 1 && it.colIndex == 1 }

        val selection = selectMockLayoutRange(
            templateState = base,
            startDomainCellId = topLeft.cellId,
            endDomainCellId = bottomRight.cellId,
        )
        val merged = mergeOrUnmergeMockLayoutSelection(base, selection)

        val mergedTap = selectMockLayoutCell(
            templateState = merged,
            current = MockLayoutSelection(),
            tappedDomainCellId = topLeft.cellId,
        )

        assertEquals(setOf(topLeft.cellId), mergedTap.selectedCellIds)
        assertEquals(0, mergedTap.range?.minRow)
        assertEquals(1, mergedTap.range?.maxRow)
        assertEquals(0, mergedTap.range?.minCol)
        assertEquals(1, mergedTap.range?.maxCol)
        assertTrue(isMockLayoutSelectionMerged(merged, mergedTap))
    }

    @Test
    fun `merge with multiple populated cells requires confirmation`() {
        val base = newBlankTableTemplateState(rows = 2, cols = 2)
        val topLeft = base.cells.first { it.rowIndex == 0 && it.colIndex == 0 }
        val bottomRight = base.cells.first { it.rowIndex == 1 && it.colIndex == 1 }

        val selection = selectMockLayoutRange(
            templateState = base,
            startDomainCellId = topLeft.cellId,
            endDomainCellId = bottomRight.cellId,
        )

        val decision = resolveMockLayoutMergeDecision(
            templateState = base,
            selection = selection,
            populatedCellIds = setOf(topLeft.cellId, bottomRight.cellId),
        )

        assertEquals(TableMergeDecisionType.CONFIRM_MERGE, decision.type)
    }

    @Test
    fun `row deletion clears deleted slot refs and demotes G2 when G1 disappears`() {
        val base = newBlankTableTemplateState(rows = 2, cols = 1)
        val g1Cell = base.cells.first { it.rowIndex == 0 }
        val g2Cell = base.cells.first { it.rowIndex == 1 }
        val prepared = base.copy(
            cells = base.cells.map { cell ->
                when (cell.cellId) {
                    g1Cell.cellId -> cell.copy(groupLevel = GroupLevel.G1)
                    g2Cell.cellId -> cell.copy(groupLevel = GroupLevel.G2)
                    else -> cell
                }
            },
            fileNameSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "CELL", label = "G1", cellId = g1Cell.cellId),
                TableEditorSlotDraft(kind = "CELL", label = "G2", cellId = g2Cell.cellId),
                null,
            ),
            pathSlotDrafts = listOf(
                TableEditorSlotDraft(kind = "CELL", label = "G1", cellId = g1Cell.cellId),
                TableEditorSlotDraft(kind = "CELL", label = "G2", cellId = g2Cell.cellId),
                null,
            ),
        )
        val selection = selectMockLayoutCell(
            templateState = prepared,
            current = MockLayoutSelection(),
            tappedDomainCellId = g1Cell.cellId,
        )

        val next = removeMockLayoutRows(prepared, selection)

        assertEquals(1, next.rows)
        assertEquals(GroupLevel.NONE, next.cells.single().groupLevel)
        assertEquals(g2Cell.cellId, next.fileNameSlotDrafts.first()?.cellId)
        assertEquals(g2Cell.cellId, next.pathSlotDrafts.first()?.cellId)
        assertTrue(next.fileNameSlotDrafts.none { it?.cellId == g1Cell.cellId })
        assertTrue(next.pathSlotDrafts.none { it?.cellId == g1Cell.cellId })
    }


}
