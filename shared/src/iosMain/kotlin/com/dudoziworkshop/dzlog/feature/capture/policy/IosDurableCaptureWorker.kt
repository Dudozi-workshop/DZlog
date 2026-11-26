package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.data.media.CaptureMediaRepository
import com.dudoziworkshop.dzlog.data.media.IosCaptureSaveCoordinator
import com.dudoziworkshop.dzlog.data.media.IosCaptureWorkspaceJournal
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole

enum class IosDurableCaptureWorkerStatus {
    COMMITTED,
    RECOVERED,
    WAITING_FOR_RECOVERY,
    NEEDS_REVIEW,
}

data class IosDurableCaptureWorkerResult(
    val captureId: String,
    val status: IosDurableCaptureWorkerStatus,
)

/**
 * Serial execution engine for already frozen captures. The UI chooses the coroutine scope;
 * this class guarantees that only one drain owns the queue and that no later reservation
 * can skip an unresolved earlier one.
 */
class IosDurableCaptureWorker(
    private val staging: IosDurableCaptureStagingStore,
    media: CaptureMediaRepository,
    private val loadWorkspace: () -> EditorWorkspace,
    private val persistWorkspace: (EditorWorkspace) -> Boolean,
    private val buildPayloads: suspend (
        snapshot: DurableCaptureSnapshot,
        sourceJpeg: ByteArray,
    ) -> Map<CaptureMediaRole, ByteArray>,
    journal: IosCaptureWorkspaceJournal = IosCaptureWorkspaceJournal(),
) {
    private val coordinator = IosCaptureSaveCoordinator(media, journal)
    private var draining = false
    private var rerunRequested = false
    private var activeCaptureId: String? = null

    fun activeCaptureIds(): Set<String> = setOfNotNull(activeCaptureId)

    suspend fun processAvailable(): List<IosDurableCaptureWorkerResult> {
        if (draining) {
            rerunRequested = true
            return emptyList()
        }
        draining = true
        return try {
            val results = mutableListOf<IosDurableCaptureWorkerResult>()
            while (true) {
                val records = staging.listSnapshots()
                val first = records.firstOrNull { it.record.stage !in terminalStages }
                if (first == null) {
                    if (rerunRequested) {
                        rerunRequested = false
                        continue
                    }
                    break
                }
                activeCaptureId = first.record.captureId
                val result = when (first.record.stage) {
                    DurableCaptureStage.READY -> processReady(records, first)
                    DurableCaptureStage.PROCESSING -> recoverProcessing(first)
                    DurableCaptureStage.MEDIA_SAVED -> recoverMediaSaved(first)
                    DurableCaptureStage.NEEDS_REVIEW,
                    DurableCaptureStage.RESERVED -> IosDurableCaptureWorkerResult(
                        first.record.captureId, IosDurableCaptureWorkerStatus.NEEDS_REVIEW)
                    DurableCaptureStage.COMMITTED,
                    DurableCaptureStage.CANCELLED -> error("Terminal capture was not filtered.")
                }
                activeCaptureId = null
                results += result
                if (result.status == IosDurableCaptureWorkerStatus.WAITING_FOR_RECOVERY ||
                    result.status == IosDurableCaptureWorkerStatus.NEEDS_REVIEW) break
            }
            results
        } finally {
            activeCaptureId = null
            draining = false
        }
    }

    private suspend fun processReady(
        snapshots: List<DurableCaptureSnapshot>,
        snapshot: DurableCaptureSnapshot,
    ): IosDurableCaptureWorkerResult {
        check(DurableCaptureQueuePolicy.nextReady(snapshots.map(DurableCaptureSnapshot::record),
            emptySet())?.captureId == snapshot.record.captureId) {
            "A later capture cannot skip the queue head."
        }
        if (loadWorkspace() != snapshot.workspaceBefore) {
            staging.markNeedsReview(snapshot.record.captureId,
                DurableCaptureReviewReason.WORKSPACE_CHANGED_BEFORE_SAVE)
            return IosDurableCaptureWorkerResult(snapshot.record.captureId,
                IosDurableCaptureWorkerStatus.NEEDS_REVIEW)
        }

        // Rendering is deterministic and reversible. Keep READY until every payload exists.
        val source = staging.readSourceJpeg(snapshot.record.captureId)
        val payloads = buildPayloads(snapshot, source)
        snapshot.savePlan.validatePayloads(payloads)

        staging.transition(snapshot.record.captureId, DurableCaptureStage.PROCESSING)
        val committed = coordinator.saveAndCommit(
            originalWorkspace = snapshot.workspaceBefore,
            selectedDocument = snapshot.capturedDocument,
            plan = snapshot.savePlan,
            jpegByRole = payloads,
            loadWorkspace = loadWorkspace,
            persistWorkspace = persistWorkspace,
        )
        if (!committed) {
            return IosDurableCaptureWorkerResult(snapshot.record.captureId,
                IosDurableCaptureWorkerStatus.WAITING_FOR_RECOVERY)
        }
        staging.transition(snapshot.record.captureId, DurableCaptureStage.MEDIA_SAVED)
        staging.transition(snapshot.record.captureId, DurableCaptureStage.COMMITTED)
        return IosDurableCaptureWorkerResult(snapshot.record.captureId,
            IosDurableCaptureWorkerStatus.COMMITTED)
    }

    private suspend fun recoverProcessing(
        snapshot: DurableCaptureSnapshot,
    ): IosDurableCaptureWorkerResult {
        if (!coordinator.hasPendingCapture()) {
            staging.markNeedsReview(snapshot.record.captureId,
                DurableCaptureReviewReason.PROCESSING_RECEIPT_MISSING)
            return IosDurableCaptureWorkerResult(snapshot.record.captureId,
                IosDurableCaptureWorkerStatus.NEEDS_REVIEW)
        }
        if (!coordinator.recoverPending(loadWorkspace, persistWorkspace)) {
            return IosDurableCaptureWorkerResult(snapshot.record.captureId,
                IosDurableCaptureWorkerStatus.WAITING_FOR_RECOVERY)
        }
        if (loadWorkspace() != snapshot.workspaceAfter) {
            staging.markNeedsReview(snapshot.record.captureId,
                DurableCaptureReviewReason.RECOVERED_WORKSPACE_MISMATCH)
            return IosDurableCaptureWorkerResult(snapshot.record.captureId,
                IosDurableCaptureWorkerStatus.NEEDS_REVIEW)
        }
        staging.transition(snapshot.record.captureId, DurableCaptureStage.MEDIA_SAVED)
        staging.transition(snapshot.record.captureId, DurableCaptureStage.COMMITTED)
        return IosDurableCaptureWorkerResult(snapshot.record.captureId,
            IosDurableCaptureWorkerStatus.RECOVERED)
    }

    private fun recoverMediaSaved(snapshot: DurableCaptureSnapshot): IosDurableCaptureWorkerResult {
        if (coordinator.hasPendingCapture() || loadWorkspace() != snapshot.workspaceAfter) {
            staging.markNeedsReview(snapshot.record.captureId,
                DurableCaptureReviewReason.MEDIA_SAVED_STATE_UNCERTAIN)
            return IosDurableCaptureWorkerResult(snapshot.record.captureId,
                IosDurableCaptureWorkerStatus.NEEDS_REVIEW)
        }
        staging.transition(snapshot.record.captureId, DurableCaptureStage.COMMITTED)
        return IosDurableCaptureWorkerResult(snapshot.record.captureId,
            IosDurableCaptureWorkerStatus.RECOVERED)
    }

    private companion object {
        val terminalStages = setOf(DurableCaptureStage.COMMITTED, DurableCaptureStage.CANCELLED)
    }
}
