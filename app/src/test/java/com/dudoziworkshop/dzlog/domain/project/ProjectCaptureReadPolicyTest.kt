package com.dudoziworkshop.dzlog.domain.project

import org.junit.Assert.assertEquals
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
