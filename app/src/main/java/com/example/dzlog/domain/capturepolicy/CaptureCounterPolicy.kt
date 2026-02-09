package com.example.dzlog.domain.capturepolicy

import android.content.Context

/**
 * Counter SSOT boundary.
 *
 * Step 3.1: 뼈대만 추가함(아직 호출하지 않음).
 * 다음 스텝에서 CounterManager/CounterIndexRepository를 내부 구현으로 연결한다.
 */
internal object CaptureCounterPolicy {

    internal fun getNextCounter(
        context: Context,
        key: CaptureStreamKey,
        counterDigits: Int,
        fnDelim: String
    ): Int {
        error("CaptureCounterPolicy.getNextCounter is not wired yet (step 3.1 skeleton)")
    }

    internal fun commitCounter(
        context: Context,
        key: CaptureStreamKey,
        usedCounter: Int
    ) {
        error("CaptureCounterPolicy.commitCounter is not wired yet (step 3.1 skeleton)")
    }

    internal fun setNextCounter(
        context: Context,
        key: CaptureStreamKey,
        desired: Int,
        force: Boolean
    ) {
        error("CaptureCounterPolicy.setNextCounter is not wired yet (step 3.1 skeleton)")
    }
}
