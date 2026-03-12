package com.dudoziworkshop.dzlog.feature.capture.policy

// 정책 요약:
// - 새 scope면 stream(policy) 값을 그대로 사용한다.
// - 같은 scope면 stream/current 중 큰 값을 사용해 기본 rollback을 방지한다.
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



// undo 기반 외부 재동기화처럼 하향 동기화가 필요한 경우에는 allowDownwardSync로 예외 허용한다.
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
