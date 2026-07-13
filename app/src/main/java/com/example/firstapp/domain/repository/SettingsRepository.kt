package com.example.firstapp.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val fontSize: Flow<Float>
    val enterIsSend: Flow<Boolean>
    val mediaVisibility: Flow<Boolean>
    val animationsEnabled: Flow<Boolean>
    val autoNightMode: Flow<Boolean>
    
    // Notifications
    val privateChatsEnabled: Flow<Boolean>
    val groupsEnabled: Flow<Boolean>
    val channelsEnabled: Flow<Boolean>
    val soundsEnabled: Flow<Boolean>
    val vibrateEnabled: Flow<Boolean>
    val previewsEnabled: Flow<Boolean>

    // Storage
    val autoplayGifs: Flow<Boolean>
    val autoplayVideos: Flow<Boolean>
    val streamingVideos: Flow<Boolean>

    // Language
    val selectedLanguage: Flow<String>

    // Security
    val securityNotificationsEnabled: Flow<Boolean>

    suspend fun setFontSize(size: Float)
    suspend fun setEnterIsSend(enabled: Boolean)
    suspend fun setMediaVisibility(enabled: Boolean)
    suspend fun setAnimationsEnabled(enabled: Boolean)
    suspend fun setAutoNightMode(enabled: Boolean)

    suspend fun setPrivateChatsEnabled(enabled: Boolean)
    suspend fun setGroupsEnabled(enabled: Boolean)
    suspend fun setChannelsEnabled(enabled: Boolean)
    suspend fun setSoundsEnabled(enabled: Boolean)
    suspend fun setVibrateEnabled(enabled: Boolean)
    suspend fun setPreviewsEnabled(enabled: Boolean)

    suspend fun setAutoplayGifs(enabled: Boolean)
    suspend fun setAutoplayVideos(enabled: Boolean)
    suspend fun setStreamingVideos(enabled: Boolean)

    suspend fun setSelectedLanguage(language: String)

    suspend fun setSecurityNotificationsEnabled(enabled: Boolean)
}
