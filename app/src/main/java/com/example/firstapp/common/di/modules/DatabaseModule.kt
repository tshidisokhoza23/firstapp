package com.example.firstapp.common.di.modules

import android.content.Context
import com.example.firstapp.common.data.local.AppDatabase
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
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        sessionManager: com.example.firstapp.common.domain.auth.SessionManager
    ): AppDatabase {
        return AppDatabase.getInstance(context, sessionManager)
    }

    @Provides
    fun provideChatDao(database: AppDatabase) = database.chatDao()

    @Provides
    fun provideSongDao(database: AppDatabase) = database.songDao()

    @Provides
    fun provideJobDao(database: AppDatabase) = database.jobDao()

    @Provides
    fun provideAdvertDao(database: AppDatabase) = database.advertDao()

    @Provides
    fun provideChannelDao(database: AppDatabase) = database.channelDao()

    @Provides
    fun providePostDao(database: AppDatabase) = database.postDao()

    @Provides
    fun provideStoryDao(database: AppDatabase) = database.storyDao()

    @Provides
    fun provideCallLogDao(database: AppDatabase) = database.callLogDao()

    @Provides
    fun provideContactDao(database: AppDatabase) = database.contactDao()

    @Provides
    fun provideJobApplicationDao(database: AppDatabase) = database.jobApplicationDao()

    @Provides
    fun provideDriveDao(database: AppDatabase) = database.driveDao()

    @Provides
    fun provideUserDao(database: AppDatabase) = database.userDao()

    @Provides
    fun provideLessonProgressDao(database: AppDatabase) = database.lessonProgressDao()

    @Provides
    fun providePlaylistDao(database: AppDatabase) = database.playlistDao()

    @Provides
    fun providePostRemoteKeysDao(database: AppDatabase) = database.postRemoteKeysDao()
}
