package com.dudoziworkshop.dzlog.ui.table.mock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TableEditorRuleCellLinkTest {
    private fun cell(
        id: String? = "cell-a",
        covered: Boolean = false,
        type: TableEditorCellType = TableEditorCellType.TEXT,
    ) = TableEditorCellUiModel(
        id = 1, value = "Blueberry", domainCellId = id,
        isCovered = covered, type = type,
    )

    @Test fun validCellProducesBoundRule() {
        val linked = linkedRuleItemFromCell(cell())
        assertEquals(MockRuleSourceType.CELL, linked?.sourceType)
        assertEquals("cell-a", linked?.cellId)
        assertEquals("Blueberry", linked?.value)
    }

    @Test fun emptyOrCoveredCellsNeverCreateUnboundRules() {
        assertNull(linkedRuleItemFromCell(null))
        assertNull(linkedRuleItemFromCell(cell(id = null)))
        assertNull(linkedRuleItemFromCell(cell(id = "")))
        assertNull(linkedRuleItemFromCell(cell(covered = true)))
        assertNull(linkedRuleItemFromCell(cell(type = TableEditorCellType.COUNTER)))
    }
}
