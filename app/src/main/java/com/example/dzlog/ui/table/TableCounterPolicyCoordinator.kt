package com.example.dzlog.ui.table

import android.content.Context
import com.example.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.example.dzlog.domain.capturepolicy.CaptureStreamKey

internal object TableCounterPolicyCoordinator {

    data class CounterSeedSyncInput(
        val hasCounterCell: Boolean,
        val currentSeed: Int,
        val streamNext: Int,
        val isNewStream: Boolean,
        val preserveManualCounterSeed: Boolean,
        val manualSeedOverride: Int?
    )

    data class CounterSeedSyncResult(
        val desiredSeed: Int,
        val shouldClearManualOverride: Boolean,
        val preserveManualCounterSeed: Boolean
    )

    fun streamKey(relativePathKey: String, prefix: String): CaptureStreamKey {
        return CaptureStreamKey(
            relativePathKey = relativePathKey,
            prefix = prefix
        )
    }

    suspend fun getNextCounter(
        context: Context,
        key: CaptureStreamKey,
        counterDigits: Int,
        fnDelim: String = "_"
    ): Int {
        return CaptureCounterPolicy.getNextCounter(
            context = context,
            key = key,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        ).coerceAtLeast(1)
    }

    suspend fun setNextCounter(
        context: Context,
        key: CaptureStreamKey,
        desired: Int,
        force: Boolean,
        counterDigits: Int,
        fnDelim: String = "_"
    ) {
        CaptureCounterPolicy.setNextCounter(
            context = context,
            key = key,
            desired = desired,
            force = force,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        )
    }

    suspend fun isManualOverrideActive(
        context: Context,
        key: CaptureStreamKey
    ): Boolean {
        return CaptureCounterPolicy.isManualOverrideActive(
            context = context,
            key = key
        )
    }

    suspend fun resetToAutoNext(
        context: Context,
        key: CaptureStreamKey,
        counterDigits: Int,
        fnDelim: String = "_"
    ): Int {
        CaptureCounterPolicy.clearManualCounterOverride(
            context = context,
            key = key
        )
        return getNextCounter(
            context = context,
            key = key,
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
            input.preserveManualCounterSeed
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
