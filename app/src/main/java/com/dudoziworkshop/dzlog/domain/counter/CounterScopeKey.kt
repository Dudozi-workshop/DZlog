package com.dudoziworkshop.dzlog.domain.counter

data class CounterScopeParts(
    val relativePathKey: String,
    val prefix: String,
    val scopeKey: String,
)


fun buildCounterScopeParts(
    relativePath: String,
    prefix: String,
    includePathInScope: Boolean,
    includeFilenameInScope: Boolean,
): CounterScopeParts {
    val relativePathKey = if (includePathInScope) relativePath else "*"
    val prefixKey = if (includeFilenameInScope) prefix else "*"
    val scopeKey = listOf(relativePathKey, prefixKey).joinToString("|")

    return CounterScopeParts(
        relativePathKey = relativePathKey,
        prefix = prefixKey,
        scopeKey = scopeKey,
    )
}
