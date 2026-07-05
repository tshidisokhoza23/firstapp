package com.example.firstapp.common.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.firstapp.common.data.local.dao.*
import com.example.firstapp.common.data.local.entity.*
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
        ShortEntity::class,
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
    version = 51,
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
    abstract fun shortDao(): ShortDao
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

        fun getInstance(context: android.content.Context): AppDatabase {
            return instance ?: synchronized(this) {
                // Initialize SQLCipher
                System.loadLibrary("sqlcipher")
                
                // Ensure TokenManager is initialized to get the passphrase
                com.example.firstapp.common.data.TokenManager.initialize(context)
                val passphrase = com.example.firstapp.common.data.TokenManager.getDbPassphrase().toByteArray()
                val factory = SupportOpenHelperFactory(passphrase)

                val db = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                .openHelperFactory(factory)
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .fallbackToDestructiveMigration()
                .build()
                
                instance = db
                db
            }
        }
    }
}
