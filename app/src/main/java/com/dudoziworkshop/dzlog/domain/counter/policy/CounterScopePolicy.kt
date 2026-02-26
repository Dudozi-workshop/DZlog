package com.dudoziworkshop.dzlog.domain.counter.policy

import com.dudoziworkshop.dzlog.domain.counter.CounterStreamContext
import com.dudoziworkshop.dzlog.domain.counter.buildCounterScopeParts

data class CounterScopeSnapshot(
    val relativePathKey: String,
    val prefix: String,
    val scopeKey: String,
    val includePathInScope: Boolean,
    val includeFilenameInScope: Boolean,
)

fun isNewCounterScope(previous: CounterScopeSnapshot?, current: CounterScopeSnapshot): Boolean {
    if (previous == null) return false
    return previous.scopeKey != current.scopeKey
}

fun buildCounterScopeSnapshot(
    streamContext: CounterStreamContext,
    includePathInScope: Boolean,
    includeFilenameInScope: Boolean,
): CounterScopeSnapshot {
    val scopeParts = buildCounterScopeParts(
        relativePath = streamContext.relativePathKey,
        prefix = streamContext.streamPrefix,
        includePathInScope = includePathInScope,
        includeFilenameInScope = includeFilenameInScope,
    )
    return CounterScopeSnapshot(
        relativePathKey = scopeParts.relativePathKey,
        prefix = scopeParts.prefix,
        scopeKey = scopeParts.scopeKey,
        includePathInScope = includePathInScope,
        includeFilenameInScope = includeFilenameInScope,
    )
}
