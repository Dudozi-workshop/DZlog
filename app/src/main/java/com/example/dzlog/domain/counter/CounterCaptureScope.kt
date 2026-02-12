package com.example.dzlog.domain.counter

import com.example.dzlog.domain.capturepolicy.CaptureStreamKey

/**
 * CounterStreamContext + 앱 scope 옵션으로 실제 캡처 정책 키를 구성한 결과.
 */
internal data class CaptureScopedCounterStream(
    val scopeParts: CounterScopeParts,
    val captureStreamKey: CaptureStreamKey
)

internal fun toCaptureStreamKey(streamContext: CounterStreamContext): CaptureStreamKey {
    return CaptureStreamKey(
        relativePathKey = streamContext.relativePathKey,
        prefix = streamContext.streamPrefix
    )
}

internal fun toCaptureScopedCounterStream(
    streamContext: CounterStreamContext,
    includePathInScope: Boolean,
    includeFilenameInScope: Boolean,
): CaptureScopedCounterStream {
    val scopeParts = buildCounterScopeParts(
        relativePath = streamContext.relativePathKey,
        prefix = streamContext.streamPrefix,
        includePathInScope = includePathInScope,
        includeFilenameInScope = includeFilenameInScope,
    )
    return CaptureScopedCounterStream(
        scopeParts = scopeParts,
        captureStreamKey = CaptureStreamKey(
            relativePathKey = scopeParts.relativePathKey,
            prefix = scopeParts.prefix
        )
    )
}
