package com.dudoziworkshop.dzlog.domain.naming

import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupPathRotatingTextTest {

    @Test
    fun `resolveGroupValue uses rotating text resolved value for G1`() {
        val raw = TableCellState(
            rowIndex = 0,
            colIndex = 0,
            dataType = TableCellDataType.ROTATING_TEXT,
            groupLevel = GroupLevel.G1,
        )
        val resolved = ResolvedCell(
            id = raw.cellId,
            type = TableCellDataType.ROTATING_TEXT,
            raw = raw,
            resolvedText = "A",
            isEmpty = false,
        )

        val g1 = resolveGroupValue(listOf(resolved), GroupLevel.G1)

        assertEquals("A", g1)
        assertEquals("Pictures/DZlog/A/", buildGalleryRelativePath(g1, ""))
    }

    @Test
    fun `buildGalleryRelativePath sanitizes rotating group segments`() {
        val path = buildGalleryRelativePath("A:B", "C/D")

        assertEquals("Pictures/DZlog/A_B/C_D/", path)
    }
}
