package com.example.dzlog.domain.counter.policy

import com.example.dzlog.domain.counter.CounterStreamContext

data class CounterScopeSnapshot(
    val relativePathKey: String,
    val prefix: String,
    val includePathInScope: Boolean,
    val includeFilenameInScope: Boolean,
)

fun isNewCounterScope(previous: CounterScopeSnapshot?, current: CounterScopeSnapshot): Boolean {
    if (previous == null) return false
    if (current.includePathInScope && previous.relativePathKey != current.relativePathKey) return true
    if (current.includeFilenameInScope && previous.prefix != current.prefix) return true
    return false
}

fun buildCounterScopeSnapshot(
    streamContext: CounterStreamContext,
    includePathInScope: Boolean,
    includeFilenameInScope: Boolean,
): CounterScopeSnapshot = CounterScopeSnapshot(
    relativePathKey = streamContext.relativePathKey,
    prefix = streamContext.streamPrefix,
    includePathInScope = includePathInScope,
    includeFilenameInScope = includeFilenameInScope,
)

