package com.dudoziworkshop.dzlog.ui.common

fun ellipsizePrefix(text: String, maxChars: Int): String {
    val t = text.trim()
    if (t.length <= maxChars) return t
    if (maxChars <= 0) return "…"
    return t.take(maxChars) + "…"
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
    val sepUnits = weightedLenUnits(sep)
    val ellUnits = weightedLenUnits("…")
    val totalUnits = totalBudget.toDouble()

    if (p2.isBlank()) {
        if (weightedLenUnits(p1) <= totalUnits) return p1
        val limitUnits = maxOf(0.1, totalUnits - ellUnits)
        return ellipsizeByUnitsPrefix(p1, limitUnits)
    }

    val p1Units = weightedLenUnits(p1)
    val p2Units = weightedLenUnits(p2)
    val rawUnits = p1Units + sepUnits + p2Units
    if (rawUnits <= totalUnits) return p1 + sep + p2

    if (p1Units > bothLongFixed && p2Units > bothLongFixed) {
        val avail = (totalUnits - sepUnits).coerceAtLeast(0.2)
        val each = maxOf(0.1, minOf(bothLongFixed.toDouble(), (avail / 2.0) - ellUnits))
        return ellipsizeByUnitsPrefix(p1, each) + sep + ellipsizeByUnitsPrefix(p2, each)
    }

    if (p1Units <= p2Units) {
        val remainingForP2 = totalUnits - sepUnits - p1Units
        if (remainingForP2 <= 0.0) {
            val l = maxOf(0.1, totalUnits - ellUnits)
            return ellipsizeByUnitsPrefix(p1, l)
        }
        if (p2Units <= remainingForP2) return p1 + sep + p2
        val p2Limit = maxOf(0.1, remainingForP2 - ellUnits)
        return p1 + sep + ellipsizeByUnitsPrefix(p2, p2Limit)
    }

    val remainingForP1 = totalUnits - sepUnits - p2Units
    if (remainingForP1 <= 0.0) {
        val l = maxOf(0.1, totalUnits - ellUnits)
        return ellipsizeByUnitsPrefix(p2, l)
    }
    if (p1Units <= remainingForP1) return p1 + sep + p2
    val p1Limit = maxOf(0.1, remainingForP1 - ellUnits)
    return ellipsizeByUnitsPrefix(p1, p1Limit) + sep + p2
}


fun buildThreePartTitle(
    parts: List<String>,
    totalBudget: Int,
    sep: String = "/",
): String {
    val cleaned = parts.map { it.trim() }.filter { it.isNotBlank() }.take(3)
    if (cleaned.isEmpty()) return ""
    if (cleaned.size == 1) return ellipsizePrefix(cleaned[0], maxOf(1, totalBudget - 1))

    val sepLen = sep.length
    val rawLen = cleaned.sumOf { it.length } + sepLen * (cleaned.size - 1)
    if (rawLen <= totalBudget) return cleaned.joinToString(sep)

    val baseBudget = (totalBudget - sepLen * (cleaned.size - 1)).coerceAtLeast(cleaned.size)
    val lens = cleaned.map { it.length }
    val limits = MutableList(cleaned.size) { 1 }
    var remaining = baseBudget - cleaned.size

    val order = lens.indices.sortedBy { lens[it] }
    while (remaining > 0) {
        var allocated = false
        for (idx in order) {
            if (limits[idx] < lens[idx]) {
                limits[idx] += 1
                remaining -= 1
                allocated = true
                if (remaining == 0) break
            }
        }
        if (!allocated) break
    }

    return cleaned.indices.joinToString(sep) { i ->
        val limit = limits[i]
        if (cleaned[i].length <= limit) cleaned[i] else ellipsizePrefix(cleaned[i], maxOf(1, limit - 1))
    }
}

private val TRAILING_DIGITS_REGEX = Regex("(\\d+)$")

fun stripExtension(fileName: String): String {
    val t = fileName.trim()
    val idx = t.lastIndexOf('.')
    return if (idx > 0) t.substring(0, idx) else t
}

private fun charUnit(ch: Char): Double {
    val code = ch.code
    val isHangul = code in 0xAC00..0xD7A3
    val isDigit = ch in '0'..'9'
    val isAsciiLetter = (ch in 'a'..'z') || (ch in 'A'..'Z')

    return when {
        isHangul -> 1.0
        isDigit -> 0.63
        isAsciiLetter -> 0.43
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
    return sb.toString() + "…"
}

fun buildThreePartTitlePinnedLast(
    parts: List<String>,
    totalBudget: Int,
    sep: String = "_",
): String {
    val base = parts.map { it.trim() }.filter { it.isNotBlank() }.joinToString("_")
    return buildFileNameTitleWithCounter(base, sep)
}

fun buildFileNameTitleWithCounter(
    fileName: String,
    sep: String = "_",
): String {
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

    val slotText = displaySlots.mapIndexed { idx, slot ->
        val limit = if (idx < limits.size) limits[idx] else baseLimitUnits
        ellipsizeByUnitsPrefix(slot, limit)
    }.joinToString(sep)

    return when {
        counter.isNullOrBlank() -> slotText
        slotText.isBlank() -> counter
        else -> "$slotText$sep$counter"
    }
}

fun buildPrefixedTwoPartPath(
    prefix: String,
    g1: String,
    g2: String?,
    totalBudget: Int,
    sep: String = "/",
    bothLongFixed: Int = 5,
): String {
    val fixedPrefix = prefix.trim()
    val p1 = g1.trim()
    val p2 = g2?.trim().orEmpty().ifBlank { "" }

    val sepUnits = weightedLenUnits(sep)
    val totalUnits = totalBudget.toDouble()

    val tailRaw = if (p2.isNotBlank()) "$p1$sep$p2" else p1
    val raw = "$fixedPrefix$sep$tailRaw"
    if (weightedLenUnits(raw) <= totalUnits) return raw

    val remaining = totalUnits - (weightedLenUnits(fixedPrefix) + sepUnits)
    if (remaining <= 0.0) {
        return fixedPrefix
    }
    if (remaining < weightedLenUnits("…")) {
        return "$fixedPrefix$sep…"
    }
    val tail = buildTwoPartTitle(
        g1 = p1,
        g2 = p2.ifBlank { null },
        totalBudget = kotlin.math.floor(remaining).toInt().coerceAtLeast(1),
        bothLongFixed = bothLongFixed,
        sep = sep,
    )
    return "$fixedPrefix$sep$tail"
}
