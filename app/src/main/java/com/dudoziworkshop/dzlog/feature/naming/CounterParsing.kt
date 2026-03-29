package com.dudoziworkshop.dzlog.feature.naming

import com.dudoziworkshop.dzlog.data.counter.parseCounterForPolicy

internal data class ParsedCounterSeed(
    val latestCounter: Int?,
    val nextCounter: Int
)

internal fun parseNextCounterFromDisplayName(
    latestDisplayName: String?,
    fileNamePrefix: String,
    fnDelim: String
): ParsedCounterSeed {
    val latestCounter = latestDisplayName
        ?.let {
            parseCounterForPolicy(
                displayName = it,
                fileNamePrefix = fileNamePrefix,
                fnDelim = fnDelim
            )
        }
        ?.coerceAtLeast(0)

    val nextCounter = ((latestCounter ?: 0) + 1).coerceAtLeast(1)
    return ParsedCounterSeed(latestCounter = latestCounter, nextCounter = nextCounter)
}
