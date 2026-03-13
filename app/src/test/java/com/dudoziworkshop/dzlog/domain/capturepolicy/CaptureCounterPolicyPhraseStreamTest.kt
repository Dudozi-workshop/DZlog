package com.dudoziworkshop.dzlog.domain.capturepolicy

import androidx.test.core.app.ApplicationProvider
import com.dudoziworkshop.dzlog.domain.counter.CaptureScopedCounterStream
import com.dudoziworkshop.dzlog.domain.counter.CounterScopeParts
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class CaptureCounterPolicyPhraseStreamTest {

    @Test
    fun `same phrase stream increments to two after one commit`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val stream = createScopedStream("rp_왜|g2=0")

        CaptureCounterPolicy.commitCounter(
            context = context,
            scopedStream = stream,
            usedCounter = 1,
            mediaStoreId = 1001L,
        )

        val next = CaptureCounterPolicy.getNextCounter(
            context = context,
            scopedStream = stream,
            scanPrefix = stream.captureStreamKey.scanPrefix,
            counterDigits = 0,
            fnDelim = "_",
            saveMode = SaveMode.WATERMARK_ONLY,
        )

        assertEquals(2, next)
    }

    @Test
    fun `different phrase stream remains separated`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val whyStream = createScopedStream("rp_왜|g2=0")
        val wowStream = createScopedStream("rp_헐|g2=0")

        val wowBefore = CaptureCounterPolicy.getNextCounter(
            context = context,
            scopedStream = wowStream,
            scanPrefix = wowStream.captureStreamKey.scanPrefix,
            counterDigits = 0,
            fnDelim = "_",
            saveMode = SaveMode.WATERMARK_ONLY,
        )

        CaptureCounterPolicy.commitCounter(
            context = context,
            scopedStream = whyStream,
            usedCounter = 1,
            mediaStoreId = 2001L,
        )

        val wowAfter = CaptureCounterPolicy.getNextCounter(
            context = context,
            scopedStream = wowStream,
            scanPrefix = wowStream.captureStreamKey.scanPrefix,
            counterDigits = 0,
            fnDelim = "_",
            saveMode = SaveMode.WATERMARK_ONLY,
        )

        assertEquals(1, wowBefore)
        assertEquals(1, wowAfter)
    }

    @Test
    fun `same phrase stream reaches three after two commits`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val stream = createScopedStream("rp_왜|g2=0")

        CaptureCounterPolicy.commitCounter(
            context = context,
            scopedStream = stream,
            usedCounter = 1,
            mediaStoreId = 3001L,
        )
        CaptureCounterPolicy.commitCounter(
            context = context,
            scopedStream = stream,
            usedCounter = 2,
            mediaStoreId = 3002L,
        )

        val next = CaptureCounterPolicy.getNextCounter(
            context = context,
            scopedStream = stream,
            scanPrefix = stream.captureStreamKey.scanPrefix,
            counterDigits = 0,
            fnDelim = "_",
            saveMode = SaveMode.WATERMARK_ONLY,
        )

        assertEquals(3, next)
    }

    private fun createScopedStream(prefix: String): CaptureScopedCounterStream {
        val relativePath = "Pictures/DZlog/A/${UUID.randomUUID()}/"
        val scopeParts = CounterScopeParts(
            relativePathKey = relativePath,
            prefix = prefix,
            scopeKey = "$relativePath|$prefix",
        )
        return CaptureScopedCounterStream(
            scopeParts = scopeParts,
            captureStreamKey = CaptureStreamKey(
                relativePathKey = relativePath,
                prefix = prefix,
                scanPrefix = prefix,
            )
        )
    }
}
