package com.dudoziworkshop.dzlog.feature.log.policy

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GalleryFolderSummaryPolicyTest {
    private val zone = TimeZone.getTimeZone("Asia/Seoul")

    private fun at(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance(zone).apply {
            clear()
            set(year, month - 1, day, 12, 0)
        }.timeInMillis

    private fun image(
        id: String,
        folder: String,
        original: Boolean = false,
        captured: Long? = null,
        added: Long? = null,
    ) = GallerySummaryImage(id, folder, original, captured, added)

    @Test
    fun recursiveTotalsIncludeOriginalsAndDeduplicateMediaIds() {
        val day = at(2026, 10, 9)
        val images = listOf(
            image("result", "Pictures/DZlog/A", captured = day),
            image("original", "Pictures/DZlog/A/original", original = true, captured = day),
            image("nested", "Pictures/DZlog/A/B", captured = day),
            image("nestedOriginal", "Pictures/DZlog/A/B/original", original = true, captured = day),
            image("result", "Pictures/DZlog/A", captured = day),
        )
        val actual = GalleryFolderSummaryPolicy.summarize(
            "Pictures/DZlog/A/", images,
            listOf("Pictures/DZlog/A/B/", "Pictures/DZlog/A/original/", "Pictures/DZlog/A/B/original/"),
        )
        assertEquals(4, actual.totalPhotoCount)
        assertEquals(1, actual.directPhotoCount)
        assertEquals(2, actual.originalPhotoCount)
        assertEquals(1, actual.directChildFolderCount)
    }

    @Test
    fun directChildrenExcludeOriginalFoldersAndDeeperDescendants() {
        val actual = GalleryFolderSummaryPolicy.summarize(
            "Pictures/DZlog/A",
            emptyList(),
            listOf(
                "Pictures/DZlog/A/B/",
                "Pictures/DZlog/A/C/",
                "Pictures/DZlog/A/B/deeper/",
                "Pictures/DZlog/A/original/",
                "Pictures/DZlog/A/B/original/",
                "Pictures/DZlog/A/B",
            ),
        )
        assertEquals(2, actual.directChildFolderCount)
    }

    @Test
    fun folderBoundaryDoesNotIncludeSimilarNamedSibling() {
        val images = listOf(
            image("inside", "Pictures/DZlog/A"),
            image("insideChild", "Pictures/DZlog/A/B"),
            image("outside", "Pictures/DZlog/AA"),
        )
        val actual = GalleryFolderSummaryPolicy.summarize("Pictures/DZlog/A", images, emptyList())
        assertEquals(2, actual.totalPhotoCount)
        assertEquals(1, actual.directPhotoCount)
    }

    @Test
    fun coverUsesNewestResultNotOriginalIncludingDescendants() {
        val newest = at(2026, 10, 9)
        val older = at(2026, 10, 8)
        val images = listOf(
            image("resultOld", "Pictures/DZlog/A", captured = older),
            image("resultNew", "Pictures/DZlog/A/B", captured = newest),
            image("originalNewest", "Pictures/DZlog/A/B/original", original = true, captured = newest + 60000),
        )
        val actual = GalleryFolderSummaryPolicy.summarize("Pictures/DZlog/A", images, emptyList())
        assertEquals("resultNew", actual.coverImageId)
        assertEquals(newest + 60000, actual.latestPhotoEpochMillis)
    }

    @Test
    fun captureTimeIsPreferredAndAddedTimeIsFallback() {
        val added = at(2026, 10, 8)
        val captured = at(2026, 10, 7)
        val images = listOf(
            image("captured", "Pictures/DZlog/A", captured = captured, added = at(2026, 10, 10)),
            image("fallback", "Pictures/DZlog/A", added = added),
        )
        val actual = GalleryFolderSummaryPolicy.summarize("Pictures/DZlog/A", images, emptyList())
        assertEquals(added, actual.latestPhotoEpochMillis)
        assertEquals("fallback", actual.coverImageId)
    }

    @Test
    fun originalOnlyAndEmptyFoldersHaveNoCover() {
        val originalOnly = GalleryFolderSummaryPolicy.summarize(
            "Pictures/DZlog/A",
            listOf(image("original", "Pictures/DZlog/A/original", original = true)),
            emptyList(),
        )
        assertEquals(1, originalOnly.totalPhotoCount)
        assertEquals(1, originalOnly.originalPhotoCount)
        assertNull(originalOnly.coverImageId)
        val empty = GalleryFolderSummaryPolicy.summarize("Pictures/DZlog/B", emptyList(), emptyList())
        assertEquals(0, empty.totalPhotoCount)
        assertNull(empty.coverImageId)
        assertNull(empty.latestPhotoEpochMillis)
    }

    @Test
    fun compactDateUsesYearAwareFormatAndUnknownFallback() {
        val reference = at(2026, 10, 9)
        assertEquals("10.09", GalleryFolderSummaryPolicy.compactDate(reference, reference, zone))
        assertEquals("25.12.08", GalleryFolderSummaryPolicy.compactDate(at(2025, 12, 8), reference, zone))
        assertEquals("—", GalleryFolderSummaryPolicy.compactDate(null, reference, zone))
        assertEquals("—", GalleryFolderSummaryPolicy.compactDate(0L, reference, zone))
    }
}
