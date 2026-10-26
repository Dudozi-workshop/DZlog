package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.counter.policy.CounterScopeSnapshot
import com.dudoziworkshop.dzlog.domain.counter.policy.isNewCounterScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterScopeKeyTest {

    @Test
    fun mode_toggle_only_does_not_create_new_scope_when_values_same() {
        val previous = CounterScopeSnapshot(
            relativePathKey = "Pictures/DZlog/A/B/",
            prefix = "C",
            scopeKey = "Pictures/DZlog/A/B/|C",
            includePathInScope = true,
            includeFilenameInScope = true,
        )
        val current = previous.copy(
            scopeKey = "Pictures/DZlog/A/B/|C",
            includePathInScope = true,
            includeFilenameInScope = false,
        )

        assertFalse(isNewCounterScope(previous, current))
    }

    @Test
    fun filename_change_affects_only_filename_on_mode() {
        val previous = CounterScopeSnapshot(
            relativePathKey = "Pictures/DZlog/A/B/",
            prefix = "C",
            scopeKey = "Pictures/DZlog/A/B/|*",
            includePathInScope = true,
            includeFilenameInScope = false,
        )

        val current = previous.copy(
            prefix = "D",
            scopeKey = "Pictures/DZlog/A/B/|*",
        )

        assertFalse(isNewCounterScope(previous, current))
        assertTrue(
            isNewCounterScope(
                previous,
                current.copy(
                    includeFilenameInScope = true,
                    scopeKey = "Pictures/DZlog/A/B/|D",
                )
            )
        )
    }

    @Test
    fun path_change_affects_only_path_on_mode() {
        val previous = CounterScopeSnapshot(
            relativePathKey = "Pictures/DZlog/A/B/",
            prefix = "C",
            scopeKey = "*|C",
            includePathInScope = false,
            includeFilenameInScope = true,
        )

        val current = previous.copy(
            relativePathKey = "Pictures/DZlog/A/C/",
            scopeKey = "*|C",
        )

        assertFalse(isNewCounterScope(previous, current))
        assertTrue(
            isNewCounterScope(
                previous,
                current.copy(
                    includePathInScope = true,
                    scopeKey = "Pictures/DZlog/A/C/|C",
                )
            )
        )
    }

    @Test
    fun both_off_never_treats_value_changes_as_new_stream() {
        val previous = CounterScopeSnapshot(
            relativePathKey = "Pictures/DZlog/A/B/",
            prefix = "C",
            scopeKey = "*|*",
            includePathInScope = false,
            includeFilenameInScope = false,
        )

        val current = previous.copy(
            relativePathKey = "Pictures/DZlog/X/Y/",
            prefix = "Z",
            scopeKey = "*|*",
        )

        assertFalse(isNewCounterScope(previous, current))
    }

    @Test
    fun path_scope_on_splits_stream_when_path_changes() {
        val a = buildCounterScopeParts(
            relativePath = "Pictures/DZlog/A/B/",
            prefix = "P",
            includePathInScope = true,
            includeFilenameInScope = false,
        )
        val b = buildCounterScopeParts(
            relativePath = "Pictures/DZlog/C/D/",
            prefix = "P",
            includePathInScope = true,
            includeFilenameInScope = false,
        )

        assertTrue(a.scopeKey != b.scopeKey)
    }

    @Test
    fun path_scope_off_keeps_stream_when_path_changes() {
        val a = buildCounterScopeParts(
            relativePath = "Pictures/DZlog/A/B/",
            prefix = "P",
            includePathInScope = false,
            includeFilenameInScope = false,
        )
        val b = buildCounterScopeParts(
            relativePath = "Pictures/DZlog/C/D/",
            prefix = "P",
            includePathInScope = false,
            includeFilenameInScope = false,
        )

        assertEquals(a.scopeKey, b.scopeKey)
    }

}
