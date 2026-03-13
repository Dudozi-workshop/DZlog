package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.data.counter.parseCounterForPolicy

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
                parseCounterForPolicy(
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


}
