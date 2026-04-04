package com.example.firstapp.data

import androidx.compose.ui.graphics.Color
import com.example.firstapp.model.Advert

object AdvertService {
    val dummyAdverts = listOf(
        Advert(
            id = 1,
            advertiserName = "Bafedile",
            time = "19:32",
            avatarColor = Color(0xFF5D4037),
            contentColor = Color(0xFFEFEBE9),
            contentText = "Botho le bona bomolimo ba Jesu bo nehoa batho..."
        ),
        Advert(
            id = 2,
            advertiserName = "Valencia WINNIE Moganedi",
            time = "20:51",
            avatarColor = Color(0xFF00796B),
            contentColor = Color(0xFFE0F2F1)
        ),
        Advert(
            id = 3,
            advertiserName = "Safiyyah Boolay",
            time = "1 minute ago",
            avatarColor = Color(0xFFFBC02D),
            contentColor = Color(0xFFFFF9C4)
        ),
        Advert(
            id = 4,
            advertiserName = "Benny",
            time = "38 minutes ago",
            avatarColor = Color(0xFFC2185B),
            contentColor = Color(0xFFFCE4EC)
        ),
        Advert(
            id = 5,
            advertiserName = "vutshila",
            time = "19:12",
            avatarColor = Color(0xFF1976D2),
            contentColor = Color(0xFFE3F2FD)
        )
    )
}
