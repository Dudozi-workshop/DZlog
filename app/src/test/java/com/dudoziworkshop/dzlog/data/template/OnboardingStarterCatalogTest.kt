package com.dudoziworkshop.dzlog.data.template

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingStarterCatalogTest {
    @Test
    fun `eleven unique starters are all Free 3 plus 3 compatible`() {
        val defs = OnboardingStarterCatalog.definitions
        assertEquals(11, defs.size)
        assertEquals(11, defs.map { it.id }.distinct().size)
        assertEquals(1, defs.count { it.category == "basic" })
        assertEquals(5, defs.count { it.category == "daily" })
        assertEquals(5, defs.count { it.category == "work" })
        defs.forEach { def ->
            val state = requireNotNull(OnboardingStarterCatalog.buildState(def.id))
            assertEquals(2, state.rows)
            assertEquals(def.labels.size, state.cols)
            assertEquals(state.rows * state.cols, state.cells.size)
            assertTrue(state.fileNameSlotDrafts.count { it != null } <= 3)
            assertTrue(state.pathSlotDrafts.count { it != null } <= 3)
            val headers = state.cells.filter { it.rowIndex == 0 }
            assertEquals(def.labels, headers.sortedBy { it.colIndex }.map { it.rawText })
            assertTrue(headers.none { it.quickEditable })
            val values = state.cells.filter { it.rowIndex == 1 }
            assertTrue(values.filter { it.dataType == TableCellDataType.TEXT }.all { it.rawText.isEmpty() && it.quickEditable })
            assertEquals(def.dateColumn != null, values.any { it.dataType == TableCellDataType.DATE })
            assertEquals(def.counterColumn != null, values.any { it.dataType == TableCellDataType.COUNTER })
            val validCells = values.map { it.cellId }.toSet()
            (state.fileNameSlotDrafts + state.pathSlotDrafts)
                .filterNotNull().filter { it.kind == "CELL" }
                .forEach { assertTrue(it.cellId in validCells) }
        }
    }

    @Test
    fun `basic starter has one editable label and automatic date and counter`() {
        val s = requireNotNull(OnboardingStarterCatalog.buildState(OnboardingStarterCatalog.DEFAULT_ID))
        assertEquals(listOf("기록명", "날짜", "번호"), s.cells.filter { it.rowIndex == 0 }.map { it.rawText })
        assertEquals(1, s.cells.count { it.rowIndex == 1 && it.quickEditable && it.dataType == TableCellDataType.TEXT })
        assertEquals(3, s.fileNameSlotDrafts.size)
        assertEquals("DZlog", s.fileNameSlotDrafts[0]?.manualText)
        assertEquals("yyyyMMdd", s.fileNameSlotDrafts[2]?.formatPattern)
        assertTrue(s.pathSlotDrafts.isEmpty())
    }

    @Test
    fun `unknown starter cannot create a template`() {
        assertNull(OnboardingStarterCatalog.buildState("missing"))
    }

    @Test
    fun `readonly headers survive template serialization and default old json remains editable`() {
        val input = requireNotNull(OnboardingStarterCatalog.buildState("research"))
        val decoded = requireNotNull(tableTemplateStateFromJson(input.toJsonString()))
        assertEquals(input.cells.map { it.quickEditable }, decoded.cells.map { it.quickEditable })
        assertEquals(input.fileNameSlotDrafts.map { it?.cellId }, decoded.fileNameSlotDrafts.take(3).map { it?.cellId })
        val legacy = input.toJsonString()
            .replace("\"quickEditable\":false,", "")
            .replace("\"quickEditable\":true,", "")
        val back = requireNotNull(tableTemplateStateFromJson(legacy))
        assertTrue(back.cells.all { it.quickEditable })
    }
}
