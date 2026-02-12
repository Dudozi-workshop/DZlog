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
 * Counter SSOT boundary.
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

    internal suspend fun getNextCounter(
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

        // 수동 override 모드인 경우: 캡처 후 다음 값으로 전진시켜 연속 촬영 시에도 유지한다.
        val streamKey = streamKey(key)
        val manualOverrides = loadManualNextOverrides(context)
        val currentOverride = manualOverrides[streamKey]
        if (currentOverride != null) {
            val nextManual = (usedCounter + 1).coerceAtLeast(1)
            saveManualNextOverride(context, streamKey, nextManual)
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

    internal suspend fun setNextCounter(
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

        // 자동 범위(>=autoNext)로 설정하면 수동 override 해제
        clearManualNextOverride(context, streamKey)

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

    internal suspend fun isManualOverrideActive(
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

    internal suspend fun clearManualCounterOverride(
        context: Context,
        key: CaptureStreamKey
    ) {
        clearManualNextOverride(context, streamKey(key))
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
