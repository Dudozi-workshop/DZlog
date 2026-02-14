package com.example.dzlog.data.log

import android.content.Context

class LogRepository private constructor(
    private val logDao: LogDao
) {
    suspend fun insert(log: LogEntity): Long = logDao.insert(log)

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
