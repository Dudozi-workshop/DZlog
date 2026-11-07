package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.data.template.OnboardingStarterCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingQuickValuePolicyTest {
    @Test
    fun `starter quick editor exposes headers and manual input values`() {
        OnboardingStarterCatalog.definitions.forEach { definition ->
            val template = requireNotNull(OnboardingStarterCatalog.buildState(definition.id))
            val automaticCount = listOfNotNull(definition.dateColumn, definition.counterColumn).size
            val quick = quickEditableCells(template)
            assertEquals(definition.id, definition.labels.size * 2 - automaticCount, quick.size)
            assertEquals(definition.id, definition.labels.size, quick.count { it.rowIndex == 0 })
            assertTrue(quick.all { it.quickEditable })
            assertEquals(
                definition.id,
                definition.labels.size * 2 - (if (definition.counterColumn != null) 1 else 0),
                quickSelectableCells(template).size,
            )
        }
    }

    @Test
    fun `legacy locked header can be edited while counter and save rules stay intact`() {
        val starter = requireNotNull(OnboardingStarterCatalog.buildState("basic"))
        val template = starter.copy(cells = starter.cells.map { cell ->
            if (cell.rowIndex == 0) cell.copy(quickEditable = false) else cell
        })
        val header = template.cells.first { it.rowIndex == 0 }
        val value = template.cells.first { it.rowIndex == 1 && it.dataType == com.dudoziworkshop.dzlog.domain.model.TableCellDataType.TEXT }
        val counter = template.cells.first { it.dataType == com.dudoziworkshop.dzlog.domain.model.TableCellDataType.COUNTER }
        assertTrue(quickSelectableCells(template).any { it.cellId == header.cellId })
        val updated = updateQuickCellValues(template, mapOf(
            header.cellId to "OVERWRITE",
            value.cellId to "Test",
            counter.cellId to "999",
        ))
        assertEquals("OVERWRITE", updated.cells.first { it.cellId == header.cellId }.rawText)
        assertEquals("Test", updated.cells.first { it.cellId == value.cellId }.rawText)
        assertEquals(counter, updated.cells.first { it.cellId == counter.cellId })
        assertEquals(template.fileNameSlotDrafts, updated.fileNameSlotDrafts)
        assertEquals(template.pathSlotDrafts, updated.pathSlotDrafts)
    }
}
