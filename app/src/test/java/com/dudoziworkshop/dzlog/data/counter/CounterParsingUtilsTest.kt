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
            fnDelim = "_"
        )

        assertEquals(12, parsed)
    }

    @Test
    fun parse_with_wildcard_prefix_reads_last_numeric_token() {
        val parsed = parseCounterForPolicy(
            displayName = "ANY_PREFIX_27.jpg",
            fileNamePrefix = "*",
            fnDelim = "_"
        )

        assertEquals(27, parsed)
    }

    @Test
    fun parse_is_padding_agnostic_when_digits_config_changes() {
        for (suffix in listOf("12", "012", "0012", "00012", "000012")) {
            assertEquals(12, parseCounterForPolicy("ABC_DEF_${suffix}.jpg", "ABC_DEF", "_"))
        }
        assertEquals(12345, parseCounterForPolicy("ABC_DEF_12345.jpg", "ABC_DEF", "_"))
        assertNull(parseCounterForPolicy("UNRELATED_000012.jpg", "ABC_DEF", "_"))
    }

    @Test
    fun parse_fails_for_non_numeric_suffix() {
        val parsed = parseCounterForPolicy(
            displayName = "ANY_PREFIX_XX.jpg",
            fileNamePrefix = "*",
            fnDelim = "_"
        )

        assertNull(parsed)
    }
}
