package com.dudoziworkshop.dzlog.data.counterindex

import android.content.Context
import com.dudoziworkshop.dzlog.data.log.LogDatabase

/**
 * CounterIndex 접근 레이어.
 * - 목적: 촬영 시점 카운터 사용 기록(record)과 placeholder 동기화(replace/backfill)
 * - 파일명/워터마크는 수정하지 않는다. (내부 논리 카운터 기록만 관리)
 *
 * 정책:
 * - 다음 counter 계산은 MediaStore 실파일 스캔(max+1)으로 수행된다.
 * - 이 저장소는 보조 기록/캐시 용도이며, next 계산 근거로 직접 사용하지 않는다.
 */
class CounterIndexRepository private constructor(
    private val dao: CounterIndexDao
) {

    suspend fun getCommittedCounters(relativePath: String, prefix: String): Set<Int> =
        dao.listCommittedCountersByPath(relativePath, prefix).toSet()

    /**
     * 촬영 시점 기록.
     * - 중복이면 무시(이미 사용된 카운터라는 의미)
     */
    suspend fun record(
        relativePath: String,
        prefix: String,
        mediaId: Long,
        counterValue: Int,
        dateAddedSeconds: Long
    ) {
        if (relativePath.isBlank()) return
        if (prefix.isBlank()) return
        if (mediaId <= 0L) return
        if (counterValue < 0) return
        dao.insertIgnore(
            CounterIndexEntity(
                mediaId = mediaId,
                relativePath = relativePath,
                prefix = prefix,
                counterValue = counterValue,
                dateAddedSeconds = dateAddedSeconds
            )
        )

        // 동일 스트림/카운터로 commit된 실제 파일이 생기면 placeholder(-1)는 즉시 제거한다.
        // 그렇지 않으면 UNIQUE(relativePath,prefix,counterValue)에 막혀 다음 commit 기록이 유실될 수 있다.
        dao.deletePlaceholderByCounter(relativePath, prefix, counterValue)
    }

    /**
     * 특정 스트림 카운터 보조 기록을 현재 스캔 결과에 맞춰 동기화한다.
     * - committed 레코드는 보존하고 placeholder 집합만 갱신한다.
     * - MediaStore 스캔 결과를 보조 기록으로 반영하는 목적이며,
     *   next 계산 근거를 대체하지 않는다.
     */
    suspend fun replaceCounters(relativePath: String, prefix: String, counters: Set<Int>) {
        if (relativePath.isBlank()) return
        if (prefix.isBlank()) return

        // 실제 commit 레코드(mediaId>0)는 보존하고 placeholder만 스캔 결과로 재동기화한다.
        // 전체 삭제를 하면 commit 직후 누적 used counter가 축소될 수 있다.
        val committed = getCommittedCounters(relativePath, prefix)
        val merged = committed + counters

        dao.deletePlaceholdersByPath(relativePath, prefix)
        backfillPlaceholders(relativePath, prefix, merged)
    }

    suspend fun backfillPlaceholders(relativePath: String, prefix: String, counters: Set<Int>) {
        if (relativePath.isBlank()) return
        if (prefix.isBlank()) return
        if (counters.isEmpty()) return

        val nowSec = System.currentTimeMillis() / 1000L
        counters.forEach { c ->
            if (c < 0) return@forEach
            dao.insertIgnore(
                CounterIndexEntity(
                    mediaId = -1L,
                    relativePath = relativePath,
                    prefix = prefix,
                    counterValue = c,
                    dateAddedSeconds = nowSec
                )
            )
        }
    }

    companion object {
        @Volatile private var INSTANCE: CounterIndexRepository? = null

        fun getInstance(context: Context): CounterIndexRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: CounterIndexRepository(
                    LogDatabase.getInstance(context).counterIndexDao()
                ).also { INSTANCE = it }
            }
    }
}
