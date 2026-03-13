package com.dudoziworkshop.dzlog.feature.capture.policy

import android.content.Context
import com.dudoziworkshop.dzlog.data.counter.parseCounterFromDisplayNameForPolicy
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.dudoziworkshop.dzlog.domain.counter.CounterManager
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.counter.toScopedCounter
import kotlinx.coroutines.flow.first

internal object CounterResyncPolicy {

    internal data class ParsedCounterSeed(
        val latestCounter: Int?,
        val nextCounter: Int
    )

    internal fun parseNextCounterFromDisplayName(
        latestDisplayName: String?,
        fileNamePrefix: String,
        counterDigits: Int,
        fnDelim: String
    ): ParsedCounterSeed {
        val latestCounter = latestDisplayName
            ?.let {
                parseCounterFromDisplayNameForPolicy(
                    displayName = it,
                    fileNamePrefix = fileNamePrefix,
                    counterDigits = counterDigits,
                    fnDelim = fnDelim
                )
            }
            ?.coerceAtLeast(0)

        val nextCounter = ((latestCounter ?: 0) + 1).coerceAtLeast(1)
        return ParsedCounterSeed(latestCounter = latestCounter, nextCounter = nextCounter)
    }

    internal suspend fun refreshNextCounter(
        context: Context,
        counterScope: CounterScope,
        scanPrefix: String,
        counterDigits: Int,
        fnDelim: String
    ): Int {
        val appSettings = AppSettingsStore.flow(context).first()
        // scoped stream 계산은 CounterScope 단일 모델을 기준으로 수행한다.
        val scopedStream = toScopedCounter(
            counterScope = counterScope,
            includePathInScope = appSettings.includePathInCounterScope,
            includeFilenameInScope = appSettings.includeFilenameInCounterScope,
            scanPrefix = scanPrefix,
        )

        val relativePath = scopedStream.captureStreamKey.relativePathKey
            .substringBefore("|g2=", scopedStream.captureStreamKey.relativePathKey)

        val nextCounterFromScan = CounterManager.computeNextCounter(
            context = context,
            relativePath = relativePath,
            counterPrefix = scopedStream.captureStreamKey.prefix,
            scanPrefix = scopedStream.captureStreamKey.scanPrefix,
            counterDigits = counterDigits,
            fnDelim = fnDelim,
            saveMode = appSettings.saveMode,
        )

        CaptureCounterPolicy.clearManualCounterOverride(
            context = context,
            scopedStream = scopedStream
        )

        return nextCounterFromScan
    }
}
