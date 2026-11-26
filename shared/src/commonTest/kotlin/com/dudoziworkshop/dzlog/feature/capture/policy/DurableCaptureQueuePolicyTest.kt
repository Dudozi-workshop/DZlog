package com.dudoziworkshop.dzlog.feature.capture.policy

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DurableCaptureQueuePolicyTest {
    private fun record(
        order: Long,
        stage: DurableCaptureStage,
        templateId: String = "template-a",
    ) = DurableCaptureRecord(
        captureId = "capture-$order",
        reservationOrder = order,
        templateId = templateId,
        projectId = "project-a",
        counterValue = order.toInt() + 1,
        phraseProgressCursor = order.toInt(),
        stage = stage,
    )

    @Test fun admitsAtMostThreeOutstandingCaptures() {
        val records = listOf(
            record(0, DurableCaptureStage.PROCESSING),
            record(1, DurableCaptureStage.READY),
            record(2, DurableCaptureStage.RESERVED),
        )
        assertFalse(DurableCaptureQueuePolicy.canReserve(
            records, setOf("capture-0"), "template-a", "project-a"))
        assertTrue(DurableCaptureQueuePolicy.canReserve(
            records.dropLast(1), setOf("capture-0"), "template-a", "project-a"))
    }

    @Test fun liveWorkerMayContinueButOrphanedUncertaintyBlocksAdmission() {
        val processing = listOf(record(0, DurableCaptureStage.PROCESSING))
        assertTrue(DurableCaptureQueuePolicy.canReserve(
            processing, setOf("capture-0"), "template-a", "project-a"))
        assertFalse(DurableCaptureQueuePolicy.canReserve(
            processing, emptySet(), "template-a", "project-a"))
        assertFalse(DurableCaptureQueuePolicy.canReserve(
            listOf(record(0, DurableCaptureStage.MEDIA_SAVED)), emptySet(), "template-a", "project-a"))
        assertFalse(DurableCaptureQueuePolicy.canReserve(
            listOf(record(0, DurableCaptureStage.NEEDS_REVIEW)), emptySet(), "template-a", "project-a"))
        assertFalse(DurableCaptureQueuePolicy.canReserve(
            listOf(record(0, DurableCaptureStage.READY, "template-b")),
            emptySet(), "template-a", "project-a"))
    }

    @Test fun serialWorkerNeverSkipsAnEarlierReservation() {
        val records = listOf(
            record(2, DurableCaptureStage.READY),
            record(0, DurableCaptureStage.PROCESSING),
            record(1, DurableCaptureStage.READY),
        )
        assertNull(DurableCaptureQueuePolicy.nextReady(records, setOf("capture-0")))
        val afterFirst = records.map {
            if (it.captureId == "capture-0") it.copy(stage = DurableCaptureStage.COMMITTED) else it
        }
        assertEquals("capture-1", DurableCaptureQueuePolicy.nextReady(afterFirst, emptySet())?.captureId)
    }

    @Test fun recoveryResumesOnlyMatchingReadyWork() {
        assertEquals(DurableCaptureRecoveryAction.CANCEL_INCOMPLETE_RESERVATION,
            DurableCaptureQueuePolicy.recoveryAction(
                record(0, DurableCaptureStage.RESERVED), emptySet(), "template-a"))
        assertEquals(DurableCaptureRecoveryAction.RESUME_READY,
            DurableCaptureQueuePolicy.recoveryAction(
                record(0, DurableCaptureStage.READY), emptySet(), "template-a"))
        assertEquals(DurableCaptureRecoveryAction.NONE,
            DurableCaptureQueuePolicy.recoveryAction(
                record(0, DurableCaptureStage.PROCESSING), setOf("capture-0"), "template-a"))
        assertEquals(DurableCaptureRecoveryAction.REQUIRE_REVIEW,
            DurableCaptureQueuePolicy.recoveryAction(
                record(0, DurableCaptureStage.READY, "template-b"), emptySet(), "template-a"))
        assertEquals(DurableCaptureRecoveryAction.REQUIRE_REVIEW,
            DurableCaptureQueuePolicy.recoveryAction(
                record(0, DurableCaptureStage.PROCESSING), emptySet(), "template-a"))
        assertEquals(DurableCaptureRecoveryAction.REQUIRE_REVIEW,
            DurableCaptureQueuePolicy.recoveryAction(
                record(0, DurableCaptureStage.MEDIA_SAVED), emptySet(), "template-a"))
    }

    @Test fun progressCommitFollowsReservationOrder() {
        val blocked = listOf(
            record(0, DurableCaptureStage.PROCESSING),
            record(1, DurableCaptureStage.MEDIA_SAVED),
        )
        assertFalse(DurableCaptureQueuePolicy.canCommit(blocked, "capture-1"))
        val allowed = blocked.map {
            if (it.captureId == "capture-0") it.copy(stage = DurableCaptureStage.COMMITTED) else it
        }
        assertTrue(DurableCaptureQueuePolicy.canCommit(allowed, "capture-1"))
    }

    @Test fun undoWaitsUntilEveryCaptureSettles() {
        assertNull(DurableCaptureQueuePolicy.latestUndoableCaptureId(listOf(
            record(0, DurableCaptureStage.COMMITTED),
            record(1, DurableCaptureStage.READY),
        )))
        assertEquals("capture-1", DurableCaptureQueuePolicy.latestUndoableCaptureId(listOf(
            record(0, DurableCaptureStage.COMMITTED),
            record(1, DurableCaptureStage.COMMITTED),
        )))
    }

    @Test fun transitionGraphForbidsBlindRetryAndTerminalMutation() {
        assertTrue(DurableCaptureQueuePolicy.canTransition(
            DurableCaptureStage.RESERVED, DurableCaptureStage.READY))
        assertTrue(DurableCaptureQueuePolicy.canTransition(
            DurableCaptureStage.PROCESSING, DurableCaptureStage.NEEDS_REVIEW))
        assertFalse(DurableCaptureQueuePolicy.canTransition(
            DurableCaptureStage.PROCESSING, DurableCaptureStage.READY))
        assertFalse(DurableCaptureQueuePolicy.canTransition(
            DurableCaptureStage.MEDIA_SAVED, DurableCaptureStage.PROCESSING))
        assertFalse(DurableCaptureQueuePolicy.canTransition(
            DurableCaptureStage.COMMITTED, DurableCaptureStage.READY))
    }

    @Test fun duplicateIdsOrReservationOrdersAreRejected() {
        val first = record(0, DurableCaptureStage.READY)
        assertFalse(DurableCaptureQueuePolicy.isWellFormed(listOf(first, first.copy())))
        assertFalse(DurableCaptureQueuePolicy.isWellFormed(listOf(
            first,
            record(1, DurableCaptureStage.READY).copy(reservationOrder = 0),
        )))
        assertFalse(DurableCaptureQueuePolicy.canReserve(
            listOf(first, first.copy()), emptySet(), "template-a", "project-a"))
    }
}
