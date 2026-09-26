package com.tpsk.tj.chat.jobs.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent

object CustomTabsHelper {
    /**
     * Safely launches an external apply URL in Chrome Custom Tabs or falls back to system browser.
     * Never pre-fetches destination URLs or auto-clicks in the background to ensure affiliate network compliance.
     */
    fun launchUrl(context: Context, url: String) {
        if (url.isBlank()) {
            Toast.makeText(context, "Invalid external application URL", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = try {
            Uri.parse(url)
        } catch (e: Exception) {
            Toast.makeText(context, "Invalid URL format", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                .build()
            customTabsIntent.launchUrl(context, uri)
        } catch (e: Exception) {
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, uri)
                context.startActivity(browserIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "No web browser found to open link", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
