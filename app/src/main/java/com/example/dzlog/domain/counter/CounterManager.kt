package com.example.dzlog.domain.counter

import android.content.Context
import com.example.dzlog.data.counter.scanUsedCountersFromMediaStore
import com.example.dzlog.data.counterindex.CounterIndexRepository
import com.example.dzlog.domain.naming.buildFileNamePrefixFromResolvedCells
import com.example.dzlog.domain.table.ResolvedCell
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
        return buildFileNamePrefixFromResolvedCells(
            resolvedCells = resolvedCells,
            fnDelim = fnDelim,
            includeDate = false,
            includeTime = false,
            now = Date()
        )
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

        // 1) Room 기준 조회
        val fromDb: Set<Int> = runCatching {
            repo.getUsedCounters(relativePath, counterPrefix)
        }.getOrDefault(emptySet())

        if (fromDb.isNotEmpty()) return fromDb

        // 2) MediaStore 스캔 fallback
        val scanned: Set<Int> = runCatching {
            scanUsedCountersFromMediaStore(
                context = context,
                relativePathPrefix = relativePath,
                fileNamePrefix = counterPrefix,
                counterDigits = counterDigits,
                fnDelim = fnDelim
            )
        }.getOrDefault(emptySet())

        // 3) Room backfill (placeholder)
        runCatching {
            repo.backfillPlaceholders(relativePath, counterPrefix, scanned)
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
