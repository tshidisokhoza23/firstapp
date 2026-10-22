package com.tpsk.tj.common.di.modules

import com.tpsk.tj.BuildConfig
import com.tpsk.tj.auth.data.api.AuthApi
import com.tpsk.tj.chat.data.api.ChatApi
import com.tpsk.tj.chat.jobs.data.api.JobApi
import com.tpsk.tj.chat.rewards.data.api.RewardsApi
import com.tpsk.tj.wallet.data.api.CreatorWalletApi
import com.tpsk.tj.common.domain.auth.SessionManager
import com.tpsk.tj.common.data.api.AiApi
import com.tpsk.tj.common.data.api.StudioApi
import com.tpsk.tj.common.data.api.SystemApi
import com.tpsk.tj.common.data.api.UserApi
import com.tpsk.tj.common.data.network.ActivePassiveInterceptor
import com.tpsk.tj.drive.data.api.DriveApi
import com.tpsk.tj.media.data.api.MediaApi
import com.tpsk.tj.media.data.api.SocialApi
import com.tpsk.tj.stories.data.api.StoryApi
import com.tpsk.tj.channels.data.api.ChannelApi
import com.tpsk.tj.music.data.api.MusicApi
import com.tpsk.tj.mingle.data.api.MingleApi
import com.tpsk.tj.common.data.network.NetworkConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideBaseUrl(): String = NetworkConstants.BASE_URL

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
    }

    private fun isAuthPath(path: String): Boolean {
        return path.contains("/auth/login") || 
               path.contains("/auth/register") || 
               path.contains("/auth/refresh") || 
               path.contains("/auth/logout") ||
               path.contains("/auth/google") ||
               path.contains("/auth/request-otp") ||
               path.contains("/auth/verify-otp") ||
               path.contains("/auth/availability")
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(sessionManager: SessionManager): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val requestBuilder = request.newBuilder()
            val path = request.url.encodedPath
            
            if (!isAuthPath(path)) {
                val token = sessionManager.getToken()
                if (token != null && token.length > 0) {
                    requestBuilder.addHeader("Authorization", "Bearer " + token)
                }
            }
            chain.proceed(requestBuilder.build())
        }
    }

    @Provides
    @Singleton
    fun provideAuthenticator(
        sessionManager: SessionManager,
        envManager: com.tpsk.tj.common.data.network.NetworkEnvironmentManager
    ): Authenticator {
        val refreshLock = Any()
        return Authenticator { _, response ->
            val path = response.request.url.encodedPath
            if (isAuthPath(path)) return@Authenticator null

            if (responseCount(response) >= 2) return@Authenticator null

            val previousAuthHeader = response.request.header("Authorization")
            var previousToken: String? = null
            if (previousAuthHeader != null && previousAuthHeader.startsWith("Bearer ")) {
                previousToken = previousAuthHeader.substring(7)
            }

            synchronized(refreshLock) {
                val latestToken = sessionManager.getToken()
                if (latestToken != null && latestToken.length > 0 && latestToken != previousToken) {
                    return@Authenticator response.request.newBuilder()
                        .header("Authorization", "Bearer " + latestToken)
                        .build()
                }

                val refreshToken = sessionManager.getRefreshToken()
                if (refreshToken == null || refreshToken.length == 0) return@Authenticator null

                val currentBaseUrl = envManager.currentUrl

                return@Authenticator try {
                    val refreshOkHttpClient = okhttp3.OkHttpClient.Builder()
                        .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                        .writeTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                    val authApi = Retrofit.Builder()
                        .baseUrl(currentBaseUrl)
                        .client(refreshOkHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(AuthApi::class.java)

                    android.util.Log.d("NetworkModule", "Attempting token refresh on $currentBaseUrl...")
                    val refreshResponse = runBlocking {
                        authApi.refresh(com.tpsk.tj.common.data.api.TokenRefreshRequest(refreshToken))
                    }
                    
                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val body = refreshResponse.body()!!
                        android.util.Log.i("NetworkModule", "Refresh successful, token updated")
                        sessionManager.saveSession(body.token, body.refreshToken, sessionManager.currentUser.value)
                        response.request.newBuilder()
                            .header("Authorization", "Bearer " + body.token)
                            .build()
                    } else {
                        val code = refreshResponse.code()
                        android.util.Log.e("NetworkModule", "Refresh failed with code $code")
                        // Pro App Behavior: Only clear session if token is explicitly revoked/rejected (400/401/403)
                        if (code == 400 || code == 401 || code == 403) {
                            android.util.Log.w("NetworkModule", "Refresh token invalid or revoked, clearing session")
                            sessionManager.clearSession()
                        }
                        null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NetworkModule", "Exception during token refresh (network/transient)", e)
                    // Do NOT clear session on transient network errors!
                    null
                }
            }
        }
    }

    private fun responseCount(response: okhttp3.Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor,
        activePassiveInterceptor: ActivePassiveInterceptor,
        authenticator: Authenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .authenticator(authenticator)
            .addInterceptor(activePassiveInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            // Photos and voice notes are multipart uploads. 15s dropped them
            // before the server stored the file.
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(baseUrl: String, okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideChatApi(retrofit: Retrofit): ChatApi = retrofit.create(ChatApi::class.java)

    @Provides
    @Singleton
    fun provideMusicApi(retrofit: Retrofit): MusicApi = retrofit.create(MusicApi::class.java)

    @Provides
    @Singleton
    fun provideDriveApi(retrofit: Retrofit): DriveApi = retrofit.create(DriveApi::class.java)

    @Provides
    @Singleton
    fun provideMediaApi(retrofit: Retrofit): MediaApi = retrofit.create(MediaApi::class.java)

    @Provides
    @Singleton
    fun provideSocialApi(retrofit: Retrofit): SocialApi = retrofit.create(SocialApi::class.java)

    @Provides
    @Singleton
    fun provideStoryApi(retrofit: Retrofit): StoryApi = retrofit.create(StoryApi::class.java)

    @Provides
    @Singleton
    fun provideChannelApi(retrofit: Retrofit): ChannelApi = retrofit.create(ChannelApi::class.java)

    @Provides
    @Singleton
    fun provideJobApi(retrofit: Retrofit): JobApi = retrofit.create(JobApi::class.java)

    @Provides
    @Singleton
    fun provideRewardsApi(retrofit: Retrofit): RewardsApi = retrofit.create(RewardsApi::class.java)

    @Provides
    @Singleton
    fun provideCreatorWalletApi(retrofit: Retrofit): CreatorWalletApi = retrofit.create(CreatorWalletApi::class.java)

    @Provides
    @Singleton
    fun provideSystemApi(retrofit: Retrofit): SystemApi = retrofit.create(SystemApi::class.java)

    @Provides
    @Singleton
    fun provideMingleApi(retrofit: Retrofit): MingleApi = retrofit.create(MingleApi::class.java)

    @Provides
    @Singleton
    fun provideAiApi(retrofit: Retrofit): AiApi = retrofit.create(AiApi::class.java)

    @Provides
    @Singleton
    fun provideStudioApi(retrofit: Retrofit): StudioApi = retrofit.create(StudioApi::class.java)

    @Provides
    @Singleton
    fun provideNewsApi(retrofit: Retrofit): com.tpsk.tj.media.data.api.NewsApi = retrofit.create(com.tpsk.tj.media.data.api.NewsApi::class.java)

    @Provides
    @Singleton
    fun provideNearbyApi(retrofit: Retrofit): com.tpsk.tj.chat.nearby.data.api.NearbyApi = retrofit.create(com.tpsk.tj.chat.nearby.data.api.NearbyApi::class.java)
}
