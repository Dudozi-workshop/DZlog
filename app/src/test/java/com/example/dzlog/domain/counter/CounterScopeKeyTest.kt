package com.example.dzlog.domain.counter

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
}
