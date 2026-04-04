package com.example.firstapp.data

import androidx.compose.ui.graphics.Color
import com.example.firstapp.model.Song

object MusicService {
    val dummySongs = listOf(
        Song(1, "Sleepless", "The Big Hash", Color(0xFFE91E63), "3:45", "Trending"),
        Song(2, "Amandla", "Kabza De Small", Color(0xFF9C27B0), "6:12", "Amapiano"),
        Song(3, "Imithandazo", "Mthunzi", Color(0xFF673AB7), "5:34", "Amapiano"),
        Song(4, "Paris", "Masego", Color(0xFF3F51B5), "3:20", "Soul"),
        Song(5, "Starboy", "The Weeknd", Color(0xFF2196F3), "3:50", "Pop"),
        Song(6, "Bling", "Tyla", Color(0xFF00BCD4), "2:55", "Pop"),
        Song(7, "Mnike", "Tyler ICU", Color(0xFF009688), "7:10", "Amapiano"),
        Song(8, "Dalie", "Kamo Mphela", Color(0xFF4CAF50), "4:20", "Amapiano"),
        Song(9, "Lemons", "AKA", Color(0xFF8BC34A), "3:15", "Hip Hop"),
        Song(10, "SETE", "K.O", Color(0xFFCDDC39), "4:05", "Hip Hop")
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
