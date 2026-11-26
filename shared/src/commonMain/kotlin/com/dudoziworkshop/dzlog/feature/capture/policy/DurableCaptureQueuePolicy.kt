package com.dudoziworkshop.dzlog.feature.capture.policy

/**
 * Platform-neutral safety contract for a bounded, serial post-shutter queue.
 *
 * This policy owns no files and performs no media writes. A platform adapter
 * must durably store the JPEG and complete capture snapshot before moving a
 * reservation to [DurableCaptureStage.READY].
 */
enum class DurableCaptureStage {
    RESERVED,
    READY,
    PROCESSING,
    MEDIA_SAVED,
    COMMITTED,
    CANCELLED,
    NEEDS_REVIEW,
}

data class DurableCaptureRecord(
    val captureId: String,
    val reservationOrder: Long,
    val templateId: String,
    val projectId: String?,
    val counterValue: Int,
    val phraseProgressCursor: Int,
    val stage: DurableCaptureStage,
) {
    init {
        require(captureId.isNotBlank())
        require(reservationOrder >= 0)
        require(templateId.isNotBlank())
        require(counterValue >= 1)
        require(phraseProgressCursor >= 0)
    }
}

enum class DurableCaptureRecoveryAction {
    NONE,
    CANCEL_INCOMPLETE_RESERVATION,
    RESUME_READY,
    REQUIRE_REVIEW,
}

object DurableCaptureQueuePolicy {
    const val MAX_OUTSTANDING = 3

    private val outstandingStages = setOf(
        DurableCaptureStage.RESERVED,
        DurableCaptureStage.READY,
        DurableCaptureStage.PROCESSING,
        DurableCaptureStage.MEDIA_SAVED,
    )

    private val completedStages = setOf(
        DurableCaptureStage.COMMITTED,
        DurableCaptureStage.CANCELLED,
    )

    fun isWellFormed(records: List<DurableCaptureRecord>): Boolean =
        records.map(DurableCaptureRecord::captureId).distinct().size == records.size &&
            records.map(DurableCaptureRecord::reservationOrder).distinct().size == records.size

    fun canReserve(
        records: List<DurableCaptureRecord>,
        activeWorkerIds: Set<String>,
        expectedTemplateId: String,
        expectedProjectId: String?,
    ): Boolean {
        if (!isWellFormed(records)) return false
        if (records.count { it.stage in outstandingStages } >= MAX_OUTSTANDING) return false
        return records.none { record ->
            record.stage == DurableCaptureStage.NEEDS_REVIEW ||
                (record.stage in outstandingStages &&
                    (record.templateId != expectedTemplateId || record.projectId != expectedProjectId)) ||
                ((record.stage == DurableCaptureStage.PROCESSING ||
                    record.stage == DurableCaptureStage.MEDIA_SAVED) &&
                    record.captureId !in activeWorkerIds)
        }
    }

    /** The serial worker may process only the earliest unresolved reservation. */
    fun nextReady(
        records: List<DurableCaptureRecord>,
        activeWorkerIds: Set<String>,
    ): DurableCaptureRecord? {
        if (!isWellFormed(records)) return null
        val firstUnresolved = records.sortedBy(DurableCaptureRecord::reservationOrder)
            .firstOrNull { it.stage !in completedStages }
            ?: return null
        return firstUnresolved.takeIf {
            it.stage == DurableCaptureStage.READY && it.captureId !in activeWorkerIds
        }
    }

    fun recoveryAction(
        record: DurableCaptureRecord,
        activeWorkerIds: Set<String>,
        expectedTemplateId: String,
    ): DurableCaptureRecoveryAction = when (record.stage) {
        DurableCaptureStage.RESERVED -> DurableCaptureRecoveryAction.CANCEL_INCOMPLETE_RESERVATION
        DurableCaptureStage.READY -> when {
            record.captureId in activeWorkerIds -> DurableCaptureRecoveryAction.NONE
            record.templateId == expectedTemplateId -> DurableCaptureRecoveryAction.RESUME_READY
            else -> DurableCaptureRecoveryAction.REQUIRE_REVIEW
        }
        DurableCaptureStage.PROCESSING,
        DurableCaptureStage.MEDIA_SAVED -> if (record.captureId in activeWorkerIds) {
            DurableCaptureRecoveryAction.NONE
        } else {
            DurableCaptureRecoveryAction.REQUIRE_REVIEW
        }
        DurableCaptureStage.NEEDS_REVIEW -> DurableCaptureRecoveryAction.REQUIRE_REVIEW
        DurableCaptureStage.COMMITTED,
        DurableCaptureStage.CANCELLED -> DurableCaptureRecoveryAction.NONE
    }

    fun canCommit(records: List<DurableCaptureRecord>, captureId: String): Boolean {
        if (!isWellFormed(records)) return false
        val ordered = records.sortedBy(DurableCaptureRecord::reservationOrder)
        val index = ordered.indexOfFirst { it.captureId == captureId }
        if (index < 0 || ordered[index].stage != DurableCaptureStage.MEDIA_SAVED) return false
        return ordered.take(index).all { it.stage in completedStages }
    }

    fun latestUndoableCaptureId(records: List<DurableCaptureRecord>): String? {
        if (!isWellFormed(records)) return null
        if (records.any { it.stage in outstandingStages || it.stage == DurableCaptureStage.NEEDS_REVIEW }) {
            return null
        }
        return records.filter { it.stage == DurableCaptureStage.COMMITTED }
            .maxByOrNull(DurableCaptureRecord::reservationOrder)
            ?.captureId
    }

    fun canTransition(from: DurableCaptureStage, to: DurableCaptureStage): Boolean = when (from) {
        DurableCaptureStage.RESERVED -> to == DurableCaptureStage.READY ||
            to == DurableCaptureStage.CANCELLED || to == DurableCaptureStage.NEEDS_REVIEW
        DurableCaptureStage.READY -> to == DurableCaptureStage.PROCESSING ||
            to == DurableCaptureStage.NEEDS_REVIEW
        DurableCaptureStage.PROCESSING -> to == DurableCaptureStage.MEDIA_SAVED ||
            to == DurableCaptureStage.NEEDS_REVIEW
        DurableCaptureStage.MEDIA_SAVED -> to == DurableCaptureStage.COMMITTED ||
            to == DurableCaptureStage.NEEDS_REVIEW
        DurableCaptureStage.COMMITTED,
        DurableCaptureStage.CANCELLED,
        DurableCaptureStage.NEEDS_REVIEW -> false
    }
}
