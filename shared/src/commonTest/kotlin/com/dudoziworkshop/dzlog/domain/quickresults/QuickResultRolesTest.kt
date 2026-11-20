package com.dudoziworkshop.dzlog.domain.quickresults

import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuickResultRolesTest {
    @Test
    fun movedCompanionsKeepTheirRolesAndMissingCapturesLeaveNumberingGaps() {
        val path = "Pictures/DZlog/field/"
        val original = QuickResultPhoto("o", "raw.jpg", path, CaptureMediaRole.ORIGINAL)
        val result = QuickResultPhoto("r", "table.jpg", path + "original/", CaptureMediaRole.RESULT)
        val other = original.copy(uri = "single", relativePath = path + "original/")
        val captures = QuickResultsSessionPolicy.captures(
            listOf(listOf("o", "r"), listOf("deleted"), listOf("single", "missing-result")),
            listOf(result, other, original),
        )
        assertEquals(listOf(2, 0), captures.map { it.captureIndex })
        assertTrue(captures.first().isOriginalOnly)
        assertEquals(path, captures.first().folderPath)
        assertEquals(result, captures.last().displayedPhoto)
        assertEquals(original, captures.last().original)
        assertEquals(result.relativePath, captures.last().folderPath)
    }
}
