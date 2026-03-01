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
    val p2 = g2?.trim().orEmpty().ifBlank { "" }

    if (p2.isBlank()) {
        val limit = minOf(p1.length, totalBudget)
        return ellipsizePrefix(p1, limit)
    }

    val sepLen = sep.length
    val avail = (totalBudget - sepLen).coerceAtLeast(2)
    val len1 = p1.length
    val len2 = p2.length

    var l1: Int
    var l2: Int

    if (len1 > bothLongFixed && len2 > bothLongFixed) {
        l1 = bothLongFixed
        l2 = bothLongFixed
    } else if (len1 <= len2) {
        l1 = minOf(len1, avail)
        l2 = minOf(maxOf(0, avail - l1), len2)
    } else {
        l2 = minOf(len2, avail)
        l1 = minOf(maxOf(0, avail - l2), len1)
    }

    l1 = if (len1 > 0) maxOf(1, l1) else 0
    l2 = if (len2 > 0) maxOf(1, l2) else 0

    val s1 = ellipsizePrefix(p1, l1)
    val s2 = ellipsizePrefix(p2, l2)
    return "$s1$sep$s2"
}
