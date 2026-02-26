package com.dudoziworkshop.dzlog.feature.table.policy

import android.content.Context
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.dudoziworkshop.dzlog.domain.counter.CaptureScopedCounterStream
import com.dudoziworkshop.dzlog.domain.counter.policy.CounterScopeSnapshot
import com.dudoziworkshop.dzlog.domain.counter.policy.CounterSeedInput
import com.dudoziworkshop.dzlog.domain.counter.policy.decideCounterSeed
import com.dudoziworkshop.dzlog.domain.counter.policy.isNewCounterScope
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults

/**
 * Table counter policy SSOT coordinator.
 *
 * Rule:
 * - UI/Screen layer must call this coordinator only.
 * - Direct use of CounterManager/CaptureCounterPolicy from UI layer is discouraged.
 * - Low-level counter index/media-scan strategy remains encapsulated under policy/domain layer.
 */
internal object TableCounterPolicyCoordinator {

    data class CounterSeedSyncInput(
        val currentScopeSnapshot: CounterScopeSnapshot,
        val isManualMode: Boolean,
        val hasCounterCell: Boolean,
        val currentSeed: Int,
        val streamNext: Int,
        val previousScopeSnapshot: CounterScopeSnapshot?,
        val preserveManualCounterSeed: Boolean,
        val manualSeedOverride: Int?
    ) {
        val isNewStream: Boolean
            get() = isNewCounterScope(previous = previousScopeSnapshot, current = currentScopeSnapshot)
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
    ): Int {
        return CaptureCounterPolicy.getNextCounter(
            context = context,
            scopedStream = scopedStream,
            counterDigits = counterDigits,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER
        ).coerceAtLeast(1)
    }


    suspend fun setNextCounter(
        context: Context,
        scopedStream: CaptureScopedCounterStream,
        desired: Int,
        force: Boolean,
        counterDigits: Int,
    ) {
        CaptureCounterPolicy.setNextCounter(
            context = context,
            scopedStream = scopedStream,
            desired = desired,
            force = force,
            counterDigits = counterDigits,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER
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
    ): Int {
        return CaptureCounterPolicy.resetToAutoNext(
            context = context,
            scopedStream = scopedStream,
            counterDigits = counterDigits,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER
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
            shouldClearManualOverride =
                normalizedManualOverride != null && decision.shouldClearPreserveManualSeed,
            preserveManualCounterSeed = preserveManual
        )
    }
}