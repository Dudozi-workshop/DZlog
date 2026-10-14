package com.dudoziworkshop.dzlog.data.repository

import android.os.SystemClock
import android.util.Log
import java.util.concurrent.atomic.AtomicLong

/**
 * Capture pipeline timing without filenames, paths, or image contents.
 * Timestamps are monotonic; -1 means a stage was not reached.
 */
internal class CaptureTimingTracker {
    private val captureId = nextCaptureId.getAndIncrement()
    private val requestedAtNs = SystemClock.elapsedRealtimeNanos()

    @Volatile private var startedAtNs: Long = 0L
    @Volatile private var tempReadyAtNs: Long = 0L
    @Volatile private var bitmapReadyAtNs: Long = 0L
    @Volatile private var mediaReadyAtNs: Long = 0L

    fun onCaptureStarted() {
        startedAtNs = SystemClock.elapsedRealtimeNanos()
    }

    fun onTempFileReady() {
        tempReadyAtNs = SystemClock.elapsedRealtimeNanos()
    }

    fun onBitmapReady() {
        bitmapReadyAtNs = SystemClock.elapsedRealtimeNanos()
    }

    fun onMediaSaved() {
        mediaReadyAtNs = SystemClock.elapsedRealtimeNanos()
    }

    fun finish(outcome: String) {
        val finishedAtNs = SystemClock.elapsedRealtimeNanos()
        Log.d(
            TAG,
            "captureId=$captureId outcome=$outcome " +
                "requestToStartMs=${elapsedMs(requestedAtNs, startedAtNs)} " +
                "startToTempMs=${elapsedMs(startedAtNs, tempReadyAtNs)} " +
                "tempToBitmapMs=${elapsedMs(tempReadyAtNs, bitmapReadyAtNs)} " +
                "bitmapToMediaMs=${elapsedMs(bitmapReadyAtNs, mediaReadyAtNs)} " +
                "mediaToFinishMs=${elapsedMs(mediaReadyAtNs, finishedAtNs)} " +
                "totalMs=${elapsedMs(requestedAtNs, finishedAtNs)}"
        )
    }

    private fun elapsedMs(start: Long, end: Long): Long =
        if (start <= 0L || end < start) -1L else (end - start) / 1_000_000L

    private companion object {
        const val TAG = "DZlogCaptureTiming"
        val nextCaptureId = AtomicLong(1L)
    }
}
