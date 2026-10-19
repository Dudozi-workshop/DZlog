package com.dudoziworkshop.dzlog.domain.naming

/**
 * File-name sanitizer for generic (non date/time) parts.
 *
 * Policy:
 * - Replace illegal characters with '_'.
 * - Trim leading/trailing dots.
 */
fun sanitizeFilePart(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return ""

    val illegal = Regex("[\\\\/:*?\"<>|]")
    val cleaned = trimmed.replace(illegal, "_")
    return cleaned.trim().trim('.')
}
