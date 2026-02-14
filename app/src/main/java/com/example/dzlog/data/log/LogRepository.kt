package com.example.dzlog.data.log

import android.content.Context
import android.provider.MediaStore
import androidx.core.net.toUri

class LogRepository private constructor(
    private val logDao: LogDao
) {
    suspend fun insert(log: LogEntity): Long = logDao.insert(log)

    suspend fun listAll(): List<LogEntity> = logDao.listAll()

    suspend fun getById(id: Long): LogEntity? = logDao.getById(id)

    suspend fun getLatest(): LogEntity? = logDao.getLatest()

    suspend fun deleteById(id: Long) {
        logDao.deleteById(id)
    }

    /**
     * 로그  이미지(MediaStore) 동시 삭제.
     *
     * 정책(로그 안정성):
     * - URI가 유효하고, 이미지가 실제로 존재하면 -> 이미지 삭제 성공 시에만 DB 로그 삭제
     * - URI가 유효하지만 이미지가 이미 없으면(갤러리에서 수동 삭제 등) -> DB 로그만 정리(성공으로 처리)
     * - URI 파싱 실패/권한 문제 등으로 존재 여부 확인도 불가하면 -> 실패 반환(로그 유지)
     */
    suspend fun deleteLogWithImage(context: Context, log: LogEntity): Boolean {
        val uri = runCatching { log.imageUri.toUri() }.getOrNull() ?: return false
        val resolver = context.contentResolver

        val exists = runCatching {
            resolver.query(
                uri,
                arrayOf(MediaStore.Images.Media._ID),
                null,
                null,
                null
            )?.use { c -> c.moveToFirst() } ?: false
        }.getOrNull()

        // 존재 여부 판단 자체가 불가하면(권한/URI 문제) 안전하게 실패 처리
        if (exists == null) return false

        // 이미 이미지가 없는 상태면 DB 로그만 정리
        if (!exists) {
            logDao.deleteById(log.id)
            return true
        }

        val deleted = runCatching {
            resolver.delete(uri, null, null)
        }.getOrDefault(0)

        if (deleted > 0) {
            logDao.deleteById(log.id)
            return true
        }
        return false
    }

    companion object {
        @Volatile
        private var INSTANCE: LogRepository? = null

        fun getInstance(context: Context): LogRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LogRepository(LogDatabase.getInstance(context).logDao()).also {
                    INSTANCE = it
                }
            }
        }
    }
}
