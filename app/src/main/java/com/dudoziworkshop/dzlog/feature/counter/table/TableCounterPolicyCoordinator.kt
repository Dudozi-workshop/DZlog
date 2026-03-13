package com.dudoziworkshop.dzlog.feature.counter.table

import com.dudoziworkshop.dzlog.domain.counter.policy.CounterScopeSnapshot
import com.dudoziworkshop.dzlog.domain.counter.policy.CounterSeedInput
import com.dudoziworkshop.dzlog.domain.counter.policy.decideCounterSeed
import com.dudoziworkshop.dzlog.domain.counter.policy.isNewCounterScope

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
