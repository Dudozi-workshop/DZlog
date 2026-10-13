package com.dudoziworkshop.dzlog.data.repository

import android.content.Context
import androidx.camera.core.ImageCapture
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
 * This is not enabled by CameraScreen until reserved previews and progress
 * commits can be safely reconciled with active templates and Undo.
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
                    synchronized(stagingLock) { stagingIds -= staged.reservation.id }
                    if (queue.submit(staged.reservation.id)) {
                        callbacks.onQueued(staged)
                    } else {
                        // Preserve the READY job for explicit recovery. It must never
                        // be silently dropped when the worker reaches its bound.
                        callbacks.onQueuedLater(staged)
                    }
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

    fun acknowledgeTemplateApplied(id: String) = store.acknowledgeTemplateApplied(id)

    fun outstandingCount(): Int = queue.outstandingCount()

    /** Active CameraX staging and queue IDs; persisted but orphaned work is excluded. */
    internal fun activeIds(): Set<String> =
        synchronized(stagingLock) { stagingIds.toSet() } + queue.activeIds()

    fun inspectRecovery(): CapturePendingStore.RecoveryInventory = store.inspectRecovery()
}
