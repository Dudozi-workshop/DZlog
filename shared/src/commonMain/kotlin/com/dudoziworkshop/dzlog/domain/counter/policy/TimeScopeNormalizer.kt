package com.dudoziworkshop.dzlog.domain.counter.policy

import com.dudoziworkshop.dzlog.domain.time.formatMinuteToken

fun normalizeTimeToMinute(text: String): String {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return ""

    // 주요 정책: counter scope 시간 토큰은 항상 HHmm으로 정규화한다.
    val colonMatch = Regex("""^(\d{1,2})\s*:\s*(\d{2})""").find(trimmed)
    if (colonMatch != null) {
        val hour = colonMatch.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: return ""
        val minute = colonMatch.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: return ""
        return formatMinuteToken(hour, minute)
    }

    val digitMatch = Regex("""^(\d{2})(\d{2})""").find(trimmed)
    if (digitMatch != null) {
        val hour = digitMatch.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: return ""
        val minute = digitMatch.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: return ""
        return formatMinuteToken(hour, minute)
    }

    return ""
}
