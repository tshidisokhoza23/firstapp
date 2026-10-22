package com.tpsk.tj.common.di.modules

import com.tpsk.tj.account.data.repository.AccountRepositoryImpl
import com.tpsk.tj.account.domain.repository.AccountRepository
import com.tpsk.tj.chat.data.repository.AdRepositoryImpl
import com.tpsk.tj.chat.data.repository.ChatRepositoryImpl
import com.tpsk.tj.channels.data.ChannelRepositoryImpl
import com.tpsk.tj.channels.domain.ChannelRepository
import com.tpsk.tj.direct.data.DirectRepositoryImpl
import com.tpsk.tj.direct.domain.DirectRepository
import com.tpsk.tj.groups.data.GroupRepositoryImpl
import com.tpsk.tj.groups.domain.GroupRepository
import com.tpsk.tj.stories.data.StoryRepositoryImpl
import com.tpsk.tj.stories.domain.StoryRepository
import com.tpsk.tj.chat.domain.repository.AdRepository
import com.tpsk.tj.settings.data.repository.SettingsRepositoryImpl
import com.tpsk.tj.domain.repository.SettingsRepository
import com.tpsk.tj.common.data.api.SystemApi
import com.tpsk.tj.common.data.api.UserApi

import com.tpsk.tj.common.data.repository.SystemRepositoryImpl
import com.tpsk.tj.common.domain.repository.SystemRepository
import com.tpsk.tj.domain.repository.ChatRepository
import com.tpsk.tj.domain.repository.JobRepository
import com.tpsk.tj.drive.data.repository.DriveRepositoryImpl
import com.tpsk.tj.drive.domain.repository.DriveRepository
import com.tpsk.tj.chat.jobs.data.repository.JobRepositoryImpl
import com.tpsk.tj.media.data.api.MediaApi
import com.tpsk.tj.media.data.api.SocialApi
import com.tpsk.tj.media.data.repository.*
import com.tpsk.tj.media.domain.repository.*
import com.tpsk.tj.music.data.api.MusicApi
import com.tpsk.tj.music.data.local.dao.SongDao
import com.tpsk.tj.music.data.local.dao.PlaylistDao
import com.tpsk.tj.music.data.repository.MusicRepositoryImpl
import com.tpsk.tj.music.domain.repository.MusicRepository
import com.tpsk.tj.search.data.repository.SearchRepositoryImpl
import com.tpsk.tj.search.domain.repository.SearchRepository
import com.tpsk.tj.vocab.data.repository.MockLessonRepositoryImpl
import com.tpsk.tj.vocab.data.repository.UserRepositoryImpl
import com.tpsk.tj.vocab.domain.repository.LessonRepository
import com.tpsk.tj.vocab.domain.repository.UserRepository
import com.tpsk.tj.mingle.data.repository.MingleRepositoryImpl
import com.tpsk.tj.mingle.domain.repository.MingleRepository
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
    abstract fun bindStoryRepository(impl: StoryRepositoryImpl): StoryRepository

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
    abstract fun bindGroupRepository(impl: GroupRepositoryImpl): GroupRepository

    @Binds
    @Singleton
    abstract fun bindDirectRepository(impl: DirectRepositoryImpl): DirectRepository

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

    @Binds
    @Singleton
    abstract fun bindMingleRepository(impl: MingleRepositoryImpl): MingleRepository

    @Binds
    @Singleton
    abstract fun bindNewsRepository(impl: NewsRepositoryImpl): NewsRepository
}
