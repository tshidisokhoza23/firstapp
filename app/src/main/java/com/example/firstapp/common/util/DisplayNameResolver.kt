package com.example.firstapp.common.util

import android.content.Context
import android.provider.ContactsContract
import androidx.annotation.WorkerThread
import com.example.firstapp.common.model.User
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.text.*

/**
 * DisplayNameResolver implements the strict display name fallback hierarchy for UI rendering:
 *
 * 1. If target user's phone number exists in device's local phone contacts → display Contact Name
 * 2. Else if user has registered online profile full_name → display registered name
 * 3. Else → display raw phone number (NEVER display username as per requirements)
 */
class DisplayNameResolver(private val context: Context) {

    private val contactCache = ConcurrentHashMap<String, String>()

    @WorkerThread
    fun resolve(user: User, phoneNumber: String): String {
        if (phoneNumber.isEmpty()) return resolveFromProfile(user, phoneNumber)

        // Check cache first
        val cached = contactCache[phoneNumber]
        if (cached != null) {
            return if (cached.isNotEmpty()) cached else resolveFromProfile(user, phoneNumber)
        }

        // 1. Check local contacts via PhoneLookup (handles normalization automatically)
        val contactName = queryContactName(phoneNumber)
        contactCache[phoneNumber] = contactName ?: ""

        if (contactName != null && contactName.isNotEmpty()) {
            return contactName
        }

        // 2. Fall back to phone number (Skip username fallback)
        return resolveFromProfile(user, phoneNumber)
    }

    private fun queryContactName(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null
        return try {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

            val cursor = context.contentResolver.query(uri, projection, null, null, null)

            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIdx >= 0) c.getString(nameIdx) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun resolveFromProfile(user: User, phoneNumber: String): String {
        val d = user.displayName
        
        // 1. Profile Display Name (often contains "Full Name")
        if (d.isNotBlank() && d.lowercase() != "unknown" && d.lowercase() != "null") {
            return d
        }
        
        // 2. Phone Number (Skip username fallback as requested)
        if (phoneNumber.isNotBlank() && phoneNumber.lowercase() != "unknown" && phoneNumber.lowercase() != "null") {
            return phoneNumber
        }
        
        // 3. Last resort ID
        return user.id
    }

    fun clearCache() {
        contactCache.clear()
    }
}
