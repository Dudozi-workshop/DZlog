package com.dudoziworkshop.dzlog.data.counterindex

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 경로(relativePath)별 카운터 사용 이력 인덱스.
 *
 * 목적:
 * - 경로별 유일성(중복) 체크
 * - MAX+1 리셋 계산
 *
 * 주의:
 * - 파일명/워터마크를 수정하지 않는다. (내부 논리 카운터만 관리)
 */
@Entity(
    tableName = "counter_index",
    indices = [
        Index(value = ["relativePath", "prefix", "counterValue"], unique = true),
        Index(value = ["relativePath", "prefix"]),
        Index(value = ["mediaId"])
    ]
)
data class CounterIndexEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val mediaId: Long,
    val relativePath: String,
    val prefix: String,
    val counterValue: Int,
    val dateAddedSeconds: Long
)
