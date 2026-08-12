package com.example.firstapp.common.util

import android.content.Context
import android.provider.ContactsContract
import com.example.firstapp.common.data.api.AccountContactDTO

class ContactsFetcher(private val context: Context) {
    fun fetchAll(): List<AccountContactDTO> {
        val contacts = mutableMapOf<String, String>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                if (nameIdx < 0 || numIdx < 0) return emptyList()

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIdx) ?: ""
                    val rawNumber = cursor.getString(numIdx) ?: ""
                    
                    // Normalize to numeric only for E.164 (simplification)
                    // Keep '+' if it exists at the start
                    val cleanNumber = rawNumber.replace(Regex("[^0-9+]"), "").let {
                        if (it.startsWith("+")) it else if (it.isNotEmpty()) "+$it" else it
                    }
                    
                    if (cleanNumber.length >= 7) {
                        // Use name as value, phone as key to deduplicate by phone
                        // WhatsApp style: prioritize name if multiple exist for same number?
                        // Here we just keep the last one found.
                        contacts[cleanNumber] = name
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ContactsFetcher", "Error fetching contacts", e)
        }
        
        return contacts.map { AccountContactDTO(name = it.value, phone_number = it.key) }
    }
}
