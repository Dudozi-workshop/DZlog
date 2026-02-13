package com.example.dzlog.feature.table.policy

data class CounterEditConflict(
    val pendingCounterCommitValue: Int,
    val streamNextValue: Int
)

fun parseNonNegativeInt(text: String): Int? {
    val value = text.trim().toIntOrNull() ?: return null
    return if (value < 0) null else value
}

fun evaluateCounterEditConflict(
    oldValueText: String,
    newValueText: String,
    streamNext: Int
): CounterEditConflict? {
    val newValue = parseNonNegativeInt(newValueText) ?: return null
    val oldValue = parseNonNegativeInt(oldValueText)
    val changed = (oldValue == null) || (newValue != oldValue)
    val normalizedStreamNext = streamNext.coerceAtLeast(1)
    return if (changed && newValue < normalizedStreamNext) {
        CounterEditConflict(
            pendingCounterCommitValue = newValue,
            streamNextValue = normalizedStreamNext
        )
    } else {
        null
    }
}
