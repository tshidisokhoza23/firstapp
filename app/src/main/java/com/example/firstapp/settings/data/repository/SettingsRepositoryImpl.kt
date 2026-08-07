package com.example.firstapp.settings.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.firstapp.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("chat_settings", Context.MODE_PRIVATE)

    override val fontSize: Flow<Float> = observeKey("font_size", 16f)
    override val enterIsSend: Flow<Boolean> = observeKey("enter_is_send", false)
    override val mediaVisibility: Flow<Boolean> = observeKey("media_visibility", true)
    override val animationsEnabled: Flow<Boolean> = observeKey("animations_enabled", true)
    override val autoNightMode: Flow<Boolean> = observeKey("auto_night_mode", true)
    override val isDarkMode: Flow<Boolean> = observeKey("is_dark_mode", false)

    override val privateChatsEnabled: Flow<Boolean> = observeKey("private_chats_enabled", true)
    override val groupsEnabled: Flow<Boolean> = observeKey("groups_enabled", true)
    override val channelsEnabled: Flow<Boolean> = observeKey("channels_enabled", true)
    override val soundsEnabled: Flow<Boolean> = observeKey("sounds_enabled", true)
    override val vibrateEnabled: Flow<Boolean> = observeKey("vibrate_enabled", true)
    override val previewsEnabled: Flow<Boolean> = observeKey("previews_enabled", true)

    override val autoplayGifs: Flow<Boolean> = observeKey("autoplay_gifs", true)
    override val autoplayVideos: Flow<Boolean> = observeKey("autoplay_videos", true)
    override val streamingVideos: Flow<Boolean> = observeKey("streaming_videos", true)

    override val selectedLanguage: Flow<String> = observeKey("selected_language", "English")

    override val securityNotificationsEnabled: Flow<Boolean> = observeKey("security_notifications_enabled", true)

    override suspend fun setFontSize(size: Float) {
        prefs.edit().putFloat("font_size", size).apply()
    }

    override suspend fun setEnterIsSend(enabled: Boolean) {
        prefs.edit().putBoolean("enter_is_send", enabled).apply()
    }

    override suspend fun setMediaVisibility(enabled: Boolean) {
        prefs.edit().putBoolean("media_visibility", enabled).apply()
    }

    override suspend fun setAnimationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("animations_enabled", enabled).apply()
    }

    override suspend fun setAutoNightMode(enabled: Boolean) {
        prefs.edit().putBoolean("auto_night_mode", enabled).apply()
    }

    override suspend fun setIsDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("is_dark_mode", enabled).apply()
    }

    override suspend fun setPrivateChatsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("private_chats_enabled", enabled).apply()
    }

    override suspend fun setGroupsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("groups_enabled", enabled).apply()
    }

    override suspend fun setChannelsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("channels_enabled", enabled).apply()
    }

    override suspend fun setSoundsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sounds_enabled", enabled).apply()
    }

    override suspend fun setVibrateEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("vibrate_enabled", enabled).apply()
    }

    override suspend fun setPreviewsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("previews_enabled", enabled).apply()
    }

    override suspend fun setAutoplayGifs(enabled: Boolean) {
        prefs.edit().putBoolean("autoplay_gifs", enabled).apply()
    }

    override suspend fun setAutoplayVideos(enabled: Boolean) {
        prefs.edit().putBoolean("autoplay_videos", enabled).apply()
    }

    override suspend fun setStreamingVideos(enabled: Boolean) {
        prefs.edit().putBoolean("streaming_videos", enabled).apply()
    }

    override suspend fun setSelectedLanguage(language: String) {
        prefs.edit().putString("selected_language", language).apply()
    }

    override suspend fun setSecurityNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("security_notifications_enabled", enabled).apply()
    }

    private fun <T> observeKey(key: String, default: T): Flow<T> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, k ->
            if (key == k) {
                @Suppress("UNCHECKED_CAST")
                trySend(getValue(p, key, default))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getValue(prefs, key, default))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getValue(p: SharedPreferences, key: String, default: T): T {
        return when (default) {
            is Float -> p.getFloat(key, default) as T
            is Boolean -> p.getBoolean(key, default) as T
            is String -> p.getString(key, default) as T
            is Int -> p.getInt(key, default) as T
            is Long -> p.getLong(key, default) as T
            else -> default
        }
    }
}
