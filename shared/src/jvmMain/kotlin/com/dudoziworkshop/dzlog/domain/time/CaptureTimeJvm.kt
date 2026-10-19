package com.dudoziworkshop.dzlog.domain.time

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun Date.asCaptureTime(
    locale: Locale = Locale.getDefault(),
    timeZone: TimeZone = TimeZone.getDefault(),
): CaptureTime = CaptureTime(time, locale.toLanguageTag(), timeZone.id)

actual fun formatCaptureTime(pattern: String, time: CaptureTime): String =
    SimpleDateFormat(pattern, Locale.forLanguageTag(time.localeTag)).apply {
        timeZone = TimeZone.getTimeZone(time.timeZoneId)
    }.format(Date(time.epochMilliseconds))

internal actual fun formatMinuteToken(hour: Int, minute: Int): String = "%02d%02d".format(hour, minute)
