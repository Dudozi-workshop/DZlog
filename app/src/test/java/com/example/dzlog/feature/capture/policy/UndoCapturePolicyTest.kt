package com.example.dzlog.feature.capture.policy

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoCapturePolicyTest {

    @Test
    fun push_capture_stores_one_batch_per_capture() {
        val stack = mutableListOf<List<Uri>>()

        UndoCapturePolicy.pushCapture(
            stack = stack,
            captureUris = listOf(Uri.parse("content://test/1"), Uri.parse("content://test/2"))
        )

        assertEquals(1, stack.size)
        assertEquals(
            listOf(Uri.parse("content://test/1"), Uri.parse("content://test/2")),
            stack.first()
        )
    }

    @Test
    fun consume_latest_capture_returns_last_batch_and_removes_it() {
        val stack = mutableListOf(
            listOf(Uri.parse("content://test/1")),
            listOf(Uri.parse("content://test/2"), Uri.parse("content://test/3"))
        )

        val consumed = UndoCapturePolicy.consumeLatestCapture(stack)

        assertEquals(listOf(Uri.parse("content://test/2"), Uri.parse("content://test/3")), consumed)
        assertEquals(listOf(listOf(Uri.parse("content://test/1"))), stack)
    }

    @Test
    fun restore_capture_appends_batch_back() {
        val stack = mutableListOf(listOf(Uri.parse("content://test/1")))

        UndoCapturePolicy.restoreCapture(
            stack = stack,
            captureUris = listOf(Uri.parse("content://test/2"), Uri.parse("content://test/3"))
        )

        assertEquals(
            listOf(
                listOf(Uri.parse("content://test/1")),
                listOf(Uri.parse("content://test/2"), Uri.parse("content://test/3"))
            ),
            stack
        )
    }

    @Test
    fun push_capture_ignores_empty_or_uri_empty_values() {
        val stack = mutableListOf<List<Uri>>()

        UndoCapturePolicy.pushCapture(stack, listOf(Uri.EMPTY, Uri.EMPTY))

        assertTrue(stack.isEmpty())
    }
}
