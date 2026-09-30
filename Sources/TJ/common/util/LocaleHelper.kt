package com.tpsk.tj.common.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {

    fun applyLocale(context: Context, language: String): Context {
        val locale = when (language.lowercase()) {
            "english" -> Locale("en")
            "spanish" -> Locale("es")
            "french" -> Locale("fr")
            "german" -> Locale("de")
            "chinese" -> Locale("zh")
            "japanese" -> Locale("ja")
            "russian" -> Locale("ru")
            "portuguese" -> Locale("pt")
            "italian" -> Locale("it")
            "korean" -> Locale("ko")
            "turkish" -> Locale("tr")
            "arabic" -> Locale("ar")
            else -> Locale("en")
        }
        
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            config.setLocale(locale)
        }
        
        return context.createConfigurationContext(config)
    }

    fun wrap(context: Context, language: String): Context {
        return applyLocale(context, language)
    }
}
