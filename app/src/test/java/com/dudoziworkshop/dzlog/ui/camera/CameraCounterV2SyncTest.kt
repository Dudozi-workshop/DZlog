package com.dudoziworkshop.dzlog.ui.camera

import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureStreamKey
import com.dudoziworkshop.dzlog.domain.counter.CaptureScopedCounterStream
import com.dudoziworkshop.dzlog.domain.counter.CounterScopeParts
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.feature.counter.CounterReadResult
import com.dudoziworkshop.dzlog.feature.counter.CounterSyncReason
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraCounterV2SyncTest {

    @Test
    fun `save mode change adopts empty new stream one`() {
        val reason = detectCameraV2SyncReason(
            isInitial = false,
            isResumeEvent = false,
            counterEvent = null,
            previousSaveMode = SaveMode.WATERMARK_ONLY,
            currentSaveMode = SaveMode.ORIGINAL_ONLY,
            previousRequestKey = "water",
            currentRequestKey = "orig",
        )
        val next = applyCameraV2SyncedNext(
            reason = reason,
            currentDisplayedNext = 2,
            read = readResult(next = 1),
            previousRequestKey = "water",
            currentRequestKey = "orig",
        )

        assertEquals(CounterSyncReason.SAVE_MODE_CHANGE, reason)
        assertEquals(1, next)
    }

    @Test
    fun `same stream resume prevents unnecessary downward`() {
        val reason = detectCameraV2SyncReason(
            isInitial = false,
            isResumeEvent = true,
            counterEvent = null,
            previousSaveMode = SaveMode.BOTH,
            currentSaveMode = SaveMode.BOTH,
            previousRequestKey = "same",
            currentRequestKey = "same",
        )
        val next = applyCameraV2SyncedNext(
            reason = reason,
            currentDisplayedNext = 5,
            read = readResult(next = 3),
            previousRequestKey = "same",
            currentRequestKey = "same",
        )

        assertEquals(CounterSyncReason.RESUME, reason)
        assertEquals(5, next)
    }

    @Test
    fun `undo allows downward sync`() {
        val reason = detectCameraV2SyncReason(
            isInitial = false,
            counterEvent = CameraCounterSyncEvent.UNDO_COMMITTED,
            isResumeEvent = false,
            previousSaveMode = SaveMode.BOTH,
            currentSaveMode = SaveMode.BOTH,
            previousRequestKey = "same",
            currentRequestKey = "same",
        )
        val next = applyCameraV2SyncedNext(
            reason = reason,
            currentDisplayedNext = 4,
            read = readResult(next = 2),
            previousRequestKey = "same",
            currentRequestKey = "same",
        )

        assertEquals(CounterSyncReason.UNDO, reason)
        assertEquals(2, next)
    }

    @Test
    fun `capture committed event uses dedicated forward sync reason`() {
        val reason = detectCameraV2SyncReason(
            isInitial = false,
            counterEvent = CameraCounterSyncEvent.CAPTURE_COMMITTED,
            isResumeEvent = false,
            previousSaveMode = SaveMode.BOTH,
            currentSaveMode = SaveMode.BOTH,
            previousRequestKey = "same",
            currentRequestKey = "same",
        )
        val next = applyCameraV2SyncedNext(
            reason = reason,
            currentDisplayedNext = 4,
            read = readResult(next = 5),
            previousRequestKey = "same",
            currentRequestKey = "same",
        )

        assertEquals(CounterSyncReason.CAPTURE_COMMITTED, reason)
        assertEquals(5, next)
    }

    @Test
    fun `without explicit event uses resume-equivalent fallback handling`() {
        val reason = detectCameraV2SyncReason(
            isInitial = false,
            counterEvent = null,
            isResumeEvent = false,
            previousSaveMode = SaveMode.BOTH,
            currentSaveMode = SaveMode.BOTH,
            previousRequestKey = "same",
            currentRequestKey = "same",
        )

        assertEquals(CounterSyncReason.RESUME, reason)
    }

    private fun readResult(next: Int): CounterReadResult {
        val scoped = CaptureScopedCounterStream(
            scopeParts = CounterScopeParts("Pictures/DZlog/", "p", "Pictures/DZlog/|p"),
            captureStreamKey = CaptureStreamKey("Pictures/DZlog/", "p", "p"),
        )
        return CounterReadResult(next = next, hasManualOverride = false, scopedStream = scoped)
    }
}
