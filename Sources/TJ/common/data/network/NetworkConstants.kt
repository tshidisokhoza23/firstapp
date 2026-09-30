package com.tpsk.tj.common.data.network

import com.tpsk.tj.BuildConfig

object NetworkConstants {
    // Single point of truth for the entire app
    val BASE_URL: String = BuildConfig.API_BASE_URL
    val WS_URL: String = BuildConfig.WS_BASE_URL
    
    // Helper to format URLs consistently
    fun formatMediaUrl(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        
        // Use standard MediaUtils logic but ensure it pulls from here
        return com.tpsk.tj.common.util.MediaUtils.formatUrl(url)
    }
}
