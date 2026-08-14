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
 * 2. Else → display raw phone number (STRICT BAN on username/display_name/ID as per requirements)
 */
class DisplayNameResolver(private val context: Context) {

    private val contactCache = ConcurrentHashMap<String, String>()

    @WorkerThread
    fun resolve(user: User?, phoneNumber: String): String {
        val cleanPhone = phoneNumber.trim()
        if (cleanPhone.isEmpty()) {
             // If phone is missing, and we have a user object with a phone, use that as last resort before "Unknown"
             val fallbackPhone = user?.phoneNumber?.trim() ?: ""
             return if (fallbackPhone.isNotEmpty()) fallbackPhone else "Unknown"
        }

        // Check cache first
        val cached = contactCache[cleanPhone]
        if (cached != null && cached.isNotEmpty()) {
            return cached
        }

        // 1. Check local contacts via PhoneLookup (handles normalization automatically)
        val contactName = queryContactName(cleanPhone)
        if (contactName != null && contactName.isNotEmpty()) {
            contactCache[cleanPhone] = contactName
            return contactName
        }

        // 2. Fall back to raw phone number (Skip profile display_name/username as per Rule 1.3)
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
