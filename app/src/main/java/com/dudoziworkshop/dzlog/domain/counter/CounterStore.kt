package com.dudoziworkshop.dzlog.domain.counter

import android.content.Context
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.dudoziworkshop.dzlog.domain.model.SaveMode

/**
 * 카운터 저장소 진입점.
 *
 * 핵심 흐름에서 `CaptureCounterPolicy.resolveNext` 대신
 * `CounterStore.getNext` 의미로 읽히도록 제공하는 얇은 어댑터다.
 */
internal object CounterStore {
    internal suspend fun getNext(
        context: Context,
        counterScope: CounterScope,
        scanPrefix: String,
        counterDigits: Int,
        fnDelim: String,
        saveMode: SaveMode,
    ): Int {
        return CaptureCounterPolicy.resolveNext(
            context = context,
            counterScope = counterScope,
            scanPrefix = scanPrefix,
            counterDigits = counterDigits,
            fnDelim = fnDelim,
            saveMode = saveMode,
        )
    }

    internal suspend fun getNext(
        context: Context,
        scopedStream: ScopedCounter,
        scanPrefix: String,
        counterDigits: Int,
        fnDelim: String,
        saveMode: SaveMode,
    ): Int {
        return CaptureCounterPolicy.resolveNext(
            context = context,
            scopedStream = scopedStream,
            scanPrefix = scanPrefix,
            counterDigits = counterDigits,
            fnDelim = fnDelim,
            saveMode = saveMode,
        )
    }
}
