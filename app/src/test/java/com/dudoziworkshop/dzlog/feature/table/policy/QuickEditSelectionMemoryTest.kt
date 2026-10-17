package com.dudoziworkshop.dzlog.feature.table.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickEditSelectionMemoryTest {
    @Test fun remembersLastCellPerTemplateInSameSession() {
        val memory = QuickEditSelectionMemory()
        val editable = listOf("first", "second")
        assertEquals("first", memory.initialSelection("A", editable))
        memory.recordSelection("A", "second", editable)
        assertEquals("second", memory.initialSelection("A", editable))
        assertEquals("first", memory.initialSelection("B", editable))
        memory.recordSelection("B", "first", editable)
        assertEquals("second", memory.initialSelection("A", editable))
    }

    @Test fun invalidSelectionFallsBackWithoutLosingOtherTemplate() {
        val memory = QuickEditSelectionMemory()
        memory.recordSelection("A", "second", listOf("first", "second"))
        assertEquals("first", memory.initialSelection("A", listOf("first")))
        assertNull(memory.initialSelection("A", emptyList()))
        memory.recordSelection("A", "locked", listOf("first"))
        assertEquals("second", memory.initialSelection("A", listOf("first", "second")))
    }

    @Test fun freshMemoryDoesNotRestorePreviousAppSession() {
        val previous = QuickEditSelectionMemory()
        previous.recordSelection("A", "second", listOf("first", "second"))
        assertEquals("first", QuickEditSelectionMemory().initialSelection("A", listOf("first", "second")))
    }

    @Test fun missingTemplateDoesNotSaveSharedSelection() {
        val memory = QuickEditSelectionMemory()
        memory.recordSelection(null, "second", listOf("first", "second"))
        assertEquals("first", memory.initialSelection("A", listOf("first", "second")))
    }
}
