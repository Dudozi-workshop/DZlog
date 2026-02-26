package com.dudoziworkshop.dzlog.domain.counter

import android.content.Context
import com.dudoziworkshop.dzlog.data.counter.scanUsedCountersFromMediaStore
import com.dudoziworkshop.dzlog.data.counterindex.CounterIndexRepository
import com.dudoziworkshop.dzlog.domain.model.CellKey
import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
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
        // Virtual stream key: distinguish empty-enabled G2 from true G1-only.
        return "$baseRelativePath|g2=enabled_empty"
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
        fnDelim: String,
        fileNameSlots: List<CellKey?>,
        includeFilenameInScope: Boolean,
    ): String {
        if (!includeFilenameInScope) return "name=off"

        val delim = fnDelim.ifBlank { "_" }
        val normalizedSlots = fileNameSlots.take(FILE_NAME_SLOT_COUNT) + List((FILE_NAME_SLOT_COUNT - fileNameSlots.size).coerceAtLeast(0)) { null }

        val parts = normalizedSlots
            .asSequence()
            .mapNotNull { slot -> slot?.let { key -> resolvedCells.firstOrNull { rc -> rc.id == key } } }
            .mapNotNull { rc ->
                when (rc.type) {
                    TableCellDataType.COUNTER,
                    TableCellDataType.DATE,
                    TableCellDataType.TIME -> null
                    else -> sanitizeFilePart(rc.resolvedText)
                }?.takeIf { it.isNotBlank() }
            }
            .toList()

        return if (parts.isEmpty()) "DZlog" else parts.joinToString(delim)
    }

    /**
     * 카운터 스트림용 prefix(단일 소스)
     *
     * - basePrefix: 슬롯/해결값 기반 prefix (COUNTER 제외)
     * - g2Enabled : "G2 그룹 셀 존재 여부" (값이 비어도 true)
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
    ): String {
        val basePrefix = computeCounterPrefix(
            resolvedCells = resolvedCells,
            fnDelim = fnDelim,
            fileNameSlots = fileNameSlots,
            includeFilenameInScope = includeFilenameInScope
        )
        val g2Enabled = resolvedCells.any { it.raw?.groupLevel == GroupLevel.G2 }
        val tag = if (g2Enabled) "g2=1" else "g2=0"
        return "$basePrefix|$tag"
    }

    private fun basePrefixFromStreamPrefix(streamPrefix: String): String {
        return streamPrefix.substringBefore("|g2=", streamPrefix)
    }


    private fun physicalRelativePathFromStreamKey(relativePathKey: String): String {
        // relativePathKey may include virtual stream discriminator (e.g. "|g2=enabled_empty").
        // MediaStore path filtering must use physical folder path only.
        return relativePathKey.substringBefore("|g2=", relativePathKey)
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
        val physicalRelativePath = physicalRelativePathFromStreamKey(relativePath)

        // 1) Room 기준 조회 (현재 streamPrefix)
        val fromDb: Set<Int> = runCatching {
            repo.getUsedCounters(relativePath, streamPrefix)
        }.getOrDefault(emptySet())

        if (fromDb.isNotEmpty()) {
            val scannedFromMediaStore: Set<Int> = runCatching {
                scanUsedCountersFromMediaStore(
                    context = context,
                    relativePathPrefix = physicalRelativePath,
                    fileNamePrefix = basePrefix,
                    counterDigits = counterDigits,
                    fnDelim = fnDelim
                )
            }.getOrDefault(fromDb)

            // 파일 삭제 등으로 DB 인덱스가 실제 보유 파일과 달라진 경우 현재 파일 기준으로 재동기화
            if (scannedFromMediaStore != fromDb) {
                runCatching { repo.replaceCounters(relativePath, streamPrefix, scannedFromMediaStore) }
            }
            return scannedFromMediaStore
        }

        // 2) MediaStore 스캔 fallback
        // - 실제 파일명 prefix에는 stream 구분 태그(|g2=0/1)가 포함되지 않는다.
        // - 따라서 스캔은 항상 basePrefix로 수행해야 한다.
        //   (streamPrefix로 스캔하면 파일이 있어도 못 찾고 next=1로 되돌아갈 수 있음)
        val scanned: Set<Int> = runCatching {
            scanUsedCountersFromMediaStore(
                context = context,
                relativePathPrefix = physicalRelativePath,
                fileNamePrefix = basePrefix,
                counterDigits = counterDigits,
                fnDelim = fnDelim
            )
        }.getOrDefault(emptySet())

        // 3) Room backfill
        runCatching {
            repo.backfillPlaceholders(relativePath, streamPrefix, scanned)
        }

        return scanned
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
