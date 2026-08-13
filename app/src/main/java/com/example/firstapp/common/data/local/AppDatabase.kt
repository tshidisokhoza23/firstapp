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
        const val DATABASE_NAME = "firstapp_v8_encrypted.db"

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: android.content.Context, sessionManager: com.example.firstapp.common.domain.auth.SessionManager): AppDatabase {
            return instance ?: synchronized(this) {
                val db = try {
                    val d = buildDatabase(context, sessionManager)
                    // Trigger a database open to catch encryption/corruption errors early
                    d.openHelper.writableDatabase
                    d
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Database open failed, deleting and recreating...", e)
                    // If it's a SQLCipher key mismatch or corruption, delete the file and try again
                    try {
                        context.deleteDatabase(DATABASE_NAME)
                    } catch (_: Exception) {}
                    val d = buildDatabase(context, sessionManager)
                    d.openHelper.writableDatabase
                    d
                }
                instance = db
                db
            }
        }

        private fun buildDatabase(context: android.content.Context, sessionManager: com.example.firstapp.common.domain.auth.SessionManager): AppDatabase {
            // Initialize SQLCipher
            System.loadLibrary("sqlcipher")
            
            val passphrase = sessionManager.getDbPassphrase().toByteArray()
            val factory = SupportOpenHelperFactory(passphrase)

            return androidx.room.Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
            .openHelperFactory(factory)
            .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
