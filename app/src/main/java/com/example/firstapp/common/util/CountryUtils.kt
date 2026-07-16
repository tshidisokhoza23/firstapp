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
        Country("Afghanistan", "AF", "+93", "🇦🇫"),
        Country("Algeria", "DZ", "+213", "🇩🇿"),
        Country("Angola", "AO", "+244", "🇦🇴"),
        Country("Argentina", "AR", "+54", "🇦🇷"),
        Country("Australia", "AU", "+61", "🇦🇺"),
        Country("Austria", "AT", "+43", "🇦🇹"),
        Country("Belgium", "BE", "+32", "🇧🇪"),
        Country("Benin", "BJ", "+229", "🇧🇯"),
        Country("Botswana", "BW", "+267", "🇧🇼"),
        Country("Brazil", "BR", "+55", "🇧🇷"),
        Country("Burkina Faso", "BF", "+226", "🇧🇫"),
        Country("Burundi", "BI", "+257", "🇧🇮"),
        Country("Cameroon", "CM", "+237", "🇨🇲"),
        Country("Canada", "CA", "+1", "🇨🇦"),
        Country("Central African Republic", "CF", "+236", "🇨🇫"),
        Country("Chad", "TD", "+235", "🇹🇩"),
        Country("China", "CN", "+86", "🇨🇳"),
        Country("Colombia", "CO", "+57", "🇨🇴"),
        Country("Congo", "CG", "+242", "🇨🇬"),
        Country("DR Congo", "CD", "+243", "🇨🇩"),
        Country("Denmark", "DK", "+45", "🇩🇰"),
        Country("Egypt", "EG", "+20", "🇪🇬"),
        Country("Equatorial Guinea", "GQ", "+240", "🇬🇶"),
        Country("Ethiopia", "ET", "+251", "🇪🇹"),
        Country("France", "FR", "+33", "🇫🇷"),
        Country("Gabon", "GA", "+241", "🇬🇦"),
        Country("Gambia", "GM", "+220", "🇬🇲"),
        Country("Germany", "DE", "+49", "🇩🇪"),
        Country("Ghana", "GH", "+233", "🇬🇭"),
        Country("Greece", "GR", "+30", "🇬🇷"),
        Country("Guinea", "GN", "+224", "🇬🇳"),
        Country("India", "IN", "+91", "🇮🇳"),
        Country("Indonesia", "ID", "+62", "🇮🇩"),
        Country("Italy", "IT", "+39", "🇮🇹"),
        Country("Ivory Coast", "CI", "+225", "🇨🇮"),
        Country("Japan", "JP", "+81", "🇯🇵"),
        Country("Kenya", "KE", "+254", "🇰🇪"),
        Country("Lesotho", "LS", "+266", "🇱🇸"),
        Country("Liberia", "LR", "+231", "🇱🇷"),
        Country("Libya", "LY", "+218", "🇱🇾"),
        Country("Madagascar", "MG", "+261", "🇲🇬"),
        Country("Malawi", "MW", "+265", "🇲🇼"),
        Country("Mali", "ML", "+223", "🇲🇱"),
        Country("Mauritania", "MR", "+222", "🇲🇷"),
        Country("Mauritius", "MU", "+230", "🇲🇺"),
        Country("Mexico", "MX", "+52", "🇲🇽"),
        Country("Morocco", "MA", "+212", "🇲🇦"),
        Country("Mozambique", "MZ", "+258", "🇲🇿"),
        Country("Namibia", "NA", "+264", "🇳🇦"),
        Country("Netherlands", "NL", "+31", "🇳🇱"),
        Country("New Zealand", "NZ", "+64", "🇳🇿"),
        Country("Niger", "NE", "+227", "🇳🇪"),
        Country("Nigeria", "NG", "+234", "🇳🇬"),
        Country("Norway", "NO", "+47", "🇳🇴"),
        Country("Pakistan", "PK", "+92", "🇵🇰"),
        Country("Philippines", "PH", "+63", "🇵🇭"),
        Country("Poland", "PL", "+48", "🇵🇱"),
        Country("Portugal", "PT", "+351", "🇵🇹"),
        Country("Russia", "RU", "+7", "🇷🇺"),
        Country("Rwanda", "RW", "+250", "🇷🇼"),
        Country("Saudi Arabia", "SA", "+966", "🇸🇦"),
        Country("Senegal", "SN", "+221", "🇸🇳"),
        Country("Sierra Leone", "SL", "+232", "🇸🇱"),
        Country("Singapore", "SG", "+65", "🇸🇬"),
        Country("Somalia", "SO", "+252", "🇸🇴"),
        Country("South Africa", "ZA", "+27", "🇿🇦"),
        Country("South Korea", "KR", "+82", "🇰🇷"),
        Country("South Sudan", "SS", "+211", "🇸🇸"),
        Country("Spain", "ES", "+34", "🇪🇸"),
        Country("Sudan", "SD", "+249", "🇸🇩"),
        Country("Swaziland", "SZ", "+268", "🇸🇿"),
        Country("Sweden", "SE", "+46", "🇸🇪"),
        Country("Switzerland", "CH", "+41", "🇨🇭"),
        Country("Tanzania", "TZ", "+255", "🇹🇿"),
        Country("Thailand", "TH", "+66", "🇹🇭"),
        Country("Togo", "TG", "+228", "🇹🇬"),
        Country("Tunisia", "TN", "+216", "🇹🇳"),
        Country("Uganda", "UG", "+256", "🇺🇬"),
        Country("United Arab Emirates", "AE", "+971", "🇦🇪"),
        Country("United Kingdom", "GB", "+44", "🇬🇧"),
        Country("United States", "US", "+1", "🇺🇸"),
        Country("Zambia", "ZM", "+260", "🇿🇲"),
        Country("Zimbabwe", "ZW", "+263", "🇿🇼")
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
