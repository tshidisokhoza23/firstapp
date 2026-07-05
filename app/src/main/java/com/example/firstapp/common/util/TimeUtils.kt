package com.example.firstapp.common.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Formats an ISO 8601 timestamp string the WhatsApp way:
 *  - Today      → "2:30 PM"
 *  - Yesterday  → "Yesterday"
 *  - This week  → "Monday" / "Tuesday" / …
 *  - Older      → "09/04/2026"
 */
fun formatWhatsAppTime(timestamp: Long): String {
    return formatWhatsAppTime(Date(timestamp))
}

fun formatWhatsAppTime(isoTimestamp: String?): String {
    if (isoTimestamp.isNullOrBlank()) return ""
    val date = parseIso(isoTimestamp) ?: return isoTimestamp
    return formatWhatsAppTime(date)
}

fun formatWhatsAppTime(date: Date): String {
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { time = date }

    val isToday = now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)

    val isYesterday = run {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        yesterday.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
    }

    val daysDiff = ((now.timeInMillis - cal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

    return when {
        isToday -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
        isYesterday -> "Yesterday"
        daysDiff < 7 -> SimpleDateFormat("EEEE", Locale.getDefault()).format(date) // e.g. "Monday"
        else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
    }
}

fun formatChatDateHeader(timestamp: Long): String {
    return formatChatDateHeader(Date(timestamp))
}

fun formatChatDateHeader(date: Date): String {
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { time = date }

    val isToday = now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)

    val isYesterday = run {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        yesterday.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
    }

    return when {
        isToday -> "Today"
        isYesterday -> "Yesterday"
        else -> SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date)
    }
}

fun formatChatDateHeader(isoTimestamp: String): String {
    val date = parseIso(isoTimestamp) ?: return isoTimestamp
    return formatChatDateHeader(date)
}

fun formatMessageTime(timestamp: Long): String {
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
}

/** Same as above but always returns HH:mm (used inside chat bubble). */
fun formatMessageTime(isoTimestamp: String?): String {
    if (isoTimestamp.isNullOrBlank()) return ""
    val date = parseIso(isoTimestamp) ?: return isoTimestamp
    return SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
}

/** Returns a relative "posted" label: "2 hours ago", "3 days ago", "2 weeks ago". */
fun formatPostedTime(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val diffMs = System.currentTimeMillis() - timestamp
    val mins = diffMs / 60_000
    val hours = mins / 60
    val days = hours / 24
    val weeks = days / 7
    return when {
        mins < 60 -> "Posted ${mins}m ago"
        hours < 24 -> "Posted ${hours}h ago"
        days < 7 -> "Posted ${days}d ago"
        weeks < 4 -> "Posted ${weeks}w ago"
        else -> "Posted ${days / 30}mo ago"
    }
}

fun formatPostedTime(isoTimestamp: String?): String {
    if (isoTimestamp.isNullOrBlank()) return ""
    val date = parseIso(isoTimestamp) ?: return ""
    return formatPostedTime(date.time)
}

/** Returns Twitter-style relative time: 5m, 2h, 3d, or Apr 9 for older posts. */
fun formatTwitterTime(isoTimestamp: String?): String {
    if (isoTimestamp.isNullOrBlank()) return ""
    
    // Try to parse as Long first (milliseconds)
    val date = isoTimestamp.toLongOrNull()?.let { java.util.Date(it) } ?: parseIso(isoTimestamp) ?: return ""
    
    val diffMs = System.currentTimeMillis() - date.time
    val mins = diffMs / 60_000
    val hours = mins / 60
    val days = hours / 24

    return when {
        mins < 1 -> "now"
        mins < 60 -> "${mins}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}


private val ISO_FORMATS = listOf(
    "yyyy-MM-dd'T'HH:mm:ssXXX",
    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    "yyyy-MM-dd HH:mm:ss",
)

fun parseIso(s: String): Date? {
    for (fmt in ISO_FORMATS) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.getDefault())
            sdf.isLenient = false
            return sdf.parse(s)
        } catch (_: Exception) {}
    }
    return null
}
