package com.example.firstapp.data

import androidx.compose.ui.graphics.Color
import com.example.firstapp.model.Short

object ShortService {
    val dummyShorts = List(30) { index ->
        Short(
            id = index,
            creatorName = "@User_${index + 1}",
            description = "Funny video #${index + 1}! #short #trending #viral",
            likes = "${(10..999).random()}K",
            comments = "${(100..9999).random()}",
            thumbnailColor = Color(
                (0..255).random(),
                (0..255).random(),
                (0..255).random()
            )
        )
    }
}
