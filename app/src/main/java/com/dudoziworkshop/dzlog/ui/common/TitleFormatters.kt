package com.dudoziworkshop.dzlog.ui.common

private fun ellipsizePrefix(text: String, maxChars: Int): String {
    val t = text.trim()
    if (t.length <= maxChars) return t
    if (maxChars <= 0) return "…"
    return "${t.take(maxChars)}…"
}

/**
 * Compact a folder breadcrumb without assuming a fixed folder depth.
 * Prefer the complete path, then retain as many trailing folders as fit.
 * Earlier folders are indicated by an ellipsis rather than silently dropped.
 */
fun buildFolderBreadcrumbTitle(pathTitle: String, totalBudget: Int): String {
    val segments = pathTitle.split('/').map { it.trim() }.filter { it.isNotEmpty() }
    if (segments.isEmpty()) return ""
    val budget = totalBudget.coerceAtLeast(1)
    val spaced = segments.joinToString(" / ")
    if (spaced.length <= budget) return spaced

    val compact = segments.joinToString("/")
    if (compact.length <= budget) return compact

    for (count in (segments.size - 1) downTo 1) {
        val suffix = "…/" + segments.takeLast(count).joinToString("/")
        if (suffix.length <= budget) return suffix
    }

    return ellipsizePrefix(segments.last(), budget - 1)
}


private val TRAILING_DIGITS_REGEX = Regex("(\\d+)$")

private fun stripExtension(fileName: String): String {
    val t = fileName.trim()
    val idx = t.lastIndexOf('.')
    return if (idx > 0) t.take(idx) else t
}

private fun charUnit(ch: Char): Double {
    val code = ch.code
    val isHangul = code in 0xAC00..0xD7A3
    val isDigit = ch in '0'..'9'
    val isAsciiLetter = (ch in 'a'..'z') || (ch in 'A'..'Z')

    return when {
        isHangul -> 1.0
        isDigit -> 0.6
        isAsciiLetter -> 0.4
        else -> 1.0
    }
}

private fun weightedLenUnits(s: String): Double {
    var units = 0.0
    for (ch in s) units += charUnit(ch)
    return units
}

private fun ellipsizeByUnitsPrefix(s: String, unitLimit: Double): String {
    val t = s.trim()
    if (t.isEmpty()) return ""
    if (weightedLenUnits(t) <= unitLimit) return t

    var acc = 0.0
    val sb = StringBuilder()
    for (ch in t) {
        val add = charUnit(ch)
        if (acc + add > unitLimit) break
        sb.append(ch)
        acc += add
    }
    if (sb.isEmpty()) return "…"
    return "${sb}…"
}

data class FileNameDisplayParts(
    val prefixText: String,
    val counter: String?,
)

fun splitFileNameForDisplay(fileName: String): FileNameDisplayParts {
    val base = stripExtension(fileName)
    val counter = TRAILING_DIGITS_REGEX.find(base)?.groupValues?.getOrNull(1)
    val prefixName = if (counter != null) base.removeSuffix(counter).trimEnd('_') else base

    val displaySlots = prefixName
        .split("_")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(3)

    val baseLimitUnits = when (displaySlots.size) {
        0 -> 0.0
        1 -> 20.0
        2 -> 9.0
        else -> 5.0
    }

    val needs = displaySlots.map { weightedLenUnits(it) }
    val limits = MutableList(displaySlots.size) { baseLimitUnits }

    var unused = 0.0
    val overflow = mutableListOf<Int>()
    needs.forEachIndexed { idx, need ->
        if (need < baseLimitUnits) {
            unused += (baseLimitUnits - need)
            limits[idx] = need
        } else if (need > baseLimitUnits) {
            overflow += idx
        }
    }

    var rr = 0
    while (unused >= 1.0 && overflow.isNotEmpty()) {
        val idx = overflow[rr % overflow.size]
        if (limits[idx] < needs[idx]) {
            limits[idx] += 1.0
            unused -= 1.0
        }
        rr++
        if (rr > 10000) break
    }

    val prefixText = displaySlots.mapIndexed { idx, slot ->
        val limit = if (idx < limits.size) limits[idx] else baseLimitUnits
        ellipsizeByUnitsPrefix(slot, limit)
    }.joinToString("_")

    return FileNameDisplayParts(prefixText = prefixText, counter = counter)
}
