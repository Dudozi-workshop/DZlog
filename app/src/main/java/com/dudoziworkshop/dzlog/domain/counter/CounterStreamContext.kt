package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.model.CellKey
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.naming.buildGalleryRelativePath
import com.dudoziworkshop.dzlog.domain.naming.resolveGroupValue
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell

/**
 * Counter stream scope를 계산할 때 사용하는 단일 컨텍스트 모델.
 */
data class CounterStreamContext(
    val relativePathKey: String,
    val streamPrefix: String,
    val nextCounter: Int,
    val isManualMode: Boolean
)

fun buildCounterStreamContext(
    resolvedCells: List<ResolvedCell>,
    fileNameSlots: List<CellKey?>,
    nextCounter: Int,
    isManualMode: Boolean,
    fnDelim: String = "_",
    includeFilenameInScope: Boolean = true,
    scopeOptions: CounterScopeOptions = CounterScopeOptions(),
    relativePathOverride: String? = null,
): CounterStreamContext {
    val g1 = resolveGroupValue(resolvedCells, GroupLevel.G1)
    val g2 = resolveGroupValue(resolvedCells, GroupLevel.G2)
    val baseRelativePath = relativePathOverride ?: buildGalleryRelativePath(g1, g2)
    val hasG2Group = resolvedCells.any { it.raw?.groupLevel == GroupLevel.G2 }

    val relativePathKey = CounterManager.computeCounterStreamRelativePathKey(
        baseRelativePath = baseRelativePath,
        hasG2Group = hasG2Group,
        group2Value = g2
    )
    val streamPrefix = CounterManager.computeCounterStreamPrefix(
        resolvedCells = resolvedCells,
        fnDelim = fnDelim,
        fileNameSlots = fileNameSlots,
        includeFilenameInScope = includeFilenameInScope,
        scopeOptions = scopeOptions,
    )

    return CounterStreamContext(
        relativePathKey = relativePathKey,
        streamPrefix = streamPrefix,
        nextCounter = nextCounter.coerceAtLeast(1),
        isManualMode = isManualMode
    )
}
