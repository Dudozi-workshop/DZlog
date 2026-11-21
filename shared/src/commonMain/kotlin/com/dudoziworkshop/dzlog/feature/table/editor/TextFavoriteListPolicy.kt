package com.dudoziworkshop.dzlog.feature.table.editor

/** Pure transformations for the ordered, app-wide text favorite list. */
object TextFavoriteListPolicy {
    fun toggle(items: List<String>, text: String, maxItems: Int): List<String>? = when {
        text.isBlank() -> null
        text in items -> items.filterNot { it == text }
        items.size >= maxItems -> null
        else -> listOf(text) + items
    }

    fun rename(items: List<String>, old: String, replacement: String): List<String>? {
        if (old !in items || replacement.isBlank() ||
            (replacement != old && replacement in items)) return null
        return items.map { if (it == old) replacement else it }
    }

    fun move(items: List<String>, text: String, direction: Int): List<String> {
        val index = items.indexOf(text)
        val target = index + direction
        if (index < 0 || target !in items.indices || kotlin.math.abs(direction) != 1) return items
        val reordered = items.toMutableList()
        val moved = reordered.removeAt(index)
        reordered.add(target, moved)
        return reordered
    }
}
