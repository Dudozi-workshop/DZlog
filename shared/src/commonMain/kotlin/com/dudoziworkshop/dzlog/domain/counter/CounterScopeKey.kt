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
    val rotatingPolicy = prefix.substringAfter("|rotating=", "").substringBefore("|g2=")
    val prefixKey = when {
        includeFilenameInScope -> prefix
        rotatingPolicy.isNotBlank() -> "*|rotating=$rotatingPolicy"
        else -> "*"
    }
    val scopeKey = listOf(relativePathKey, prefixKey).joinToString("|")

    return CounterScopeParts(
        relativePathKey = relativePathKey,
        prefix = prefixKey,
        scopeKey = scopeKey,
    )
}

