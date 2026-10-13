package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.data.template.OnboardingStarterCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingQuickValuePolicyTest {
    @Test
    fun `starter quick editor exposes only editable manual input values`() {
        OnboardingStarterCatalog.definitions.forEach { definition ->
            val template = requireNotNull(OnboardingStarterCatalog.buildState(definition.id))
            val quick = quickEditableCells(template)
            assertEquals(definition.id, definition.labels.size -
                listOfNotNull(definition.dateColumn, definition.counterColumn).size,
                quick.size)
            assertTrue(quick.all { it.rowIndex == 1 && it.quickEditable })
            assertFalse(quick.any { it.rowIndex == 0 })
        }
    }

    @Test
    fun `quick updates ignore read only headers even when a caller supplies their IDs`() {
        val template = requireNotNull(OnboardingStarterCatalog.buildState("basic"))
        val header = template.cells.first { it.rowIndex == 0 }
        val value = quickEditableCells(template).single()
        val updated = updateQuickCellValues(template, mapOf(
            header.cellId to "OVERWRITE",
            value.cellId to "Test",
        ))
        assertEquals(header.rawText, updated.cells.first { it.cellId == header.cellId }.rawText)
        assertEquals("Test", updated.cells.first { it.cellId == value.cellId }.rawText)
    }
}
