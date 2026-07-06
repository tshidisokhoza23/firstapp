package com.example.firstapp.common.util

import android.content.Context
import android.provider.ContactsContract
import androidx.annotation.WorkerThread
import com.example.firstapp.domain.model.User
import java.util.concurrent.ConcurrentHashMap

/**
 * DisplayNameResolver implements the strict display name fallback hierarchy for UI rendering:
 *
 * 1. If target user's phone number exists in device's local phone contacts → display Contact Name
 * 2. Else if user has registered online profile full_name → display registered name
 * 3. Else → display raw phone number
 *
 * This ensures consistent UX across chat, job applications, and channel membership lists.
 * Contact lookups are cached to avoid repeated ContentProvider queries.
 */
class DisplayNameResolver(private val context: Context) {

    // Cache contact names to avoid repeated database queries
    private val contactCache = ConcurrentHashMap<String, String?>()

    /**
     * Resolve display name for a user following strict fallback order.
     * MUST be called on background thread (database query).
     */
    @WorkerThread
    fun resolve(user: User, phoneNumber: String): String {
        return when {
            // 1. Check local contacts first
            hasLocalContact(phoneNumber) -> {
                val contactName = getContactName(phoneNumber)
                if (contactName != null) contactName else resolveFromProfile(user, phoneNumber)
            }

            // 2. Fall back to profile name
            !user.fullName.isNullOrBlank() -> user.fullName!!

            // 3. Fall back to phone number
            else -> phoneNumber
        }
    }

    /**
     * Quick check if phone number exists in contacts (cached).
     */
    private fun hasLocalContact(phoneNumber: String): Boolean {
        if (contactCache.containsKey(phoneNumber)) {
            return contactCache[phoneNumber] != null
        }
        val name = queryContactName(phoneNumber)
        contactCache[phoneNumber] = name
        return name != null
    }

    /**
     * Get contact name from device contacts (with caching).
     */
    private fun getContactName(phoneNumber: String): String? {
        // Check cache first
        if (contactCache.containsKey(phoneNumber)) {
            return contactCache[phoneNumber]
        }

        val name = queryContactName(phoneNumber)
        contactCache[phoneNumber] = name
        return name
    }

    /**
     * Query Android contacts database for display name.
     * Returns null if phone number not found in contacts.
     */
    private fun queryContactName(phoneNumber: String): String? {
        return try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
            )

            // Normalize phone number for matching (remove spaces, dashes, etc.)
            val normalizedPhone = normalizePhoneNumber(phoneNumber)

            val cursor = context.contentResolver.query(
                uri,
                projection,
                "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
                arrayOf(normalizedPhone),
                null
            )

            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    if (nameIdx >= 0) {
                        c.getString(nameIdx)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            // Handle security exceptions, null pointers gracefully
            null
        }
    }

    /**
     * Helper to resolve display name from user profile when contact lookup fails.
     */
    private fun resolveFromProfile(user: User, phoneNumber: String): String {
        return when {
            !user.fullName.isNullOrBlank() -> user.fullName!!
            !user.username.isNullOrBlank() -> user.username!!
            else -> phoneNumber
        }
    }

    /**
     * Normalize phone number for consistent matching:
     * - Remove spaces, dashes, parentheses
     * - Preserve country code prefix if present
     */
    private fun normalizePhoneNumber(phone: String): String {
        return phone
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace("+", "00") // Handle country code variations
    }

    /**
     * Clear contact cache (e.g., after permission grant or on app resume).
     */
    fun clearCache() {
        contactCache.clear()
    }

    /**
     * Get cached contact count (for debugging).
     */
    fun getCacheSize(): Int = contactCache.size
}
