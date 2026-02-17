package com.example.dzlog.feature.capture.policy

import android.net.Uri
import com.example.dzlog.domain.model.SaveMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoCapturePolicyTest {

    @Test
    fun both_mode_consumes_two_items_in_single_undo() {
        val stack = mutableListOf(
            Uri.parse("content://test/1"),
            Uri.parse("content://test/2"),
            Uri.parse("content://test/3"),
            Uri.parse("content://test/4")
        )

        val targets = UndoCapturePolicy.consumeUndoTargets(stack, SaveMode.BOTH)

        assertEquals(listOf(Uri.parse("content://test/3"), Uri.parse("content://test/4")), targets)
        assertEquals(listOf(Uri.parse("content://test/1"), Uri.parse("content://test/2")), stack)
    }

    @Test
    fun both_mode_with_single_item_consumes_one_item_only() {
        val stack = mutableListOf(Uri.parse("content://test/1"))

        val targets = UndoCapturePolicy.consumeUndoTargets(stack, SaveMode.BOTH)

        assertEquals(listOf(Uri.parse("content://test/1")), targets)
        assertTrue(stack.isEmpty())
    }

    @Test
    fun restore_appends_targets_back_to_stack() {
        val stack = mutableListOf(Uri.parse("content://test/1"))
        val targets = listOf(Uri.parse("content://test/2"), Uri.parse("content://test/3"))

        UndoCapturePolicy.restoreUndoTargets(stack, targets)

        assertEquals(
            listOf(
                Uri.parse("content://test/1"),
                Uri.parse("content://test/2"),
                Uri.parse("content://test/3")
            ),
            stack
        )
    }
}
