package com.example.dzlog.domain.capturepolicy

import android.content.Context
import com.example.dzlog.data.counterindex.CounterIndexRepository
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
        usedCounter: Int,
        mediaStoreId: Long
    ) {
        if (usedCounter < 0) return
        if (mediaStoreId <= 0L) return

        val repo = CounterIndexRepository.getInstance(context)
        val dateAddedSeconds = System.currentTimeMillis() / 1000L
        runCatching {
            repo.record(
                relativePath = key.relativePathKey,
                prefix = key.prefix,
                mediaId = mediaStoreId,
                counterValue = usedCounter,
                dateAddedSeconds = dateAddedSeconds
            )
        }
    }

    internal suspend fun setNextCounter(
        context: Context,
        key: CaptureStreamKey,
        desired: Int,
        force: Boolean,
        counterDigits: Int,
        fnDelim: String
    ) {
        val normalized = desired.coerceAtLeast(1)
        val currentNext = getNextCounter(
            context = context,
            key = key,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        )

        if (!force && normalized < currentNext) {
            return
        }

        if (normalized <= 1) return

        // getNextCounter=max+1 구조에서 desired를 다음 값으로 강제하려면 desired-1을 점유시킨다.
        val repo = CounterIndexRepository.getInstance(context)
        runCatching {
            repo.backfillPlaceholders(
                relativePath = key.relativePathKey,
                prefix = key.prefix,
                counters = setOf(normalized - 1)
            )
        }
    }
}
