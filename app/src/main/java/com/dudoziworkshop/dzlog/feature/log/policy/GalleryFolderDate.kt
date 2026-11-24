package com.dudoziworkshop.dzlog.feature.log.policy

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun GalleryFolderSummaryPolicy.compactDate(
    epochMillis: Long?,
    referenceMillis: Long,
    timeZone: TimeZone,
): String {
    if (epochMillis == null || epochMillis <= 0L) return "—"
    val taken = Calendar.getInstance(timeZone).apply { timeInMillis = epochMillis }
    val reference = Calendar.getInstance(timeZone).apply { timeInMillis = referenceMillis }
    val pattern = if (taken.get(Calendar.YEAR) == reference.get(Calendar.YEAR)) "MM.dd" else "yy.MM.dd"
    return SimpleDateFormat(pattern, Locale.KOREA).apply { this.timeZone = timeZone }
        .format(Date(epochMillis))
}
