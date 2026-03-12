package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import org.junit.Assert.assertEquals
import org.junit.Test

class CounterScopePathPolicyTest {

    @Test
    fun `relativePathOverride is used as-is regardless of group level`() {
        val g1 = resolvedCell("c1", GroupLevel.G1, "legacyA")
        val g2 = resolvedCell("c2", GroupLevel.G2, "legacyB")

        val counterScope = buildCounterScope(
            resolvedCells = listOf(g1, g2),
            fileNameSlots = emptyList(),
            nextCounter = 1,
            isManualMode = false,
            relativePathOverride = "Pictures/DZlog/pathSlotA/pathSlotB/",
        )

        assertEquals("Pictures/DZlog/pathSlotA/pathSlotB/", counterScope.relativePathKey)
    }

    private fun resolvedCell(id: String, groupLevel: GroupLevel, text: String): ResolvedCell {
        val raw = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            cellId = id,
            rawText = text,
            dataType = TableCellDataType.TEXT,
            groupLevel = groupLevel,
        )
        return ResolvedCell(
            id = id,
            type = raw.dataType,
            raw = raw,
            resolvedText = text,
            isEmpty = false,
        )
    }
}
