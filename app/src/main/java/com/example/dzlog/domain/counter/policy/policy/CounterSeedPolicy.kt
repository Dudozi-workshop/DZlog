package com.example.dzlog.domain.counter.policy

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

fun decideCounterSeed(input: CounterSeedInput): CounterSeedDecision {
    val streamNext = input.streamNext.coerceAtLeast(1)
    val currentSeed = input.currentSeed.coerceAtLeast(1)
    val shouldResyncForScopeChange = input.isNewStream

    var desiredSeed = when {
        !input.hasCounterCell -> streamNext
        input.manualSeedOverride != null -> input.manualSeedOverride.coerceAtLeast(1)
        shouldResyncForScopeChange -> streamNext
        input.preserveManualSeed -> currentSeed
        else -> streamNext
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
