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
    @Test fun deletingResultKeepsOriginalForSameCapture() {
        val groups = listOf(listOf("r1", "o1"))
        val before = QuickResultsSessionPolicy.captures(
            groups,
            listOf(
                photo("r1", "Pictures/DZlog/A/"),
                photo("o1", "Pictures/DZlog/A/original/"),
            ),
        )
        val after = QuickResultsSessionPolicy.captures(
            groups,
            listOf(photo("o1", "Pictures/DZlog/A/original/")),
        )
        assertEquals(1, before.size)
        assertEquals(1, after.size)
        assertTrue(after.single().isOriginalOnly)
        assertEquals("o1", after.single().displayedPhoto.uri)
    }

    @Test fun fullyDeletedCaptureDisappearsWithoutRenumberingRemainingCaptures() {
        val groups = listOf(listOf("a1"), listOf("b1"), listOf("a2"))
        val remaining = QuickResultsSessionPolicy.captures(
            groups,
            listOf(
                photo("a1", "Pictures/DZlog/A/"),
                photo("a2", "Pictures/DZlog/A/"),
            ),
        )
        assertEquals(2, remaining.size)
        assertEquals(listOf(2, 0), remaining.map { it.captureIndex })
        assertEquals(1, QuickResultsSessionPolicy.sections(remaining).size)
    }

    @Test fun duplicateUrisInsideCaptureDoNotDoubleCount() {
        val captures = QuickResultsSessionPolicy.captures(
            savedUriGroups = listOf(listOf("r1", "r1")),
            availablePhotos = listOf(photo("r1", "Pictures/DZlog/A/")),
        )
        assertEquals(1, captures.size)
    }

    @Test fun folderHeaderShowsFullRelativePathUnderDzlog() {
        assertEquals("로도비트 / T4 / 261016",
            QuickResultsSessionPolicy.folderLabel("Pictures/DZlog/로도비트/T4/261016/"))
        assertEquals("DZlog",
            QuickResultsSessionPolicy.folderLabel("Pictures/DZlog/"))
        assertEquals("A / B", QuickResultsSessionPolicy.folderLabel("A/B/"))
    }

    @Test fun contiguousSessionSegmentDisplaysCaptureRange() {
        val result = QuickResultsSessionPolicy.captures(
            (0 until 25).map { listOf("photo$it") },
            (0 until 25).map { photo("photo$it", "Pictures/DZlog/A/") },
        )
        val section = QuickResultsSessionPolicy.sections(result).single()
        assertEquals("25건 · #1~25", QuickResultsSessionPolicy.sectionLabel(section))
        val five = section.copy(captures = section.captures.take(5))
        assertEquals("5건 · #21~25", QuickResultsSessionPolicy.sectionLabel(five))
    }

    @Test fun filteredNoncontiguousCapturesDoNotClaimContinuousRange() {
        val a = "Pictures/DZlog/A/"
        val b = "Pictures/DZlog/B/"
        val result = QuickResultsSessionPolicy.captures(
            listOf(listOf("a1"), listOf("b1"), listOf("a2")),
            listOf(photo("a1", a), photo("b1", b), photo("a2", a)),
        )
        val section = QuickResultsSessionPolicy.sections(result, a).single()
        assertEquals("2건 · #3 외 1건", QuickResultsSessionPolicy.sectionLabel(section))
    }

}
