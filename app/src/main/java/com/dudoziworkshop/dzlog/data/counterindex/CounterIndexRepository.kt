package com.dudoziworkshop.dzlog.data.counterindex

import android.content.Context
import com.dudoziworkshop.dzlog.data.log.LogDatabase

/**
 * CounterIndex 접근 레이어.
 * - 목적: 중복 체크 / MAX+1 리셋 / 촬영 시점 기록
 * - 파일명/워터마크는 수정하지 않는다. (내부 논리 카운터만 관리)
 */
class CounterIndexRepository private constructor(
    private val dao: CounterIndexDao
) {

    suspend fun getUsedCounters(relativePath: String, prefix: String): Set<Int> =
        dao.listCountersByPath(relativePath, prefix).toSet()

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
    }

    /**
     * 특정 스트림 카운터를 현재 스캔 결과로 완전 동기화한다.
     * - 기존 레코드를 모두 제거 후 placeholders로 재적재
     * - 파일 삭제 이후 stale 인덱스가 남아 next가 커지는 문제를 방지
     */
    suspend fun replaceCounters(relativePath: String, prefix: String, counters: Set<Int>) {
        if (relativePath.isBlank()) return
        if (prefix.isBlank()) return

        dao.deleteByPath(relativePath, prefix)
        backfillPlaceholders(relativePath, prefix, counters)
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
