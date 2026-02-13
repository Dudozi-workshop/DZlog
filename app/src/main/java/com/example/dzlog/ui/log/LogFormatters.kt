package com.example.dzlog.ui.log

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val DATE_FMT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
    timeZone = TimeZone.getDefault()
}

fun dzFormatDate(dateAddedSeconds: Long): String {
    if (dateAddedSeconds <= 0L) return "-"
    return DATE_FMT.format(Date(dateAddedSeconds * 1_000L))
}
