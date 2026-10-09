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
    fun `commit index alone does not advance without physical media`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val stream = createScopedStream("rp_왜|g2=0")

        CaptureCounterPolicy.commit(
            context = context,
            scopedStream = stream,
            usedCounter = 1,
            mediaStoreId = 1001L,
        )

        val next = CaptureCounterPolicy.resolveNext(
            context = context,
            scopedStream = stream,
            scanPrefix = stream.captureStreamKey.scanPrefix,
            counterDigits = 0,
            fnDelim = "_",
            saveMode = SaveMode.WATERMARK_ONLY,
        )

        assertEquals(1, next)
    }

    @Test
    fun `different phrase stream remains separated`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val whyStream = createScopedStream("rp_왜|g2=0")
        val wowStream = createScopedStream("rp_헐|g2=0")

        val wowBefore = CaptureCounterPolicy.resolveNext(
            context = context,
            scopedStream = wowStream,
            scanPrefix = wowStream.captureStreamKey.scanPrefix,
            counterDigits = 0,
            fnDelim = "_",
            saveMode = SaveMode.WATERMARK_ONLY,
        )

        CaptureCounterPolicy.commit(
            context = context,
            scopedStream = whyStream,
            usedCounter = 1,
            mediaStoreId = 2001L,
        )

        val wowAfter = CaptureCounterPolicy.resolveNext(
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
    fun `two auxiliary commits still do not advance without physical media`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val stream = createScopedStream("rp_왜|g2=0")

        CaptureCounterPolicy.commit(
            context = context,
            scopedStream = stream,
            usedCounter = 1,
            mediaStoreId = 3001L,
        )
        CaptureCounterPolicy.commit(
            context = context,
            scopedStream = stream,
            usedCounter = 2,
            mediaStoreId = 3002L,
        )

        val next = CaptureCounterPolicy.resolveNext(
            context = context,
            scopedStream = stream,
            scanPrefix = stream.captureStreamKey.scanPrefix,
            counterDigits = 0,
            fnDelim = "_",
            saveMode = SaveMode.WATERMARK_ONLY,
        )

        assertEquals(1, next)
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
