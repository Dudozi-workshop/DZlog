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
    val sepLen = sep.length
    val ellLen = 1

    if (p2.isBlank()) {
        if (p1.length <= totalBudget) return p1
        val limit = maxOf(1, totalBudget - ellLen)
        return ellipsizePrefix(p1, limit)
    }

    val rawLen = p1.length + sepLen + p2.length
    if (rawLen <= totalBudget) return p1 + sep + p2

    if (p1.length > bothLongFixed && p2.length > bothLongFixed) {
        val avail = (totalBudget - sepLen).coerceAtLeast(2)
        val each = maxOf(1, minOf(bothLongFixed, (avail / 2) - ellLen))
        return ellipsizePrefix(p1, each) + sep + ellipsizePrefix(p2, each)
    }

    if (p1.length <= p2.length) {
        val remainingForP2 = totalBudget - sepLen - p1.length
        if (remainingForP2 <= 0) {
            return ellipsizePrefix(p1, maxOf(1, totalBudget - ellLen))
        }
        if (p2.length <= remainingForP2) return p1 + sep + p2
        val p2Limit = maxOf(1, remainingForP2 - ellLen)
        return p1 + sep + ellipsizePrefix(p2, p2Limit)
    }

    val remainingForP1 = totalBudget - sepLen - p2.length
    if (remainingForP1 <= 0) {
        return ellipsizePrefix(p2, maxOf(1, totalBudget - ellLen))
    }
    if (p1.length <= remainingForP1) return p1 + sep + p2
    val p1Limit = maxOf(1, remainingForP1 - ellLen)
    return ellipsizePrefix(p1, p1Limit) + sep + p2
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

fun buildThreePartTitlePinnedLast(
    parts: List<String>,
    totalBudget: Int,
    sep: String = "/",
): String {
    val base = parts.map { it.trim() }.filter { it.isNotBlank() }.joinToString("_")
    return buildFileNameTitleWithCounter(
        fileName = base,
        totalBudget = totalBudget,
        sep = sep,
        maxSlots = 2,
        slotsBothLongFixed = 5,
    )
}


fun buildFileNameTitleWithCounter(
    fileName: String,
    totalBudget: Int,
    sep: String = "/",
    maxSlots: Int,
    slotsBothLongFixed: Int,
): String {
    val base = stripExtension(fileName)
    val counter = TRAILING_DIGITS_REGEX.find(base)?.groupValues?.getOrNull(1)
    val prefixName = if (counter != null) base.removeSuffix(counter).trimEnd('_') else base
    val slots = prefixName
        .split("_")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(maxSlots)

    if (counter == null) {
        return when (slots.size) {
            0 -> ""
            1 -> ellipsizePrefix(slots[0], maxOf(1, totalBudget - 1))
            2 -> buildTwoPartTitle(slots[0], slots[1], totalBudget, bothLongFixed = slotsBothLongFixed, sep = sep)
            else -> {
                val allLong = slots.all { it.length > slotsBothLongFixed }
                if (allLong) {
                    slots.joinToString(sep) { part -> ellipsizePrefix(part, slotsBothLongFixed) }
                } else {
                    buildThreePartTitle(slots, totalBudget, sep)
                }
            }
        }
    }

    val sepLen = sep.length
    val remaining = totalBudget - (counter.length + sepLen)
    if (remaining <= 0) return "…$sep$counter"

    val slotsCompressed = when (slots.size) {
        0 -> "…"
        1 -> {
            val p = slots[0]
            if (p.length <= remaining) p else ellipsizePrefix(p, maxOf(1, remaining - 1))
        }
        2 -> buildTwoPartTitle(
            g1 = slots[0],
            g2 = slots[1],
            totalBudget = remaining,
            bothLongFixed = slotsBothLongFixed,
            sep = sep,
        )
        else -> {
            val allLong = slots.all { it.length > slotsBothLongFixed }
            if (allLong) {
                val fixedSlots = slots.joinToString(sep) { part -> ellipsizePrefix(part, slotsBothLongFixed) }
                if (fixedSlots.length <= remaining) fixedSlots else buildThreePartTitle(slots, remaining, sep)
            } else {
                buildThreePartTitle(slots, remaining, sep)
            }
        }
    }

    return "$slotsCompressed$sep$counter"
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
    val sepLen = sep.length

    val tailRaw = if (p2.isNotBlank()) "$p1$sep$p2" else p1
    val raw = "$fixedPrefix$sep$tailRaw"
    if (raw.length <= totalBudget) return raw

    val remaining = totalBudget - (fixedPrefix.length + sepLen)
    if (remaining <= 0) {
        return fixedPrefix
    }
    if (remaining < 2) {
        return "$fixedPrefix$sep…"
    }
    val tail = buildTwoPartTitle(
        g1 = p1,
        g2 = p2.ifBlank { null },
        totalBudget = remaining,
        bothLongFixed = bothLongFixed,
        sep = sep,
    )
    return "$fixedPrefix$sep$tail"
}
