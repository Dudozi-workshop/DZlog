package com.dudoziworkshop.dzlog.feature.counter.camera

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class CameraCounterReadTest {
    @Test
    fun query_failure_does_not_become_next_one_or_success() = runBlocking {
        val error = SecurityException("MediaStore denied")
        val result = readCameraCounterSafely<Int> { throw error }
        assertTrue(result.isFailure)
        assertNull(result.getOrNull())
        assertSame(error, result.exceptionOrNull())
        assertEquals(12, readCameraCounterSafely { 12 }.getOrThrow())
    }

    @Test
    fun cancelled_old_stream_read_remains_cancelled_instead_of_becoming_failure_ui() = runBlocking {
        val cancelled = CancellationException("stream changed")
        val result = runCatching { readCameraCounterSafely<Int> { throw cancelled } }
        assertSame(cancelled, result.exceptionOrNull())
    }
}
