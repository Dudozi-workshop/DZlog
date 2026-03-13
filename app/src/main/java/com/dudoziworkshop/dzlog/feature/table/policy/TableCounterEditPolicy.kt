package com.dudoziworkshop.dzlog.feature.table.policy

data class CounterEditConflict(
    val pendingCounterCommitValue: Int,
    val streamNextValue: Int
)

fun parseNonNegativeInt(text: String): Int? {
    val value = text.trim().toIntOrNull() ?: return null
    return if (value < 0) null else value
}

fun evaluateCounterEditConflict(
    newValueText: String,
    streamNext: Int,
    lowCounterWarningLatchedInSession: Boolean,
): CounterEditConflict? {
    val newValue = parseNonNegativeInt(newValueText) ?: return null
    val normalizedStreamNext = streamNext.coerceAtLeast(1)
    val isLowCounterInput = newValue < normalizedStreamNext
    // 정책: 경고는 low 상태(pending < media auto-next)에 "처음 진입"할 때만 1회.
    return if (isLowCounterInput && !lowCounterWarningLatchedInSession) {
        CounterEditConflict(
            pendingCounterCommitValue = newValue,
            streamNextValue = normalizedStreamNext
        )
    } else {
        null
    }
}

fun nextLowCounterWarningLatch(
    pendingCounterCommitValue: Int,
    streamNext: Int,
): Boolean {
    val normalizedStreamNext = streamNext.coerceAtLeast(1)
    val isLowCounterInput = pendingCounterCommitValue < normalizedStreamNext
    return if (isLowCounterInput) true else false
}
