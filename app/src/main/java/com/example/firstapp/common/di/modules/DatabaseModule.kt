package com.example.firstapp.common.di.modules

import android.content.Context
import com.example.firstapp.common.data.local.AppDatabase
import com.example.firstapp.chat.data.local.dao.*
import com.example.firstapp.media.data.local.dao.*
import com.example.firstapp.chat.jobs.data.local.dao.*
import com.example.firstapp.drive.data.local.dao.*
import com.example.firstapp.music.data.local.dao.*
import com.example.firstapp.vocab.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context,
        sessionManager: com.example.firstapp.common.domain.auth.SessionManager
    ): AppDatabase {
        return AppDatabase.getInstance(context, sessionManager)
    }

    // DAOs should NOT be singletons themselves if they are provided directly.
    // However, since they are obtained from a Database instance that IS managed by our own singleton logic,
    // we must ensure that repositories using them can get fresh instances if the DB is recreated.
    // Using @Provides without @Singleton here is correct for Dagger if the consumer uses Provider<DAO>.

    @Provides
    fun provideChatDao(database: AppDatabase): ChatDao = database.chatDao()

    @Provides
    fun provideSongDao(database: AppDatabase): SongDao = database.songDao()

    @Provides
    fun provideJobDao(database: AppDatabase): JobDao = database.jobDao()

    @Provides
    fun provideAdvertDao(database: AppDatabase): AdvertDao = database.advertDao()

    @Provides
    fun provideChannelDao(database: AppDatabase): ChannelDao = database.channelDao()

    @Provides
    fun providePostDao(database: AppDatabase): PostDao = database.postDao()

    @Provides
    fun provideStoryDao(database: AppDatabase): StoryDao = database.storyDao()

    @Provides
    fun provideCallLogDao(database: AppDatabase): CallLogDao = database.callLogDao()

    @Provides
    fun provideContactDao(database: AppDatabase): ContactDao = database.contactDao()

    @Provides
    fun provideJobApplicationDao(database: AppDatabase): JobApplicationDao = database.jobApplicationDao()

    @Provides
    fun provideDriveDao(database: AppDatabase): DriveDao = database.driveDao()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideLessonProgressDao(database: AppDatabase): LessonProgressDao = database.lessonProgressDao()

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun providePostRemoteKeysDao(database: AppDatabase): PostRemoteKeysDao = database.postRemoteKeysDao()
}
