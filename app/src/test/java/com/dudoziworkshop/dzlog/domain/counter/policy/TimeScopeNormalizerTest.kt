package com.dudoziworkshop.dzlog.domain.counter.policy

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeScopeNormalizerTest {

    @Test
    fun `returns HH:mm when seconds are present`() {
        assertEquals("11:22", normalizeTimeToMinute("11:22:33"))
    }

    @Test
    fun `trims whitespace around tokens`() {
        assertEquals("11:22", normalizeTimeToMinute(" 11 : 22 : 33 "))
    }

    @Test
    fun `returns original when colon format is not available`() {
        assertEquals("1122", normalizeTimeToMinute("1122"))
    }

    @Test
    fun `returns empty when blank`() {
        assertEquals("", normalizeTimeToMinute("   "))
    }
}
