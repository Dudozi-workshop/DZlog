package com.dudoziworkshop.dzlog.feature.table.policy

import android.content.Context
import com.dudoziworkshop.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.dudoziworkshop.dzlog.domain.counter.CaptureScopedCounterStream
import com.dudoziworkshop.dzlog.domain.model.SaveMode
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
        // preserveManualCounterSeed:
        // UI에 manual 후보 seed를 계속 보여줄지 여부만 뜻한다.
        // 저장소 auto-next 기준값(streamNext)을 변경하는 플래그가 아니다.
        val preserveManualCounterSeed: Boolean,
        val manualSeedOverride: Int?,
        // CounterManager scopeKey 계산이 커버하지 못하는 UI scope 변경(예: filename slot 재배치)을
        // 상위에서 명시적으로 새 scope로 승격할 때 사용한다.
        val forceTreatAsNewScope: Boolean = false,
    ) {
        val isNewStream: Boolean
            get() = forceTreatAsNewScope || isNewCounterScope(previous = previousScopeSnapshot, current = currentScopeSnapshot)
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
        saveMode: SaveMode,
    ): Int {
        // 정책 명시: auto-next는 CounterManager.computeNextCounter(max+1)를 기준으로 계산되고,
        // CaptureCounterPolicy에서 manual override를 결합한 값을 반환한다.
        return CaptureCounterPolicy.getNextCounter(
            context = context,
            scopedStream = scopedStream,
            scanPrefix = scopedStream.captureStreamKey.scanPrefix,
            counterDigits = counterDigits,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
            saveMode = saveMode,
        ).coerceAtLeast(1)
    }


    suspend fun setNextCounter(
        context: Context,
        scopedStream: CaptureScopedCounterStream,
        desired: Int,
        force: Boolean,
        counterDigits: Int,
        saveMode: SaveMode,
    ) {
        CaptureCounterPolicy.setNextCounter(
            context = context,
            scopedStream = scopedStream,
            desired = desired,
            force = force,
            counterDigits = counterDigits,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
            saveMode = saveMode,
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
        saveMode: SaveMode,
    ): Int {
        return CaptureCounterPolicy.resetToAutoNext(
            context = context,
            scopedStream = scopedStream,
            scanPrefix = scopedStream.captureStreamKey.scanPrefix,
            counterDigits = counterDigits,
            fnDelim = NamingFormatDefaults.FILE_NAME_DELIMITER,
            saveMode = saveMode,
        )
    }

    fun resolveSeedForScope(input: CounterSeedSyncInput): CounterSeedSyncResult {
        val normalizedManualOverride = input.manualSeedOverride?.coerceAtLeast(1)

        // 새 scope에서는 manual 표시 상태를 초기화하고,
        // 같은 scope에서는 UI 표시 유지 여부만 이어간다.
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
