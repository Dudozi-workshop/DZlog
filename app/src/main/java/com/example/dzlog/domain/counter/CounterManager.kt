package com.example.dzlog.domain.counter

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.example.dzlog.data.counter.scanUsedCountersFromMediaStore
import com.example.dzlog.data.counterindex.CounterIndexRepository
import com.example.dzlog.data.preferences.KEY_COUNTER_MIGRATED_STREAMS_V1
import com.example.dzlog.data.preferences.dataStore
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.naming.buildFileNamePrefixFromResolvedCells
import com.example.dzlog.domain.naming.resolveGroupValue
import com.example.dzlog.domain.table.ResolvedCell
import kotlinx.coroutines.flow.first
import java.util.Date

/**
 * CounterManager
 *
 * 책임:
 * 1) 카운터 스트림 키 계산 (relativePath + counterPrefix)
 * 2) 해당 스트림의 next counter 계산 (max + 1)
 *
 * 정책:
 * - 스트림 키 = relativePath + counterPrefix
 * - counterPrefix = "파일명 포함 셀" 기반 prefix
 * - 날짜/시간은 파일명에는 포함되더라도, counterPrefix에는 포함하지 않음
 * - 초기화 의미 = 해당 스트림의 max + 1
 */
object CounterManager {

    /**
     * Build a counter-stream key for relativePath.
     *
     * Rationale:
     * - Physical save path is derived from (group1, group2).
     * - If G2 is *enabled* but its value is blank, the physical path becomes the same as G1-only.
     *   We must prevent counter streams from mixing between:
     *     (A) G1-only
     *     (B) G1 + (G2 enabled but empty)
     *
     * This function keeps the physical path unchanged, but adds a virtual suffix for DB keying.
     */
    fun computeCounterStreamRelativePathKey(
        baseRelativePath: String,
        hasG2Group: Boolean,
        group2Value: String
    ): String {
        if (!hasG2Group) return baseRelativePath
        if (group2Value.isNotBlank()) return baseRelativePath
        // Virtual stream key: distinguish "G2 enabled but empty" from true G1-only.
        return baseRelativePath + "|g2=enabled_empty"
    }


    /**
     * 카운터 스트림용 prefix 계산
     *
     * @param resolvedCells TableResolver.plan().resolvedCells 그대로 전달
     * @param fnDelim 파일명 구분자 (ex "_")
     *
     * 날짜/시간은 counter 스트림에 영향을 주지 않도록 항상 제외한다.
     */
    fun computeCounterPrefix(
        resolvedCells: List<ResolvedCell>,
        fnDelim: String
    ): String {
        // NOTE: includeDate/includeTime는 "강제 토큰" 추가 옵션이며,
        // DATE/TIME 셀의 fileNameInclude 여부와는 별개다.
        // counter 스트림에는 "설정된 셀 기반 prefix"만 반영하고, 강제 date/time 토큰은 제외한다.
        return buildFileNamePrefixFromResolvedCells(
            resolvedCells = resolvedCells,
            fnDelim = fnDelim,
            includeDate = false,
            includeTime = false,
            now = Date()
        )
    }

    /**
     * 카운터 스트림용 prefix(단일 소스)
     *
     * - basePrefix: fileNameInclude 셀 기반 prefix (COUNTER 제외)
     * - g2Enabled : "G2 값이 유효한지" 기준
     *
     * 요구사항:
     * - 저장경로가 우연히 같아도(G2 값이 비어 g1 폴더만 쓰는 경우 등),
     *   "G2 그룹을 쓰는 템플릿"과 "G1만 쓰는 템플릿"의 카운팅 스트림은 분리되어야 한다.
     */
    fun computeCounterStreamPrefix(
        resolvedCells: List<ResolvedCell>,
        fnDelim: String
    ): String {
        val basePrefix = computeCounterPrefix(resolvedCells, fnDelim)
        // ✅ 스트림 분리 기준: "G2 사용"은 값이 실제로 유효할 때만 true로 본다.
        //
        // - 사용자가 G2를 "해제"하면(= 값이 비워짐), G1-only 스트림과 합류해야 함
        // - 과거 로직(셀 존재 여부)은 값이 비어도 g2=1로 남아 스트림이 분리되는 문제가 있었음
        val g2Value = resolveGroupValue(resolvedCells, GroupLevel.G2)
        val g2Enabled = !g2Value.isNullOrBlank()
        val tag = if (g2Enabled) "g2=1" else "g2=0"
        return "$basePrefix|$tag"
    }

    private fun basePrefixFromStreamPrefix(streamPrefix: String): String {
        return streamPrefix.substringBefore("|g2=", streamPrefix)
    }

    /**
     * 해당 스트림(relativePath  counterPrefix)에서 사용된 카운터 집합을 반환한다.
     *
     * - Room(DB)에 값이 있으면: DB 그대로 반환
     * - 없으면: MediaStore 스캔 → Room backfill → 스캔 결과 반환
     */
    suspend fun getUsedCounters(
        context: Context,
        relativePath: String,
        counterPrefix: String,
        counterDigits: Int,
        fnDelim: String
    ): Set<Int> {
        val repo = CounterIndexRepository.getInstance(context)

        // counterPrefix는 "streamPrefix"(basePrefix|g2=0/1)로 들어올 수 있다.
        val streamPrefix = counterPrefix
        val basePrefix = basePrefixFromStreamPrefix(streamPrefix)
        val legacyKey = "$relativePath|$basePrefix"

        // 0) (선택) legacy prefix -> discriminated prefix 마이그레이션
        // - 과거 버전은 basePrefix만 저장했으므로, 첫 접근 스트림만 legacy 히스토리를 흡수한다.
        // - 이렇게 하면 (G2 on/off) 스트림 분리 요구사항을 지키면서도, 기존 데이터는 가능한 한 보존된다.
        val migrated = loadMigratedLegacyKeys(context)
        val canMigrateLegacy = !migrated.contains(legacyKey)

        // 1) Room 기준 조회 (현재 streamPrefix)
        val fromDb: Set<Int> = runCatching {
            repo.getUsedCounters(relativePath, streamPrefix)
        }.getOrDefault(emptySet())
        if (fromDb.isNotEmpty()) return fromDb

        // 1-legacy) DB에 없고, 아직 마이그레이션 전이면 legacy(basePrefix)도 조회해본다.
        if (canMigrateLegacy) {
            val legacyFromDb: Set<Int> = runCatching {
                repo.getUsedCounters(relativePath, basePrefix)
            }.getOrDefault(emptySet())

            if (legacyFromDb.isNotEmpty()) {
                // 현재 streamPrefix로 backfill
                runCatching { repo.backfillPlaceholders(relativePath, streamPrefix, legacyFromDb) }
                markLegacyMigrated(context, legacyKey)
                return legacyFromDb
            }
        }

        // 2) MediaStore 스캔 fallback
        // - canMigrateLegacy이면 legacy(basePrefix) 스캔도 허용하여 기존 갤러리 데이터로 복원
        val scanned: Set<Int> = runCatching {
            scanUsedCountersFromMediaStore(
                context = context,
                relativePathPrefix = relativePath,
                fileNamePrefix = if (canMigrateLegacy) basePrefix else streamPrefix,
                counterDigits = counterDigits,
                fnDelim = fnDelim
            )
        }.getOrDefault(emptySet())

        // 3) Room backfill
        runCatching {
            repo.backfillPlaceholders(relativePath, streamPrefix, scanned)
        }

        if (canMigrateLegacy) {
            markLegacyMigrated(context, legacyKey)
        }

        return scanned
    }

    private suspend fun loadMigratedLegacyKeys(context: Context): Set<String> {
        val prefs = context.dataStore.data.first()
        val raw = prefs[KEY_COUNTER_MIGRATED_STREAMS_V1].orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    private suspend fun markLegacyMigrated(context: Context, legacyKey: String) {
        runCatching {
            context.dataStore.edit { prefs ->
                val raw = prefs[KEY_COUNTER_MIGRATED_STREAMS_V1].orEmpty()
                val set = raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet()
                if (set.add(legacyKey)) {
                    prefs[KEY_COUNTER_MIGRATED_STREAMS_V1] = set.sorted().joinToString("\n")
                }
            }
        }
    }

    /**

     * 해당 스트림(relativePath + counterPrefix)의 다음 카운터 값을 계산한다.
     *
     * - Room(DB)에 값이 있으면: max + 1
     * - 없으면: MediaStore 스캔 → Room backfill → max + 1
     */
    suspend fun getNextCounter(
        context: Context,
        relativePath: String,
        counterPrefix: String,
        counterDigits: Int,
        fnDelim: String
    ): Int {
        val used = getUsedCounters(
            context = context,
            relativePath = relativePath,
            counterPrefix = counterPrefix,
            counterDigits = counterDigits,
            fnDelim = fnDelim
        )
        return (used.maxOrNull() ?: 0) + 1
    }
}
