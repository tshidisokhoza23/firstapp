package com.example.firstapp.common.di.modules

import com.example.firstapp.BuildConfig
import com.example.firstapp.auth.data.api.AuthApi
import com.example.firstapp.chat.data.api.ChatApi
import com.example.firstapp.chat.jobs.data.api.JobApi
import com.example.firstapp.chat.rewards.data.api.RewardsApi
import com.example.firstapp.common.domain.auth.SessionManager
import com.example.firstapp.common.data.api.SystemApi
import com.example.firstapp.common.data.api.UserApi
import com.example.firstapp.common.data.network.ActivePassiveInterceptor
import com.example.firstapp.drive.data.api.DriveApi
import com.example.firstapp.media.data.api.MediaApi
import com.example.firstapp.media.data.api.SocialApi
import com.example.firstapp.music.data.api.MusicApi
import com.example.firstapp.common.data.network.NetworkConstants
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
            level = HttpLoggingInterceptor.Level.BODY
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
        envManager: com.example.firstapp.common.data.network.NetworkEnvironmentManager
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
                    val authApi = Retrofit.Builder()
                        .baseUrl(currentBaseUrl)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(AuthApi::class.java)

                    android.util.Log.d("NetworkModule", "Attempting token refresh on $currentBaseUrl...")
                    val refreshResponse = runBlocking {
                        authApi.refresh(com.example.firstapp.common.data.api.TokenRefreshRequest(refreshToken))
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
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
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
