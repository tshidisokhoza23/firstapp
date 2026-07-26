package com.example.firstapp.chat.data

import android.content.Context
import android.provider.Settings
import com.example.firstapp.common.domain.auth.SessionManager
import com.example.firstapp.common.data.api.ApiClient
import com.example.firstapp.common.data.api.PushTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PushTokenRegistrar {

    fun syncCurrentToken(context: Context, sessionManager: SessionManager) {
        if (!sessionManager.isLoggedIn()) {
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                uploadToken(context, token, sessionManager)
            }
            .addOnFailureListener {
                // no-op
            }
    }

    fun uploadToken(context: Context, token: String?, sessionManager: SessionManager) {
        val cleanToken = token?.trim().orEmpty()
        if (cleanToken.isEmpty() || !sessionManager.isLoggedIn()) {
            return
        }

        val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.trim()
            .takeUnless { it.isNullOrEmpty() }
            ?: "android-device"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.userApi.upsertPushToken(
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
