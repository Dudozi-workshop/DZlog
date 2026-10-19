package com.dudoziworkshop.dzlog.domain.time

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class JvmDateCompatibilityTest {
    @Test
    fun capturedContextMatchesExistingJavaFormattingAcrossLocalesZonesAndDatePatterns() {
        for (tag in listOf("ko-KR", "en-US", "ar-EG", "th-TH")) {
            for (zone in listOf("UTC", "Asia/Seoul", "America/Los_Angeles")) {
                for (millis in listOf(0L, 1791936300000L, 1798729500000L, 1772964000000L)) {
                    for (pattern in listOf("yyyyMMdd", "yyMMdd", "MMdd", "yyyy-MM-dd", "HHmm", "HH:mm:ss")) {
                        val locale = Locale.forLanguageTag(tag)
                        val timezone = TimeZone.getTimeZone(zone)
                        val date = Date(millis)
                        val expected = SimpleDateFormat(pattern, locale).apply { timeZone = timezone }.format(date)
                        assertEquals(expected, formatCaptureTime(pattern, date.asCaptureTime(locale, timezone)),
                            "$tag/$zone/$millis/$pattern")
                    }
                }
            }
        }
    }
}
