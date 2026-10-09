package com.dudoziworkshop.dzlog.data.repository

import android.net.Uri
import com.dudoziworkshop.dzlog.data.mediastore.SavedMedia
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptureMediaSaveCoordinatorTest {
    private val watermark = SavedMedia(Uri.parse("content://media/external/images/media/11"), 11, "A_0001.jpg", false)
    private val original = SavedMedia(Uri.parse("content://media/external/images/media/12"), 12, "A_0001.jpg", false)

    @Test
    fun success_returns_exact_required_files_and_representative_for_each_mode() {
        for (mode in SaveMode.entries) {
            val calls = mutableListOf<String>()
            val result = CaptureMediaSaveCoordinator.save(mode,
                { calls += "watermark"; watermark }, { calls += "original"; original },
                { fail("Successful save must not delete media"); false })
            val expected = when (mode) {
                SaveMode.WATERMARK_ONLY -> listOf(watermark)
                SaveMode.ORIGINAL_ONLY -> listOf(original)
                SaveMode.BOTH -> listOf(watermark, original)
            }
            assertEquals(expected, result.files)
            assertEquals(expected.first(), result.primary)
            assertEquals(expected.map { if (it == watermark) "watermark" else "original" }, calls)
        }
    }

    @Test
    fun first_write_failure_in_any_mode_never_returns_success_or_attempts_later_write() {
        for (mode in SaveMode.entries) {
            val failure = IllegalStateException("storage unavailable")
            var writes = 0
            val result = runCatching { CaptureMediaSaveCoordinator.save(mode,
                { writes++; throw failure }, { writes++; throw failure },
                { fail("No created media to delete"); false }) }
            assertSame(failure, result.exceptionOrNull())
            assertEquals(1, writes)
        }
    }

    @Test
    fun second_write_failure_rolls_back_watermark_and_does_not_return_partial_success() {
        val failure = IllegalStateException("original write failed")
        val deleted = mutableListOf<Uri>()
        val result = runCatching { CaptureMediaSaveCoordinator.save(SaveMode.BOTH,
            { watermark }, { throw failure }, { deleted += it; true }) }
        assertSame(failure, result.exceptionOrNull())
        assertEquals(listOf(watermark.uri), deleted)
        assertTrue(failure.suppressed.isEmpty())
    }

    @Test
    fun cleanup_failure_is_visible_and_does_not_hide_original_save_failure() {
        for (throws in listOf(false, true)) {
            val failure = IllegalStateException("original write failed")
            val result = runCatching { CaptureMediaSaveCoordinator.save(SaveMode.BOTH,
                { watermark }, { throw failure }, {
                    if (throws) throw SecurityException("delete denied") else false
                }) }
            assertSame(failure, result.exceptionOrNull())
            assertEquals(1, failure.suppressed.size)
        }
    }

    @Test
    fun invalid_saved_media_is_rejected_and_created_uris_are_cleaned_in_reverse_order() {
        val invalid = original.copy(mediaStoreId = -1)
        val deleted = mutableListOf<Uri>()
        val result = runCatching { CaptureMediaSaveCoordinator.save(SaveMode.BOTH,
            { watermark }, { invalid }, { deleted += it; true }) }
        assertTrue(result.isFailure)
        assertEquals(listOf(original.uri, watermark.uri), deleted)
    }

    @Test
    fun empty_uri_is_never_accepted_as_saved_capture() {
        val result = runCatching { CaptureMediaSaveCoordinator.save(SaveMode.ORIGINAL_ONLY,
            { fail("Watermark is not requested"); watermark },
            { original.copy(uri = Uri.EMPTY) }, { fail("Empty URI cannot be deleted"); false }) }
        assertTrue(result.isFailure)
    }
}
