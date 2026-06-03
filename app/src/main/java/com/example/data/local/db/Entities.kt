package com.example.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_activities")
data class RecentActivity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS",
    val description: String = ""
)

@Entity(tableName = "backup_records")
data class BackupRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // FULL, PREFERENCES, DATABASE
    val size: Long,
    val filePath: String,
    val timestamp: Long = System.currentTimeMillis(),
    val itemCount: Int = 0,
    val isVerified: Boolean = true
)

@Entity(tableName = "saved_queries")
data class SavedQuery(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val sqlText: String,
    val timestamp: Long = System.currentTimeMillis()
)
