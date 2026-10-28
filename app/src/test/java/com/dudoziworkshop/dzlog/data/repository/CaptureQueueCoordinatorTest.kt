package com.dudoziworkshop.dzlog.data.repository

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.CaptureRequest
import com.dudoziworkshop.dzlog.domain.model.LogEntry
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.WatermarkConfig
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.table.TablePatch
import com.dudoziworkshop.dzlog.feature.capture.policy.CaptureReservationScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException
import java.util.Collections

@RunWith(RobolectricTestRunner::class)
class CaptureQueueCoordinatorTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun snapshot(number: Int) = CaptureJobSnapshot(
        request = CaptureRequest(
            relativePath = "Pictures/DZlog/queue/",
            displayName = "queue_$number.jpg",
            resolvedCells = emptyList(),
            watermarkCells = emptyList(),
            saveMode = SaveMode.ORIGINAL_ONLY,
            captureAspect = CaptureAspect.R3_4,
            tableTemplate = TableTemplateState(rows = 1, cols = 1, cells = emptyList()),
            watermark = WatermarkConfig(
                anchor = WatermarkTableAnchor.TOP_LEFT,
                offsetXRatio = 0, offsetYRatio = 0,
                tableHeightRatio = 20, tableWidthRatio = 50,
                tableBgAlpha = 100, bgStyle = 0,
                valueScale = 100, textColorMode = 0,
                manualTextColor = 0, textAlign = 0
            )
        ),
        tablePatch = TablePatch.EMPTY,
    )

    private fun reserveAndStage(
        store: CapturePendingStore,
        scope: CaptureReservationScope,
        input: File,
    ): CapturePendingStore.PendingCapture {
        val reservation = store.reserveNext(scope, 1, ::snapshot)
        return store.stageJpeg(reservation.reservation.id, input)
    }

    private fun cleanup(ids: List<String>) {
        val directory = File(context.filesDir, "capture-pending-v2")
        ids.forEach { id ->
            directory.listFiles().orEmpty().filter { it.name.startsWith(id) }
                .forEach(File::delete)
        }
    }

    @Test fun serialProcessorCompletesThreeAndRejectsFourthWhileBusy() = runBlocking {
        val store = CapturePendingStore(context)
        val input = File(context.cacheDir, "dzlog-queue-test-${System.nanoTime()}.jpg")
        input.writeBytes(byteArrayOf(1, 2, 3))
        val scope = CaptureReservationScope("Pictures/DZlog/queue/${System.nanoTime()}/", "queue_")
        val jobs = (1..4).map { reserveAndStage(store, scope, input) }
        val startedFirst = CompletableDeferred<Unit>()
        val allowFirst = CompletableDeferred<Unit>()
        val completedThird = CompletableDeferred<Unit>()
        val order = Collections.synchronizedList(mutableListOf<String>())
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val queue = CaptureQueueCoordinator(
                store = store,
                process = { job ->
                    order.add(job.reservation.id)
                    if (job.reservation.id == jobs[0].reservation.id) {
                        startedFirst.complete(Unit)
                        allowFirst.await()
                    }
                    val uri = Uri.parse("content://test/${job.reservation.id}")
                    LogEntry(
                        mediaStoreId = 1L,
                        contentUri = uri,
                        savedContentUris = listOf(uri),
                        displayName = job.snapshot.request.displayName,
                        isNameAdjusted = false,
                        createdAt = System.currentTimeMillis(),
                    )
                },
                commitProgress = { _, _ -> },
                scope = workerScope,
                onEvent = { event ->
                    if (event is CaptureQueueCoordinator.Event.Completed &&
                        event.id == jobs[2].reservation.id) {
                        completedThird.complete(Unit)
                    }
                }
            )
            assertTrue(queue.submit(jobs[0].reservation.id))
            withTimeout(5000) { startedFirst.await() }
            assertTrue(queue.submit(jobs[1].reservation.id))
            assertTrue(queue.submit(jobs[2].reservation.id))
            assertFalse(queue.submit(jobs[3].reservation.id))
            assertEquals(3, queue.outstandingCount())
            allowFirst.complete(Unit)
            withTimeout(5000) { completedThird.await() }
            assertEquals(jobs.take(3).map { it.reservation.id }, order.toList())
            assertTrue(store.inspectRecovery().needsReview.any { it.reservation.id == jobs[3].reservation.id }.not())
        } finally {
            allowFirst.complete(Unit)
            workerScope.cancel()
            input.delete()
            cleanup(jobs.map { it.reservation.id })
        }
    }

    @Test fun settledSignalFollowsQueueRemovalAndReadyCannotRunTwice() = runBlocking {
        val store = CapturePendingStore(context)
        val input = File(context.cacheDir, "dzlog-recover-${System.nanoTime()}.jpg")
        input.writeBytes(byteArrayOf(1, 2, 3))
        val scope = CaptureReservationScope("Pictures/DZlog/resume/${System.nanoTime()}/", "resume_")
        val pending = reserveAndStage(store, scope, input)
        val id = pending.reservation.id
        val settled = CompletableDeferred<Unit>()
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var saves = 0
        try {
            lateinit var queue: CaptureQueueCoordinator
            queue = CaptureQueueCoordinator(
                store = store,
                process = { job ->
                    saves++
                    val uri = Uri.parse("content://test/${job.reservation.id}")
                    LogEntry(
                        mediaStoreId = 1L, contentUri = uri,
                        savedContentUris = listOf(uri),
                        displayName = job.snapshot.request.displayName,
                        isNameAdjusted = false, createdAt = System.currentTimeMillis(),
                    )
                },
                commitProgress = { _, _ -> },
                scope = workerScope,
                onEvent = { event ->
                    if (event is CaptureQueueCoordinator.Event.Settled && event.id == id) {
                        assertEquals(0, queue.outstandingCount())
                        settled.complete(Unit)
                    }
                },
            )
            // A verified READY journal from a previous UI session is safe to
            // resume. An already in-flight ID must never be enqueued twice.
            assertTrue(queue.submit(id))
            assertFalse(queue.submit(id))
            withTimeout(5000) { settled.await() }
            assertEquals(1, saves)
            assertEquals(
                CapturePendingStore.Status.COMMITTED,
                store.inspectRecovery().completedAwaitingApply.single {
                    it.reservation.id == id
                }.status,
            )
        } finally {
            workerScope.cancel()
            input.delete()
            cleanup(listOf(id))
        }
    }

    @Test fun corruptedReadyJpegIsQuarantinedWithoutAnyMediaStoreAttempt() = runBlocking {
        val store = CapturePendingStore(context)
        val input = File(context.cacheDir, "dzlog-corrupt-${System.nanoTime()}.jpg")
        input.writeBytes(byteArrayOf(7, 8, 9))
        val scope = CaptureReservationScope("Pictures/DZlog/corrupt/${System.nanoTime()}/", "bad_")
        val ready = reserveAndStage(store, scope, input)
        ready.imageFile.appendBytes(byteArrayOf(10))
        val failed = CompletableDeferred<Unit>()
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var mediaWrites = 0
        try {
            val queue = CaptureQueueCoordinator(
                store = store,
                process = {
                    mediaWrites++
                    throw IOException("Must not reach the saver")
                },
                commitProgress = { _, _ -> },
                scope = workerScope,
                onEvent = { event ->
                    if (event is CaptureQueueCoordinator.Event.ReviewRequired) {
                        failed.complete(Unit)
                    }
                },
            )
            assertTrue(queue.submit(ready.reservation.id))
            withTimeout(5000) { failed.await() }
            assertEquals(0, mediaWrites)
            val report = store.inspectRecovery()
            assertTrue(report.needsReview.any {
                it.reservation.id == ready.reservation.id &&
                    it.status == CapturePendingStore.Status.NEEDS_REVIEW
            })
            assertTrue(
                CaptureRecoveryStatusPolicy.resumableReadyIds(report, emptySet()).isEmpty()
            )
        } finally {
            workerScope.cancel()
            input.delete()
            cleanup(listOf(ready.reservation.id))
        }
    }

    @Test fun failedProcessingIsQuarantinedWithoutAutomaticRetry() = runBlocking {
        val store = CapturePendingStore(context)
        val input = File(context.cacheDir, "dzlog-queue-fail-${System.nanoTime()}.jpg")
        input.writeBytes(byteArrayOf(4, 5, 6))
        val scope = CaptureReservationScope("Pictures/DZlog/queue-fail/${System.nanoTime()}/", "queue_")
        val job = reserveAndStage(store, scope, input)
        val failed = CompletableDeferred<Unit>()
        val workerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var attempts = 0
        try {
            val queue = CaptureQueueCoordinator(
                store = store,
                process = {
                    attempts++
                    throw IOException("Simulated MediaStore failure")
                },
                commitProgress = { _, _ -> },
                scope = workerScope,
                onEvent = { event ->
                    if (event is CaptureQueueCoordinator.Event.ReviewRequired) failed.complete(Unit)
                }
            )
            assertTrue(queue.submit(job.reservation.id))
            withTimeout(5000) { failed.await() }
            assertEquals(1, attempts)
            assertTrue(store.inspectRecovery().needsReview.any {
                it.reservation.id == job.reservation.id
            })
        } finally {
            workerScope.cancel()
            input.delete()
            cleanup(listOf(job.reservation.id))
        }
    }
}
