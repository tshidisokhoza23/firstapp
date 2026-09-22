package com.tpsk.tj.chat.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.tpsk.tj.common.domain.auth.SessionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class CallServiceBootstrapReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SessionManagerEntryPoint {
        fun sessionManager(): SessionManager
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }

        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SessionManagerEntryPoint::class.java
        )
        val sessionManager = entryPoint.sessionManager()

        if (!sessionManager.isLoggedIn()) {
            return
        }

        val serviceIntent = Intent(context, CallListenerService::class.java)
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            // Background start not allowed
        }
    }
}
