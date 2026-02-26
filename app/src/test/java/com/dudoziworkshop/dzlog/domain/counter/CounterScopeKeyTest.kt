package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.counter.policy.CounterScopeSnapshot
import com.dudoziworkshop.dzlog.domain.counter.policy.isNewCounterScope
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CounterScopeKeyTest {

    @Test
    fun mode_toggle_only_does_not_create_new_scope_when_values_same() {
        val previous = CounterScopeSnapshot(
            relativePathKey = "Pictures/DZlog/A/B/",
            prefix = "C|g2=0",
            includePathInScope = true,
            includeFilenameInScope = true,
        )
        val current = previous.copy(
            includePathInScope = true,
            includeFilenameInScope = false,
        )

        assertFalse(isNewCounterScope(previous, current))
    }

    @Test
    fun filename_change_affects_only_filename_on_mode() {
        val previous = CounterScopeSnapshot(
            relativePathKey = "Pictures/DZlog/A/B/",
            prefix = "C|g2=0",
            includePathInScope = true,
            includeFilenameInScope = false,
        )

        val current = previous.copy(prefix = "D|g2=0")

        assertFalse(isNewCounterScope(previous, current))
        assertTrue(isNewCounterScope(previous, current.copy(includeFilenameInScope = true)))
    }

    @Test
    fun path_change_affects_only_path_on_mode() {
        val previous = CounterScopeSnapshot(
            relativePathKey = "Pictures/DZlog/A/B/",
            prefix = "C|g2=0",
            includePathInScope = false,
            includeFilenameInScope = true,
        )

        val current = previous.copy(relativePathKey = "Pictures/DZlog/A/C/")

        assertFalse(isNewCounterScope(previous, current))
        assertTrue(isNewCounterScope(previous, current.copy(includePathInScope = true)))
    }

    @Test
    fun both_off_never_treats_value_changes_as_new_stream() {
        val previous = CounterScopeSnapshot(
            relativePathKey = "Pictures/DZlog/A/B/",
            prefix = "C|g2=0",
            includePathInScope = false,
            includeFilenameInScope = false,
        )

        val current = previous.copy(
            relativePathKey = "Pictures/DZlog/X/Y/",
            prefix = "Z|g2=1"
        )

        assertFalse(isNewCounterScope(previous, current))
    }
}
