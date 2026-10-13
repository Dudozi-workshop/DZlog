package com.dudoziworkshop.dzlog.data.repository

import android.content.Context
import androidx.camera.core.ImageCapture
import com.dudoziworkshop.dzlog.domain.model.LogEntry
import com.dudoziworkshop.dzlog.feature.capture.policy.CaptureReservationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

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
        onEvent = { event -> mutableEvents.tryEmit(event) },
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
        gateway.capture(
            imageCapture = imageCapture,
            reserved = reservation,
            callbacks = CaptureStagedCameraGateway.Callbacks(
                onCaptureStarted = callbacks.onCaptureStarted,
                onImageCaptured = callbacks.onImageCaptured,
                onStagedReady = { staged ->
                    if (queue.submit(staged.reservation.id)) {
                        callbacks.onQueued(staged)
                    } else {
                        // Preserve the READY job for explicit recovery. It must never
                        // be silently dropped when the worker reaches its bound.
                        callbacks.onQueuedLater(staged)
                    }
                },
                onFailure = callbacks.onFailure,
            ),
        )
    }

    fun acknowledgeTemplateApplied(id: String) = store.acknowledgeTemplateApplied(id)

    fun outstandingCount(): Int = queue.outstandingCount()

    fun inspectRecovery(): CapturePendingStore.RecoveryInventory = store.inspectRecovery()
}
