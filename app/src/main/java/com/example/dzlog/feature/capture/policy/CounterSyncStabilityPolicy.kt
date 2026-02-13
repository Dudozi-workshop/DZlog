package com.example.dzlog.feature.capture.policy

fun stabilizeStreamNextCounter(
    streamNextFromPolicy: Int,
    currentScopeNext: Int,
    isNewStream: Boolean
): Int {
    val normalizedStreamNext = streamNextFromPolicy.coerceAtLeast(1)
    val normalizedCurrentScopeNext = currentScopeNext.coerceAtLeast(1)
    if (isNewStream) return normalizedStreamNext
    return maxOf(normalizedStreamNext, normalizedCurrentScopeNext)
}
