package com.dudoziworkshop.dzlog.domain.project

import org.junit.Assert.assertEquals
import com.dudoziworkshop.dzlog.domain.quickresults.QuickResultPhoto
import com.dudoziworkshop.dzlog.domain.quickresults.QuickResultsCaptureDeletePolicy
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectCaptureReadPolicyTest {
    private fun capture(
        id: String,
        at: Long,
        result: String? = "content://media/$id/result",
        original: String? = "content://media/$id/original",
    ): CaptureRecord = requireNotNull(
        CaptureReservation(id, "project-A", "T", at).committed(
            resultUri = result,
            originalUri = original,
        ),
    )

    @Test fun allFilesOfOneShutterCountOnceAndResultIsPreferred() {
        val shot = capture("one", 1L)
        val snapshot = ProjectCaptureReadPolicy.visible(listOf(shot), shot.mediaUris.toSet())
        assertEquals(1, snapshot.captureCount)
        assertEquals(listOf(listOf(shot.resultUri, shot.originalUri)), snapshot.uriGroupsOldestFirst)
        assertEquals(shot.resultUri, snapshot.latestUri)
    }

    @Test fun partialDeletionFallsBackToOriginalWithoutLosingShutterCount() {
        val shot = capture("one", 1L)
        val snapshot = ProjectCaptureReadPolicy.visible(
            listOf(shot), setOf(requireNotNull(shot.originalUri)),
        )
        assertEquals(1, snapshot.captureCount)
        assertEquals(shot.originalUri, snapshot.latestUri)
    }

    @Test fun missingAllFilesRemovesOnlyTheVisibleProjection() {
        val shot = capture("one", 1L)
        val snapshot = ProjectCaptureReadPolicy.visible(listOf(shot), emptySet())
        assertEquals(0, snapshot.captureCount)
        assertNull(snapshot.latestUri)
        assertEquals(emptyList<List<String>>(), snapshot.uriGroupsOldestFirst)
        assertEquals(2, shot.mediaUris.size) // Original record is not erased.
    }

    @Test fun orderIsByShutterTimeAndReturningFolderStaysSeparate() {
        val oldest = capture("A1", 1L, result = "content://a/1", original = null)
        val middle = capture("B", 2L, result = "content://b", original = null)
        val newest = capture("A2", 3L, result = "content://a/2", original = null)
        val snapshot = ProjectCaptureReadPolicy.visible(
            listOf(middle, newest, oldest), setOf("content://a/1", "content://b", "content://a/2"),
        )
        assertEquals(listOf(
            listOf("content://a/1"),
            listOf("content://b"),
            listOf("content://a/2"),
        ), snapshot.uriGroupsOldestFirst)
        assertEquals("content://a/2", snapshot.latestUri)
    }

    @Test fun movedOriginalRetainsItsRoleAndExactlyPairedDeletionTarget() {
        val record = capture("one", 10L)
        val photos = listOf(
            QuickResultPhoto(requireNotNull(record.resultUri), "result.jpg", "Pictures/DZlog/A/"),
            QuickResultPhoto(requireNotNull(record.originalUri), "original.jpg", "Pictures/Exported/"),
        )
        val shown = ProjectCaptureReadPolicy.quickResults(listOf(record), photos)
        assertEquals(1, shown.size)
        assertEquals(record.resultUri, shown.single().result?.uri)
        assertEquals(record.originalUri, shown.single().original?.uri)
        assertEquals("Pictures/DZlog/A/", shown.single().folderPath)
        val targets = QuickResultsCaptureDeletePolicy.mediaUrisForCapture(
            requireNotNull(record.resultUri), shown,
        )
        // Quick Results returns result before original; record.mediaUris stores
        // the same pair in original-first order. Both must target exactly this shutter.
        assertEquals(listOf(record.resultUri, record.originalUri), targets)
        assertEquals(record.mediaUris.toSet(), targets.toSet())
    }

    @Test fun movedOriginalOnlyCaptureStaysOriginalAndUsesItsActualLocation() {
        val record = capture("one", 10L, result = null)
        val photo = QuickResultPhoto(requireNotNull(record.originalUri), "original.jpg", "Pictures/Exported/")
        val shown = ProjectCaptureReadPolicy.quickResults(listOf(record), listOf(photo)).single()
        assertEquals(null, shown.result)
        assertEquals(photo, shown.original)
        assertEquals("Pictures/Exported/", shown.folderPath)
    }

    @Test fun missingResultLeavesOneOriginalWithOriginalRole() {
        val record = capture("one", 10L)
        val original = QuickResultPhoto(requireNotNull(record.originalUri), "original.jpg", "Pictures/DZlog/A/original/")
        val shown = ProjectCaptureReadPolicy.quickResults(listOf(record), listOf(original)).single()
        assertEquals(null, shown.result)
        assertEquals(original, shown.original)
        assertEquals("Pictures/DZlog/A/", shown.folderPath)
    }

    @Test fun missingNewestCaptureFallsBackToPreviousVisibleOne() {
        val first = capture("first", 1L, original = null)
        val second = capture("second", 2L, original = null)
        val snapshot = ProjectCaptureReadPolicy.visible(
            listOf(first, second), setOf(requireNotNull(first.resultUri)),
        )
        assertEquals(1, snapshot.captureCount)
        assertEquals(first.resultUri, snapshot.latestUri)
    }
}
