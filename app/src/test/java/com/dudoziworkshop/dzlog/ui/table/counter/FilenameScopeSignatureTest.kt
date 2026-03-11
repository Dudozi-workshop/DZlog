package com.dudoziworkshop.dzlog.ui.table.counter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class FilenameScopeSignatureTest {

    @Test
    fun `signature changes when manual token changes`() {
        val a = buildFilenameScopeSignature(true, listOf("A"))
        val b = buildFilenameScopeSignature(true, listOf("B"))

        assertNotEquals(a, b)
    }

    @Test
    fun `signature changes when token order changes`() {
        val first = buildFilenameScopeSignature(true, listOf("CELL", "MANUAL"))
        val second = buildFilenameScopeSignature(true, listOf("MANUAL", "CELL"))

        assertNotEquals(first, second)
    }

    @Test
    fun `signature is fixed when filename scope disabled`() {
        val a = buildFilenameScopeSignature(false, listOf("X"))
        val b = buildFilenameScopeSignature(false, listOf("Y", "Z"))

        assertEquals("filename-scope-disabled", a)
        assertEquals(a, b)
    }
}
