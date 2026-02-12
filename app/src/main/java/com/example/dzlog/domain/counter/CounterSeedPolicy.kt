package com.example.dzlog.domain.counter

data class CounterSeedInput(
    val streamNext: Int,
    val currentSeed: Int,
    val isNewStream: Boolean,
    val hasCounterCell: Boolean = true,
    val preserveManualSeed: Boolean = false,
    val manualSeedOverride: Int? = null,
    val templateCounterSeed: Int? = null,
)

data class CounterSeedDecision(
    val desiredSeed: Int,
    val shouldResyncForScopeChange: Boolean,
    val shouldClearPreserveManualSeed: Boolean,
)

fun isNewCounterScope(previousScopeKey: String?, currentScopeKey: String): Boolean {
    return previousScopeKey != null && previousScopeKey != currentScopeKey
}

fun decideCounterSeed(input: CounterSeedInput): CounterSeedDecision {
    val streamNext = input.streamNext.coerceAtLeast(1)
    val currentSeed = input.currentSeed.coerceAtLeast(1)
    val shouldResyncForScopeChange = input.isNewStream

    var desiredSeed = when {
        !input.hasCounterCell -> streamNext
        input.manualSeedOverride != null -> input.manualSeedOverride.coerceAtLeast(1)
        shouldResyncForScopeChange -> streamNext
        input.preserveManualSeed -> currentSeed
        else -> maxOf(streamNext, currentSeed)
    }

    val templateCounterSeed = input.templateCounterSeed
    if (templateCounterSeed != null && templateCounterSeed > 1 && templateCounterSeed != desiredSeed) {
        desiredSeed = templateCounterSeed
    }

    return CounterSeedDecision(
        desiredSeed = desiredSeed,
        shouldResyncForScopeChange = shouldResyncForScopeChange,
        shouldClearPreserveManualSeed = shouldResyncForScopeChange,
    )
}

fun buildCounterSyncLog(
    source: String,
    scopeKey: String,
    input: CounterSeedInput,
    decision: CounterSeedDecision,
    extras: List<Pair<String, Any?>> = emptyList(),
): String {
    val lines = mutableListOf(
        "[$source] counter sync",
        "scopeKey=$scopeKey",
        "streamNext=${input.streamNext}",
        "currentSeed=${input.currentSeed}",
        "isNewStream=${input.isNewStream}",
        "preserveManualSeed=${input.preserveManualSeed}",
        "manualSeedOverride=${input.manualSeedOverride}",
        "templateCounterSeed=${input.templateCounterSeed}",
        "shouldResyncForScopeChange=${decision.shouldResyncForScopeChange}",
        "desiredSeed=${decision.desiredSeed}"
    )
    extras.forEach { (key, value) -> lines += "$key=$value" }
    return lines.joinToString("\n")
}
