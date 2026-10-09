package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import org.junit.Assert.assertEquals
import org.junit.Test

class CounterScopeResolverTest {

    @Test
    fun `rotating text in filename slot includes rp identity token`() {
        val rotating = rotatingCell()
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
        val rotating = rotatingCell()
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
    fun `rotating text outside filename slots excludes phrase scope`() {
        val rotating = rotatingCell()
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
    fun `blank resolved text uses blank identity token`() {
        val rotating = rotatingCell()
        val result = CounterScopeResolver.resolve(
            CounterScopeResolver.Inputs(
                cells = listOf(rotating),
                fileNameSlots = listOf("r1", null, null),
                resolvedCells = listOf(resolved(rotating, "   ")),
            )
        )

        assertEquals(listOf("rp___blank__"), result.phraseScopeValues)
    }

    private fun rotatingCell(): TableCellState =
        TableCellState(
            rowIndex = 0,
            colIndex = 0,
            cellId = "r1",
            dataType = TableCellDataType.ROTATING_TEXT,
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
