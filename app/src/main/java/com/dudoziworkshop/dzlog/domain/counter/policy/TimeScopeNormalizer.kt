package com.dudoziworkshop.dzlog.domain.counter.policy

fun normalizeTimeToMinute(text: String): String {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return ""

    val parts = trimmed.split(":")
    return if (parts.size >= 2) {
        "${parts[0].trim()}:${parts[1].trim()}"
    } else {
        trimmed
    }
}
