package com.example.dzlog.ui.table

import android.content.Context
import com.example.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.example.dzlog.domain.counter.CounterStreamContext
import com.example.dzlog.domain.counter.isNewCounterScope

internal object TableCounterPolicyCoordinator {

    data class CounterSeedSyncInput(
        val streamContext: CounterStreamContext,
        val hasCounterCell: Boolean,
        val currentSeed: Int,
        val streamNext: Int,
        val previousScopeKey: String?,
        val preserveManualCounterSeed: Boolean,
        val manualSeedOverride: Int?
    ) {
        val isNewStream: Boolean
            get() = isNewCounterScope(previousScopeKey, streamContext.scopeKey)
    }

    data class CounterSeedSyncResult(
        val desiredSeed: Int,
        val shouldClearManualOverride: Boolean,
        val preserveManualCounterSeed: Boolean
    )

    suspend fun getNextCounter(
        context: Context,
        streamContext: CounterStreamContext,
        counterDigits: Int,
        fnDelim: String = "_"
    ): Int {
        return CaptureCounterPolicy.getNextCounter(
            context = context,
            streamContext = streamContext,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        ).coerceAtLeast(1)
    }

    suspend fun setNextCounter(
        context: Context,
        streamContext: CounterStreamContext,
        desired: Int,
        force: Boolean,
        counterDigits: Int,
        fnDelim: String = "_"
    ) {
        CaptureCounterPolicy.setNextCounter(
            context = context,
            streamContext = streamContext,
            desired = desired,
            force = force,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        )
    }

    suspend fun isManualOverrideActive(
        context: Context,
        streamContext: CounterStreamContext
    ): Boolean {
        return CaptureCounterPolicy.isManualOverrideActive(
            context = context,
            streamContext = streamContext
        )
    }

    suspend fun resetToAutoNext(
        context: Context,
        streamContext: CounterStreamContext,
        counterDigits: Int,
        fnDelim: String = "_"
    ): Int {
        CaptureCounterPolicy.clearManualCounterOverride(
            context = context,
            streamContext = streamContext
        )
        return getNextCounter(
            context = context,
            streamContext = streamContext,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        )
    }

    fun resolveSeedForScope(input: CounterSeedSyncInput): CounterSeedSyncResult {
        val normalizedCurrentSeed = input.currentSeed.coerceAtLeast(1)
        val normalizedStreamNext = input.streamNext.coerceAtLeast(1)
        val normalizedManualOverride = input.manualSeedOverride?.coerceAtLeast(1)

        val preserveManual = if (input.isNewStream) {
            false
        } else {
            input.preserveManualCounterSeed || input.streamContext.isManualMode
        }

        val desiredSeed = when {
            !input.hasCounterCell -> normalizedStreamNext
            normalizedManualOverride != null -> normalizedManualOverride
            input.isNewStream -> normalizedStreamNext
            preserveManual -> normalizedCurrentSeed
            else -> maxOf(normalizedStreamNext, normalizedCurrentSeed)
        }

        return CounterSeedSyncResult(
            desiredSeed = desiredSeed,
            shouldClearManualOverride = normalizedManualOverride != null,
            preserveManualCounterSeed = preserveManual
        )
    }
}
