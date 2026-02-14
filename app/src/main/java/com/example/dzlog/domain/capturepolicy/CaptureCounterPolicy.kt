package com.example.dzlog.domain.capturepolicy

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.example.dzlog.data.counterindex.CounterIndexRepository
import com.example.dzlog.data.preferences.KEY_COUNTER_MANUAL_NEXT_OVERRIDES_V1
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.domain.counter.CaptureScopedCounterStream
import com.example.dzlog.domain.counter.CounterManager
import com.example.dzlog.domain.counter.CounterStreamContext
import com.example.dzlog.domain.counter.toCaptureStreamKey
import kotlinx.coroutines.flow.first
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Counter xSSOT boundary.
 *
 * - getNextCounter: stream next 계산(자동 + 수동 override 반영)
 * - commitCounter: 저장 완료된 카운터를 기록하고, 수동 override 모드면 다음값으로 전진
 * - setNextCounter: 사용자가 next seed를 명시 변경할 때 정책을 적용
 */
internal object CaptureCounterPolicy {

    internal suspend fun getNextCounter(
        context: Context,
        streamContext: CounterStreamContext,
        counterDigits: Int,
        fnDelim: String
    ): Int {
        return getNextCounter(
            context = context,
            key = toCaptureStreamKey(streamContext),
            counterDigits = counterDigits,
            fnDelim = fnDelim
        )
    }

    internal suspend fun getNextCounter(
        context: Context,
        scopedStream: CaptureScopedCounterStream,
        counterDigits: Int,
        fnDelim: String
    ): Int {
        return getNextCounter(
            context = context,
            key = scopedStream.captureStreamKey,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        )
    }

    private suspend fun getNextCounter(
        context: Context,
        key: CaptureStreamKey,
        counterDigits: Int,
        fnDelim: String
    ): Int {
        val autoNext = CounterManager.getNextCounter(
            context = context,
            relativePath = key.relativePathKey,
            counterPrefix = key.prefix,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        ).coerceAtLeast(1)

        val manualOverride = loadManualNextOverrides(context)[streamKey(key)]
        return (manualOverride ?: autoNext).coerceAtLeast(1)
    }

    private suspend fun commitCounter(
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

        // 수동 override 모드인 경우: 실제 캡처가 1회 완료되면 자동 스트림으로 복귀한다.
        val streamKey = streamKey(key)
        val manualOverrides = loadManualNextOverrides(context)
        val currentOverride = manualOverrides[streamKey]
        if (currentOverride != null) {
            clearManualNextOverride(context, streamKey)
        }
    }

    internal suspend fun commitCounter(
        context: Context,
        streamContext: CounterStreamContext,
        usedCounter: Int,
        mediaStoreId: Long
    ) {
        commitCounter(
            context = context,
            key = toCaptureStreamKey(streamContext),
            usedCounter = usedCounter,
            mediaStoreId = mediaStoreId
        )
    }

    internal suspend fun commitCounter(
        context: Context,
        scopedStream: CaptureScopedCounterStream,
        usedCounter: Int,
        mediaStoreId: Long
    ) {
        commitCounter(
            context = context,
            key = scopedStream.captureStreamKey,
            usedCounter = usedCounter,
            mediaStoreId = mediaStoreId
        )
    }

    private suspend fun setNextCounter(
        context: Context,
        key: CaptureStreamKey,
        desired: Int,
        force: Boolean,
        counterDigits: Int,
        fnDelim: String
    ) {
        val normalized = desired.coerceAtLeast(1)

        // 자동 기준 next(max+1)
        val autoNext = CounterManager.getNextCounter(
            context = context,
            relativePath = key.relativePathKey,
            counterPrefix = key.prefix,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        ).coerceAtLeast(1)

        val streamKey = streamKey(key)

        // force + 낮은 값: 의도적 구멍 메우기 모드로 간주하고 수동 override 저장
        if (force && normalized < autoNext) {
            saveManualNextOverride(context, streamKey, normalized)
            return
        }

        // force가 아니면 기존 안전 정책 유지
        if (!force && normalized < autoNext) {
            return
        }

        // auto 기준과 동일한 값으로 맞춘 경우는 manual override를 유지하지 않는다.
        if (!force && normalized == autoNext) {
            clearManualNextOverride(context, streamKey)
            return
        }

        // 사용자가 next 값을 명시 지정한 경우(>=autoNext 포함)는 수동 override 모드로 보관한다.
        // 실제 캡처가 완료되면 commitCounter에서 수동 override를 해제해 자동 모드로 복귀한다.
        saveManualNextOverride(context, streamKey, normalized)
    }

    internal suspend fun setNextCounter(
        context: Context,
        streamContext: CounterStreamContext,
        desired: Int,
        force: Boolean,
        counterDigits: Int,
        fnDelim: String
    ) {
        setNextCounter(
            context = context,
            key = toCaptureStreamKey(streamContext),
            desired = desired,
            force = force,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        )
    }

    internal suspend fun setNextCounter(
        context: Context,
        scopedStream: CaptureScopedCounterStream,
        desired: Int,
        force: Boolean,
        counterDigits: Int,
        fnDelim: String
    ) {
        setNextCounter(
            context = context,
            key = scopedStream.captureStreamKey,
            desired = desired,
            force = force,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        )
    }

    private suspend fun isManualOverrideActive(
        context: Context,
        key: CaptureStreamKey
    ): Boolean {
        return loadManualNextOverrides(context).containsKey(streamKey(key))
    }

    internal suspend fun isManualOverrideActive(
        context: Context,
        streamContext: CounterStreamContext
    ): Boolean {
        return isManualOverrideActive(
            context = context,
            key = toCaptureStreamKey(streamContext)
        )
    }

    internal suspend fun isManualOverrideActive(
        context: Context,
        scopedStream: CaptureScopedCounterStream
    ): Boolean {
        return isManualOverrideActive(
            context = context,
            key = scopedStream.captureStreamKey
        )
    }

    private suspend fun clearManualCounterOverride(
        context: Context,
        key: CaptureStreamKey
    ) {
        clearManualNextOverride(context, streamKey(key))
    }

    internal suspend fun clearManualCounterOverride(
        context: Context,
        streamContext: CounterStreamContext
    ) {
        clearManualCounterOverride(
            context = context,
            key = toCaptureStreamKey(streamContext)
        )
    }

    internal suspend fun clearManualCounterOverride(
        context: Context,
        scopedStream: CaptureScopedCounterStream
    ) {
        clearManualCounterOverride(
            context = context,
            key = scopedStream.captureStreamKey
        )
    }

    internal suspend fun resetToAutoNext(
        context: Context,
        streamContext: CounterStreamContext,
        counterDigits: Int,
        fnDelim: String
    ): Int {
        clearManualCounterOverride(
            context = context,
            streamContext = streamContext
        )
        return getNextCounter(
            context = context,
            streamContext = streamContext,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        )
    }

    internal suspend fun resetToAutoNext(
        context: Context,
        scopedStream: CaptureScopedCounterStream,
        counterDigits: Int,
        fnDelim: String
    ): Int {
        clearManualCounterOverride(
            context = context,
            scopedStream = scopedStream
        )
        return getNextCounter(
            context = context,
            scopedStream = scopedStream,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        )
    }

    private fun streamKey(key: CaptureStreamKey): String = "${key.relativePathKey}|${key.prefix}"

    private suspend fun loadManualNextOverrides(context: Context): Map<String, Int> {
        val prefs = context.dataStore.data.first()
        val raw = prefs[KEY_COUNTER_MANUAL_NEXT_OVERRIDES_V1].orEmpty()
        if (raw.isBlank()) return emptyMap()
        return raw
            .lineSequence()
            .mapNotNull { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size != 2) return@mapNotNull null
                val key = decode(parts[0])
                val value = parts[1].toIntOrNull()?.coerceAtLeast(1) ?: return@mapNotNull null
                key to value
            }
            .toMap()
    }

    private suspend fun saveManualNextOverride(context: Context, key: String, value: Int) {
        val normalized = value.coerceAtLeast(1)
        val updated = loadManualNextOverrides(context).toMutableMap().apply {
            this[key] = normalized
        }
        persistManualNextOverrides(context, updated)
    }

    private suspend fun clearManualNextOverride(context: Context, key: String) {
        val updated = loadManualNextOverrides(context).toMutableMap().apply {
            remove(key)
        }
        persistManualNextOverrides(context, updated)
    }

    private suspend fun persistManualNextOverrides(context: Context, map: Map<String, Int>) {
        val serialized = map.entries
            .sortedBy { it.key }
            .joinToString("\n") { (k, v) -> "${encode(k)}=$v" }

        runCatching {
            context.dataStore.edit { prefs ->
                prefs[KEY_COUNTER_MANUAL_NEXT_OVERRIDES_V1] = serialized
            }
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    private fun decode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}
