package com.dudoziworkshop.dzlog.feature.counter.core

import androidx.test.core.app.ApplicationProvider
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class CounterFacadeTest {

    @Test
    fun `same request returns same next`() = runBlocking {
        val facade = CounterFacade(ApplicationProvider.getApplicationContext())
        val request = newRequest()

        val first = facade.read(request)
        val second = facade.read(request)

        assertEquals(first.next, second.next)
    }

    @Test
    fun `empty stream returns one`() = runBlocking {
        val facade = CounterFacade(ApplicationProvider.getApplicationContext())

        val result = facade.read(newRequest())

        assertEquals(1, result.next)
    }

    @Test
    fun `manual set is reflected in read`() = runBlocking {
        val facade = CounterFacade(ApplicationProvider.getApplicationContext())
        val request = newRequest()

        facade.setManualNext(request, 7)
        val result = facade.read(request)

        assertEquals(7, result.next)
        assertTrue(result.hasManualOverride)
    }

    @Test
    fun `readAutoNext ignores manual override and returns media based auto next`() = runBlocking {
        val facade = CounterFacade(ApplicationProvider.getApplicationContext())
        val request = newRequest()

        facade.setManualNext(request, 9)

        val resolved = facade.read(request)
        val autoNext = facade.readAutoNext(request)

        assertEquals(9, resolved.next)
        assertTrue(resolved.hasManualOverride)
        assertEquals(1, autoNext)
    }

    @Test
    fun `manual clear returns to auto`() = runBlocking {
        val facade = CounterFacade(ApplicationProvider.getApplicationContext())
        val request = newRequest()

        facade.setManualNext(request, 5)
        facade.clearManualNext(request)
        val result = facade.read(request)

        assertEquals(1, result.next)
        assertFalse(result.hasManualOverride)
    }

    @Test
    fun `refresh after undo does not clear manual override`() = runBlocking {
        val facade = CounterFacade(ApplicationProvider.getApplicationContext())
        val request = newRequest()

        facade.setManualNext(request, 9)
        val refreshed = facade.refreshAfterUndo(request)

        assertEquals(9, refreshed.next)
        assertTrue(refreshed.hasManualOverride)
    }

    @Test
    fun `facade consumes resolver normalized contract for both and watermark including effective mode`() = runBlocking {
        val facade = CounterFacade(ApplicationProvider.getApplicationContext())
        val suffix = UUID.randomUUID().toString()

        val watermark = CounterRequestResolver.fromCamera(
            saveMode = SaveMode.WATERMARK_ONLY,
            relativePathKey = "Pictures/DZlog/tests/$suffix/",
            prefix = "stream_$suffix",
            scanPrefix = "scan_$suffix",
            includePathInScope = true,
            includeFilenameInScope = true,
        )
        val both = CounterRequestResolver.fromCamera(
            saveMode = SaveMode.BOTH,
            relativePathKey = "Pictures/DZlog/tests/$suffix/",
            prefix = "stream_$suffix",
            scanPrefix = "scan_$suffix",
            includePathInScope = true,
            includeFilenameInScope = true,
        )

        assertEquals(SaveMode.WATERMARK_ONLY, watermark.effectiveSaveMode)
        assertEquals(SaveMode.WATERMARK_ONLY, both.effectiveSaveMode)

        val watermarkResult = facade.read(watermark)
        val bothResult = facade.read(both)

        assertEquals(
            watermarkResult.scopedStream.captureStreamKey.relativePathKey,
            bothResult.scopedStream.captureStreamKey.relativePathKey,
        )
        assertEquals(
            watermarkResult.scopedStream.captureStreamKey.prefix,
            bothResult.scopedStream.captureStreamKey.prefix,
        )
        assertEquals(
            watermarkResult.scopedStream.captureStreamKey.scanPrefix,
            bothResult.scopedStream.captureStreamKey.scanPrefix,
        )
    }

    @Test
    fun `facade consumes resolver normalized contract for original only separation including effective mode`() = runBlocking {
        val facade = CounterFacade(ApplicationProvider.getApplicationContext())
        val suffix = UUID.randomUUID().toString()

        val watermark = CounterRequestResolver.fromCamera(
            saveMode = SaveMode.WATERMARK_ONLY,
            relativePathKey = "Pictures/DZlog/tests/$suffix/",
            prefix = "stream_$suffix",
            scanPrefix = "scan_$suffix",
            includePathInScope = true,
            includeFilenameInScope = true,
        )
        val original = CounterRequestResolver.fromCamera(
            saveMode = SaveMode.ORIGINAL_ONLY,
            relativePathKey = "Pictures/DZlog/tests/$suffix/",
            prefix = "stream_$suffix",
            scanPrefix = "scan_$suffix",
            includePathInScope = true,
            includeFilenameInScope = true,
        )

        assertEquals(SaveMode.WATERMARK_ONLY, watermark.effectiveSaveMode)
        assertEquals(SaveMode.ORIGINAL_ONLY, original.effectiveSaveMode)

        val watermarkResult = facade.read(watermark)
        val originalResult = facade.read(original)

        assertNotEquals(
            watermarkResult.scopedStream.captureStreamKey.relativePathKey,
            originalResult.scopedStream.captureStreamKey.relativePathKey,
        )
        // 중요: saveMode 문자열 분기가 아니라 resolver가 만든 정규화된 stream 입력 차이로 분리된다.
        assertEquals(
            watermarkResult.scopedStream.captureStreamKey.prefix,
            originalResult.scopedStream.captureStreamKey.prefix,
        )
    }


    @Test
    fun `table request manual set and clear flows through facade consistently`() = runBlocking {
        val facade = CounterFacade(ApplicationProvider.getApplicationContext())
        val suffix = UUID.randomUUID().toString()
        val request = CounterRequestResolver.fromTable(
            saveMode = SaveMode.BOTH,
            relativePathKey = "Pictures/DZlog/table/$suffix/",
            prefix = "table_stream_$suffix",
            scanPrefix = "table_scan_$suffix",
            includePathInScope = true,
            includeFilenameInScope = true,
            tableTemplateId = "table-template",
        )

        facade.setManualNext(request, 6)
        val manualRead = facade.read(request)
        assertEquals(6, manualRead.next)
        assertTrue(manualRead.hasManualOverride)

        facade.clearManualNext(request)
        val autoRead = facade.read(request)
        assertEquals(1, autoRead.next)
        assertFalse(autoRead.hasManualOverride)
    }

    private fun newRequest(saveMode: SaveMode = SaveMode.WATERMARK_ONLY): CounterRequest {
        val suffix = UUID.randomUUID().toString()
        return CounterRequestResolver.fromCamera(
            saveMode = saveMode,
            relativePathKey = "Pictures/DZlog/tests/$suffix/",
            prefix = "stream_$suffix",
            scanPrefix = "scan_$suffix",
            includePathInScope = true,
            includeFilenameInScope = true,
        )
    }
}
