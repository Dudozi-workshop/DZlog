package com.dudoziworkshop.dzlog.domain.time

import platform.Foundation.*

actual fun formatCaptureTime(pattern: String, time: CaptureTime): String {
    val formatter = NSDateFormatter()
    formatter.locale = NSLocale(localeIdentifier = time.localeTag)
    formatter.timeZone = requireNotNull(NSTimeZone.timeZoneWithName(time.timeZoneId)) {
        "Unknown time zone: ${time.timeZoneId}"
    }
    formatter.dateFormat = pattern
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(time.epochMilliseconds / 1000.0))
}
