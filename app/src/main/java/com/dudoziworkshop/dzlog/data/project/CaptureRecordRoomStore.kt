package com.dudoziworkshop.dzlog.data.project

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.withTransaction
import com.dudoziworkshop.dzlog.domain.project.CaptureRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Independent, indexed capture index. Do not migrate dzlog.db or overwrite the
 * version-1 DataStore project/URI groups as part of initializing this database.
 */
@Database(entities = [CaptureRecordEntity::class], version = 1, exportSchema = true)
internal abstract class CaptureRecordRoomDatabase : RoomDatabase() {
    abstract fun captureRecordDao(): CaptureRecordDao

    companion object {
        @Volatile private var instance: CaptureRecordRoomDatabase? = null

        fun get(context: Context): CaptureRecordRoomDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CaptureRecordRoomDatabase::class.java,
                    "dzlog_capture_records.db",
                ).build().also { instance = it }
            }
    }
}

/**
 * A committed capture is immutable until a *verified* media deletion occurs.
 * Room transactions prevent duplicate or cross-record URI ownership during
 * overlapping completion callbacks, without reserializing a project-wide JSON.
 */
internal class CaptureRecordRoomStore(private val db: CaptureRecordRoomDatabase) {
    constructor(context: Context) : this(CaptureRecordRoomDatabase.get(context))

    /**
     * Returns true on first durable insert; false on exact replay.
     * Conflicting reuse of capture ID or media URI fails closed.
     */
    suspend fun appendCommitted(record: CaptureRecord): Boolean = db.withTransaction {
        val dao = db.captureRecordDao()
        val existing = dao.findById(record.captureId)
        if (existing != null) {
            require(existing.toDomain() == record) { "Conflicting capture receipt" }
            return@withTransaction false
        }
        record.mediaUris.forEach { uri ->
            check(dao.findByMediaUri(uri) == null) {
                "Media URI already belongs to another capture"
            }
        }
        dao.insert(CaptureRecordEntity.from(record))
        true
    }

    suspend fun findByMediaUri(uri: String): CaptureRecord? =
        db.captureRecordDao().findByMediaUri(uri)?.toDomain()

    suspend fun findById(captureId: String): CaptureRecord? =
        db.captureRecordDao().findById(captureId)?.toDomain()

    suspend fun forProject(projectId: String): List<CaptureRecord> =
        db.captureRecordDao().findForProject(projectId).map(CaptureRecordEntity::toDomain)

    fun observeProject(projectId: String): Flow<List<CaptureRecord>> =
        db.captureRecordDao().observeForProject(projectId).map { rows ->
            rows.map(CaptureRecordEntity::toDomain)
        }

    suspend fun countForProject(projectId: String): Int =
        db.captureRecordDao().countForProject(projectId)

    suspend fun latestForProject(projectId: String): CaptureRecord? =
        db.captureRecordDao().latestForProject(projectId)?.toDomain()

    /**
     * The caller supplies actual, successfully deleted URIs (not requested
     * targets). A surviving original/result keeps its logical capture record.
     */
    suspend fun afterVerifiedDeletion(deletedUris: Set<String>): Int {
        if (deletedUris.isEmpty()) return 0
        return db.withTransaction {
            val dao = db.captureRecordDao()
            val affected = dao.findUsingMediaUris(deletedUris.toList())
            affected.forEach { row ->
                val updated = row.toDomain().afterVerifiedDeletion(deletedUris)
                if (updated == null) dao.deleteById(row.captureId)
                else dao.update(CaptureRecordEntity.from(updated))
            }
            affected.size
        }
    }
}
