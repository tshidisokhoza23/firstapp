package com.example.firstapp.data

import androidx.compose.ui.graphics.Color
import com.example.firstapp.model.Short

object ShortService {
    private val videoUrls = listOf(
        "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackAds.mp4",
        "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
    )

    val dummyShorts = List(10) { index ->
        Short(
            id = index,
            creatorName = "User_${index + 1}",
            description = when(index) {
                0 -> "Big Buck Bunny - Amazing animation! #animation #bunny"
                1 -> "Elephants Dream - Surreal journey. #art #surreal"
                2 -> "For Bigger Blazes - Check this out! #fire #blazes"
                3 -> "For Bigger Escapes - Ultimate escape. #escape #adventure"
                4 -> "For Bigger Fun - Pure entertainment. #fun #entertainment"
                5 -> "For Bigger Joyrides - Let's ride! #joyride #cars"
                6 -> "For Bigger Meltdowns - Intense! #meltdown #intense"
                7 -> "Sintel - A brave story. #sintel #dragon"
                8 -> "Subaru Outback - Offroad fun. #subaru #offroad"
                9 -> "Tears of Steel - Sci-fi masterpiece. #scifi #vfx"
                else -> "Funny video #${index + 1}! #short #trending #viral"
            },
            likes = "${(10..999).random()}K",
            comments = "${(100..9999).random()}",
            shares = "${(5..99).random()}K",
            videoUrl = videoUrls[index % videoUrls.size],
            thumbnailColor = Color(
                (0..255).random(),
                (0..255).random(),
                (0..255).random()
            ),
            profilePictureColor = Color(
                (0..255).random(),
                (0..255).random(),
                (0..255).random()
            )
        )
    }
}
