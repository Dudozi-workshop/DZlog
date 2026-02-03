package com.example.dzlog.data.counterindex

import android.content.Context
import com.example.dzlog.data.log.LogDatabase

/**
 * CounterIndex 접근 레이어.
 * - 목적: 중복 체크 / MAX+1 리셋 / 촬영 시점 기록
 * - 파일명/워터마크는 수정하지 않는다. (내부 논리 카운터만 관리)
 */
class CounterIndexRepository private constructor(
    private val dao: CounterIndexDao
) {

    suspend fun getUsedCounters(relativePath: String): Set<Int> =
        dao.listCountersByPath(relativePath).toSet()

    suspend fun getMaxCounter(relativePath: String): Int =
        dao.maxCounterByPath(relativePath) ?: 0

    suspend fun isDuplicate(relativePath: String, counterValue: Int): Boolean =
        dao.existsCounter(relativePath, counterValue)

    /**
     * 촬영 시점 기록.
     * - 중복이면 무시(이미 사용된 카운터라는 의미)
     */
    suspend fun record(relativePath: String, mediaId: Long, counterValue: Int, dateAddedSeconds: Long) {
        if (relativePath.isBlank()) return
        if (mediaId <= 0L) return
        if (counterValue < 0) return
        dao.insertIgnore(
            CounterIndexEntity(
                mediaId = mediaId,
                relativePath = relativePath,
                counterValue = counterValue,
                dateAddedSeconds = dateAddedSeconds
            )
        )
    }

    /**
     * 기존 사진 백필(backfill)용.
     * - mediaId를 모르거나 굳이 저장할 필요가 없을 때 mediaId=-1로 예약해둔다.
     */
    suspend fun backfillPlaceholders(relativePath: String, counters: Set<Int>) {
        if (relativePath.isBlank()) return
        if (counters.isEmpty()) return

        val nowSec = System.currentTimeMillis() / 1000L
        counters.forEach { c ->
            if (c < 0) return@forEach
            dao.insertIgnore(
                CounterIndexEntity(
                    mediaId = -1L,
                    relativePath = relativePath,
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
