package com.example.firstapp.common.di.modules

import com.example.firstapp.chat.data.CallTonePlayer
import com.example.firstapp.chat.data.WebRtcCallManager
import com.example.firstapp.media.ads.AdManager
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
    fun provideAdManager(@ApplicationContext context: android.content.Context): AdManager {
        return AdManager(context)
    }

    @Provides
    @Singleton
    fun provideWebRtcCallManager(@ApplicationContext context: android.content.Context): WebRtcCallManager {
        return WebRtcCallManager(context)
    }

    @Provides
    @Singleton
    fun provideCallTonePlayer(): CallTonePlayer {
        return CallTonePlayer()
    }

    @Provides
    @Singleton
    fun provideDisplayNameResolver(@ApplicationContext context: android.content.Context): com.example.firstapp.common.util.DisplayNameResolver {
        return com.example.firstapp.common.util.DisplayNameResolver(context)
    }
}
