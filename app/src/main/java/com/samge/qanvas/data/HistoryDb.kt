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
import kotlinx.coroutines.flow.Flow

/**
 * One finished generation (t2i / edit / sticker).
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
)

@Dao
interface GenDao {
    @Insert
    suspend fun insert(rec: GenRecord): Long

    @Query("SELECT * FROM generations ORDER BY createdAt DESC LIMIT 200")
    fun recent(): Flow<List<GenRecord>>

    @Query("DELETE FROM generations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM generations")
    suspend fun count(): Int
}

@Database(entities = [GenRecord::class], version = 1, exportSchema = false)
abstract class HistoryDb : RoomDatabase() {
    abstract fun dao(): GenDao

    companion object {
        fun build(context: Context): HistoryDb =
            Room.databaseBuilder(context, HistoryDb::class.java, "qanvas.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
