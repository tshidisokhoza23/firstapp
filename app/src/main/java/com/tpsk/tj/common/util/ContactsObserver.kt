package com.tpsk.tj.common.util

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ContactsObserver(
    private val context: Context,
    private val onChange: () -> Unit = {}
) : ContentObserver(Handler(Looper.getMainLooper())) {

    fun register() {
        try {
            context.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                this
            )
        } catch (e: SecurityException) {
            android.util.Log.e("ContactsObserver", "SecurityException registering observer: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("ContactsObserver", "Error registering observer: ${e.message}")
        }
    }

    fun unregister() {
        context.contentResolver.unregisterContentObserver(this)
    }

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        android.util.Log.d("ContactsObserver", "Phone book change detected, triggering sync")
        
        onChange()
        triggerSyncWorker(context)
    }

    companion object {
        fun triggerSyncWorker(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<ContactSyncWorker>()
                .setConstraints(constraints)
                .setInitialDelay(5, TimeUnit.SECONDS) // Debounce rapid changes
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "ContactSyncWork",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
        }
    }
}
