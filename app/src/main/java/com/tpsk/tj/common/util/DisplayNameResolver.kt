package com.tpsk.tj.common.util

import android.content.Context
import android.provider.ContactsContract
import androidx.annotation.WorkerThread
import com.tpsk.tj.common.model.User
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.text.*

/**
 * Checks if a given text string appears to be a raw ID rather than a human-readable name.
 */
fun isIdString(text: String?, id: String? = null): Boolean {
    if (text.isNullOrBlank()) return true
    val trimmed = text.trim()
    val lower = trimmed.lowercase()

    if (lower == "unknown" || lower == "null" || lower == "undefined" || lower == "none" || lower == "new chat" || lower == "chat" || lower == "user" || lower == "mingle user") {
        return true
    }

    if (id != null && trimmed.equals(id.trim(), ignoreCase = true)) {
        return true
    }

    // 16 to 64 hex characters (e.g. 24-character MongoDB ObjectId like "6aad32d424ccedc1eb...")
    if (trimmed.matches(Regex("^[0-9a-fA-F]{16,64}$"))) {
        return true
    }

    // UUID format check
    if (trimmed.matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))) {
        return true
    }

    return false
}

/**
 * Sanitizes display name ensuring raw IDs or hardcoded placeholder strings are never displayed.
 * Always prefers valid human display name or phone number.
 */
fun sanitizeDisplayName(name: String?, id: String? = null, phone: String? = null): String {
    if (!isIdString(name, id)) {
        return name!!.trim()
    }
    val cleanPhone = phone?.trim().orEmpty()
    if (cleanPhone.isNotEmpty()) {
        return cleanPhone
    }
    return ""
}

/**
 * DisplayNameResolver implements the display name fallback hierarchy for UI rendering:
 *
 * 1. If target user's phone number exists in device's local phone contacts → display Contact Name
 * 2. Else if user profile has a display name / username (not an ID) → display Profile Name / Username
 * 3. Else → display raw phone number
 */
class DisplayNameResolver(private val context: Context) {

    private val contactCache = ConcurrentHashMap<String, String>()

    @WorkerThread
    fun resolve(user: User?, phoneNumber: String): String {
        val cleanPhone = phoneNumber.trim().ifEmpty { user?.phoneNumber?.trim().orEmpty() }

        // 1. Check local contacts via PhoneLookup if phone number is available
        if (cleanPhone.isNotEmpty()) {
            val cached = contactCache[cleanPhone]
            if (cached != null && cached.isNotEmpty()) {
                return cached
            }
            val contactName = queryContactName(cleanPhone)
            if (contactName != null && contactName.isNotEmpty()) {
                contactCache[cleanPhone] = contactName
                return contactName
            }
        }

        // 2. Fall back to profile display_name or username if valid and not an ID
        if (user != null) {
            val displayName = user.displayName.trim()
            if (!isIdString(displayName, user.id)) {
                return displayName
            }
            val username = user.username.trim()
            if (!isIdString(username, user.id)) {
                return username
            }
        }

        // 3. Fall back to raw phone number if available
        if (cleanPhone.isNotEmpty()) {
            return cleanPhone
        }

        return ""
    }

    private fun queryContactName(phoneNumber: String): String? {
        if (phoneNumber.isBlank()) return null

        // 1. Direct PhoneLookup query with original number
        var name = lookupContactNameUri(phoneNumber)
        if (!name.isNullOrBlank()) return name

        // 2. Format variations (e.g. +2773... <-> 073...)
        val digits = phoneNumber.replace(Regex("[^0-9]"), "")
        if (phoneNumber.startsWith("+27") && digits.length >= 11) {
            val localFormat = "0" + digits.substring(2)
            name = lookupContactNameUri(localFormat)
            if (!name.isNullOrBlank()) return name
        } else if (phoneNumber.startsWith("0") && digits.length >= 10) {
            val intlFormat = "+27" + digits.substring(1)
            name = lookupContactNameUri(intlFormat)
            if (!name.isNullOrBlank()) return name
        }

        // 3. Fallback match by last 9 digits against device contacts
        val last9 = digits.takeLast(9)
        if (last9.length >= 9) {
            val deviceContacts = ContactUtils.getPhoneContactsWithNames(context)
            val match = deviceContacts.find { 
                it.phoneNumber.replace(Regex("[^0-9]"), "").takeLast(9) == last9 
            }
            if (match != null && match.displayName.isNotBlank()) {
                return match.displayName
            }
        }

        return null
    }

    private fun lookupContactNameUri(phone: String): String? {
        return try {
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phone)
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

fun looksLikePhone(value: String?): Boolean {
    if (value.isNullOrBlank()) return false
    val digits = value.count { it.isDigit() }
    val compact = value.count { !it.isWhitespace() }
    return digits >= 8 && compact > 0 && digits * 10 >= compact * 7
}
