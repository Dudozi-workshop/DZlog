package com.dudoziworkshop.dzlog.domain.capturepolicy

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.dudoziworkshop.dzlog.data.counterindex.CounterIndexRepository
import com.dudoziworkshop.dzlog.data.preferences.KEY_COUNTER_MANUAL_NEXT_OVERRIDES_V1
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.domain.counter.CaptureScopedCounterStream
import com.dudoziworkshop.dzlog.domain.counter.CounterManager
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.counter.buildStreamKey
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import kotlinx.coroutines.flow.first
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Counter xSSOT boundary.
 *
 * - resolveNext: stream next 계산(자동 + 수동 override 반영)
 * - commit: 저장 완료된 카운터를 기록하고, 수동 override 모드면 다음값으로 전진
 * - setNext: 사용자가 next seed를 명시 변경할 때 정책을 적용
 */
internal object CaptureCounterPolicy {

    internal suspend fun resolveNext(
        context: Context,
        counterScope: CounterScope,
        scanPrefix: String,
        counterDigits: Int,
        fnDelim: String,
        saveMode: SaveMode,
    ): Int {
        return resolveNext(
            context = context,
            key = buildStreamKey(counterScope, scanPrefix),
            counterDigits = counterDigits,
            fnDelim = fnDelim,
            saveMode = saveMode,
        )
    }

    internal suspend fun resolveNext(
        context: Context,
        scopedStream: CaptureScopedCounterStream,
        scanPrefix: String,
        counterDigits: Int,
        fnDelim: String,
        saveMode: SaveMode,
    ): Int {
        return resolveNext(
            context = context,
            key = scopedStream.captureStreamKey.copy(scanPrefix = scanPrefix),
            counterDigits = counterDigits,
            fnDelim = fnDelim,
            saveMode = saveMode,
        )
    }

    private suspend fun resolveNext(
        context: Context,
        key: CaptureStreamKey,
        counterDigits: Int,
        fnDelim: String,
        saveMode: SaveMode,
    ): Int {
        // auto-next는 MediaStore 실파일 기준 max+1이며(hole fill 없음),
        // 아래에서 manual override가 있으면 UI/수동 정책으로 덮어쓴다.
        val autoNext = CounterManager.computeNext(
            context = context,
            relativePath = key.relativePathKey,
            counterPrefix = key.prefix,
            scanPrefix = key.scanPrefix,
            counterDigits = counterDigits,
            fnDelim = fnDelim,
            saveMode = saveMode,
        ).coerceAtLeast(1)

        val manualOverride = loadManualNextOverrides(context)[streamKey(key)]
        val next = (manualOverride ?: autoNext).coerceAtLeast(1)
        Log.d(
            "CounterReadback",
            "CaptureCounterPolicy.resolveNext relativePathKey=${key.relativePathKey}, prefix=${key.prefix}, autoNext=$autoNext, manualOverride=$manualOverride, resolvedNext=$next, saveMode=$saveMode"
        )
        return next
    }

    private suspend fun commit(
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

    internal suspend fun commit(
        context: Context,
        counterScope: CounterScope,
        scanPrefix: String,
        usedCounter: Int,
        mediaStoreId: Long
    ) {
        commit(
            context = context,
            key = buildStreamKey(counterScope, scanPrefix),
            usedCounter = usedCounter,
            mediaStoreId = mediaStoreId
        )
    }

    internal suspend fun commit(
        context: Context,
        scopedStream: CaptureScopedCounterStream,
        usedCounter: Int,
        mediaStoreId: Long
    ) {
        commit(
            context = context,
            key = scopedStream.captureStreamKey,
            usedCounter = usedCounter,
            mediaStoreId = mediaStoreId
        )
    }

    private suspend fun setNext(
        context: Context,
        key: CaptureStreamKey,
        desired: Int,
        force: Boolean,
        counterDigits: Int,
        fnDelim: String,
        saveMode: SaveMode,
    ) {
        val normalized = desired.coerceAtLeast(1)

        // 자동 기준 next(max+1)
        val autoNext = CounterManager.computeNext(
            context = context,
            relativePath = key.relativePathKey,
            counterPrefix = key.prefix,
            scanPrefix = key.scanPrefix,
            counterDigits = counterDigits,
            fnDelim = fnDelim,
            saveMode = saveMode,
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
        // 실제 캡처가 완료되면 commit에서 수동 override를 해제해 자동 모드로 복귀한다.
        saveManualNextOverride(context, streamKey, normalized)
    }

    internal suspend fun setNext(
        context: Context,
        counterScope: CounterScope,
        scanPrefix: String,
        desired: Int,
        force: Boolean,
        counterDigits: Int,
        fnDelim: String,
        saveMode: SaveMode,
    ) {
        setNext(
            context = context,
            key = buildStreamKey(counterScope, scanPrefix),
            desired = desired,
            force = force,
            counterDigits = counterDigits,
            fnDelim = fnDelim,
            saveMode = saveMode,
        )
    }

    internal suspend fun setNext(
        context: Context,
        scopedStream: CaptureScopedCounterStream,
        desired: Int,
        force: Boolean,
        counterDigits: Int,
        fnDelim: String,
        saveMode: SaveMode,
    ) {
        setNext(
            context = context,
            key = scopedStream.captureStreamKey,
            desired = desired,
            force = force,
            counterDigits = counterDigits,
            fnDelim = fnDelim,
            saveMode = saveMode,
        )
    }

    private suspend fun hasManualOverride(
        context: Context,
        key: CaptureStreamKey
    ): Boolean {
        return loadManualNextOverrides(context).containsKey(streamKey(key))
    }

    internal suspend fun hasManualOverride(
        context: Context,
        counterScope: CounterScope,
        scanPrefix: String
    ): Boolean {
        return hasManualOverride(
            context = context,
            key = buildStreamKey(counterScope, scanPrefix)
        )
    }

    internal suspend fun hasManualOverride(
        context: Context,
        scopedStream: CaptureScopedCounterStream
    ): Boolean {
        return hasManualOverride(
            context = context,
            key = scopedStream.captureStreamKey
        )
    }

    private suspend fun clearManualOverride(
        context: Context,
        key: CaptureStreamKey
    ) {
        clearManualNextOverride(context, streamKey(key))
    }

    internal suspend fun clearManualOverride(
        context: Context,
        counterScope: CounterScope,
        scanPrefix: String
    ) {
        clearManualOverride(
            context = context,
            key = buildStreamKey(counterScope, scanPrefix)
        )
    }

    internal suspend fun clearManualOverride(
        context: Context,
        scopedStream: CaptureScopedCounterStream
    ) {
        clearManualOverride(
            context = context,
            key = scopedStream.captureStreamKey
        )
    }

    internal suspend fun resetToAuto(
        context: Context,
        counterScope: CounterScope,
        scanPrefix: String,
        counterDigits: Int,
        fnDelim: String,
        saveMode: SaveMode,
    ): Int {
        clearManualOverride(
            context = context,
            counterScope = counterScope,
            scanPrefix = scanPrefix
        )
        return resolveNext(
            context = context,
            counterScope = counterScope,
            scanPrefix = scanPrefix,
            counterDigits = counterDigits,
            fnDelim = fnDelim,
            saveMode = saveMode,
        )
    }

    internal suspend fun resetToAuto(
        context: Context,
        scopedStream: CaptureScopedCounterStream,
        scanPrefix: String,
        counterDigits: Int,
        fnDelim: String,
        saveMode: SaveMode,
    ): Int {
        clearManualOverride(
            context = context,
            scopedStream = scopedStream
        )
        return resolveNext(
            context = context,
            scopedStream = scopedStream,
            scanPrefix = scanPrefix,
            counterDigits = counterDigits,
            fnDelim = fnDelim,
            saveMode = saveMode,
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
