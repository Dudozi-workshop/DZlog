package com.dudoziworkshop.dzlog.feature.capture.policy

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


fun resolveSyncedScopeNext(
    streamNextFromPolicy: Int,
    currentScopeNext: Int,
    isNewScope: Boolean,
    allowDownwardSync: Boolean,
): Int {
    val normalizedStreamNext = streamNextFromPolicy.coerceAtLeast(1)
    if (allowDownwardSync) return normalizedStreamNext
    return stabilizeStreamNextCounter(
        streamNextFromPolicy = normalizedStreamNext,
        currentScopeNext = currentScopeNext,
        isNewStream = isNewScope,
    )
}
