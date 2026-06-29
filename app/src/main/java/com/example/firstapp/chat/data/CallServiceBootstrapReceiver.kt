package com.example.firstapp.chat.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.firstapp.common.data.TokenManager

class CallServiceBootstrapReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }

        TokenManager.initialize(context.applicationContext)
        if (!TokenManager.isLoggedIn()) {
            return
        }

        val serviceIntent = Intent(context, CallListenerService::class.java)
        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            // Background start not allowed or other service error
        }
    }
}
