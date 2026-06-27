package com.example.firstapp.common.data.api

import com.example.firstapp.BuildConfig
import com.example.firstapp.common.data.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import com.example.firstapp.chat.data.api.ChatApi
import com.example.firstapp.music.data.api.MusicApi
import com.example.firstapp.drive.data.api.DriveApi
import com.example.firstapp.media.data.api.MediaApi
import com.example.firstapp.media.data.api.SocialApi
import com.example.firstapp.jobs.data.api.JobApi
import com.example.firstapp.auth.data.api.AuthApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private val BASE_URL = if (BuildConfig.API_BASE_URL.endsWith("/")) {
        BuildConfig.API_BASE_URL
    } else {
        "${BuildConfig.API_BASE_URL}/"
    }
    private val refreshLock = Any()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
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

    private val tokenAuthenticator = Authenticator { route, response ->
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
            // Another request may have already refreshed the token while this one was waiting.
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
                        .baseUrl(BASE_URL)
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
                }
            } catch (_: Exception) {
                // refresh failed
            }

            // Keep current token state to avoid wiping valid tokens during concurrent refresh races.
            return@Authenticator null
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
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .pingInterval(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
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
    val systemApi: SystemApi = retrofit.create(SystemApi::class.java)
}
