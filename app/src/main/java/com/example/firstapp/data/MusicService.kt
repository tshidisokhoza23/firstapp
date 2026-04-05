package com.example.firstapp.data

import androidx.compose.ui.graphics.Color
import com.example.firstapp.model.Song

object MusicService {
    val dummySongs = listOf(
        Song(
            id = 1,
            title = "Sleepless",
            artist = "The Big Hash",
            albumArtColor = Color(0xFFE91E63),
            duration = "3:45",
            category = "Trending"
        ),
        Song(
            id = 2,
            title = "Amandla",
            artist = "Kabza De Small",
            albumArtColor = Color(0xFF9C27B0),
            duration = "6:12",
            category = "Amapiano"
        ),
        Song(
            id = 3,
            title = "Imithandazo",
            artist = "Mthunzi",
            albumArtColor = Color(0xFF673AB7),
            duration = "5:34",
            category = "Amapiano"
        ),
        Song(
            id = 4,
            title = "Paris",
            artist = "Masego",
            albumArtColor = Color(0xFF3F51B5),
            duration = "3:20",
            category = "Soul"
        ),
        Song(
            id = 5,
            title = "Starboy",
            artist = "The Weeknd",
            albumArtColor = Color(0xFF2196F3),
            duration = "3:50",
            category = "Pop"
        ),
        Song(
            id = 6,
            title = "Bling",
            artist = "Tyla",
            albumArtColor = Color(0xFF00BCD4),
            duration = "2:55",
            category = "Pop"
        ),
        Song(
            id = 7,
            title = "Mnike",
            artist = "Tyler ICU",
            albumArtColor = Color(0xFF009688),
            duration = "7:10",
            category = "Amapiano"
        ),
        Song(
            id = 8,
            title = "Dalie",
            artist = "Kamo Mphela",
            albumArtColor = Color(0xFF4CAF50),
            duration = "4:20",
            category = "Amapiano"
        ),
        Song(
            id = 9,
            title = "Lemons",
            artist = "AKA",
            albumArtColor = Color(0xFF8BC34A),
            duration = "3:15",
            category = "Hip Hop"
        ),
        Song(
            id = 10,
            title = "SETE",
            artist = "K.O",
            albumArtColor = Color(0xFFCDDC39),
            duration = "4:05",
            category = "Hip Hop"
        )
    ) + List(20) { index ->
        Song(
            id = index + 11,
            title = "Song ${index + 11}",
            artist = "Artist ${index + 1}",
            albumArtColor = Color((0..255).random(), (0..255).random(), (0..255).random()),
            duration = "4:00",
            category = if (index % 2 == 0) "Recommended" else "New Releases"
        )
    }
}
