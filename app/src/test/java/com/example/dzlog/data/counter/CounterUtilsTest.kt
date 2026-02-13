package com.example.dzlog.data.counter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterUtilsTest {

    @Test
    fun parse_with_exact_prefix_keeps_existing_behavior() {
        val parsed = parseCounterFromDisplayNameForPolicy(
            displayName = "ABC_DEF_0012.jpg",
            fileNamePrefix = "ABC_DEF",
            counterDigits = 4,
            fnDelim = "_"
        )

        assertEquals(12, parsed)
    }

    @Test
    fun parse_with_wildcard_prefix_reads_last_numeric_token() {
        val parsed = parseCounterFromDisplayNameForPolicy(
            displayName = "ANY_PREFIX_27.jpg",
            fileNamePrefix = "*",
            counterDigits = 0,
            fnDelim = "_"
        )

        assertEquals(27, parsed)
    }

    @Test
    fun parse_with_wildcard_prefix_respects_digits_when_configured() {
        val parsed = parseCounterFromDisplayNameForPolicy(
            displayName = "ANY_PREFIX_27.jpg",
            fileNamePrefix = "*",
            counterDigits = 4,
            fnDelim = "_"
        )

        assertNull(parsed)
    }
}
