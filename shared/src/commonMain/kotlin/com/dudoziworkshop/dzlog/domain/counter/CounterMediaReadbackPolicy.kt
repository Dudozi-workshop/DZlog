package com.dudoziworkshop.dzlog.domain.counter

/** Minimal platform-neutral view of a saved image used for counter readback. */
data class CounterMediaRecord(
    val relativePath: String,
    val displayName: String,
)

/**
 * Resolves the automatic counter from actual saved filenames.
 *
 * Platform adapters remain responsible for excluding deleted or inaccessible
 * media. This policy only handles the shared path/prefix/number contract.
 */
object CounterMediaReadbackPolicy {
    fun resolveStableNext(
        records: List<CounterMediaRecord>,
        relativePathKey: String,
        scanPrefix: String,
        fnDelim: String,
        currentNext: Int,
    ): Int {
        val used = usedCounters(records, relativePathKey, scanPrefix, fnDelim)
        val mediaNext = used.maxOrNull()?.let { maximum ->
            check(maximum < Int.MAX_VALUE) { "Capture counter is exhausted." }
            maximum + 1
        } ?: 1
        // Re-entry and delayed readback must never roll the displayed stream backwards.
        return maxOf(currentNext.coerceAtLeast(1), mediaNext)
    }

    fun usedCounters(
        records: List<CounterMediaRecord>,
        relativePathKey: String,
        scanPrefix: String,
        fnDelim: String,
    ): Set<Int> {
        val path = relativePathKey.substringBefore("|g2=", relativePathKey)
        val wildcardPath = path.isBlank() || path == "*"
        val normalizedPath = normalizePath(path)
        return records.asSequence()
            .filter { wildcardPath || normalizePath(it.relativePath) == normalizedPath }
            .mapNotNull { parseCounter(it.displayName, scanPrefix, fnDelim) }
            .toSet()
    }

    /** Matches Android's MediaStore parser and intentionally ignores padding changes. */
    fun parseCounter(displayName: String, scanPrefix: String, fnDelim: String): Int? {
        val base = displayName.substringBeforeLast('.', displayName).trim()
        if (base.isBlank()) return null

        val delimiter = fnDelim.ifBlank { "_" }
        val wildcardPrefix = scanPrefix == "*" || scanPrefix.isBlank()
        val token = when {
            CounterScanPrefixes.isEncoded(scanPrefix) -> {
                // Generated automatic counters always use the final underscore suffix.
                val prefix = base.substringBeforeLast('_', missingDelimiterValue = "")
                if (!CounterScanPrefixes.matches(prefix, scanPrefix, delimiter)) return null
                base.substringAfterLast('_', missingDelimiterValue = "").trim()
            }
            wildcardPrefix -> base.substringAfterLast(delimiter, missingDelimiterValue = base).trim()
            else -> {
                val prefixToken = "$scanPrefix$delimiter"
                if (!base.startsWith(prefixToken)) return null
                base.removePrefix(prefixToken).trim()
            }
        }
        if (token.isBlank() || token.any { !it.isDigit() }) return null
        return token.toIntOrNull()?.takeIf { it >= 0 }
    }

    private fun normalizePath(value: String): String {
        val collapsed = value.trim().replace(Regex("/+"), "/")
        return if (collapsed.isBlank() || collapsed.endsWith('/')) collapsed else "$collapsed/"
    }
}
