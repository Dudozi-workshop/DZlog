package com.dudoziworkshop.dzlog.domain.gallery

/**
 * Presentation-only counter badges. Never changes filenames or counter state.
 *
 * Capture-session photos should supply the counter saved at capture time via
 * [labelForVerifiedCounter]. Historical MediaStore images lack that receipt,
 * so a deliberately conservative DZlog filename fallback is used.
 */
object GalleryCounterBadgePolicy {
    fun labelForVerifiedCounter(counter: Int?, enabled: Boolean): String? {
        if (!enabled) return null
        return counter?.takeIf { it > 0 }?.toString()
    }

    private val suffixPattern = Regex("^(.+)_([0-9]{1,6})\\.(jpg|jpeg)$", RegexOption.IGNORE_CASE)
    private val bareDatePrefix = Regex(".*(?:^|_)(?:19|20)[0-9]{2}_[01]?[0-9]$")
    private val fullDatePrefix = Regex(".*(?:^|_)(?:19|20)[0-9]{2}_[01]?[0-9]_[0-3]?[0-9]$")

    /**
     * Fallback for historical DZlog JPGs only.
     * No badge is shown for an unrecognizable or likely date-derived suffix.
     * Even a convention match is not proof for a manually renamed photo.
     */
    fun labelFromDzlogFilename(displayName: String, relativePath: String, enabled: Boolean): String? {
        if (!enabled) return null
        val normalizedPath = relativePath.replace('\\', '/')
        if (!normalizedPath.startsWith("Pictures/DZlog/", ignoreCase = true)) return null

        val match = suffixPattern.matchEntire(displayName.trim()) ?: return null
        val prefix = match.groupValues[1]
        val rawNumber = match.groupValues[2]
        if (prefix.isBlank() || prefix.all { it.isDigit() }) return null

        // 202610 may mean YYYYMM; Sample_2026_10_16.jpg may mean a date.
        if (rawNumber.length == 6) {
            val year = rawNumber.take(4).toIntOrNull()
            val month = rawNumber.takeLast(2).toIntOrNull()
            if (year != null && year in 1900..2099 && month != null && month in 1..12) return null
        }
        if (fullDatePrefix.matches(prefix) || bareDatePrefix.matches(prefix)) return null

        return labelForVerifiedCounter(rawNumber.toIntOrNull(), enabled = true)
    }
}
