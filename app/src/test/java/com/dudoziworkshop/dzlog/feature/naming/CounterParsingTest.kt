package com.dudoziworkshop.dzlog.feature.naming

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterParsingTest {

    @Test
    fun parse_next_counter_from_latest_display_name_success() {
        val result = parseNextCounterFromDisplayName(
            latestDisplayName = "AAA_BBB_0007.jpg",
            fileNamePrefix = "AAA_BBB",
            fnDelim = "_"
        )

        assertEquals(7, result.latestCounter)
        assertEquals(8, result.nextCounter)
    }

    @Test
    fun parse_failure_falls_back_to_one() {
        val result = parseNextCounterFromDisplayName(
            latestDisplayName = "invalid_name.jpg",
            fileNamePrefix = "AAA_BBB",
            fnDelim = "_"
        )

        assertNull(result.latestCounter)
        assertEquals(1, result.nextCounter)
    }

    @Test
    fun null_latest_display_name_falls_back_to_one() {
        val result = parseNextCounterFromDisplayName(
            latestDisplayName = null,
            fileNamePrefix = "*",
            fnDelim = "_"
        )

        assertNull(result.latestCounter)
        assertEquals(1, result.nextCounter)
    }
}
