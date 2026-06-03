package com.example.data.local.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecentActivityDao {
    @Query("SELECT * FROM recent_activities ORDER BY timestamp DESC LIMIT 50")
    fun getRecentActivities(): Flow<List<RecentActivity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: RecentActivity)

    @Query("DELETE FROM recent_activities")
    suspend fun clearActivities()
}

@Dao
interface BackupRecordDao {
    @Query("SELECT * FROM backup_records ORDER BY timestamp DESC")
    fun getAllBackups(): Flow<List<BackupRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBackup(backup: BackupRecord)

    @Delete
    suspend fun deleteBackup(backup: BackupRecord)

    @Query("DELETE FROM backup_records WHERE id = :id")
    suspend fun deleteBackupById(id: Int)
}

@Dao
interface SavedQueryDao {
    @Query("SELECT * FROM saved_queries ORDER BY timestamp DESC")
    fun getAllQueries(): Flow<List<SavedQuery>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuery(query: SavedQuery)

    @Delete
    suspend fun deleteQuery(query: SavedQuery)
}
