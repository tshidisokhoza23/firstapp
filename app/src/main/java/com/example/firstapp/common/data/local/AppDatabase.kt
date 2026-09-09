package com.example.firstapp.common.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.firstapp.chat.data.local.dao.*
import com.example.firstapp.chat.data.local.entity.*
import com.example.firstapp.media.data.local.dao.*
import com.example.firstapp.media.data.local.entity.*
import com.example.firstapp.chat.jobs.data.local.dao.*
import com.example.firstapp.chat.jobs.data.local.entity.*
import com.example.firstapp.drive.data.local.dao.*
import com.example.firstapp.drive.data.local.entity.*
import com.example.firstapp.music.data.local.dao.SongDao
import com.example.firstapp.music.data.local.dao.PlaylistDao
import com.example.firstapp.music.data.local.entity.*
import com.example.firstapp.vocab.data.local.dao.UserDao
import com.example.firstapp.vocab.data.local.dao.LessonProgressDao
import com.example.firstapp.vocab.data.local.entity.UserEntity
import com.example.firstapp.vocab.data.local.entity.LessonProgressEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        ChatEntity::class, 
        MessageEntity::class, 
        SongEntity::class, 
        JobEntity::class,
        AdvertEntity::class,
        ChannelEntity::class,
        PostEntity::class,
        StoryEntity::class,
        CallLogEntity::class,
        ContactEntity::class,
        JobApplicationEntity::class,
        RideOptionEntity::class,
        DestinationEntity::class,
        UserEntity::class,
        LessonProgressEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        RecentlyPlayedEntity::class,
        PostRemoteKeys::class,
        MessageFtsEntity::class,
        SyncCursorEntity::class
    ], 
    version = 63,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun songDao(): SongDao
    abstract fun jobDao(): JobDao
    abstract fun advertDao(): AdvertDao
    abstract fun channelDao(): ChannelDao
    abstract fun postDao(): PostDao
    abstract fun storyDao(): StoryDao
    abstract fun callLogDao(): CallLogDao
    abstract fun contactDao(): ContactDao
    abstract fun jobApplicationDao(): JobApplicationDao
    abstract fun driveDao(): DriveDao
    abstract fun userDao(): UserDao
    abstract fun lessonProgressDao(): LessonProgressDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun postRemoteKeysDao(): PostRemoteKeysDao
    abstract fun syncCursorDao(): SyncCursorDao

    fun clearDatabase() {
        clearAllTables()
    }

    companion object {
        private const val BASE_DATABASE_NAME = "firstapp_v9_encrypted"

        @Volatile
        private var instance: AppDatabase? = null
        private var currentUserId: String? = null

        @Synchronized
        fun getInstance(context: android.content.Context, sessionManager: com.example.firstapp.common.domain.auth.SessionManager): AppDatabase {
            val userId = sessionManager.getUserId() ?: "anonymous"
            
            // If switching users or instance is closed, close and reset
            if (instance != null && (currentUserId != userId || instance?.isOpen != true)) {
                try {
                    android.util.Log.d("AppDatabase", "Closing old database instance for user $currentUserId (switching to $userId)")
                    instance?.close()
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error closing old instance", e)
                }
                instance = null
            }

            val current = instance
            if (current != null && current.isOpen) {
                return current
            }
            
            val dbName = getDatabaseNameForUser(userId)
            currentUserId = userId

            val db = try {
                val d = buildDatabase(context, sessionManager, dbName)
                // Force open to verify encryption key. SQLCipher will throw here if key is wrong.
                d.openHelper.writableDatabase
                d
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Database open failed ($dbName). Encryption key might have changed or file is corrupted. Recreating...", e)
                
                // Close the broken instance if it exists
                instance?.close()
                instance = null

                deleteUserDatabase(context, userId)
                
                // Try building again from scratch
                val d = buildDatabase(context, sessionManager, dbName)
                try {
                    d.openHelper.writableDatabase
                } catch (e2: Exception) {
                    android.util.Log.e("AppDatabase", "RECREATION FAILED. This is a fatal state.", e2)
                    throw e2 // Re-throw so we don't return a broken database instance
                }
                d
            }
            instance = db
            return db
        }

        private fun getDatabaseNameForUser(userId: String): String {
            return "${BASE_DATABASE_NAME}_${userId}.db"
        }

        /**
         * Thoroughly deletes the database files for a specific user or all databases.
         * Used on logout or when corruption is detected.
         */
        @Synchronized
        fun closeAndClearAllDatabases(context: android.content.Context) {
            try {
                android.util.Log.w("AppDatabase", "Closing and deleting all local databases")
                instance?.close()
                instance = null
                currentUserId = null

                val dbPath = context.getDatabasePath("dummy")
                val dbDir = dbPath.parentFile
                if (dbDir != null && dbDir.exists()) {
                    dbDir.listFiles()?.forEach { file ->
                        android.util.Log.w("AppDatabase", "Deleting database file: ${file.name}")
                        context.deleteDatabase(file.name)
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AppDatabase", "Error clearing all database files", e)
            }
        }

        fun deleteUserDatabase(context: android.content.Context, userId: String) {
            closeAndClearAllDatabases(context)
        }

        private fun buildDatabase(context: android.content.Context, sessionManager: com.example.firstapp.common.domain.auth.SessionManager, dbName: String): AppDatabase {
            System.loadLibrary("sqlcipher")
            val passphrase = sessionManager.getDbPassphrase().toByteArray()
            val factory = SupportOpenHelperFactory(passphrase)

            return androidx.room.Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                dbName
            )
            .openHelperFactory(factory)
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
