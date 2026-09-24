package com.samge.qanvas.data

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

/**
 * One finished generation (t2i / edit / sticker).
 * v2 adds the timing breakdown (start/model load/generation/end).
 */
@Entity(tableName = "generations")
data class GenRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prompt: String,
    /** t2i | edit | sticker */
    val mode: String,
    val width: Int,
    val height: Int,
    val steps: Int,
    val seed: Long,
    /** output PNG path (app external files dir) */
    val outPath: String,
    /** optional input image path for edit mode */
    val inPath: String? = null,
    val durationMs: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    // ---- v2: timing breakdown ----
    /** epoch ms when the job started (== createdAt) */
    val startAt: Long = 0,
    /** ms spent loading the model stages before the first denoise step */
    val modelLoadMs: Long = 0,
    /** ms from first denoise progress to completion (DiT steps + VAE) */
    val genMs: Long = 0,
    /** epoch ms when the job finished */
    val endAt: Long = 0,
    /** aspect ratio ordinal used (for share-card reproduction) */
    val ratio: Int = 0,
    /** tier ordinal used */
    val tier: Int = 1,
)

@Dao
interface GenDao {
    @Insert
    suspend fun insert(rec: GenRecord): Long

    @Query("SELECT * FROM generations ORDER BY createdAt DESC LIMIT 200")
    fun recent(): Flow<List<GenRecord>>

    @Query("SELECT * FROM generations WHERE id = :id")
    suspend fun byId(id: Long): GenRecord?

    @Query("DELETE FROM generations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM generations")
    suspend fun count(): Int
}

@Database(entities = [GenRecord::class], version = 2, exportSchema = false)
abstract class HistoryDb : RoomDatabase() {
    abstract fun dao(): GenDao

    companion object {
        private val MIGRATE_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                listOf("startAt", "modelLoadMs", "genMs", "endAt", "ratio", "tier").forEach { col ->
                    val type = if (col == "startAt" || col == "endAt") "INTEGER" else "INTEGER"
                    db.execSQL("ALTER TABLE generations ADD COLUMN $col $type NOT NULL DEFAULT 0")
                }
            }
        }

        fun build(context: Context): HistoryDb =
            Room.databaseBuilder(context, HistoryDb::class.java, "qanvas.db")
                .addMigrations(MIGRATE_1_2)
                .build()
    }
}
