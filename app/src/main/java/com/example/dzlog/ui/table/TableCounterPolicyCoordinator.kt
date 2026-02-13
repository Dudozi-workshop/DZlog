package com.example.dzlog.ui.table

import android.content.Context
import com.example.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.example.dzlog.domain.counter.CaptureScopedCounterStream
import com.example.dzlog.domain.counter.CounterSeedInput
import com.example.dzlog.domain.counter.decideCounterSeed
import com.example.dzlog.domain.counter.isNewCounterScope

internal object TableCounterPolicyCoordinator {

    data class CounterSeedSyncInput(
        val currentScopeKey: String,
        val isManualMode: Boolean,
        val hasCounterCell: Boolean,
        val currentSeed: Int,
        val streamNext: Int,
        val previousScopeKey: String?,
        val preserveManualCounterSeed: Boolean,
        val manualSeedOverride: Int?
    ) {
        val isNewStream: Boolean
            get() = isNewCounterScope(previousScopeKey, currentScopeKey)
    }

    data class CounterSeedSyncResult(
        val desiredSeed: Int,
        val shouldClearManualOverride: Boolean,
        val preserveManualCounterSeed: Boolean
    )

    suspend fun getNextCounter(
        context: Context,
        scopedStream: CaptureScopedCounterStream,
        counterDigits: Int,
        fnDelim: String = "_"
    ): Int {
        return CaptureCounterPolicy.getNextCounter(
            context = context,
            scopedStream = scopedStream,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        ).coerceAtLeast(1)
    }

    suspend fun setNextCounter(
        context: Context,
        scopedStream: CaptureScopedCounterStream,
        desired: Int,
        force: Boolean,
        counterDigits: Int,
        fnDelim: String = "_"
    ) {
        CaptureCounterPolicy.setNextCounter(
            context = context,
            scopedStream = scopedStream,
            desired = desired,
            force = force,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        )
    }

    suspend fun isManualOverrideActive(
        context: Context,
        scopedStream: CaptureScopedCounterStream
    ): Boolean {
        return CaptureCounterPolicy.isManualOverrideActive(
            context = context,
            scopedStream = scopedStream
        )
    }

    suspend fun resetToAutoNext(
        context: Context,
        scopedStream: CaptureScopedCounterStream,
        counterDigits: Int,
        fnDelim: String = "_"
    ): Int {
        return CaptureCounterPolicy.resetToAutoNext(
            context = context,
            scopedStream = scopedStream,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        )
    }

    fun resolveSeedForScope(input: CounterSeedSyncInput): CounterSeedSyncResult {
        val normalizedManualOverride = input.manualSeedOverride?.coerceAtLeast(1)

        val preserveManual = if (input.isNewStream) {
            false
        } else {
            input.preserveManualCounterSeed || input.isManualMode
        }

        val decision = decideCounterSeed(
            CounterSeedInput(
                streamNext = input.streamNext,
                currentSeed = input.currentSeed,
                isNewStream = input.isNewStream,
                hasCounterCell = input.hasCounterCell,
                preserveManualSeed = preserveManual,
                manualSeedOverride = normalizedManualOverride,
                templateCounterSeed = null
            )
        )

        return CounterSeedSyncResult(
            desiredSeed = decision.desiredSeed,
            shouldClearManualOverride = normalizedManualOverride != null,
            preserveManualCounterSeed = preserveManual
        )
    }
}
