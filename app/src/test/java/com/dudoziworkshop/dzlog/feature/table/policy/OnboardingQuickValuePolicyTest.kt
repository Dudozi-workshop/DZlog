package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.data.template.OnboardingStarterCatalog
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingQuickValuePolicyTest {
    @Test
    fun `starter quick editor includes headers and manual input values`() {
        OnboardingStarterCatalog.definitions.forEach { definition ->
            val template = requireNotNull(OnboardingStarterCatalog.buildState(definition.id))
            val quick = quickEditableCells(template)
            assertEquals(definition.id, 2 * definition.labels.size -
                listOfNotNull(definition.dateColumn, definition.counterColumn).size,
                quick.size)
            assertEquals(definition.labels.size, quick.count { it.rowIndex == 0 })
        }
    }

    @Test
    fun `quick updates support legacy headers alongside manual values`() {
        val template = requireNotNull(OnboardingStarterCatalog.buildState("basic"))
        val header = template.cells.first { it.rowIndex == 0 }
        val value = quickEditableCells(template).single { it.rowIndex == 1 }
        val updated = updateQuickCellValues(template, mapOf(
            header.cellId to "OVERWRITE",
            value.cellId to "Test",
        ))
        assertEquals("OVERWRITE", updated.cells.first { it.cellId == header.cellId }.rawText)
        assertEquals("Test", updated.cells.first { it.cellId == value.cellId }.rawText)
    }
}
