package com.example.firstapp.common.data.network

import com.example.firstapp.BuildConfig

object NetworkConstants {
    // Single point of truth for the entire app
    val BASE_URL: String = BuildConfig.API_BASE_URL
    val WS_URL: String = BuildConfig.WS_BASE_URL
    
    // Helper to format URLs consistently
    fun formatMediaUrl(url: String?): String? {
        if (url.isNullOrEmpty()) return null
        
        // Use standard MediaUtils logic but ensure it pulls from here
        return com.example.firstapp.common.util.MediaUtils.formatUrl(url)
    }
}
