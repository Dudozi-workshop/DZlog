package com.dudoziworkshop.dzlog.data.counterindex

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 경로(relativePath)/prefix별 카운터 사용 기록 엔티티.
 *
 * 목적:
 * - 촬영 결과의 counter 사용 이력을 중복 없이 저장한다.
 * - CounterIndexRepository의 보조 기록(placeholder 포함) 동기화에 사용한다.
 *
 * 주의:
 * - 다음 counter 계산은 MediaStore 실파일 스캔으로 수행되며,
 *   이 엔티티는 그 결과를 보조 기록으로 유지하는 용도다.
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
