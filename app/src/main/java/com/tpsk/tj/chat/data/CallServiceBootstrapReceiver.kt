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
        // Boot used to start a foreground listener, which pinned a "TJ / Calls"
        // notification on the lock screen. Incoming calls start the service from FCM.
        if (
            action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
    }
}
