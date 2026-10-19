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
    @Test
    fun cellPoliciesPreserveLocalizedDisplayAndAsciiScopes() {
        for (tag in listOf("ar-EG", "th-TH", "ko-KR", "en-US")) {
            val locale = Locale.forLanguageTag(tag)
            val zone = TimeZone.getTimeZone("Asia/Seoul")
            val date = Date(1792422299000L)
            val capture = date.asCaptureTime(locale, zone)
            fun legacy(pattern: String, selectedLocale: Locale) = SimpleDateFormat(pattern, selectedLocale).apply { timeZone = zone }.format(date)
            assertEquals(CaptureCellTime(legacy("yyyyMMdd", locale), legacy("yyyyMMdd", Locale.US)), resolveCaptureDate(capture, "invalid"))
            assertEquals(CaptureCellTime(legacy("HHmm", locale), legacy("HHmm", Locale.US)), resolveCaptureMinute(capture))
        }
    }
}
