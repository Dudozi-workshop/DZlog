package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.model.RotatingCounterMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import org.junit.Assert.assertEquals
import org.junit.Test

class CounterScopeResolverTest {

    @Test
    fun `per-phrase rotating text in filename slot includes rp token`() {
        val rotating = rotatingCell(id = "r1", mode = RotatingCounterMode.PER_PHRASE)
        val result = CounterScopeResolver.resolve(
            CounterScopeResolver.Inputs(
                cells = listOf(rotating),
                fileNameSlots = listOf("r1", null, null),
                resolvedCells = listOf(resolved(rotating, "왜")),
            )
        )

        assertEquals(listOf("rp_왜"), result.phraseScopeValues)
    }

    @Test
    fun `different resolved text produces different rp token`() {
        val rotating = rotatingCell(id = "r1", mode = RotatingCounterMode.PER_PHRASE)
        val result = CounterScopeResolver.resolve(
            CounterScopeResolver.Inputs(
                cells = listOf(rotating),
                fileNameSlots = listOf("r1", null, null),
                resolvedCells = listOf(resolved(rotating, "헐")),
            )
        )

        assertEquals(listOf("rp_헐"), result.phraseScopeValues)
    }

    @Test
    fun `global rotating mode excludes phrase scope even in filename slot`() {
        val rotating = rotatingCell(id = "r1", mode = RotatingCounterMode.GLOBAL)
        val result = CounterScopeResolver.resolve(
            CounterScopeResolver.Inputs(
                cells = listOf(rotating),
                fileNameSlots = listOf("r1", null, null),
                resolvedCells = listOf(resolved(rotating, "왜")),
            )
        )

        assertEquals(emptyList<String>(), result.phraseScopeValues)
    }

    @Test
    fun `per-phrase rotating text outside filename slots excludes phrase scope`() {
        val rotating = rotatingCell(id = "r1", mode = RotatingCounterMode.PER_PHRASE)
        val result = CounterScopeResolver.resolve(
            CounterScopeResolver.Inputs(
                cells = listOf(rotating),
                fileNameSlots = listOf(null, null, null),
                resolvedCells = listOf(resolved(rotating, "왜")),
            )
        )

        assertEquals(emptyList<String>(), result.phraseScopeValues)
    }

    @Test
    fun `blank resolved text is excluded from phrase scope`() {
        val rotating = rotatingCell(id = "r1", mode = RotatingCounterMode.PER_PHRASE)
        val result = CounterScopeResolver.resolve(
            CounterScopeResolver.Inputs(
                cells = listOf(rotating),
                fileNameSlots = listOf("r1", null, null),
                resolvedCells = listOf(resolved(rotating, "   ")),
            )
        )

        assertEquals(emptyList<String>(), result.phraseScopeValues)
    }

    private fun rotatingCell(id: String, mode: RotatingCounterMode): TableCellState =
        TableCellState(
            rowIndex = 0,
            colIndex = 0,
            cellId = id,
            dataType = TableCellDataType.ROTATING_TEXT,
            rotatingCounterMode = mode,
        )

    private fun resolved(cell: TableCellState, text: String): ResolvedCell =
        ResolvedCell(
            id = cell.cellId,
            type = cell.dataType,
            raw = cell,
            resolvedText = text,
            isEmpty = text.isBlank(),
        )
}
