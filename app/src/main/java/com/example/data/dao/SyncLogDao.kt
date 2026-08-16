package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SyncLog
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SyncLog): Long

    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 250")
    fun getAllLogs(): Flow<List<SyncLog>>

    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT 250")
    suspend fun getAllLogsDirect(): List<SyncLog>

    @Query("SELECT * FROM sync_logs WHERE level = 'ERROR' ORDER BY timestamp DESC LIMIT 100")
    fun getErrorLogs(): Flow<List<SyncLog>>

    @Query("SELECT * FROM sync_logs WHERE category = :category ORDER BY timestamp DESC LIMIT 150")
    fun getLogsByCategory(category: String): Flow<List<SyncLog>>

    @Query("SELECT COUNT(*) FROM sync_logs WHERE level = 'ERROR'")
    fun getErrorCount(): Flow<Int>

    @Query("DELETE FROM sync_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM sync_logs WHERE id NOT IN (SELECT id FROM sync_logs ORDER BY timestamp DESC LIMIT 300)")
    suspend fun trimOldLogs()
}
