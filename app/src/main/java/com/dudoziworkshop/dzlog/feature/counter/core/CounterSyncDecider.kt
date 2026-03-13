package com.dudoziworkshop.dzlog.feature.counter.core

internal object CounterSyncDecider {

    internal fun decide(
        reason: CounterSyncReason,
        currentDisplayedNext: Int,
        resolvedNext: Int,
        isSameStream: Boolean,
    ): CounterSyncDecision {
        val current = currentDisplayedNext.coerceAtLeast(1)
        val resolved = resolvedNext.coerceAtLeast(1)
        val allowDownwardSync = shouldAllowDownwardSync(reason)

        val appliedNext = when {
            !isSameStream -> resolved
            allowDownwardSync -> resolved
            resolved < current -> current
            else -> resolved
        }

        return CounterSyncDecision(
            appliedNext = appliedNext,
            shouldReplace = appliedNext != current,
            allowDownwardSync = allowDownwardSync,
        )
    }

    private fun shouldAllowDownwardSync(reason: CounterSyncReason): Boolean {
        return when (reason) {
            CounterSyncReason.SAVE_MODE_CHANGE,
            CounterSyncReason.UNDO,
            CounterSyncReason.STREAM_CHANGE -> true
            CounterSyncReason.INITIAL,
            CounterSyncReason.RESUME,
            CounterSyncReason.CAPTURE_COMMITTED,
            CounterSyncReason.MANUAL_SET -> false
        }
    }
}
