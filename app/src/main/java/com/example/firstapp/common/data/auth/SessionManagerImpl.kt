package com.example.firstapp.common.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.firstapp.common.domain.auth.SessionManager
import com.example.firstapp.common.model.User
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) : SessionManager {

    private companion object {
        const val PREFS_NAME = "first_app_secure_prefs"
        const val KEY_TOKEN = "auth_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_DATA = "current_user_data"
        const val KEY_PRIVATE_KEY = "private_key"
        const val KEY_PUBLIC_KEY = "public_key"
        const val KEY_DB_PASSPHRASE = "db_passphrase"
    }

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also { Log.d("SessionManager", "Successfully created EncryptedSharedPreferences") }
        } catch (e: Exception) {
            Log.e("SessionManager", "Failed to create EncryptedSharedPreferences, USING FALLBACK (unencrypted)", e)
            context.getSharedPreferences("first_app_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    private val _authState = MutableStateFlow(false)
    override val authState: StateFlow<Boolean> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        val token = getToken()
        val refreshToken = getRefreshToken()
        val userData = prefs.getString(KEY_USER_DATA, null)
        
        Log.d("SessionManager", "Init: token=${token?.take(10)}... refreshToken=${refreshToken?.take(10)}... hasUserData=${userData != null}")

        if (token != null) {
            // We have a token, so the user is technically authenticated.
            // We'll try to restore the user profile if possible.
            if (userData != null) {
                try {
                    _currentUser.value = gson.fromJson(userData, User::class.java)
                    Log.i("SessionManager", "Init: Session restored for user ${_currentUser.value?.id}")
                } catch (e: Exception) {
                    Log.e("SessionManager", "Init: Failed to parse user data", e)
                }
            }
            _authState.value = true
        } else {
            Log.w("SessionManager", "Init: No valid token found. User is logged out.")
            _authState.value = false
        }
    }

    override fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    override fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    override fun getUserId(): String? {
        val token = getToken() ?: return null
        return tokenSubject(token)
    }

    override fun isLoggedIn(): Boolean = _authState.value

    override fun saveSession(token: String, refreshToken: String, user: User?) {
        Log.d("SessionManager", "saveSession: token=${token.take(10)}... hasUser=${user != null}")
        val editor = prefs.edit()
        editor.putString(KEY_TOKEN, token)
        editor.putString(KEY_REFRESH_TOKEN, refreshToken)
        
        if (user != null) {
            editor.putString(KEY_USER_DATA, gson.toJson(user))
            _currentUser.value = user
        }
        
        // We only clear authState if we are explicitly logging out (clearSession)
        // or if we have NO token. If we have a token, we should be "logged in".
        _authState.value = true
        
        if (!editor.commit()) {
            Log.e("SessionManager", "Failed to commit session changes!")
        }
    }

    override fun updateCurrentUser(user: User) {
        Log.d("SessionManager", "updateCurrentUser: ${user.id}")
        prefs.edit().putString(KEY_USER_DATA, gson.toJson(user)).apply()
        _currentUser.value = user
    }

    override fun clearSession() {
        Log.w("SessionManager", "clearSession: Explicit logout triggered")
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_DATA)
            .remove(KEY_PRIVATE_KEY)
            .remove(KEY_PUBLIC_KEY)
            .remove(KEY_DB_PASSPHRASE)
            .commit()
        _currentUser.value = null
        _authState.value = false
    }

    override fun getPrivateKey(): String? = prefs.getString(KEY_PRIVATE_KEY, null)

    override fun setPrivateKey(key: String) {
        prefs.edit().putString(KEY_PRIVATE_KEY, key).apply()
    }

    override fun getPublicKey(): String? = prefs.getString(KEY_PUBLIC_KEY, null)

    override fun setPublicKey(key: String) {
        prefs.edit().putString(KEY_PUBLIC_KEY, key).apply()
    }

    override fun getDbPassphrase(): String {
        var pass = prefs.getString(KEY_DB_PASSPHRASE, null)
        if (pass == null) {
            pass = UUID.randomUUID().toString()
            Log.d("SessionManager", "Generated new DB passphrase")
            prefs.edit().putString(KEY_DB_PASSPHRASE, pass).commit()
        }
        return pass
    }

    private fun tokenSubject(token: String): String? {
        val payload = decodePayload(token) ?: return null
        val subject = payload.optString("sub", "").trim()
        return if (subject.isEmpty()) null else subject
    }

    private fun decodePayload(token: String): JSONObject? {
        val parts = token.split(".")
        if (parts.size < 2) return null
        return try {
            val decodedBytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            JSONObject(String(decodedBytes, StandardCharsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }
}
