package com.example.firstapp.common.data

import android.util.Base64
import android.util.Log
import com.example.firstapp.auth.data.api.AuthApi
import com.example.firstapp.common.data.api.TokenRefreshRequest
import kotlinx.coroutines.runBlocking
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.nio.charset.StandardCharsets
import java.util.UUID

object AuthSessionManager {
    private const val TAG = "AuthSessionManager"
    private const val EXPIRY_SKEW_SECONDS = 30L
    private val refreshLock = Any()

    fun isSupportedAccessToken(token: String?): Boolean {
        if (token.isNullOrBlank()) {
            return false
        }
        val subject = tokenSubject(token) ?: return false
        return try {
            UUID.fromString(subject)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    fun currentAccessTokenOrNull(): String? {
        val token = TokenManager.getToken() ?: return null
        if (!isSupportedAccessToken(token)) {
            Log.w(TAG, "Clearing unsupported legacy session token")
            TokenManager.clear()
            return null
        }
        return token
    }

    fun currentAccessTokenForWebSocket(baseUrl: String): String? {
        val token = currentAccessTokenOrNull() ?: return null
        return if (isExpired(token)) {
            refreshTokensBlocking(baseUrl)
        } else {
            token
        }
    }

    fun refreshTokensBlocking(baseUrl: String): String? {
        synchronized(refreshLock) {
            val currentToken = TokenManager.getToken()
            if (!currentToken.isNullOrBlank()) {
                if (!isSupportedAccessToken(currentToken)) {
                    Log.w(TAG, "Clearing unsupported legacy session token before refresh")
                    TokenManager.clear()
                    return null
                }
                if (!isExpired(currentToken)) {
                    return currentToken
                }
            }

            val refreshToken = TokenManager.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                return null
            }

            return try {
                val response = runBlocking {
                    Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(AuthApi::class.java)
                        .refresh(TokenRefreshRequest(refresh_token = refreshToken))
                }

                if (!response.isSuccessful) {
                    if (response.code() == 400 || response.code() == 401) {
                        Log.w(TAG, "Refresh token rejected by server; clearing session")
                        TokenManager.clear()
                    }
                    null
                } else {
                    val body = response.body()
                    if (body == null || body.token.isBlank() || body.refreshToken.isBlank() || !isSupportedAccessToken(body.token)) {
                        Log.w(TAG, "Refresh returned invalid session payload; clearing session")
                        TokenManager.clear()
                        null
                    } else {
                        TokenManager.setToken(body.token)
                        TokenManager.setRefreshToken(body.refreshToken)
                        body.token
                    }
                }
            } catch (e: RuntimeException) {
                Log.e(TAG, "Token refresh request failed", e)
                null
            }
        }
    }

    private fun isExpired(token: String): Boolean {
        val payload = decodePayload(token) ?: return true
        val expiry = payload.optLong("exp", 0L)
        if (expiry <= 0L) {
            return true
        }
        val nowSeconds = System.currentTimeMillis() / 1000L
        return expiry <= nowSeconds + EXPIRY_SKEW_SECONDS
    }

    private fun tokenSubject(token: String): String? {
        val payload = decodePayload(token) ?: return null
        val subject = payload.optString("sub", "").trim()
        return subject.ifBlank { null }
    }

    private fun decodePayload(token: String): JSONObject? {
        val parts = token.split('.')
        if (parts.size < 2) {
            return null
        }
        return try {
            val decodedBytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            JSONObject(String(decodedBytes, StandardCharsets.UTF_8))
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Failed to decode JWT payload", e)
            null
        } catch (e: JSONException) {
            Log.w(TAG, "Failed to parse JWT payload", e)
            null
        }
    }
}
