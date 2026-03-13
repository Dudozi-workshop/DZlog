package com.dudoziworkshop.dzlog.feature.counter

import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.model.SaveMode

internal object CounterRequestResolver {

    internal fun fromScope(
        counterScope: CounterScope,
        saveMode: SaveMode,
        scanPrefix: String,
        includePathInScope: Boolean,
        includeFilenameInScope: Boolean,
        tableTemplateId: String? = null,
    ): CounterRequest {
        return fromRaw(
            saveMode = saveMode,
            relativePathKey = counterScope.relativePathKey,
            prefix = counterScope.streamPrefix,
            scanPrefix = scanPrefix,
            includePathInScope = includePathInScope,
            includeFilenameInScope = includeFilenameInScope,
            tableTemplateId = tableTemplateId,
        )
    }


    internal fun fromHome(
        counterScope: CounterScope,
        saveMode: SaveMode,
        scanPrefix: String,
        includePathInScope: Boolean,
        includeFilenameInScope: Boolean,
    ): CounterRequest {
        // Home은 read-only 화면이지만 stream 정규화 계약은 동일하게 적용한다.
        return fromRaw(
            saveMode = saveMode,
            relativePathKey = counterScope.relativePathKey,
            prefix = counterScope.streamPrefix,
            scanPrefix = scanPrefix,
            includePathInScope = includePathInScope,
            includeFilenameInScope = includeFilenameInScope,
            tableTemplateId = null,
        )
    }

    internal fun fromCamera(
        saveMode: SaveMode,
        relativePathKey: String,
        prefix: String,
        scanPrefix: String,
        includePathInScope: Boolean,
        includeFilenameInScope: Boolean,
    ): CounterRequest {
        return fromRaw(
            saveMode = saveMode,
            relativePathKey = relativePathKey,
            prefix = prefix,
            scanPrefix = scanPrefix,
            includePathInScope = includePathInScope,
            includeFilenameInScope = includeFilenameInScope,
            tableTemplateId = null,
        )
    }

    internal fun fromTable(
        saveMode: SaveMode,
        relativePathKey: String,
        prefix: String,
        scanPrefix: String,
        includePathInScope: Boolean,
        includeFilenameInScope: Boolean,
        tableTemplateId: String?,
    ): CounterRequest {
        return fromRaw(
            saveMode = saveMode,
            relativePathKey = relativePathKey,
            prefix = prefix,
            scanPrefix = scanPrefix,
            includePathInScope = includePathInScope,
            includeFilenameInScope = includeFilenameInScope,
            tableTemplateId = tableTemplateId,
        )
    }

    private fun fromRaw(
        saveMode: SaveMode,
        relativePathKey: String,
        prefix: String,
        scanPrefix: String,
        includePathInScope: Boolean,
        includeFilenameInScope: Boolean,
        tableTemplateId: String?,
    ): CounterRequest {
        val streamMode = streamModeOf(saveMode)

        // 정책:
        // - BOTH는 WATERMARK 전용과 동일한 카운터 stream 입력으로 정규화한다.
        // - ORIGINAL_ONLY는 original 경로 축으로 정규화해 별도 stream 입력이 되게 한다.
        val normalizedRelativePathKey = when (streamMode) {
            CounterStreamMode.WATERMARK -> normalizeWatermarkPath(relativePathKey)
            CounterStreamMode.ORIGINAL -> normalizeOriginalPath(relativePathKey)
        }

        // 엔진 호출용 유효 mode도 동일 축으로 정규화한다.
        // 주의: saveMode 문자열 식별자를 만들지 않고 enum 자체를 재사용한다.
        val effectiveSaveMode = when (streamMode) {
            CounterStreamMode.WATERMARK -> SaveMode.WATERMARK_ONLY
            CounterStreamMode.ORIGINAL -> SaveMode.ORIGINAL_ONLY
        }

        return CounterRequest(
            saveMode = saveMode,
            effectiveSaveMode = effectiveSaveMode,
            relativePathKey = normalizedRelativePathKey,
            prefix = prefix,
            scanPrefix = scanPrefix,
            includePathInScope = includePathInScope,
            includeFilenameInScope = includeFilenameInScope,
            tableTemplateId = tableTemplateId,
        )
    }

    private fun streamModeOf(saveMode: SaveMode): CounterStreamMode {
        return when (saveMode) {
            SaveMode.WATERMARK_ONLY,
            SaveMode.BOTH -> CounterStreamMode.WATERMARK
            SaveMode.ORIGINAL_ONLY -> CounterStreamMode.ORIGINAL
        }
    }

    private fun normalizeWatermarkPath(path: String): String {
        val normalized = ensureTrailingSlash(path)
        return if (normalized.endsWith("original/")) normalized.removeSuffix("original/") else normalized
    }

    private fun normalizeOriginalPath(path: String): String {
        val normalized = ensureTrailingSlash(path)
        return if (normalized.endsWith("original/")) normalized else "${normalized}original/"
    }

    private fun ensureTrailingSlash(path: String): String {
        val trimmed = path.trim()
        if (trimmed.isBlank()) return ""
        return if (trimmed.endsWith('/')) trimmed else "$trimmed/"
    }

    private enum class CounterStreamMode {
        WATERMARK,
        ORIGINAL,
    }
}
