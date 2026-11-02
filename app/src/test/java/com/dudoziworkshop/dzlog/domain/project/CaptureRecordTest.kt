package com.dudoziworkshop.dzlog.domain.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureRecordTest {
    private fun saved(
        id: String,
        projectId: String? = null,
        templateId: String? = "template-1",
        time: Long = 100L,
        original: String? = null,
        result: String? = "content://capture/$id/result",
    ): CaptureRecord = requireNotNull(
        CaptureReservation(id, projectId, templateId, time).committed(
            originalUri = original,
            resultUri = result,
        )
    )

    @Test fun ordinaryShootingHasNoProjectAndNeverCreatesOne() {
        val ordinary = saved("ordinary", projectId = null)
        assertNull(ordinary.projectId)
        assertEquals(1, CaptureRecordPolicy.append(emptyList(), ordinary).size)
    }

    @Test fun shutterReservationPreservesProjectAndTemplateUntilSaveFinishes() {
        val shutter = CaptureReservation(
            captureId = "capture-1",
            projectId = "project-A",
            templateId = "template-X",
            capturedAtMillis = 123L,
        )
        // The UI may have switched to project B while the media write completed.
        val receipt = requireNotNull(shutter.committed(resultUri = "content://image/1"))
        assertEquals("project-A", receipt.projectId)
        assertEquals("template-X", receipt.templateId)
        assertEquals(123L, receipt.capturedAtMillis)
    }

    @Test fun bothSavedMediaItemsRemainOneCaptureWithResultThumbnail() {
        val receipt = saved(
            id = "both",
            original = "content://capture/both/original",
        )
        assertEquals(2, receipt.mediaUris.size)
        assertEquals("content://capture/both/result", receipt.displayUri)
        assertEquals(1, CaptureRecordPolicy.append(emptyList(), receipt).size)
    }

    @Test fun missingSavedMediaDoesNotCreateARecord() {
        val reservation = CaptureReservation("failed", "project-A", "template-X", 123L)
        assertNull(reservation.committed())
        assertThrows(IllegalArgumentException::class.java) {
            reservation.committed(resultUri = " ")
        }
    }

    @Test fun reprocessedSameReceiptIsIdempotent() {
        val receipt = saved("same", "project-A")
        val first = CaptureRecordPolicy.append(emptyList(), receipt)
        val replay = CaptureRecordPolicy.append(first, receipt)
        assertTrue(first === replay)
        assertEquals(1, replay.size)
    }

    @Test fun conflictingCaptureIdOrSharedMediaUriIsRejected() {
        val existing = saved("one", "project-A")
        val records = listOf(existing)
        assertThrows(IllegalArgumentException::class.java) {
            CaptureRecordPolicy.append(records, existing.copy(projectId = "project-B"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CaptureRecordPolicy.append(
                records,
                saved("two", "project-B", result = existing.resultUri),
            )
        }
    }

    @Test fun deletingOnlyResultFallsBackToOriginalWithoutLosingCapture() {
        val item = saved("both", "project-A", original = "content://capture/both/original")
        val after = CaptureRecordPolicy.afterVerifiedDeletion(
            listOf(item),
            setOf("content://capture/both/result"),
        )
        assertEquals(1, after.size)
        assertNull(after.single().resultUri)
        assertEquals("content://capture/both/original", after.single().displayUri)
    }

    @Test fun deletionOfAllVerifiedUrisRemovesExactlyTheAffectedCapture() {
        val first = saved("one", "project-A", original = "content://capture/one/original")
        val second = saved("two", "project-A")
        val after = CaptureRecordPolicy.afterVerifiedDeletion(
            listOf(first, second),
            first.mediaUris.toSet(),
        )
        assertEquals(listOf(second), after)
    }

    @Test fun projectFilteringPreservesOtherProjectsAndSortsNewestFirst() {
        val a1 = saved("a1", "A", time = 1L)
        val b1 = saved("b1", "B", time = 3L)
        val a2 = saved("a2", "A", time = 2L)
        val ordinary = saved("ordinary", time = 4L)
        assertEquals(listOf(a2, a1), CaptureRecordPolicy.forProject(
            listOf(a1, b1, a2, ordinary), "A",
        ))
    }

    @Test fun capturedPathsRemainBoundToTheirOriginalMedia() {
        val record = requireNotNull(
            CaptureReservation("one", "A", "T", 1L).committed(
                originalUri = "content://original",
                resultUri = "content://result",
                originalRelativePath = "Pictures/DZlog/A/original/",
                resultRelativePath = "Pictures/DZlog/A/",
            ),
        )
        val updated = requireNotNull(record.afterVerifiedDeletion(setOf("content://original")))
        assertNull(updated.originalRelativePath)
        assertEquals("Pictures/DZlog/A/", updated.resultRelativePath)
    }
}
