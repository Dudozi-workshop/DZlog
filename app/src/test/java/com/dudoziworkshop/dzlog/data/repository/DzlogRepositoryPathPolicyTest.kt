package com.dudoziworkshop.dzlog.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class DzlogRepositoryPathPolicyTest {

    @Test
    fun `manual path is normalized and original path appends original segment`() {
        val base = normalizeCaptureBaseRelativePath("Pictures/DZlog/A/B")
        val original = appendOriginalCaptureDirectory(base)

        assertEquals("Pictures/DZlog/A/B/", base)
        assertEquals("Pictures/DZlog/A/B/original/", original)
    }

    @Test
    fun `blank path falls back to dzlog root`() {
        val base = normalizeCaptureBaseRelativePath("   ")
        assertEquals("Pictures/DZlog/", base)
    }
}
