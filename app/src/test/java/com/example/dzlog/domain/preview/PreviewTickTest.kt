package com.example.dzlog.domain.preview

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewTickTest {

    @Test
    fun seconds_in_time_format_returns_second() {
        val result = decideTickUnit(dateFormat = "yyyy.MM.dd", timeFormat = "HH:mm:ss")
        assertEquals(TickUnit.SECOND, result)
    }

    @Test
    fun minutes_in_time_format_returns_minute() {
        val result = decideTickUnit(dateFormat = "yyyy.MM.dd", timeFormat = "HH:mm")
        assertEquals(TickUnit.MINUTE, result)
    }

    @Test
    fun no_time_tokens_returns_day() {
        val result = decideTickUnit(dateFormat = "yyyy.MM.dd", timeFormat = "")
        assertEquals(TickUnit.DAY, result)
    }

    @Test
    fun month_token_must_not_be_treated_as_minute() {
        val result = decideTickUnit(dateFormat = "yyyy.MM.dd", timeFormat = "")
        assertEquals(TickUnit.DAY, result)
    }

    @Test
    fun date_format_with_hour_token_falls_back_to_hour() {
        val result = decideTickUnit(dateFormat = "yyyy.MM.dd HH", timeFormat = "")
        assertEquals(TickUnit.HOUR, result)
    }

    @Test
    fun compute_next_delay_uses_next_boundary() {
        val result = computeNextDelayMillis(unit = TickUnit.MINUTE, nowMillis = 65_000L)
        assertEquals(55_000L, result)
    }
}
