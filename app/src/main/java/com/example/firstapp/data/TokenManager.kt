package com.example.firstapp.data

import android.content.Context
import android.content.SharedPreferences

object TokenManager {
    private const val PREFS_NAME = "first_app_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    private lateinit var prefs: SharedPreferences
    private var token: String? = null
    private var refreshToken: String? = null

    fun initialize(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            token = prefs.getString(KEY_TOKEN, null)
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        }
    }

    fun setToken(newToken: String) {
        token = newToken
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_TOKEN, newToken).apply()
        }
    }

    fun setRefreshToken(newRefreshToken: String) {
        refreshToken = newRefreshToken
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_REFRESH_TOKEN, newRefreshToken).apply()
        }
    }

    fun getToken(): String? = token

    fun getRefreshToken(): String? = refreshToken

    fun isLoggedIn(): Boolean = token != null

    fun clear() {
        token = null
        refreshToken = null
        if (::prefs.isInitialized) {
            prefs.edit().remove(KEY_TOKEN).remove(KEY_REFRESH_TOKEN).apply()
        }
    }
}