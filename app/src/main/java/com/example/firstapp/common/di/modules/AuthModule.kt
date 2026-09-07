package com.example.firstapp.common.di.modules

import com.example.firstapp.chat.data.CallWebSocketClient
import com.example.firstapp.common.data.auth.SessionManagerImpl
import com.example.firstapp.common.domain.auth.SessionManager
import com.google.gson.Gson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindSessionManager(impl: SessionManagerImpl): SessionManager

    companion object {
        @Provides
        @Singleton
        fun provideGson(): Gson = Gson()

        @Provides
        @Singleton
        fun provideCallWebSocketClient(
            sessionManager: SessionManager,
            httpClient: OkHttpClient,
            gson: Gson,
            envManager: com.example.firstapp.common.data.network.NetworkEnvironmentManager
        ): CallWebSocketClient = CallWebSocketClient(sessionManager, httpClient, gson, envManager)

        @Provides
        @Singleton
        fun providePostWebSocketClient(
            sessionManager: SessionManager,
            httpClient: OkHttpClient,
            envManager: com.example.firstapp.common.data.network.NetworkEnvironmentManager
        ): com.example.firstapp.media.data.realtime.PostWebSocketClient = com.example.firstapp.media.data.realtime.PostWebSocketClient(sessionManager, httpClient, envManager)
    }
}
