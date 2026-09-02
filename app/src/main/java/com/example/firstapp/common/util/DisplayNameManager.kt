package com.example.firstapp.common.util

import android.content.Context

object DisplayNameManager {

    /**
     * Logic Rule Hierarchy:
     * 1. Check local phone contacts.
     * 2. Fallback to Server Full Name.
     * 3. Fallback to Raw Phone Number.
     */
    fun resolve(context: Context, phoneNumber: String?, serverName: String?): String {
        if (phoneNumber == null) return serverName ?: ""

        // 1. Local Phone Contacts check
        val localName = ContactUtils.getContactNameByNumber(context, phoneNumber)
        if (!localName.isNullOrBlank()) return localName

        // 2. Fallback to Server Name
        if (!serverName.isNullOrBlank()) return serverName

        // 3. Final Fallback: Phone Number
        return phoneNumber
    }
}
