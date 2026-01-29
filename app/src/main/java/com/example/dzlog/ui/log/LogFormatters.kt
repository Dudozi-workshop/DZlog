package com.example.dzlog.ui.log

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

fun dzFormatDate(dateAddedSeconds: Long): String {
    if (dateAddedSeconds <= 0L) return "-"
    return Instant.ofEpochSecond(dateAddedSeconds)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DATE_FMT)
}
