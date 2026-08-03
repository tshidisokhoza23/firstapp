package com.example.firstapp.common.data.api

import com.example.firstapp.BuildConfig
import com.example.firstapp.auth.data.api.AuthApi
import com.example.firstapp.chat.data.api.ChatApi
import com.example.firstapp.chat.jobs.data.api.JobApi
import com.example.firstapp.chat.rewards.data.api.RewardsApi
import com.example.firstapp.common.domain.auth.SessionManager
import com.example.firstapp.drive.data.api.DriveApi
import com.example.firstapp.media.data.api.MediaApi
import com.example.firstapp.media.data.api.SocialApi
import com.example.firstapp.music.data.api.MusicApi
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import android.content.Context
import com.example.firstapp.common.util.ContextProvider

object ApiClient {
    private val baseUrl = BuildConfig.API_BASE_URL
    private val refreshLock = Any()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SessionEntryPoint {
        fun sessionManager(): SessionManager
    }

    private fun getSessionManager(): SessionManager? {
        val context = ContextProvider.context ?: return null
        return EntryPointAccessors.fromApplication(context, SessionEntryPoint::class.java).sessionManager()
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()
        val requestBuilder = request.newBuilder()
        val path = request.url.encodedPath
        
        val session = getSessionManager()
        if (session != null) {
            session.getToken()?.let { token ->
                requestBuilder.addHeader("Authorization", "Bearer $token")
            }
        }
        
        chain.proceed(requestBuilder.build())
    }

    private val tokenAuthenticator = Authenticator { _, response ->
        if (responseCount(response) >= 2) return@Authenticator null

        val session = getSessionManager() ?: return@Authenticator null
        val refreshToken = session.getRefreshToken() ?: return@Authenticator null

        synchronized(refreshLock) {
            val latestToken = session.getToken()
            val previousAuthHeader = response.request.header("Authorization")
            val previousToken = previousAuthHeader?.removePrefix("Bearer ")

            if (latestToken != null && latestToken != previousToken) {
                return@Authenticator response.request.newBuilder()
                    .header("Authorization", "Bearer $latestToken")
                    .build()
            }

            try {
                val authApi = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(AuthApi::class.java)

                val refreshResponse = kotlinx.coroutines.runBlocking {
                    authApi.refresh(TokenRefreshRequest(refreshToken))
                }

                if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                    val body = refreshResponse.body()!!
                    session.saveSession(body.token, body.refreshToken, session.currentUser.value)
                    return@Authenticator response.request.newBuilder()
                        .header("Authorization", "Bearer ${body.token}")
                        .build()
                } else if (refreshResponse.code() == 401 || refreshResponse.code() == 403 || refreshResponse.code() == 400) {
                    // Only clear session if the refresh token itself is rejected (401/403/400)
                    android.util.Log.e("ApiClient", "Refresh failed with ${refreshResponse.code()}, clearing session")
                    session.clearSession()
                } else {
                    android.util.Log.w("ApiClient", "Refresh failed with ${refreshResponse.code()}, not clearing session")
                }
            } catch (_: Exception) {}

            null
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

    internal val client = OkHttpClient.Builder()
        .authenticator(tokenAuthenticator)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
    val userApi: UserApi = retrofit.create(UserApi::class.java)
    val chatApi: ChatApi = retrofit.create(ChatApi::class.java)
    val musicApi: MusicApi = retrofit.create(MusicApi::class.java)
    val driveApi: DriveApi = retrofit.create(DriveApi::class.java)
    val mediaApi: MediaApi = retrofit.create(MediaApi::class.java)
    val socialApi: SocialApi = retrofit.create(SocialApi::class.java)
    val jobApi: JobApi = retrofit.create(JobApi::class.java)
    val rewardsApi: RewardsApi = retrofit.create(RewardsApi::class.java)
    val systemApi: SystemApi = retrofit.create(SystemApi::class.java)
}
