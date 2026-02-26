package com.dudoziworkshop.dzlog.domain.naming

import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import org.junit.Assert.assertTrue
import org.junit.Test

class NamePathBuildersCounterPaddingTest {

    @Test
    fun `counterOverride uses counterDigits even without counter cell`() {
        val textCell = resolvedTextCell()

        val name = buildDisplayNameFromResolvedCells(
            resolvedCells = listOf(textCell),
            fnDelim = "_",
            includeDate = false,
            includeTime = false,
            counterDigits = 3,
            counterOverride = 7
        )

        assertTrue(name.startsWith("SITE_007"))
    }

    private fun resolvedTextCell(): ResolvedCell {
        val text = "SITE"
        val raw = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            rawText = text,
            dataType = TableCellDataType.TEXT,
            groupLevel = GroupLevel.NONE
        )
        return ResolvedCell(
            id = "cell-1",
            type = TableCellDataType.TEXT,
            raw = raw,
            resolvedText = text,
            isEmpty = false
        )
    }
}
