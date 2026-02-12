package com.example.dzlog.domain.counter

import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.naming.buildGalleryRelativePath
import com.example.dzlog.domain.naming.resolveGroupValue
import com.example.dzlog.domain.table.ResolvedCell

/**
 * Counter stream scope를 계산할 때 사용하는 단일 컨텍스트 모델.
 */
data class CounterStreamContext(
    val relativePathKey: String,
    val streamPrefix: String,
    val scopeKey: String,
    val nextCounter: Int,
    val isManualMode: Boolean
)

fun buildCounterStreamContext(
    resolvedCells: List<ResolvedCell>,
    nextCounter: Int,
    isManualMode: Boolean,
    fnDelim: String = "_"
): CounterStreamContext {
    val g1 = resolveGroupValue(resolvedCells, GroupLevel.G1)
    val g2 = resolveGroupValue(resolvedCells, GroupLevel.G2)
    val baseRelativePath = buildGalleryRelativePath(g1, g2)
    val hasG2Group = resolvedCells.any { it.raw?.groupLevel == GroupLevel.G2 }

    val relativePathKey = CounterManager.computeCounterStreamRelativePathKey(
        baseRelativePath = baseRelativePath,
        hasG2Group = hasG2Group,
        group2Value = g2
    )
    val streamPrefix = CounterManager.computeCounterStreamPrefix(
        resolvedCells = resolvedCells,
        fnDelim = fnDelim
    )

    return CounterStreamContext(
        relativePathKey = relativePathKey,
        streamPrefix = streamPrefix,
        scopeKey = "$relativePathKey|$streamPrefix",
        nextCounter = nextCounter.coerceAtLeast(1),
        isManualMode = isManualMode
    )
}
