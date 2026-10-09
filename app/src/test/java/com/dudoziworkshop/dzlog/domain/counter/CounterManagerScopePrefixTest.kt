package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CounterManagerScopePrefixTest {

    @Test
    fun `phrase scope values split stream prefix`() {
        val text = resolvedCell()

        val whyPrefix = CounterManager.computeCounterPrefix(
            resolvedCells = listOf(text),
            fnDelim = "_",
            fileNameSlots = listOf(text.id, null, null),
            includeFilenameInScope = true,
            scopeOptions = CounterScopeOptions(phraseScopeValues = listOf("rp_왜")),
        )
        val wowPrefix = CounterManager.computeCounterPrefix(
            resolvedCells = listOf(text),
            fnDelim = "_",
            fileNameSlots = listOf(text.id, null, null),
            includeFilenameInScope = true,
            scopeOptions = CounterScopeOptions(phraseScopeValues = listOf("rp_헐")),
        )

        assertEquals("N600_rp_왜", whyPrefix)
        assertEquals("N600_rp_헐", wowPrefix)
        assertNotEquals(whyPrefix, wowPrefix)
    }

    @Test
    fun `same phrase scope value keeps same stream prefix`() {
        val text = resolvedCell()

        val first = CounterManager.computeCounterPrefix(
            resolvedCells = listOf(text),
            fnDelim = "_",
            fileNameSlots = listOf(text.id, null, null),
            includeFilenameInScope = true,
            scopeOptions = CounterScopeOptions(phraseScopeValues = listOf("rp_같은문구")),
        )
        val second = CounterManager.computeCounterPrefix(
            resolvedCells = listOf(text),
            fnDelim = "_",
            fileNameSlots = listOf(text.id, null, null),
            includeFilenameInScope = true,
            scopeOptions = CounterScopeOptions(phraseScopeValues = listOf("rp_같은문구")),
        )

        assertEquals(first, second)
    }

    @Test
    fun `computeCounterStreamRelativePathKey separates g2 enabled but empty from g1 only`() {
        val base = "Pictures/DZlog/A/"

        val g1Only = CounterManager.computeCounterStreamRelativePathKey(
            baseRelativePath = base,
            hasG2Group = false,
            group2Value = "",
        )
        val g2EnabledEmpty = CounterManager.computeCounterStreamRelativePathKey(
            baseRelativePath = base,
            hasG2Group = true,
            group2Value = "",
        )

        assertEquals("Pictures/DZlog/A/", g1Only)
        assertEquals("Pictures/DZlog/A/|g2=enabled_empty", g2EnabledEmpty)
    }

    private fun resolvedCell(): ResolvedCell {
        val col = 0
        val type = TableCellDataType.TEXT
        val text = "N600"
        val raw = TableCellState(
            rowIndex = 0,
            colIndex = col,
            dataType = type,
            groupLevel = GroupLevel.NONE,
            rawText = text,
        )

        return ResolvedCell(
            id = "0-$col",
            type = type,
            raw = raw,
            resolvedText = text,
            isEmpty = false,
        )
    }
}
