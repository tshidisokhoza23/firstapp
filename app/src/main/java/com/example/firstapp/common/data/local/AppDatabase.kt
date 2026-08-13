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
        MessageFtsEntity::class
    ], 
    version = 57,
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

    fun clearDatabase() {
        clearAllTables()
    }

    companion object {
        private const val BASE_DATABASE_NAME = "firstapp_v9_encrypted"

        @Volatile
        private var instance: AppDatabase? = null
        private var currentUserId: String? = null

        fun getInstance(context: android.content.Context, sessionManager: com.example.firstapp.common.domain.auth.SessionManager): AppDatabase {
            val userId = sessionManager.getUserId() ?: "anonymous"
            
            return if (instance != null && currentUserId == userId) {
                instance!!
            } else synchronized(this) {
                // If switching users, close old instance
                if (currentUserId != userId) {
                    instance?.close()
                    instance = null
                }
                
                val dbName = "${BASE_DATABASE_NAME}_${userId}.db"
                currentUserId = userId

                val db = try {
                    val d = buildDatabase(context, sessionManager, dbName)
                    d.openHelper.writableDatabase
                    d
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Database open failed ($dbName), deleting and recreating...", e)
                    try { context.deleteDatabase(dbName) } catch (_: Exception) {}
                    val d = buildDatabase(context, sessionManager, dbName)
                    d.openHelper.writableDatabase
                    d
                }
                instance = db
                db
            }
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
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
