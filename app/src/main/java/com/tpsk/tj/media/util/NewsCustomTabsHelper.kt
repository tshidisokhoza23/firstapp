package com.tpsk.tj.media.util

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent

object NewsCustomTabsHelper {
    fun launchNewsUrl(context: Context, url: String, fallback: () -> Unit = {}) {
        if (url.isBlank()) {
            fallback()
            return
        }
        val targetUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        val uri = try {
            Uri.parse(targetUrl)
        } catch (e: Exception) {
            fallback()
            return
        }

        try {
            val colorParams = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(android.graphics.Color.parseColor("#121B22"))
                .build()

            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                .setDefaultColorSchemeParams(colorParams)
                .build()

            customTabsIntent.launchUrl(context, uri)
        } catch (e: Exception) {
            fallback()
        }
    }
}
