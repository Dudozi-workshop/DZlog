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
    sep: String = " / ",
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
