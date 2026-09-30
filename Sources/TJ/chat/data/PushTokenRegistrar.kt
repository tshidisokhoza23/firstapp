package com.tpsk.tj.chat.data

import android.content.Context
import android.provider.Settings
import com.tpsk.tj.common.domain.auth.SessionManager
import com.tpsk.tj.common.data.api.UserApi
import com.tpsk.tj.common.data.api.PushTokenRequest
import com.tpsk.tj.common.util.ContextProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PushTokenRegistrar {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PushTokenEntryPoint {
        fun userApi(): UserApi
    }

    private fun getUserApi(): UserApi? {
        return try {
            EntryPointAccessors.fromApplication(ContextProvider.context, PushTokenEntryPoint::class.java).userApi()
        } catch (_: Exception) {
            null
        }
    }

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
                getUserApi()?.upsertPushToken(
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
