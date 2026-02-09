package com.example.dzlog.domain.capturepolicy

import android.content.Context
import com.example.dzlog.domain.counter.CounterManager

/**
 * Counter SSOT boundary.
 *
 * Step 3.2:
 * - 기존 CounterManager를 내부 구현으로 연결함.
 * - UI/Repository는 "카운터 숫자 결정"에 관여하지 않고, 이 정책을 통해서만 접근하도록 경계를 만든다.
 *
 * NOTE:
 * - commit/setNextCounter는 다음 스텝에서 실제 호출 지점(Camera/TableEditor/Repository)을 연결할 때 확정한다.
 */
internal object CaptureCounterPolicy {

    internal suspend fun getNextCounter(
        context: Context,
        key: CaptureStreamKey,
        counterDigits: Int,
        fnDelim: String
    ): Int {
        return CounterManager.getNextCounter(
            context = context,
            relativePath = key.relativePathKey,
            counterPrefix = key.prefix,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        )
    }

    internal suspend fun commitCounter(
        context: Context,
        key: CaptureStreamKey,
        usedCounter: Int
    ) {
        // Step 3.2: 호출 지점이 아직 없으므로 동작은 비워둔다.
        // - 기존 시스템에서는 Repository 내부에서 CounterIndex를 기록/백필함.
        // - Step 3.4 이후 단일 진입점으로 완전히 통합될 때 여기로 이관한다.
    }

    internal suspend fun setNextCounter(
        context: Context,
        key: CaptureStreamKey,
        desired: Int,
        force: Boolean
    ) {
        // Step 3.2: TableEditor의 "중복 경고/강제 진행" UX 정책이 확정된 뒤 연결한다.
    }
}
