package com.dudoziworkshop.dzlog.data.counter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterParsingUtilsTest {

    @Test
    fun parse_with_exact_prefix_keeps_existing_behavior() {
        val parsed = parseCounterForPolicy(
            displayName = "ABC_DEF_0012.jpg",
            fileNamePrefix = "ABC_DEF",
            counterDigits = 4,
            fnDelim = "_"
        )

        assertEquals(12, parsed)
    }

    @Test
    fun parse_with_wildcard_prefix_reads_last_numeric_token() {
        val parsed = parseCounterForPolicy(
            displayName = "ANY_PREFIX_27.jpg",
            fileNamePrefix = "*",
            counterDigits = 0,
            fnDelim = "_"
        )

        assertEquals(27, parsed)
    }

    @Test
    fun parse_is_padding_agnostic_when_digits_config_changes() {
        val parsed = parseCounterForPolicy(
            displayName = "ANY_PREFIX_27.jpg",
            fileNamePrefix = "*",
            counterDigits = 4,
            fnDelim = "_"
        )

        assertEquals(27, parsed)
    }

    @Test
    fun parse_fails_for_non_numeric_suffix() {
        val parsed = parseCounterForPolicy(
            displayName = "ANY_PREFIX_XX.jpg",
            fileNamePrefix = "*",
            counterDigits = 4,
            fnDelim = "_"
        )

        assertNull(parsed)
    }
}
