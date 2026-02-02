package com.example.dzlog.data.log

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: LogEntity): Long

    @Query("SELECT * FROM logs ORDER BY createdAt DESC")
    suspend fun listAll(): List<LogEntity>

    @Query("SELECT * FROM logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): LogEntity?

    @Query("SELECT * FROM logs ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatest(): LogEntity?

    @Query("DELETE FROM logs WHERE id = :id")
    suspend fun deleteById(id: Long)
}
