package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "astro_logs")
data class AstroLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val filePath: String,
    val date: String,
    val target: String,
    val iso: Int,
    val shutterSpeed: String, // e.g. "10s", "1/1000s", "30s"
    val bortleScale: Int, // 1 (excellent dark sky) to 9 (city)
    val notes: String,
    val rating: Int // 1 to 5
)

@Dao
interface AstroLogDao {
    @Query("SELECT * FROM astro_logs ORDER BY id DESC")
    fun getAllLogs(): Flow<List<AstroLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AstroLog)

    @Delete
    suspend fun deleteLog(log: AstroLog)
}

@Database(entities = [AstroLog::class], version = 1, exportSchema = false)
abstract class AstroDatabase : RoomDatabase() {
    abstract fun astroLogDao(): AstroLogDao
}
