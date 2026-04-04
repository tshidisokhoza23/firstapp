package com.example.firstapp.data

import androidx.compose.ui.graphics.Color
import com.example.firstapp.model.Channel

object ChannelService {
    val dummyChannels = listOf(
        Channel(
            id = 1,
            name = "Girly Wallpapers!!!🎀",
            followers = "356K",
            avatarColor = Color(0xFFE91E63),
            lastPostImageColor = Color(0xFFFFCDD2),
            lastPostText = "🌸 Hey girlies, how much do you love your parents 🥺🎀\n\n• 100% ❤️\n• 50% 🤍\n• 0% 🖤"
        ),
        Channel(
            id = 2,
            name = "LOVE LETTERS ❤️💋🎶",
            followers = "993K",
            avatarColor = Color(0xFFB71C1C)
        ),
        Channel(
            id = 3,
            name = "EMOTIONS.🥺🤍!",
            followers = "141K",
            avatarColor = Color(0xFF9E9E9E)
        ),
        Channel(
            id = 4,
            name = "Uthando Lweqiniso.🙌❤️",
            followers = "877K",
            avatarColor = Color(0xFFD1C4E9)
        )
    )
}
