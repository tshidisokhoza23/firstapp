package com.example.firstapp.common.util

import android.content.Context
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log

object ContactUtils {

    data class ContactInfo(
        @com.google.gson.annotations.SerializedName("display_name") val displayName: String,
        @com.google.gson.annotations.SerializedName("phone_number") val phoneNumber: String
    )

    fun getPhoneContactsWithNames(context: Context): List<ContactInfo> {
        val contacts = mutableListOf<ContactInfo>()
        try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                null
            )

            val countryCode = getCountryCode(context)
            val seenNumbers = mutableSetOf<String>()

            cursor?.use {
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val name = it.getString(nameIndex) ?: "Unknown"
                    val rawNumber = it.getString(numberIndex)
                    val formattedNumber = formatPhoneNumber(rawNumber, countryCode)
                    
                    if (isValidPhoneNumber(formattedNumber) && !seenNumbers.contains(formattedNumber)) {
                        contacts.add(ContactInfo(name, formattedNumber))
                        seenNumbers.add(formattedNumber)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ContactUtils", "Failed to fetch contacts", e)
        }
        return contacts
    }

    fun getPhoneContacts(context: Context): List<String> {
        val phoneNumbers = mutableSetOf<String>()
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            null,
            null,
            null
        )

        val countryCode = getCountryCode(context)

        cursor?.use {
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val rawNumber = it.getString(numberIndex)
                val formattedNumber = formatPhoneNumber(rawNumber, countryCode)
                if (isValidPhoneNumber(formattedNumber)) {
                    phoneNumbers.add(formattedNumber)
                }
            }
        }
        return phoneNumbers.toList()
    }

    fun getContactNameByNumber(context: Context, phoneNumber: String): String? {
        val contentResolver = context.contentResolver
        val uri = android.net.Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            android.net.Uri.encode(phoneNumber)
        )
        var name: String? = null
        val cursor = contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                name = it.getString(it.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        }
        return name
    }

    private fun getCountryCode(context: Context): String {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val code = tm?.networkCountryIso?.uppercase() ?: tm?.simCountryIso?.uppercase() ?: "ZA" // Default to ZA if unknown
        return getPhonePrefix(code)
    }

    private fun getPhonePrefix(countryIso: String): String {
        // Basic mapping for common country codes, or use a library. 
        // For simplicity, let's handle a few or default to +27 for South Africa as requested by user context (implied by name/style)
        return when (countryIso) {
            "ZA" -> "27"
            "US" -> "1"
            "GB" -> "44"
            else -> "27" 
        }
    }

    fun formatPhoneNumber(number: String, defaultPrefix: String): String {
        var clean = number.replace(Regex("[^0-9+]"), "")
        
        // Handle "00" prefix (common international access code)
        if (clean.startsWith("00")) {
            clean = "+" + clean.substring(2)
        }
        
        // Handle local "0" prefix by replacing with default country code
        if (clean.startsWith("0") && !clean.startsWith("00")) {
            clean = "+$defaultPrefix" + clean.substring(1)
        }
        
        // Ensure it starts with "+"
        if (!clean.startsWith("+")) {
            // If it's a long number without + or 0, assume it might already have country code but missing +
            if (clean.length >= 10) {
                clean = "+$clean"
            } else {
                clean = "+$defaultPrefix$clean"
            }
        }

        return clean
    }

    private fun isValidPhoneNumber(number: String): Boolean {
        // Basic check for E.164: + followed by 7-15 digits
        return number.matches(Regex("^\\+[1-9]\\d{6,14}$"))
    }
}
