package com.example.dzlog.ui.log

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun dzFormatDate(dateAddedSeconds: Long): String {
    if (dateAddedSeconds <= 0L) return "-"
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }
    return dateFormat.format(Date(dateAddedSeconds * 1_000L))
}
