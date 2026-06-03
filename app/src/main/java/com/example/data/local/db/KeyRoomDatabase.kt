package com.example.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RecentActivity::class, BackupRecord::class, SavedQuery::class],
    version = 1,
    exportSchema = false
)
abstract class KeyRoomDatabase : RoomDatabase() {
    abstract fun recentActivityDao(): RecentActivityDao
    abstract fun backupRecordDao(): BackupRecordDao
    abstract fun savedQueryDao(): SavedQueryDao

    companion object {
        @Volatile
        private var INSTANCE: KeyRoomDatabase? = null

        fun getDatabase(context: Context): KeyRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KeyRoomDatabase::class.java,
                    "keyroom_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
