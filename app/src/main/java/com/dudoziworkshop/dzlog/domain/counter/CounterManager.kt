package com.dudoziworkshop.dzlog.domain.counter

import android.content.Context
import com.dudoziworkshop.dzlog.data.counter.CounterScanTarget
import com.dudoziworkshop.dzlog.data.counter.scanUsedCounters
import com.dudoziworkshop.dzlog.data.counter.toCounterScanTarget
import com.dudoziworkshop.dzlog.data.counterindex.CounterIndexRepository
import com.dudoziworkshop.dzlog.domain.model.CellKey
import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.naming.sanitizeFilePart
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell

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
 * - DATE/TIME 셀은 파일명/경로에 포함될 수 있으나,
 *   counterPrefix는 scopeToken(정규화 토큰)을 우선 사용하며,
 *   반영 여부는 CounterScopeOptions(dateScopeValues/timeScopeValues)로 결정
 * - 옵션이 false이면 DATE/TIME 변화는 카운터 스트림 분리(초기화)에 영향을 주지 않음
 * - 초기화 의미 = 해당 스트림의 max + 1
 */
object CounterManager {

    fun buildScanPaths(
        baseRel: String,
        target: CounterScanTarget
    ): List<String> {
        // 주요 정책(readback): includePathInScope=false일 때 baseRel="*"(와일드카드)이 들어온다.
        // 이 경우 exact RELATIVE_PATH 매칭을 수행하면 항상 empty가 되므로,
        // scanner의 wildcard 모드를 그대로 태우기 위해 "*"를 유지한다.
        if (baseRel.trim() == "*") return listOf("*")

        val normalizedBaseRel = normalizeRelativePathPrefix(baseRel)
        val originalRel = appendOriginalDirectory(normalizedBaseRel)

        return when (target) {
            CounterScanTarget.WATER_ONLY -> listOf(normalizedBaseRel)
            CounterScanTarget.ORIGINAL_ONLY -> listOf(originalRel)
            CounterScanTarget.BOTH -> listOf(normalizedBaseRel, originalRel)
        }
    }

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
        // Virtual stream key: distinguish empty-enabled G2 from true G1-only.
        return "$baseRelativePath|g2=enabled_empty"
    }


    /**
     * 카운터 스트림용 prefix 계산
     *
     * @param resolvedCells TableResolver.plan().resolvedCells 그대로 전달
     * @param fnDelim 파일명 구분자 (ex "_")
     *
     * 규칙:
     * - COUNTER는 항상 제외
     * - DATE/TIME은 CounterScopeOptions의 date/time scope values가 있을 때만 포함
     * - 그 외 타입은 resolvedText를 sanitize 후 포함
     */
    fun computeCounterPrefix(
        resolvedCells: List<ResolvedCell>,
        fnDelim: String,
        fileNameSlots: List<CellKey?>,
        includeFilenameInScope: Boolean,
        scopeOptions: CounterScopeOptions = CounterScopeOptions(),
    ): String {
        val delim = fnDelim.ifBlank { "_" }
        val normalizedSlots = fileNameSlots.take(FILE_NAME_SLOT_COUNT) + List((FILE_NAME_SLOT_COUNT - fileNameSlots.size).coerceAtLeast(0)) { null }

        val slotParts = if (includeFilenameInScope) {
            normalizedSlots
                .asSequence()
                .mapNotNull { slot -> slot?.let { key -> resolvedCells.firstOrNull { rc -> rc.id == key } } }
                .mapNotNull { rc ->
                    val token: String? = when (rc.type) {
                        TableCellDataType.COUNTER -> null
                        TableCellDataType.DATE,
                        TableCellDataType.TIME -> null
                        TableCellDataType.ROTATING_TEXT -> {
                            // 정책: ROTATING_TEXT 분리는 phraseScopeValues(rp_...)를 단일 기준으로 사용한다.
                            // 중복 분리를 막기 위해 slot 토큰에서는 제외한다.
                            null
                        }
                        else -> rc.resolvedText
                    }

                    token
                        ?.let { sanitizeFilePart(it) }
                        ?.takeIf { it.isNotBlank() }
                }
                .toList()
        } else {
            emptyList()
        }

        val dateParts = scopeOptions.dateScopeValues
            .asSequence()
            .mapNotNull { sanitizeFilePart(it).takeIf { part -> part.isNotBlank() } }
            .map { "d_$it" }
            .toList()

        val timeParts = scopeOptions.timeScopeValues
            .asSequence()
            .mapNotNull { sanitizeFilePart(it).takeIf { part -> part.isNotBlank() } }
            .map { "t_$it" }
            .toList()

        val phraseParts = scopeOptions.phraseScopeValues
            .asSequence()
            .mapNotNull { sanitizeFilePart(it).takeIf { part -> part.isNotBlank() } }
            .toList()

        val filenameDraftParts = scopeOptions.filenameDraftScopeValues
            .asSequence()
            .mapNotNull { sanitizeFilePart(it).takeIf { part -> part.isNotBlank() } }
            .map { "fd_$it" }
            .toList()

        // 핵심 정책:
        // - filename scope ON  -> slot + phrase/date/time
        // - filename scope OFF -> 파일명 기반 스코프 토큰은 전부 무시
        val scopedParts = buildList {
            addAll(filenameDraftParts)
            addAll(phraseParts)
            addAll(dateParts)
            addAll(timeParts)
        }

        val parts = buildList {
            if (includeFilenameInScope) {
                addAll(slotParts)
                addAll(scopedParts)
            } else {
                add("name=off")
            }
        }

        return if (parts.isEmpty()) "DZlog" else parts.joinToString(delim)
    }

    /**
     * 카운터 스트림용 prefix(단일 소스)
     *
     * - basePrefix: 슬롯/해결값 기반 prefix (COUNTER 제외)
     * - g2Enabled : "G2 그룹 셀 존재 여부" (값이 비어도 true)
     * - streamPrefix = computeCounterPrefix(...) + "|g2=0/1"
     *
     * 요구사항:
     * - 저장경로가 우연히 같아도(G2 값이 비어 g1 폴더만 쓰는 경우 등),
     *   "G2 그룹을 쓰는 템플릿"과 "G1만 쓰는 템플릿"의 카운팅 스트림은 분리되어야 한다.
     */
    fun computeCounterStreamPrefix(
        resolvedCells: List<ResolvedCell>,
        fnDelim: String,
        fileNameSlots: List<CellKey?>,
        includeFilenameInScope: Boolean,
        scopeOptions: CounterScopeOptions = CounterScopeOptions(),
    ): String {
        val basePrefix = computeCounterPrefix(
            resolvedCells = resolvedCells,
            fnDelim = fnDelim,
            fileNameSlots = fileNameSlots,
            includeFilenameInScope = includeFilenameInScope,
            scopeOptions = scopeOptions,
        )
        val g2Enabled = resolvedCells.any { it.raw?.groupLevel == GroupLevel.G2 }
        val tag = if (g2Enabled) "g2=1" else "g2=0"
        return "$basePrefix|$tag"
    }

    private fun toScanPath(relativePathKey: String): String {
        // relativePathKey may include virtual stream discriminator (e.g. "|g2=enabled_empty").
        // MediaStore path filtering must use physical folder path only.
        return relativePathKey.substringBefore("|g2=", relativePathKey)
    }

    private fun normalizeRelativePathPrefix(baseRel: String): String {
        val trimmed = baseRel.trim()
        if (trimmed.isBlank()) return ""
        val collapsed = trimmed.replace(Regex("/+"), "/")
        return if (collapsed.endsWith('/')) collapsed else "$collapsed/"
    }

    private fun appendOriginalDirectory(baseRel: String): String {
        val normalized = normalizeRelativePathPrefix(baseRel)
        return if (normalized.endsWith("original/")) normalized else "${normalized}original/"
    }

    /**
     * 해당 스트림(relativePath + counterPrefix)의 used counters를 MediaStore에서 로드한다.
     *
     * 정책(최종 truth):
     * - usedCounters는 MediaStore 실파일 스캔 결과만 사용한다.
     * - Room/CounterIndexRepository는 next 계산의 truth가 아니라 보조 기록(캐시) 용도로만 유지한다.
     * - 삭제된 파일이 DB에 남아 있어도 next 계산에는 개입하지 못한다.
     */
    suspend fun loadUsedCounters(
        context: Context,
        relativePath: String,
        counterPrefix: String,
        scanPrefix: String,
        counterDigits: Int,
        fnDelim: String,
        saveMode: SaveMode,
    ): Set<Int> {
        val repo = CounterIndexRepository.getInstance(context)

        // rawStreamPrefix는 내부 스트림 분리키로 유지한다.
        // MediaStore DISPLAY_NAME 파싱은 scanPrefix(실제 저장 파일명 prefix)만 사용한다.
        val rawStreamPrefix = counterPrefix
        // 주요 정책: stream prefix(내부 스코프 분리키)와 파일명 파싱 prefix를 분리한다.
        val physicalFileNamePrefix = scanPrefix
        val physicalPath = toScanPath(relativePath)
        val scanTarget = saveMode.toCounterScanTarget()
        val scanPaths = if (physicalPath.isBlank()) {
            emptyList()
        } else {
            buildScanPaths(physicalPath, scanTarget)
        }

        // 최종 truth: MediaStore 실파일 스캔 결과만 사용한다.
        // - stream 분리키(rawStreamPrefix)는 DB key/스코프 비교에만 쓰고,
        //   실파일 파싱은 scanPrefix(=physicalFileNamePrefix)로 수행한다.
        val usedCountersFromMediaStore: Set<Int> = runCatching {
            collectUsedCountersFromPaths(
                context = context,
                relativePathPrefixes = scanPaths,
                fileNamePrefix = physicalFileNamePrefix,
                counterDigits = counterDigits,
                fnDelim = fnDelim,
            )
        }.getOrDefault(emptySet())

        // DB는 보조 기록으로만 유지한다(읽기 truth로 사용 금지).
        // - replaceCounters: 삭제/undo 후에도 DB가 실파일 상태와 동일하게 따라오도록 정리
        // - 스캔 실패/빈 결과라도 DB fallback으로 승격하지 않는다.
        runCatching {
            repo.replaceCounters(relativePath, rawStreamPrefix, usedCountersFromMediaStore)
        }

        return usedCountersFromMediaStore
    }

    /**
     * 해당 스트림(relativePath + counterPrefix)의 다음 카운터를 MediaStore 기준으로 계산한다.
     *
     * 최종 규칙:
     * - existing counters가 비어 있으면 next = 1
     * - 아니면 next = max(existing) + 1
     * - hole fill은 수행하지 않는다.
     */
    suspend fun computeNext(
        context: Context,
        relativePath: String,
        counterPrefix: String,
        scanPrefix: String,
        counterDigits: Int,
        fnDelim: String,
        saveMode: SaveMode,
    ): Int {
        val used = loadUsedCounters(
            context = context,
            relativePath = relativePath,
            counterPrefix = counterPrefix,
            scanPrefix = scanPrefix,
            counterDigits = counterDigits,
            fnDelim = fnDelim,
            saveMode = saveMode,
        )
        val next = nextFromUsed(used)
        return next
    }

    // 정책 요약: next 계산은 used counters 집합에서 max+1만 사용하고, hole fill은 하지 않는다.
    internal fun nextFromUsed(existingCounters: Set<Int>): Int {
        return (existingCounters.maxOrNull() ?: 0) + 1
    }

    private fun collectUsedCountersFromPaths(
        context: Context,
        relativePathPrefixes: List<String>,
        fileNamePrefix: String,
        counterDigits: Int,
        fnDelim: String,
    ): Set<Int> {
        if (relativePathPrefixes.isEmpty()) return emptySet()

        return relativePathPrefixes
            .asSequence()
            .filter { it.isNotBlank() }
            .flatMap { rel ->
                scanUsedCounters(
                    context = context,
                    relativePathPrefix = rel,
                    fileNamePrefix = fileNamePrefix,
                    counterDigits = counterDigits,
                    fnDelim = fnDelim,
                ).asSequence()
            }
            .toSet()
    }
}
