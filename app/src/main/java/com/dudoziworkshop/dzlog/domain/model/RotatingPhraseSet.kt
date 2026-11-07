package com.dudoziworkshop.dzlog.domain.model

enum class RotatingCounterProgressMode {
    PER_PHRASE,
    CONTINUOUS,
}

data class RotatingPhraseSet(
    val id: String,
    val name: String,
    val items: List<String>,
    val defaultEvery: Int = 1,
    val counterProgressMode: RotatingCounterProgressMode = RotatingCounterProgressMode.PER_PHRASE,
    /**
     * Per-phrase overrides, aligned with [items] by position.
     * null = inherit [defaultEvery]. Old templates omit this property.
     * Phrase insert/delete/reorder MUST transform this list together with items.
     */
    val itemEveryOverrides: List<Int?> = emptyList(),
) {
    fun everyForItem(index: Int): Int =
        (itemEveryOverrides.getOrNull(index) ?: defaultEvery).coerceAtLeast(1)
}
