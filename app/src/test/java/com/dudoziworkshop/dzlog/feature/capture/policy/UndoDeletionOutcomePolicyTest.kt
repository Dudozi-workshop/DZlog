package com.dudoziworkshop.dzlog.feature.capture.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoDeletionOutcomePolicyTest {
    @Test
    fun all_files_deleted_leaves_no_restore_targets() {
        val outcome = classifyUndoDeletion(
            listOf("original" to Result.success(true), "composite" to Result.success(true))
        )
        assertEquals(listOf("original", "composite"), outcome.deleted)
        assertTrue(outcome.remaining.isEmpty())
    }

    @Test
    fun partial_delete_restores_only_the_failed_file() {
        val outcome = classifyUndoDeletion(
            listOf("original" to Result.success(true), "composite" to Result.success(false))
        )
        assertEquals(listOf("original"), outcome.deleted)
        assertEquals(listOf("composite"), outcome.remaining)
    }

    @Test
    fun failed_delete_exception_does_not_count_as_success() {
        val outcome = classifyUndoDeletion(
            listOf(
                "original" to Result.failure<Boolean>(SecurityException("no permission")),
                "composite" to Result.success(true),
            )
        )
        assertEquals(listOf("composite"), outcome.deleted)
        assertEquals(listOf("original"), outcome.remaining)
    }

    @Test
    fun no_delete_attempts_has_no_side_effects() {
        val outcome = classifyUndoDeletion(emptyList<Pair<String, Result<Boolean>>>())
        assertTrue(outcome.deleted.isEmpty())
        assertTrue(outcome.remaining.isEmpty())
    }
}
