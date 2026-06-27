package com.example.firstapp.common.util

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

data class Country(
    val name: String,
    val code: String,
    val dialCode: String,
    val flag: String
)

object CountryUtils {
    val allCountries: List<Country> = listOf(
        Country("South Africa", "ZA", "+27", "🇿🇦"),
        Country("United States", "US", "+1", "🇺🇸"),
        Country("United Kingdom", "GB", "+44", "🇬🇧"),
        Country("Nigeria", "NG", "+234", "🇳🇬"),
        Country("Kenya", "KE", "+254", "🇰🇪"),
        Country("Ghana", "GH", "+233", "🇬🇭"),
        Country("India", "IN", "+91", "🇮🇳"),
        Country("Australia", "AU", "+61", "🇦🇺"),
        Country("Canada", "CA", "+1", "🇨🇦"),
        Country("Germany", "DE", "+49", "🇩🇪"),
        Country("France", "FR", "+33", "🇫🇷"),
        Country("Japan", "JP", "+81", "🇯🇵"),
        Country("China", "CN", "+86", "🇨🇳"),
        Country("Brazil", "BR", "+55", "🇧🇷"),
        Country("Mexico", "MX", "+52", "🇲🇽")
    ).sortedBy { it.name }

    fun getDefaultCountry(context: Context): Country {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val countryCode = try {
            val code = tm?.networkCountryIso ?: tm?.simCountryIso ?: Locale.getDefault().country
            code.uppercase()
        } catch (e: Exception) {
            "ZA"
        }
        return allCountries.find { it.code == countryCode } ?: allCountries.find { it.code == "ZA" } ?: allCountries[0]
    }

    fun getFlagEmoji(countryCode: String): String {
        if (countryCode.length != 2) return "🏳️"
        val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    }
}
