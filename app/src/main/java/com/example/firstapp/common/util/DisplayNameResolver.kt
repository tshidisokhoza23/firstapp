package com.example.firstapp.common.util

import android.content.Context
import android.provider.ContactsContract
import androidx.annotation.WorkerThread
import com.example.firstapp.common.model.User
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.text.*

/**
 * DisplayNameResolver implements the display name fallback hierarchy for UI rendering:
 *
 * 1. If target user's phone number exists in device's local phone contacts → display Contact Name
 * 2. Else if user profile has a display name → display Profile Name
 * 3. Else → display raw phone number
 */
class DisplayNameResolver(private val context: Context) {

    private val contactCache = ConcurrentHashMap<String, String>()

    @WorkerThread
    fun resolve(user: User?, phoneNumber: String): String {
        val cleanPhone = phoneNumber.trim()
        if (cleanPhone.isEmpty()) {
             // If phone is missing, check user object
             if (user != null && user.displayName.isNotBlank() && user.displayName.lowercase() != "unknown" && user.displayName.lowercase() != "null") {
                 return user.displayName
             }
             val fallbackPhone = user?.phoneNumber?.trim() ?: ""
             return if (fallbackPhone.isNotEmpty()) fallbackPhone else "Unknown"
        }

        // Check cache first
        val cached = contactCache[cleanPhone]
        if (cached != null && cached.isNotEmpty()) {
            return cached
        }

        // 1. Check local contacts via PhoneLookup
        val contactName = queryContactName(cleanPhone)
        if (contactName != null && contactName.isNotEmpty()) {
            contactCache[cleanPhone] = contactName
            return contactName
        }

        // 2. Fall back to profile display_name if local contact is missing
        if (user != null && user.displayName.isNotBlank() && 
            user.displayName.lowercase() != "unknown" && 
            user.displayName.lowercase() != "null" &&
            user.displayName != user.id) {
            return user.displayName
        }

        // 3. Fall back to raw phone number
        return cleanPhone
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

    fun clearCache() {
        contactCache.clear()
    }
}
