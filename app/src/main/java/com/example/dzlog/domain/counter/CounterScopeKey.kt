package com.example.dzlog.domain.counter

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
    val relativePathKey = if (includePathInScope) relativePath else "path=off"
    val prefixKey = if (includeFilenameInScope) prefix else "name=off"
    val scopeKey = "$relativePathKey|$prefixKey"

    return CounterScopeParts(
        relativePathKey = relativePathKey,
        prefix = prefixKey,
        scopeKey = scopeKey,
    )
}
