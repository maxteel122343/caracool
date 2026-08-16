package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.FeedDao
import com.example.data.dao.SettingsDao
import com.example.data.dao.SyncLogDao
import com.example.data.dao.UnlockDao
import com.example.data.model.AppSettings
import com.example.data.model.FeedComment
import com.example.data.model.FeedPost
import com.example.data.model.SyncLog
import com.example.data.model.UnlockLog

@Database(
    entities = [
        UnlockLog::class,
        AppSettings::class,
        FeedPost::class,
        FeedComment::class,
        SyncLog::class
    ],
    version = 10,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun unlockDao(): UnlockDao
    abstract fun settingsDao(): SettingsDao
    abstract fun feedDao(): FeedDao
    abstract fun syncLogDao(): SyncLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cara_de_pacoca_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

