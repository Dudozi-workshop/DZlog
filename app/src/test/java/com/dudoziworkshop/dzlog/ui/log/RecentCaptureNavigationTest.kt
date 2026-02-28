package com.dudoziworkshop.dzlog.ui.log

import com.dudoziworkshop.dzlog.data.mediastore.DzlogMediaStoreReader
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentCaptureNavigationTest {

    @Test
    fun `root original path maps to root and original photos title`() {
        val parsed = parseG1G2FromRelativePath("Pictures/DZlog/original/")

        assertEquals(DzlogMediaStoreReader.ROOT_G1 to ORIGINAL_PHOTOS_TITLE, parsed)
    }

    @Test
    fun `g1 original path maps to g1 and original photos title`() {
        val parsed = parseG1G2FromRelativePath("Pictures/DZlog/N600/original/")

        assertEquals("N600" to ORIGINAL_PHOTOS_TITLE, parsed)
    }

    @Test
    fun `g1 g2 original path keeps g1 and g2 labels`() {
        val parsed = parseG1G2FromRelativePath("Pictures/DZlog/N600/A1/original/")

        assertEquals("N600" to "A1", parsed)
    }

    @Test
    fun `root water path maps to root and empty g2`() {
        val parsed = parseG1G2FromRelativePath("Pictures/DZlog/")

        assertEquals(DzlogMediaStoreReader.ROOT_G1 to "", parsed)
    }

    @Test
    fun `g1 water path maps to g1 and empty g2`() {
        val parsed = parseG1G2FromRelativePath("Pictures/DZlog/N600/")

        assertEquals("N600" to "", parsed)
    }

    @Test
    fun `g1 g2 water path maps to g1 and g2`() {
        val parsed = parseG1G2FromRelativePath("Pictures/DZlog/N600/A1/")

        assertEquals("N600" to "A1", parsed)
    }

    @Test
    fun `root original path without trailing slash still maps to original photos`() {
        val parsed = parseG1G2FromRelativePath("Pictures/DZlog/original")

        assertEquals(DzlogMediaStoreReader.ROOT_G1 to ORIGINAL_PHOTOS_TITLE, parsed)
    }

    @Test
    fun `g1 original path without trailing slash still maps to original photos`() {
        val parsed = parseG1G2FromRelativePath("Pictures/DZlog/N600/original")

        assertEquals("N600" to ORIGINAL_PHOTOS_TITLE, parsed)
    }
}
