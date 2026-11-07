package com.dudoziworkshop.dzlog.domain.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class CaptureMediaReconcilePolicyTest {
    private val record = requireNotNull(
        CaptureReservation("shutter-1", "project-A", "template-T", 123L).committed(
            originalUri = "content://media/external/images/media/1",
            resultUri = "content://media/external/images/media/2",
            originalRelativePath = "Pictures/DZlog/a/original/",
            resultRelativePath = "Pictures/DZlog/a/",
        ),
    )

    @Test fun movedResultKeepsProjectMembershipAndUpdatesOnlyItsPath() {
        val modified = requireNotNull(CaptureMediaReconcilePolicy.reconcile(
            record,
            mapOf(requireNotNull(record.resultUri) to
                CaptureMediaPresence.Present("Pictures/Elsewhere/")),
        ))
        assertEquals(record.projectId, modified.projectId)
        assertEquals(record.captureId, modified.captureId)
        assertEquals(record.templateId, modified.templateId)
        assertEquals(record.capturedAtMillis, modified.capturedAtMillis)
        assertEquals(record.resultUri, modified.resultUri)
        assertEquals("Pictures/Elsewhere/", modified.resultRelativePath)
        assertEquals(record.originalRelativePath, modified.originalRelativePath)
    }

    @Test fun confirmedResultDeletionFallsBackToOriginalWithoutLosingShutter() {
        val modified = requireNotNull(CaptureMediaReconcilePolicy.reconcile(
            record, mapOf(requireNotNull(record.resultUri) to CaptureMediaPresence.ConfirmedMissing),
        ))
        assertNull(modified.resultUri)
        assertNull(modified.resultRelativePath)
        assertEquals(record.originalUri, modified.displayUri)
        assertEquals(record.projectId, modified.projectId)
    }

    @Test fun allConfirmedMissingRemovesOnlyThatCapture() {
        assertNull(CaptureMediaReconcilePolicy.reconcile(
            record, record.mediaUris.associateWith { CaptureMediaPresence.ConfirmedMissing },
        ))
    }

    @Test fun unknownReadDoesNotDestroyMediaReferences() {
        val modified = CaptureMediaReconcilePolicy.reconcile(
            record, record.mediaUris.associateWith { CaptureMediaPresence.Unknown },
        )
        assertSame(record, modified)
    }

    @Test fun missingProbeIsUnknownNotDeleted() {
        assertSame(record, CaptureMediaReconcilePolicy.reconcile(record, emptyMap()))
    }

    @Test fun oneMissingAndOtherTemporarilyUnknownPreservesShutter() {
        val modified = requireNotNull(CaptureMediaReconcilePolicy.reconcile(
            record,
            mapOf(
                requireNotNull(record.originalUri) to CaptureMediaPresence.ConfirmedMissing,
                requireNotNull(record.resultUri) to CaptureMediaPresence.Unknown,
            ),
        ))
        assertNull(modified.originalUri)
        assertEquals(record.resultUri, modified.resultUri)
        assertEquals("Pictures/DZlog/a/", modified.resultRelativePath)
    }

    @Test fun unchangedPathKeepsOriginalIdentity() {
        assertSame(record, CaptureMediaReconcilePolicy.reconcile(
            record, mapOf(requireNotNull(record.resultUri) to
                CaptureMediaPresence.Present("Pictures/DZlog/a/")),
        ))
    }
}
