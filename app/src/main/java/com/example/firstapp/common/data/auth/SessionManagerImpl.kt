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
import kotlinx.coroutines.*
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
        const val FALLBACK_PREFS = "first_app_fallback_prefs"
        const val KEY_TOKEN = "auth_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_DATA = "current_user_data"
        const val KEY_PRIVATE_KEY = "private_key"
        const val KEY_PUBLIC_KEY = "public_key"
        const val KEY_DB_PASSPHRASE = "db_passphrase"
        const val KEY_USE_FALLBACK = "use_unencrypted_fallback"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initLock = Any()
    
    @Volatile
    private var _prefs: SharedPreferences? = null

    private fun getPrefs(): SharedPreferences {
        val p = _prefs
        if (p != null) return p
        
        return synchronized(initLock) {
            _prefs ?: run {
                val created = createPrefs()
                _prefs = created
                created
            }
        }
    }

    private fun createPrefs(): SharedPreferences {
        val staticPrefs = context.getSharedPreferences("init_state", Context.MODE_PRIVATE)
        if (staticPrefs.getBoolean(KEY_USE_FALLBACK, false)) {
            Log.w("SessionManager", "Using fallback prefs due to previous Keystore hang")
            return context.getSharedPreferences(FALLBACK_PREFS, Context.MODE_PRIVATE)
        }

        // Use a dedicated thread to attempt encrypted prefs creation with a strict timeout
        val future = java.util.concurrent.Executors.newSingleThreadExecutor().submit<SharedPreferences> {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        return try {
            // Strict 5-second timeout for Keystore initialization
            val p = future.get(5, java.util.concurrent.TimeUnit.SECONDS)
            Log.d("SessionManager", "Successfully created EncryptedSharedPreferences")
            p
        } catch (e: Exception) {
            Log.e("SessionManager", "EncryptedSharedPreferences init FAILED or TIMED OUT, using fallback", e)
            staticPrefs.edit().putBoolean(KEY_USE_FALLBACK, true).apply()
            future.cancel(true)
            context.getSharedPreferences(FALLBACK_PREFS, Context.MODE_PRIVATE)
        }
    }

    private val _authState = MutableStateFlow(false)
    override val authState: StateFlow<Boolean> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        try {
            val p = getPrefs()
            val token = p.getString(KEY_TOKEN, null)
            val userData = p.getString(KEY_USER_DATA, null)
            
            if (token != null) {
                if (userData != null) {
                    try {
                        _currentUser.value = gson.fromJson(userData, User::class.java)
                    } catch (e: Exception) {
                        Log.e("SessionManager", "Init: Failed to parse user data", e)
                    }
                }
                _authState.value = true
            }
        } catch (e: Exception) {
            Log.e("SessionManager", "Init error", e)
        }
    }

    override fun getToken(): String? = getPrefs().getString(KEY_TOKEN, null)

    override fun getRefreshToken(): String? = getPrefs().getString(KEY_REFRESH_TOKEN, null)

    override fun getUserId(): String? {
        val token = getToken() ?: return null
        return tokenSubject(token)
    }

    override fun isLoggedIn(): Boolean = _authState.value

    override fun saveSession(token: String, refreshToken: String, user: User?) {
        // Update in-memory state immediately so navigation never blocks on
        // (potentially slow) encrypted-preferences / Keystore initialization.
        _authState.value = true
        if (user != null) {
            _currentUser.value = user
        }

        // Persist session tokens immediately and synchronously so they are never lost
        try {
            val p = getPrefs()
            val editor = p.edit()
            editor.putString(KEY_TOKEN, token)
            editor.putString(KEY_REFRESH_TOKEN, refreshToken)
            if (user != null) {
                editor.putString(KEY_USER_DATA, gson.toJson(user))
            }
            editor.commit()
        } catch (e: Exception) {
            Log.e("SessionManager", "Failed to persist session", e)
        }
    }

    override fun updateCurrentUser(user: User) {
        // Update the in-memory state first so navigation never depends on persistence.
        _currentUser.value = user
        try {
            getPrefs().edit().putString(KEY_USER_DATA, gson.toJson(user)).apply()
        } catch (e: Exception) {
            Log.e("SessionManager", "Failed to persist updated user", e)
        }
    }

    override fun clearSession() {
        val userId = getUserId()

        Log.w("SessionManager", "Logging out user $userId: Clearing session, databases, preferences, and cache")

        // 1. Clear SharedPreferences
        try {
            getPrefs().edit().clear().commit()
        } catch (e: Exception) {
            Log.e("SessionManager", "Failed to clear SharedPreferences", e)
        }

        // 2. Delete user database and default database
        if (userId != null) {
            com.example.firstapp.common.data.local.AppDatabase.deleteUserDatabase(context, userId)
        }
        com.example.firstapp.common.data.local.AppDatabase.deleteUserDatabase(context, "anonymous")

        // 3. Delete all database files on disk
        try {
            val dbPath = context.getDatabasePath("dummy")
            val dbDir = dbPath.parentFile
            if (dbDir != null && dbDir.exists()) {
                dbDir.listFiles()?.forEach { file ->
                    Log.w("SessionManager", "Deleting database file on logout: ${file.name}")
                    context.deleteDatabase(file.name)
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e("SessionManager", "Error clearing database directory on logout", e)
        }

        // 4. Clear cache directory
        try {
            context.cacheDir.deleteRecursively()
        } catch (e: Exception) {
            Log.e("SessionManager", "Error clearing cache directory on logout", e)
        }

        _currentUser.value = null
        _authState.value = false
    }

    override fun getPrivateKey(): String? = getPrefs().getString(KEY_PRIVATE_KEY, null)

    override fun setPrivateKey(key: String) {
        getPrefs().edit().putString(KEY_PRIVATE_KEY, key).apply()
    }

    override fun getPublicKey(): String? = getPrefs().getString(KEY_PUBLIC_KEY, null)

    override fun setPublicKey(key: String) {
        getPrefs().edit().putString(KEY_PUBLIC_KEY, key).apply()
    }

    @Synchronized
    override fun getDbPassphrase(): String {
        val p = getPrefs()
        var pass = p.getString(KEY_DB_PASSPHRASE, null)
        if (pass.isNullOrBlank()) {
            pass = UUID.randomUUID().toString()
            p.edit().putString(KEY_DB_PASSPHRASE, pass).commit()
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
