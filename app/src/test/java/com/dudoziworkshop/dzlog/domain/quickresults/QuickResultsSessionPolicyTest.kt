package com.dudoziworkshop.dzlog.domain.quickresults

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickResultsSessionPolicyTest {
    private fun photo(uri: String, folder: String) = QuickResultPhoto(uri, "$uri.jpg", folder)

    @Test fun oneCaptureWithOriginalAndResultCountsOnce() {
        val list = QuickResultsSessionPolicy.captures(
            savedUriGroups = listOf(listOf("r1", "o1")),
            availablePhotos = listOf(
                photo("r1", "Pictures/DZlog/A/"),
                photo("o1", "Pictures/DZlog/A/original/"),
            ),
        )
        assertEquals(1, list.size)
        assertEquals("r1", list.single().displayedPhoto.uri)
        assertEquals("o1", list.single().original?.uri)
        assertFalse(list.single().isOriginalOnly)
    }

    @Test fun originalOnlyAndDeletedCapturesAreHandled() {
        val list = QuickResultsSessionPolicy.captures(
            savedUriGroups = listOf(listOf("missing"), listOf("o2")),
            availablePhotos = listOf(photo("o2", "Pictures/DZlog/B/original/")),
        )
        assertEquals(1, list.size)
        assertTrue(list.single().isOriginalOnly)
        assertEquals("Pictures/DZlog/B/", list.single().folderPath)
    }

    @Test fun allFoldersKeepSegmentsButFilteredModeMergesThem() {
        val list = QuickResultsSessionPolicy.captures(
            savedUriGroups = listOf(listOf("a1"), listOf("b1"), listOf("a2")),
            availablePhotos = listOf(
                photo("a1", "Pictures/DZlog/A/"),
                photo("b1", "Pictures/DZlog/B/"),
                photo("a2", "Pictures/DZlog/A/"),
            ),
        )
        assertEquals(listOf("a2", "b1", "a1"), list.map { it.displayedPhoto.uri })
        assertEquals(3, QuickResultsSessionPolicy.sections(list).size)
        val filtered = QuickResultsSessionPolicy.sections(list, "Pictures/DZlog/A/")
        assertEquals(1, filtered.size)
        assertEquals(listOf("a2", "a1"), filtered.single().captures.map { it.displayedPhoto.uri })
    }
}
