package com.dudoziworkshop.dzlog.feature.capture.policy

import android.content.Context
import com.dudoziworkshop.dzlog.data.counter.parseCounterFromDisplayNameForPolicy
import com.dudoziworkshop.dzlog.data.datastore.AppSettingsStore
import com.dudoziworkshop.dzlog.data.mediastore.MediaStoreQueryUtils
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.dudoziworkshop.dzlog.domain.counter.CounterStreamContext
import com.dudoziworkshop.dzlog.domain.counter.toCaptureScopedCounterStream
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

    internal suspend fun refreshNextCounterFromMediaStore(
        context: Context,
        streamContext: CounterStreamContext,
        counterDigits: Int,
        fnDelim: String
    ): Int {
        val appSettings = AppSettingsStore.flow(context).first()
        val scopedStream = toCaptureScopedCounterStream(
            streamContext = streamContext,
            includePathInScope = appSettings.includePathInCounterScope,
            includeFilenameInScope = appSettings.includeFilenameInCounterScope,
        )

        val relativePath = scopedStream.captureStreamKey.relativePathKey
            .substringBefore("|g2=", scopedStream.captureStreamKey.relativePathKey)
        val fileNamePrefix = scopedStream.captureStreamKey.prefix
            .substringBefore("|g2=", scopedStream.captureStreamKey.prefix)

        val latestDisplayName = MediaStoreQueryUtils.queryLatestDisplayNameInRelativePath(
            resolver = context.contentResolver,
            relativePath = relativePath
        )

        val parsedSeed = parseNextCounterFromDisplayName(
            latestDisplayName = latestDisplayName,
            fileNamePrefix = fileNamePrefix,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        )

        CaptureCounterPolicy.clearManualCounterOverride(
            context = context,
            scopedStream = scopedStream
        )

        return parsedSeed.nextCounter
    }
}
