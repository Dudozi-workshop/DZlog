package com.dudoziworkshop.dzlog.data.repository

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Bounded, serial post-capture worker.
 *
 * - Accepts only durable capture IDs, never Bitmaps, to limit heap usage.
 * - A processing failure is quarantined, not automatically retried: the MediaStore
 *   write might have succeeded before the process/commit failed.
 * - App-start recovery may resubmit only verified READY jobs. PROCESSING and
 *   SAVED_UNCOMMITTED must be reconciled first.
 * - The caller's scope must be application-owned, not a CameraScreen composition scope.
 *
 * This component is intentionally not connected to the live shutter yet.
 */
internal class CaptureQueueCoordinator(
    private val store: CapturePendingStore,
    private val process: suspend (CapturePendingStore.PendingCapture) -> List<Uri>,
    private val commitProgress: suspend (CapturePendingStore.PendingCapture, List<Uri>) -> Unit,
    scope: CoroutineScope,
    private val onEvent: (Event) -> Unit = {},
) {
    internal sealed interface Event {
        data class Started(val id: String) : Event
        data class Completed(val id: String, val uris: List<Uri>) : Event
        data class ReviewRequired(val id: String, val cause: Throwable) : Event
    }

    private val ids = Channel<String>(capacity = MAX_OUTSTANDING)
    private val inFlight = mutableSetOf<String>()
    private val lock = Any()

    private val worker = scope.launch(Dispatchers.IO) {
        for (id in ids) {
            try {
                val job = store.markProcessing(id)
                notify(Event.Started(id))
                val outputUris = process(job)
                store.markMediaSaved(id, outputUris)
                commitProgress(job, outputUris)
                store.markCommitted(id)
                notify(Event.Completed(id, outputUris))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                // Uncertain saves must not re-enter the ready queue automatically.
                runCatching { store.markNeedsReview(id) }
                    .onFailure { Log.e(TAG, "Unable to mark capture for review", it) }
                Log.e(TAG, "Capture requires review: $id", error)
                notify(Event.ReviewRequired(id, error))
            } finally {
                synchronized(lock) { inFlight.remove(id) }
            }
        }
    }

    /** Returns false when the limit is reached, the ID is duplicated, or the worker stopped. */
    fun submit(id: String): Boolean = synchronized(lock) {
        if (!worker.isActive || inFlight.size >= MAX_OUTSTANDING || !inFlight.add(id)) {
            return@synchronized false
        }
        if (ids.trySend(id).isFailure) {
            inFlight.remove(id)
            return@synchronized false
        }
        true
    }

    fun outstandingCount(): Int = synchronized(lock) { inFlight.size }

    private fun notify(event: Event) {
        runCatching { onEvent(event) }
            .onFailure { Log.w(TAG, "Queue observer failed", it) }
    }

    private companion object {
        const val MAX_OUTSTANDING = 3
        const val TAG = "DZlogCaptureQueue"
    }
}
