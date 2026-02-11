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
    val relativePathKey = if (includePathInScope) relativePath else "*"
    val prefixKey = if (includeFilenameInScope) prefix else "*"
    val scopeKey = listOfNotNull(
        relativePathKey.takeUnless { it == "*" },
        prefixKey.takeUnless { it == "*" },
    ).takeIf { it.isNotEmpty() }
        ?.joinToString("|")
        ?: "global"

    return CounterScopeParts(
        relativePathKey = relativePathKey,
        prefix = prefixKey,
        scopeKey = scopeKey,
    )
}
