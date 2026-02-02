package com.example.dzlog.data.log

import android.content.Context
import android.net.Uri

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

    suspend fun deleteLogWithImage(context: Context, log: LogEntity): Boolean {
        val uri = runCatching { Uri.parse(log.imageUri) }.getOrNull() ?: return false
        val deleted = runCatching {
            context.contentResolver.delete(uri, null, null)
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
