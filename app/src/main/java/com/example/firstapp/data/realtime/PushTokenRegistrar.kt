package com.example.firstapp.data.realtime

import android.content.Context
import android.provider.Settings
import com.example.firstapp.data.TokenManager
import com.example.firstapp.data.api.ApiClient
import com.example.firstapp.data.api.PushTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PushTokenRegistrar {

    fun syncCurrentToken(context: Context) {
        if (!TokenManager.isLoggedIn()) {
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                uploadToken(context, token)
            }
            .addOnFailureListener {
                // no-op
            }
    }

    fun uploadToken(context: Context, token: String?) {
        val cleanToken = token?.trim().orEmpty()
        if (cleanToken.isEmpty() || !TokenManager.isLoggedIn()) {
            return
        }

        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.trim()
            .takeUnless { it.isNullOrEmpty() }
            ?: "android-device"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.apiService.upsertPushToken(
                    PushTokenRequest(
                        token = cleanToken,
                        device_id = deviceId,
                        platform = "android"
                    )
                )
            } catch (_: Exception) {
                // no-op
            }
        }
    }
}
