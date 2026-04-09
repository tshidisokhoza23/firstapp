package com.example.firstapp.data.api

import com.example.firstapp.data.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://192.168.100.7:8082/api/v1/" // Local backend with fixed queries

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

        val refreshToken = TokenManager.getRefreshToken() ?: return@Authenticator null
        val refreshRequest = TokenRefreshRequest(refresh_token = refreshToken)

        try {
            val refreshResponse = runBlocking {
                Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService::class.java)
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

        TokenManager.clearToken()
        null
    }

    private val client = OkHttpClient.Builder()
        .authenticator(tokenAuthenticator)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()

    val apiService: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}