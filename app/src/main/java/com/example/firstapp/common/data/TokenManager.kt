package com.example.firstapp.common.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TokenManager {
    private const val PREFS_NAME = "first_app_secure_prefs"
    private const val KEY_TOKEN = "auth_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_PRIVATE_KEY = "private_key"
    private const val KEY_PUBLIC_KEY = "public_key"
    private const val KEY_DB_PASSPHRASE = "db_passphrase"

    private lateinit var prefs: SharedPreferences
    private var token: String? = null
    private var refreshToken: String? = null

    private val _authState = MutableStateFlow(false)
    val authState: StateFlow<Boolean> = _authState.asStateFlow()

    fun initialize(context: Context) {
        if (!::prefs.isInitialized) {
            try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                prefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                
                // Ensure a DB passphrase exists
                if (prefs.getString(KEY_DB_PASSPHRASE, null) == null) {
                    val newPass = java.util.UUID.randomUUID().toString()
                    // Use commit() here because the DB needs this immediately to open
                    prefs.edit().putString(KEY_DB_PASSPHRASE, newPass).commit()
                }

                token = prefs.getString(KEY_TOKEN, null)
                refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
            } catch (e: Exception) {
                // Fallback or handle error (e.g., keystore corruption)
                prefs = context.getSharedPreferences("first_app_fallback_prefs", Context.MODE_PRIVATE)
            }

            // Ensure a DB passphrase exists in whatever prefs we ended up with
            if (prefs.getString(KEY_DB_PASSPHRASE, null) == null) {
                val newPass = java.util.UUID.randomUUID().toString()
                prefs.edit().putString(KEY_DB_PASSPHRASE, newPass).commit()
            }

            token = prefs.getString(KEY_TOKEN, null)
            refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
            _authState.value = token != null
        }
    }

    fun getDbPassphrase(): String {
        return prefs.getString(KEY_DB_PASSPHRASE, "SecureAppPassphrase789") ?: "SecureAppPassphrase789"
    }

    fun setToken(newToken: String) {
        token = newToken
        _authState.value = true
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

    fun setPrivateKey(key: String) {
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_PRIVATE_KEY, key).apply()
        }
    }

    fun getPrivateKey(): String? {
        return if (::prefs.isInitialized) prefs.getString(KEY_PRIVATE_KEY, null) else null
    }

    fun setPublicKey(key: String) {
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_PUBLIC_KEY, key).apply()
        }
    }

    fun getPublicKey(): String? {
        return if (::prefs.isInitialized) prefs.getString(KEY_PUBLIC_KEY, null) else null
    }

    fun isLoggedIn(): Boolean = token != null

    fun clear() {
        token = null
        refreshToken = null
        _authState.value = false
        if (::prefs.isInitialized) {
            prefs.edit().remove(KEY_TOKEN).remove(KEY_REFRESH_TOKEN).apply()
        }
    }
}