package com.dudoziworkshop.dzlog.ui.common

private fun ellipsizePrefix(text: String, maxChars: Int): String {
    val t = text.trim()
    if (t.length <= maxChars) return t
    if (maxChars <= 0) return "…"
    return "${t.take(maxChars)}…"
}

fun buildTwoPartTitle(
    g1: String,
    g2: String?,
    totalBudget: Int,
    bothLongFixed: Int = 5,
    sep: String = "/",
): String {
    val p1 = g1.trim()
    val p2 = g2?.trim().orEmpty()
    val sepLen = sep.length
    val ellLen = 1

    if (p2.isBlank()) {
        if (p1.length <= totalBudget) return p1
        val limit = maxOf(1, totalBudget - ellLen)
        return ellipsizePrefix(p1, limit)
    }

    val rawLen = p1.length + sepLen + p2.length
    if (rawLen <= totalBudget) return "$p1$sep$p2"

    if (p1.length > bothLongFixed && p2.length > bothLongFixed) {
        val avail = (totalBudget - sepLen).coerceAtLeast(2)
        val each = maxOf(1, minOf(bothLongFixed, (avail / 2) - ellLen))
        return "${ellipsizePrefix(p1, each)}$sep${ellipsizePrefix(p2, each)}"
    }

    if (p1.length <= p2.length) {
        val remainingForP2 = totalBudget - sepLen - p1.length
        if (remainingForP2 <= 0) {
            return ellipsizePrefix(p1, maxOf(1, totalBudget - ellLen))
        }
        if (p2.length <= remainingForP2) return "$p1$sep$p2"
        val p2Limit = maxOf(1, remainingForP2 - ellLen)
        return "$p1$sep${ellipsizePrefix(p2, p2Limit)}"
    }

    val remainingForP1 = totalBudget - sepLen - p2.length
    if (remainingForP1 <= 0) {
        return ellipsizePrefix(p2, maxOf(1, totalBudget - ellLen))
    }
    if (p1.length <= remainingForP1) return "$p1$sep$p2"
    val p1Limit = maxOf(1, remainingForP1 - ellLen)
    return "${ellipsizePrefix(p1, p1Limit)}$sep$p2"
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
