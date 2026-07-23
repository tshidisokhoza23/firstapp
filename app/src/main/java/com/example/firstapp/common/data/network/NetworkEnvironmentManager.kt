package com.example.firstapp.common.data.network

import android.content.Context
import android.content.SharedPreferences
import com.example.firstapp.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class NetworkMode {
    FORCE_ACTIVE,
    FORCE_PASSIVE
}

@Singleton
class NetworkEnvironmentManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("network_env_prefs", Context.MODE_PRIVATE)

    private val defaultMode: NetworkMode
        get() = when {
            BuildConfig.FLAVOR == "sit" -> NetworkMode.FORCE_PASSIVE
            else -> NetworkMode.FORCE_ACTIVE
        }

    var mode: NetworkMode
        get() {
            if (!BuildConfig.ENABLE_DEV_SWITCH) return NetworkMode.FORCE_ACTIVE
            val savedMode = prefs.getString("network_mode", null)
            return if (savedMode != null) NetworkMode.valueOf(savedMode) else defaultMode
        }
        set(value) {
            if (BuildConfig.ENABLE_DEV_SWITCH) {
                prefs.edit().putString("network_mode", value.name).apply()
            }
        }

    val activeUrl: String = BuildConfig.ACTIVE_API_URL
    val passiveUrl: String = BuildConfig.PASSIVE_API_URL

    val activeWsUrl: String = BuildConfig.WS_BASE_URL
    val passiveWsUrl: String = BuildConfig.WS_BASE_URL.replace("sit-active", "sit-passive") // Heuristic if not provided

    val currentUrl: String
        get() = if (mode == NetworkMode.FORCE_ACTIVE) activeUrl else passiveUrl

    val currentWsUrl: String
        get() = if (mode == NetworkMode.FORCE_ACTIVE) activeWsUrl else passiveWsUrl
}
