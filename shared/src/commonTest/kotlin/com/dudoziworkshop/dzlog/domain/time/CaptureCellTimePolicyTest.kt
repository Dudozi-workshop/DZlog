package com.dudoziworkshop.dzlog.domain.time

import kotlin.test.Test
import kotlin.test.assertEquals

class CaptureCellTimePolicyTest {
    private val captured = CaptureTime(1792422299000L, "en-US", "Asia/Seoul")

    @Test
    fun datePatternsAndFallbackUseTheCaptureZone() {
        for (locale in listOf("en-US", "ko-KR")) {
            val time = captured.copy(localeTag = locale)
            for ((pattern, expected) in listOf("yyyyMMdd" to "20261020", "yyMMdd" to "261020", "MMdd" to "1020", "yyyy-MM-dd" to "20261020", "" to "20261020")) {
                assertEquals(CaptureCellTime(expected, "20261020"), resolveCaptureDate(time, pattern), "$locale/$pattern")
            }
            assertEquals(CaptureCellTime("0004", "0004"), resolveCaptureMinute(time))
            assertEquals(CaptureCellTime("20261019", "20261019"), resolveCaptureDate(time.copy(timeZoneId = "UTC"), "yyyyMMdd"))
        }
    }

    @Test
    fun minutesIgnoreSecondsAndRespectDaylightSavingTransition() {
        val before = CaptureTime(1772963999000L, "en-US", "America/Los_Angeles")
        assertEquals(CaptureCellTime("0159", "0159"), resolveCaptureMinute(before))
        assertEquals(CaptureCellTime("0300", "0300"), resolveCaptureMinute(before.copy(epochMilliseconds = 1772964000000L)))
        assertEquals(resolveCaptureMinute(captured), resolveCaptureMinute(captured.copy(epochMilliseconds = captured.epochMilliseconds - 59000L)))
    }
}
