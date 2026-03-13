package com.dudoziworkshop.dzlog.feature.counter.core

import android.content.Context
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureStreamKey
import com.dudoziworkshop.dzlog.domain.counter.CaptureScopedCounterStream
import com.dudoziworkshop.dzlog.domain.counter.CounterStore
import com.dudoziworkshop.dzlog.domain.counter.buildCounterScopeParts

internal class CounterFacade(
    private val context: Context,
    private val counterDigits: Int = 0,
    private val fnDelim: String = "_",
) {

    internal suspend fun read(request: CounterRequest): CounterReadResult {
        val scopedStream = request.toScopedStream()
        val next = CounterStore.getNext(
            context = context,
            scopedStream = scopedStream,
            scanPrefix = request.scanPrefix,
            counterDigits = counterDigits,
            fnDelim = fnDelim,
            saveMode = request.effectiveSaveMode,
        )
        val hasManualOverride = CaptureCounterPolicy.hasManualOverride(
            context = context,
            scopedStream = scopedStream,
        )

        return CounterReadResult(
            next = next,
            hasManualOverride = hasManualOverride,
            scopedStream = scopedStream,
        )
    }

    internal suspend fun setManualNext(request: CounterRequest, value: Int) {
        CaptureCounterPolicy.setNext(
            context = context,
            scopedStream = request.toScopedStream(),
            desired = value,
            force = true,
            counterDigits = counterDigits,
            fnDelim = fnDelim,
            saveMode = request.effectiveSaveMode,
        )
    }

    internal suspend fun clearManualNext(request: CounterRequest) {
        CaptureCounterPolicy.clearManualOverride(
            context = context,
            scopedStream = request.toScopedStream(),
        )
    }

    internal suspend fun refreshAfterUndo(request: CounterRequest): CounterReadResult {
        // 정책 보정:
        // undo refresh는 "재조회"이지 manual override clear가 아니다.
        // manual 상태 변경은 setManualNext/clearManualNext에서만 명시적으로 수행한다.
        return read(request)
    }

    private fun CounterRequest.toScopedStream(): CaptureScopedCounterStream {
        // request는 resolver에서 path + engine mode까지 정규화된 최종 입력이다.
        // facade는 기존 SSOT(buildCounterScopeParts) 조립만 수행한다.
        val scopeParts = buildCounterScopeParts(
            relativePath = relativePathKey,
            prefix = prefix,
            includePathInScope = includePathInScope,
            includeFilenameInScope = includeFilenameInScope,
        )
        return CaptureScopedCounterStream(
            scopeParts = scopeParts,
            captureStreamKey = CaptureStreamKey(
                relativePathKey = scopeParts.relativePathKey,
                prefix = scopeParts.prefix,
                scanPrefix = scanPrefix,
            )
        )
    }
}
