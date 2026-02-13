package com.example.dzlog.data.counterindex

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CounterIndexDao {

    /** 캡처 시점 기록. (중복이면 무시) */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: CounterIndexEntity): Long

    /** 경로별 사용된 카운터 목록 */
    @Query("SELECT counterValue FROM counter_index WHERE relativePath = :relativePath AND prefix = :prefix")
    suspend fun listCountersByPath(relativePath: String, prefix: String): List<Int>

    /** 경로별 최대 카운터 */
    @Query("SELECT MAX(counterValue) FROM counter_index WHERE relativePath = :relativePath AND prefix = :prefix")
    suspend fun maxCounterByPath(relativePath: String, prefix: String): Int?

    /** 중복 여부 */
    @Query(
        "SELECT EXISTS(SELECT 1 FROM counter_index WHERE relativePath = :relativePath AND prefix = :prefix AND counterValue = :counterValue)"
    )
    suspend fun existsCounter(relativePath: String, prefix: String, counterValue: Int): Boolean

    /** mediaId 기반 정리(삭제/동기화) */
    @Query("DELETE FROM counter_index WHERE mediaId = :mediaId")
    suspend fun deleteByMediaId(mediaId: Long)

    /** 특정 경로 placeholder(mediaId=-1) 정리용 */
    @Query("DELETE FROM counter_index WHERE relativePath = :relativePath AND prefix = :prefix AND mediaId = -1")
    suspend fun deletePlaceholdersByPath(relativePath: String, prefix: String)

    /** 특정 스트림 전체 정리용(미디어 삭제 동기화 시 재구축) */
    @Query("DELETE FROM counter_index WHERE relativePath = :relativePath AND prefix = :prefix")
    suspend fun deleteByPath(relativePath: String, prefix: String)
}
