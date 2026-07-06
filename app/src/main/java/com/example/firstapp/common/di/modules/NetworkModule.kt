package com.example.firstapp.common.di.modules

import com.example.firstapp.BuildConfig
import com.example.firstapp.auth.data.api.AuthApi
import com.example.firstapp.chat.data.api.ChatApi
import com.example.firstapp.common.data.TokenManager
import com.example.firstapp.common.data.api.*
import com.example.firstapp.drive.data.api.DriveApi
import com.example.firstapp.jobs.data.api.JobApi
import com.example.firstapp.media.data.api.MediaApi
import com.example.firstapp.media.data.api.SocialApi
import com.example.firstapp.music.data.api.MusicApi
import com.example.firstapp.rewards.data.api.RewardsApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val BASE_URL_FALLBACK = "http://127.0.0.1:8082/api/v1/"

    @Provides
    @Singleton
    fun provideBaseUrl(): String {
        return BuildConfig.API_BASE_URL.takeIf { it.isNotBlank() } ?: BASE_URL_FALLBACK
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request()
            val requestBuilder = request.newBuilder()
            val path = request.url.encodedPath
            val isAuthEndpoint = path.endsWith("/auth/login") ||
                path.endsWith("/auth/register") ||
                path.endsWith("/auth/refresh") ||
                path.endsWith("/auth/logout")

            if (!isAuthEndpoint) {
                TokenManager.getToken()?.let { token ->
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
            }
            chain.proceed(requestBuilder.build())
        }
    }

    @Provides
    @Singleton
    fun provideAuthenticator(baseUrl: String): Authenticator {
        val refreshLock = Any()
        return Authenticator { _, response ->
            val path = response.request.url.encodedPath
            val isAuthEndpoint = path.endsWith("/auth/login") ||
                path.endsWith("/auth/register") ||
                path.endsWith("/auth/refresh") ||
                path.endsWith("/auth/logout")
            if (isAuthEndpoint) return@Authenticator null

            if (responseCount(response) >= 2) return@Authenticator null

            val previousAuthHeader = response.request.header("Authorization")
            val previousToken = previousAuthHeader
                ?.takeIf { it.startsWith("Bearer ") }
                ?.removePrefix("Bearer ")

            synchronized(refreshLock) {
                val latestToken = TokenManager.getToken()
                if (!latestToken.isNullOrBlank() && latestToken != previousToken) {
                    return@Authenticator response.request.newBuilder()
                        .header("Authorization", "Bearer $latestToken")
                        .build()
                }

                val refreshToken = TokenManager.getRefreshToken() ?: return@Authenticator null
                val refreshRequest = TokenRefreshRequest(refresh_token = refreshToken)

                try {
                    val refreshResponse = runBlocking {
                        Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build()
                            .create(AuthApi::class.java)
                            .refresh(refreshRequest)
                    }

                    if (refreshResponse.isSuccessful) {
                        val body = refreshResponse.body()
                        if (body != null && body.token.isNotBlank() && body.refreshToken.isNotBlank()) {
                            TokenManager.setToken(body.token)
                            TokenManager.setRefreshToken(body.refreshToken)
                            return@Authenticator response.request.newBuilder()
                                .header("Authorization", "Bearer ${body.token}")
                                .build()
                        }
                    } else if (refreshResponse.code() == 401 || refreshResponse.code() == 400) {
                        // Refresh token is invalid or expired, log out
                        TokenManager.clear()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("NetworkModule", "Token refresh failed", e)
                }
                return@Authenticator null
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
        authenticator: Authenticator
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .authenticator(authenticator)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
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
    fun provideJobApi(retrofit: Retrofit): JobApi = retrofit.create(JobApi::class.java)

    @Provides
    @Singleton
    fun provideRewardsApi(retrofit: Retrofit): RewardsApi = retrofit.create(RewardsApi::class.java)

    @Provides
    @Singleton
    fun provideSystemApi(retrofit: Retrofit): SystemApi = retrofit.create(SystemApi::class.java)
}
