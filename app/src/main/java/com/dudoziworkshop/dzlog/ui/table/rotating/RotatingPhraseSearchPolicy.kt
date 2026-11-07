package com.dudoziworkshop.dzlog.ui.table.rotating

/**
 * Search is a VIEW-ONLY filter. Return original indices so edit and delete
 * continue targeting the correct entry, including duplicate phrase texts.
 */
internal object RotatingPhraseSearchPolicy {
    fun matchingIndices(items: List<String>, query: String): List<Int> {
        val term = query.trim()
        return items.indices.filter { term.isEmpty() || items[it].contains(term, ignoreCase = true) }
    }

    fun reorderEnabled(query: String): Boolean = query.isBlank()
}
