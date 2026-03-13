package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureStreamKey

/**
 * CounterScope + 앱 scope 옵션으로 실제 캡처 정책 키를 구성한 결과.
 */
internal data class ScopedCounter(
    val scopeParts: CounterScopeParts,
    val captureStreamKey: CaptureStreamKey
)

internal fun buildStreamKey(counterScope: CounterScope, scanPrefix: String): CaptureStreamKey {
    return CaptureStreamKey(
        relativePathKey = counterScope.relativePathKey,
        prefix = counterScope.streamPrefix,
        scanPrefix = scanPrefix,
    )
}

internal fun buildScopedCounter(
    counterScope: CounterScope,
    includePathInScope: Boolean,
    includeFilenameInScope: Boolean,
    scanPrefix: String,
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
            prefix = scopeParts.prefix,
            scanPrefix = scanPrefix,
        )
    )
}

internal typealias CaptureScopedCounterStream = ScopedCounter
