package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.model.CellKey
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell

/**
 * Counter scope를 계산할 때 사용하는 단일 모델.
 */
data class CounterScope(
    val relativePathKey: String,
    val streamPrefix: String,
    val nextCounter: Int,
    val isManualMode: Boolean
)

fun buildCounterScope(
    resolvedCells: List<ResolvedCell>,
    fileNameSlots: List<CellKey?>,
    nextCounter: Int,
    isManualMode: Boolean,
    fnDelim: String = "_",
    includeFilenameInScope: Boolean = true,
    scopeOptions: CounterScopeOptions = CounterScopeOptions(),
    relativePathOverride: String? = null,
): CounterScope {
    // 주요 정책(path 축): 카운터 path 키는 pathSlotDrafts에서 계산된 최종 relativePath를 기준으로 한다.
    // relativePathOverride가 있으면 groupLevel(G1/G2) 상태와 무관하게 그대로 사용한다.
    val relativePathKey = (relativePathOverride ?: "Pictures/DZlog/").let { path ->
        val trimmed = path.trim()
        if (trimmed.isBlank()) "Pictures/DZlog/" else if (trimmed.endsWith('/')) trimmed else "$trimmed/"
    }
    val streamPrefix = CounterManager.computeCounterStreamPrefix(
        resolvedCells = resolvedCells,
        fnDelim = fnDelim,
        fileNameSlots = fileNameSlots,
        includeFilenameInScope = includeFilenameInScope,
        scopeOptions = scopeOptions,
    )

    return CounterScope(
        relativePathKey = relativePathKey,
        streamPrefix = streamPrefix,
        nextCounter = nextCounter.coerceAtLeast(1),
        isManualMode = isManualMode
    )
}

typealias CounterStreamContext = CounterScope

fun buildCounterStreamContext(
    resolvedCells: List<ResolvedCell>,
    fileNameSlots: List<CellKey?>,
    nextCounter: Int,
    isManualMode: Boolean,
    fnDelim: String = "_",
    includeFilenameInScope: Boolean = true,
    scopeOptions: CounterScopeOptions = CounterScopeOptions(),
    relativePathOverride: String? = null,
): CounterStreamContext = buildCounterScope(
    resolvedCells = resolvedCells,
    fileNameSlots = fileNameSlots,
    nextCounter = nextCounter,
    isManualMode = isManualMode,
    fnDelim = fnDelim,
    includeFilenameInScope = includeFilenameInScope,
    scopeOptions = scopeOptions,
    relativePathOverride = relativePathOverride,
)
