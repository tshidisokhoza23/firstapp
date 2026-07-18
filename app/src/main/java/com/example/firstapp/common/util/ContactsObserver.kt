package com.example.firstapp.common.util

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.util.Log

/**
 * ContactsObserver listens for changes in the system contacts database.
 * When a change occurs (add/edit/delete), it executes the provided callback.
 * This is used to keep the app's contact list and display names in sync with the phone book.
 */
class ContactsObserver(
    private val context: Context,
    private val onChange: () -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private var isRegistered = false

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        Log.d("ContactsObserver", "System contacts changed, triggering refresh...")
        onChange()
    }

    fun register() {
        if (isRegistered) return
        try {
            context.contentResolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                this
            )
            isRegistered = true
            Log.d("ContactsObserver", "Registered successfully")
        } catch (e: Exception) {
            Log.e("ContactsObserver", "Failed to register", e)
        }
    }

    fun unregister() {
        if (!isRegistered) return
        try {
            context.contentResolver.unregisterContentObserver(this)
            isRegistered = false
            Log.d("ContactsObserver", "Unregistered successfully")
        } catch (e: Exception) {
            Log.e("ContactsObserver", "Failed to unregister", e)
        }
    }
}
