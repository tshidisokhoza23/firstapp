package com.tpsk.tj.common.di.modules

import com.tpsk.tj.chat.data.CallTonePlayer
import com.tpsk.tj.chat.data.WebRtcCallManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideWebRtcCallManager(@ApplicationContext context: android.content.Context): WebRtcCallManager {
        return WebRtcCallManager(context)
    }

    @Provides
    @Singleton
    fun provideCallTonePlayer(@ApplicationContext context: android.content.Context): CallTonePlayer {
        return CallTonePlayer(context)
    }

    @Provides
    @Singleton
    fun provideDisplayNameResolver(@ApplicationContext context: android.content.Context): com.tpsk.tj.common.util.DisplayNameResolver {
        return com.tpsk.tj.common.util.DisplayNameResolver(context)
    }
}
