package com.dudoziworkshop.dzlog.data.project

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal interface CaptureRecordDao {
    @Query("SELECT * FROM capture_records WHERE captureId = :captureId LIMIT 1")
    suspend fun findById(captureId: String): CaptureRecordEntity?

    @Query("SELECT * FROM capture_records WHERE originalUri = :uri OR resultUri = :uri LIMIT 1")
    suspend fun findByMediaUri(uri: String): CaptureRecordEntity?

    @Query(
        "SELECT * FROM capture_records " +
            "WHERE originalUri IN (:uris) OR resultUri IN (:uris)"
    )
    suspend fun findUsingMediaUris(uris: List<String>): List<CaptureRecordEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CaptureRecordEntity)

    @Update
    suspend fun update(entity: CaptureRecordEntity)

    @Query("DELETE FROM capture_records WHERE captureId = :captureId")
    suspend fun deleteById(captureId: String)

    @Query(
        "SELECT * FROM capture_records WHERE projectId = :projectId " +
            "ORDER BY capturedAtMillis DESC, captureId DESC"
    )
    suspend fun findForProject(projectId: String): List<CaptureRecordEntity>

    @Query(
        "SELECT * FROM capture_records WHERE projectId = :projectId " +
            "ORDER BY capturedAtMillis DESC, captureId DESC"
    )
    fun observeForProject(projectId: String): Flow<List<CaptureRecordEntity>>

    @Query("SELECT COUNT(*) FROM capture_records WHERE projectId = :projectId")
    suspend fun countForProject(projectId: String): Int

    @Query(
        "SELECT * FROM capture_records WHERE projectId = :projectId " +
            "ORDER BY capturedAtMillis DESC, captureId DESC LIMIT 1"
    )
    suspend fun latestForProject(projectId: String): CaptureRecordEntity?
}
