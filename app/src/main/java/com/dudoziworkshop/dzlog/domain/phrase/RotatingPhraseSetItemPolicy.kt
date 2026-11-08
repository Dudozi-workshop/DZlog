package com.dudoziworkshop.dzlog.domain.phrase

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet

/**
 * Keep optional intervals attached to the SAME phrase when rows are edited,
 * inserted, removed or reordered. Duplicate texts remain separate entries.
 */
object RotatingPhraseSetItemPolicy {
    fun setEvery(set: RotatingPhraseSet, index: Int, every: Int?): RotatingPhraseSet {
        if (index !in set.items.indices || (every != null && every !in 1..RotatingPhraseSet.MAX_EVERY)) return set
        val values = overrides(set)
        if (values[index] == every) return set
        values[index] = every
        return set.copy(itemEveryOverrides = values)
    }

    fun renameItem(set: RotatingPhraseSet, index: Int, phrase: String): RotatingPhraseSet {
        if (index !in set.items.indices || phrase.isBlank()) return set
        return set.copy(items = set.items.toMutableList().apply { this[index] = phrase })
    }

    fun addItem(set: RotatingPhraseSet, phrase: String): RotatingPhraseSet {
        if (phrase.isBlank()) return set
        return set.copy(
            items = set.items + phrase,
            itemEveryOverrides = overrides(set) + listOf(null),
        )
    }

    fun removeItem(set: RotatingPhraseSet, index: Int): RotatingPhraseSet {
        if (index !in set.items.indices) return set
        return set.copy(
            items = set.items.filterIndexed { i, _ -> i != index },
            itemEveryOverrides = overrides(set).filterIndexed { i, _ -> i != index },
        )
    }

    fun moveItem(set: RotatingPhraseSet, from: Int, to: Int): RotatingPhraseSet {
        if (from !in set.items.indices || to !in set.items.indices || from == to) return set
        val items = set.items.toMutableList().apply { add(to, removeAt(from)) }
        val values = overrides(set).apply { add(to, removeAt(from)) }
        return set.copy(items = items, itemEveryOverrides = values)
    }

    fun replaceItems(set: RotatingPhraseSet, items: List<String>, periods: List<Int?>): RotatingPhraseSet {
        require(periods.size == items.size && periods.all { it == null || it in 1..RotatingPhraseSet.MAX_EVERY })
        return set.copy(items = items, itemEveryOverrides = periods)
    }

    fun overrides(set: RotatingPhraseSet): MutableList<Int?> =
        MutableList(set.items.size) { index -> set.itemEveryOverrides.getOrNull(index)?.takeIf { it >= 1 } }
}
