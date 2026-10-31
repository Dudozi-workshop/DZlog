package com.dudoziworkshop.dzlog.data.repository

import android.content.Context
import androidx.camera.core.ImageCapture
import com.dudoziworkshop.dzlog.data.project.CaptureRecordRoomStore
import com.dudoziworkshop.dzlog.domain.model.LogEntry
import com.dudoziworkshop.dzlog.feature.capture.policy.CaptureReservationScope
import com.dudoziworkshop.dzlog.feature.capture.policy.CapturePendingProjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Data-layer bridge between CameraX staging and the serial save worker.
 *
 * The host must own [applicationScope] for the entire application lifetime.
 * CameraScreen uses this service in all preview modes. Durable receipts are
 * reconciled with the bound template and Undo before journal acknowledgement.
 */
internal class CaptureAsyncCaptureService(
    context: Context,
    repository: DzlogRepositoryImpl,
    applicationScope: CoroutineScope,
    commitProgress: suspend (CapturePendingStore.PendingCapture, LogEntry) -> Unit,
) {
    private val appContext = context.applicationContext
    /** Projection survives camera Composable recreation within this process. */
    internal val pendingProjection = CapturePendingProjection()
    private val stagingIds = mutableSetOf<String>()
    private val stagingLock = Any()
    private val mutableStateVersion = MutableStateFlow(0L)
    /** A durable, conflated wake-up signal: completion cannot be lost on screen re-entry. */
    internal val stateVersion = mutableStateVersion.asStateFlow()
    private fun signalChange() { mutableStateVersion.update { it + 1 } }
    private val store = CapturePendingStore(appContext)
    private val recordStore = CaptureRecordRoomStore(appContext)
    private val gateway = CaptureStagedCameraGateway(appContext, store, applicationScope)
    private val mutableEvents =
        MutableSharedFlow<CaptureQueueCoordinator.Event>(extraBufferCapacity = 32)
    val events = mutableEvents.asSharedFlow()
    private val queue = CaptureQueueCoordinator(
        store = store,
        process = { staged -> repository.saveStagedCapture(appContext, staged) },
        commitProgress = commitProgress,
        scope = applicationScope,
        onEvent = { event ->
            mutableEvents.tryEmit(event)
            signalChange()
        },
    )

    internal data class CaptureCallbacks(
        val onCaptureStarted: () -> Unit = {},
        val onImageCaptured: () -> Unit = {},
        val onQueued: (CapturePendingStore.PendingCapture) -> Unit,
        /** The JPEG is safe, but the bounded worker could not accept it yet. */
        val onQueuedLater: (CapturePendingStore.PendingCapture) -> Unit,
        val onFailure: (Throwable) -> Unit,
    )

    fun reserve(
        scope: CaptureReservationScope,
        expectedNextCounter: Int,
        snapshotForCounter: (Int) -> CaptureJobSnapshot,
    ): CapturePendingStore.PendingCapture =
        store.reserveNext(scope, expectedNextCounter, snapshotForCounter)

    fun captureReserved(
        imageCapture: ImageCapture,
        reservation: CapturePendingStore.PendingCapture,
        callbacks: CaptureCallbacks,
    ) {
        synchronized(stagingLock) { stagingIds += reservation.reservation.id }
        gateway.capture(
            imageCapture = imageCapture,
            reserved = reservation,
            callbacks = CaptureStagedCameraGateway.Callbacks(
                onCaptureStarted = callbacks.onCaptureStarted,
                onImageCaptured = callbacks.onImageCaptured,
                onStagedReady = { staged ->
                    // Preserve staging ownership until the queue accepts or rejects
                    // this ID; a reconnecting observer must never see an unowned
                    // READY job in the hand-off window.
                    val queued = queue.submit(staged.reservation.id)
                    synchronized(stagingLock) { stagingIds -= staged.reservation.id }
                    if (queued) callbacks.onQueued(staged)
                    else callbacks.onQueuedLater(staged)
                    signalChange()
                },
                onFailure = { error ->
                    synchronized(stagingLock) { stagingIds -= reservation.reservation.id }
                    callbacks.onFailure(error)
                    signalChange()
                },
            ),
        )
    }

    /** Persist before acknowledgement; retry uses the same capture ID after a crash. */
    suspend fun persistCommittedCapture(receipt: CapturePendingStore.PendingCapture): Boolean =
        recordStore.appendCommitted(CaptureRecordReceiptMapper.fromCompleted(receipt))

    /** Camera Undo reports confirmed file removals, not requested targets. */
    suspend fun afterVerifiedMediaDeletion(deletedUris: Set<String>): Int =
        recordStore.afterVerifiedDeletion(deletedUris)

    fun acknowledgeTemplateApplied(id: String) = store.acknowledgeTemplateApplied(id)

    fun outstandingCount(): Int = queue.outstandingCount()

    /** Active CameraX staging and queue IDs; persisted but orphaned work is excluded. */
    internal fun activeIds(): Set<String> =
        synchronized(stagingLock) { stagingIds.toSet() } + queue.activeIds()

    /**
     * Restart only SHA-verified READY jobs. MediaStore writes begin after a
     * durable READY -> PROCESSING transition, so READY can be resumed without
     * duplicating an already committed capture. Uncertain statuses are not
     * automatically resumed or deleted.
     */
    internal fun resumeVerifiedReadyCaptures(activeTemplateId: String): Int {
        val report = store.inspectRecovery()
        val active = activeIds()
        if (report.corruptJournals.isNotEmpty() || report.orphanFiles.isNotEmpty() ||
            report.needsReview.any {
                it.status == CapturePendingStore.Status.NEEDS_REVIEW ||
                    it.reservation.id !in active
            }
        ) return 0

        var accepted = 0
        // Recovery must never silently continue another saved template's
        // sequence. Legacy records without template identity stay quarantined
        // until an explicit review flow is available.
        report.ready.filter {
            it.reservation.id !in active && it.snapshot.templateId == activeTemplateId
        }.forEach { record ->
            if (!queue.submit(record.reservation.id)) return@forEach
            accepted++
        }
        if (accepted > 0) signalChange()
        return accepted
    }

    fun inspectRecovery(): CapturePendingStore.RecoveryInventory = store.inspectRecovery()
}
