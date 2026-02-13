package com.example.dzlog.domain.counter

data class CounterScopeParts(
    val relativePathKey: String,
    val prefix: String,
    val scopeKey: String,
)

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
