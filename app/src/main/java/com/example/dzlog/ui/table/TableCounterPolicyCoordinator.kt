package com.example.dzlog.ui.table

import android.content.Context
import com.example.dzlog.domain.capturepolicy.CaptureCounterPolicy
import com.example.dzlog.domain.capturepolicy.CaptureStreamKey

internal object TableCounterPolicyCoordinator {

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
}
