package com.example.dzlog.data.log

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.dzlog.data.counterindex.CounterIndexDao
import com.example.dzlog.data.counterindex.CounterIndexEntity

@Database(entities = [LogEntity::class, CounterIndexEntity::class], version = 3)
abstract class LogDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao
    abstract fun counterIndexDao(): CounterIndexDao

    companion object {
        @Volatile
        private var INSTANCE: LogDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS counter_index (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        mediaId INTEGER NOT NULL,
                        relativePath TEXT NOT NULL,
                        counterValue INTEGER NOT NULL,
                        dateAddedSeconds INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_counter_index_relativePath_counterValue ON counter_index(relativePath, counterValue)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_counter_index_relativePath ON counter_index(relativePath)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_counter_index_mediaId ON counter_index(mediaId)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE counter_index ADD COLUMN prefix TEXT NOT NULL DEFAULT ''")
                db.execSQL("DROP INDEX IF EXISTS index_counter_index_relativePath_counterValue")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_counter_index_relativePath_prefix_counterValue ON counter_index(relativePath, prefix, counterValue)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_counter_index_relativePath_prefix ON counter_index(relativePath, prefix)"
                )
            }
        }

        fun getInstance(context: Context): LogDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LogDatabase::class.java,
                    "dzlog.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
