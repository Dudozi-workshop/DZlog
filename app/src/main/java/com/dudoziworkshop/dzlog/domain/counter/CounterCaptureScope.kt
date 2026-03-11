package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureStreamKey

/**
 * CounterScope + 앱 scope 옵션으로 실제 캡처 정책 키를 구성한 결과.
 */
internal data class ScopedCounter(
    val scopeParts: CounterScopeParts,
    val captureStreamKey: CaptureStreamKey
)

internal fun toCaptureStreamKey(counterScope: CounterScope): CaptureStreamKey {
    return CaptureStreamKey(
        relativePathKey = counterScope.relativePathKey,
        prefix = counterScope.streamPrefix
    )
}

internal fun toScopedCounter(
    counterScope: CounterScope,
    includePathInScope: Boolean,
    includeFilenameInScope: Boolean,
): ScopedCounter {
    val scopeParts = buildCounterScopeParts(
        relativePath = counterScope.relativePathKey,
        prefix = counterScope.streamPrefix,
        includePathInScope = includePathInScope,
        includeFilenameInScope = includeFilenameInScope,
    )
    return ScopedCounter(
        scopeParts = scopeParts,
        captureStreamKey = CaptureStreamKey(
            relativePathKey = scopeParts.relativePathKey,
            prefix = scopeParts.prefix
        )
    )
}

internal typealias CaptureScopedCounterStream = ScopedCounter

/**
 * legacy 호환 브릿지: 기존 호출부에서 쓰던 이름을 유지한다.
 *
 * 핵심 진입점은 toScopedCounter(counterScope, ...)이며,
 * 동일 시그니처 오버로드 충돌을 피하기 위해 이 함수는 1개만 유지한다.
 */
internal fun toCaptureScopedCounterStream(
    counterScope: CounterScope,
    includePathInScope: Boolean,
    includeFilenameInScope: Boolean,
): CaptureScopedCounterStream = toScopedCounter(
    counterScope = counterScope,
    includePathInScope = includePathInScope,
    includeFilenameInScope = includeFilenameInScope,
)
