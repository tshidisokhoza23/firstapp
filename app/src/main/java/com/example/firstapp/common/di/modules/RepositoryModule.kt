package com.example.firstapp.common.di.modules

import com.example.firstapp.account.data.repository.AccountRepositoryImpl
import com.example.firstapp.account.domain.repository.AccountRepository
import com.example.firstapp.chat.data.repository.ChatRepositoryImpl
import com.example.firstapp.settings.data.repository.SettingsRepositoryImpl
import com.example.firstapp.domain.repository.SettingsRepository
import com.example.firstapp.common.data.api.ApiClient
import com.example.firstapp.common.data.api.SystemApi
import com.example.firstapp.common.data.api.UserApi
import com.example.firstapp.common.data.local.AppDatabase
import com.example.firstapp.common.data.local.dao.*
import com.example.firstapp.common.data.repository.SystemRepositoryImpl
import com.example.firstapp.common.domain.repository.SystemRepository
import com.example.firstapp.domain.repository.ChatRepository
import com.example.firstapp.domain.repository.JobRepository
import com.example.firstapp.drive.data.repository.DriveRepositoryImpl
import com.example.firstapp.drive.domain.repository.DriveRepository
import com.example.firstapp.chat.jobs.data.repository.JobRepositoryImpl
import com.example.firstapp.media.data.api.MediaApi
import com.example.firstapp.media.data.api.SocialApi
import com.example.firstapp.media.data.repository.*
import com.example.firstapp.media.domain.repository.*
import com.example.firstapp.music.data.api.MusicApi
import com.example.firstapp.music.data.local.dao.SongDao
import com.example.firstapp.music.data.local.dao.PlaylistDao
import com.example.firstapp.music.data.repository.MusicRepositoryImpl
import com.example.firstapp.music.domain.repository.MusicRepository
import com.example.firstapp.search.data.repository.SearchRepositoryImpl
import com.example.firstapp.search.domain.repository.SearchRepository
import com.example.firstapp.vocab.data.repository.MockLessonRepositoryImpl
import com.example.firstapp.vocab.data.repository.UserRepositoryImpl
import com.example.firstapp.vocab.domain.repository.LessonRepository
import com.example.firstapp.vocab.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindLessonRepository(impl: MockLessonRepositoryImpl): LessonRepository

    @Binds
    @Singleton
    abstract fun bindJobRepository(impl: JobRepositoryImpl): JobRepository

    @Binds
    @Singleton
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    @Singleton
    abstract fun bindSocialRepository(impl: SocialRepositoryImpl): SocialRepository

    @Binds
    @Singleton
    abstract fun bindDriveRepository(impl: DriveRepositoryImpl): DriveRepository

    @Binds
    @Singleton
    abstract fun bindAdRepository(impl: AdRepositoryImpl): AdRepository

    @Binds
    @Singleton
    abstract fun bindChannelRepository(impl: ChannelRepositoryImpl): ChannelRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindSystemRepository(impl: SystemRepositoryImpl): SystemRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
