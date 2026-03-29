package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureStreamKey

/**
 * CounterScope 입력을 capture stream 키 + scope parts로 정규화한 결과.
 */
internal data class ScopedCounter(
    val scopeParts: CounterScopeParts,
    val captureStreamKey: CaptureStreamKey
)

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
