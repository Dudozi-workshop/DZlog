package com.dudoziworkshop.dzlog.domain.time

/** A captured instant with explicit display context; independent of Java Date and system clocks. */
data class CaptureTime(
    val epochMilliseconds: Long,
    val localeTag: String,
    val timeZoneId: String,
)

expect fun formatCaptureTime(pattern: String, time: CaptureTime): String
