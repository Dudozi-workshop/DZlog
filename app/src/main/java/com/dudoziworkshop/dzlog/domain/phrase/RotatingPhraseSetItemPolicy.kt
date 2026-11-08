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

    /** Apply one shared period to many selected rows without copying the full list per row. */
    fun setEveryForItems(
        set: RotatingPhraseSet,
        indices: Collection<Int>,
        every: Int?,
    ): RotatingPhraseSet {
        if (every != null && every !in 1..RotatingPhraseSet.MAX_EVERY) return set
        val indicesToChange = indices.filter { it in set.items.indices }.toSet()
        if (indicesToChange.isEmpty()) return set
        val values = overrides(set)
        var changed = false
        indicesToChange.forEach { index ->
            if (values[index] != every) {
                values[index] = every
                changed = true
            }
        }
        return if (changed) set.copy(itemEveryOverrides = values) else set
    }

    fun renameItem(set: RotatingPhraseSet, index: Int, phrase: String): RotatingPhraseSet {
        val normalized = phrase.trim()
        if (index !in set.items.indices || !validPhrase(normalized)) return set
        if (set.items[index] == normalized) return set
        return set.copy(items = set.items.toMutableList().apply { this[index] = normalized })
    }

    fun addItem(set: RotatingPhraseSet, phrase: String): RotatingPhraseSet {
        val normalized = phrase.trim()
        if (set.items.size >= RotatingPhraseSet.MAX_ITEMS || !validPhrase(normalized)) return set
        return set.copy(
            items = set.items + normalized,
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
        require(items.size <= RotatingPhraseSet.MAX_ITEMS && items.all(::validPhrase))
        require(periods.size == items.size && periods.all { it == null || it in 1..RotatingPhraseSet.MAX_EVERY })
        return set.copy(items = items, itemEveryOverrides = periods)
    }

    private fun validPhrase(text: String): Boolean =
        text.isNotBlank() && text.length <= RotatingPhraseSet.MAX_PHRASE_LENGTH &&
            '\n' !in text && '\r' !in text

    fun overrides(set: RotatingPhraseSet): MutableList<Int?> =
        MutableList(set.items.size) { index -> set.itemEveryOverrides.getOrNull(index)?.takeIf { it >= 1 } }
}
