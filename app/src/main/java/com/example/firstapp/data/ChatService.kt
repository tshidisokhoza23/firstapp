package com.example.firstapp.data

import androidx.compose.ui.graphics.Color
import com.example.firstapp.model.ChatMessage

object ChatService {
    val dummyChats = listOf(
        ChatMessage(1, "Baradi ba St. Phillimon ❤️🤍💛", "+27 81 711 4339: 🎧 Audio", "18:09", 6, Color(0xFFE91E63)),
        ChatMessage(2, "MABOTJA TAXI PASSENGERS", "+27 64 792 1501: 🎤 Voice mess...", "18:03", 108, Color(0xFF2196F3)),
        ChatMessage(3, "Palesa Mojaki", "📹 Video call", "17:06", 0, Color(0xFFFF9800)),
        ChatMessage(4, "Lebza", "https://x.com/i/status/203986295...", "16:56", 0, Color(0xFF4CAF50)),
        ChatMessage(5, "Sehafi", "🎤 Voice call", "16:00", 0, Color(0xFF9C27B0)),
        ChatMessage(6, "Sbongile Tindleni", "🎤 Voice message (0:20)", "15:09", 0, Color(0xFF00BCD4)),
        ChatMessage(7, "Conny", "🎤 Voice message (0:16)", "09:42", 0, Color(0xFF795548)),
        ChatMessage(8, "Mamuruti", "🎤 Voice call", "Yesterday", 0, Color(0xFF607D8B)),
        ChatMessage(9, "John Doe", "Hey there!", "Yesterday", 2, Color(0xFF3F51B5)),
        ChatMessage(10, "Family Group", "Mom: Don't forget the milk", "12:45", 15, Color(0xFF8BC34A)),
        ChatMessage(11, "Tech News", "New Android version out!", "Monday", 0, Color(0xFFFF5722)),
        ChatMessage(12, "Alice", "See you tomorrow", "Sunday", 0, Color(0xFFE91E63))
    )
}
