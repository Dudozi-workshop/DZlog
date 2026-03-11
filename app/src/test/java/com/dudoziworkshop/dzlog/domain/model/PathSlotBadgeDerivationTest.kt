package com.dudoziworkshop.dzlog.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PathSlotBadgeDerivationTest {

    @Test
    fun `path cell slot indices map to badge 1 and 2`() {
        val drafts = listOf(
            TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "c1"),
            TableEditorSlotDraft(kind = "CELL", label = "셀", cellId = "c2"),
        )

        assertEquals(0, derivePathSlotIndexByCellId(drafts, "c1"))
        assertEquals(1, derivePathSlotIndexByCellId(drafts, "c2"))
    }

    @Test
    fun `group level alone does not create path badge index`() {
        val drafts = listOf<TableEditorSlotDraft?>(null, null)
        assertNull(derivePathSlotIndexByCellId(drafts, "g1Cell"))
    }
}
