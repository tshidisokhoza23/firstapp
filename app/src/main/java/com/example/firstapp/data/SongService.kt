package com.example.firstapp.data

import androidx.compose.ui.graphics.Color
import com.example.firstapp.model.Song

object SongService {
    val dummySongs = createDummySongs()

    private fun createDummySongs(): List<Song> {
        val songs = mutableListOf<Song>()
        val categories = listOf("Pop", "Rock", "Hip Hop", "R&B", "Jazz", "Classical", "Electronic", "Country", "Indie", "Focus")
        val artists = mapOf(
            "Pop" to listOf("The Weeknd", "Dua Lipa", "Justin Bieber", "Ed Sheeran", "Olivia Rodrigo", "Harry Styles", "Taylor Swift", "Ariana Grande", "Billie Eilish", "Katy Perry"),
            "Rock" to listOf("Queen", "Led Zeppelin", "Pink Floyd", "The Beatles", "Arctic Monkeys", "Imagine Dragons", "Coldplay", "The Killers", "U2", "Nirvana"),
            "Hip Hop" to listOf("Drake", "Kanye West", "Kendrick Lamar", "J. Cole", "Travis Scott", "Eminem", "Post Malone", "Lil Nas X", "Jack Harlow", "Future"),
            "R&B" to listOf("SZA", "Frank Ocean", "Daniel Caesar", "H.E.R.", "Khalid", "Brent Faiyaz", "Giveon", "Summer Walker", "Jhene Aiko", "Lucky Daye"),
            "Jazz" to listOf("Miles Davis", "John Coltrane", "Louis Armstrong", "Ella Fitzgerald", "Duke Ellington", "Charlie Parker", "Thelonious Monk", "Bill Evans", "Nina Simone", "Chet Baker"),
            "Classical" to listOf("Beethoven", "Mozart", "Bach", "Chopin", "Tchaikovsky", "Debussy", "Vivaldi", "Schubert", "Wagner", "Brahms"),
            "Electronic" to listOf("Daft Punk", "Avicii", "Calvin Harris", "Skrillex", "Marshmello", "Zedd", "The Chainsmokers", "Martin Garrix", "Kygo", "Flume"),
            "Country" to listOf("Luke Combs", "Morgan Wallen", "Carrie Underwood", "Blake Shelton", "Kane Brown", "Thomas Rhett", "Dolly Parton", "Johnny Cash", "Taylor Swift", "Tim McGraw"),
            "Indie" to listOf("Tame Impala", "Glass Animals", "Bon Iver", "The 1975", "Beach House", "Lana Del Rey", "Mitski", "Phoebe Bridgers", "Vampire Weekend", "Mac DeMarco"),
            "Focus" to listOf("Study Beats", "Lofi Girl", "Deep Focus", "Rain Sounds", "Binaural Beats", "Chillhop", "Ambient Flow", "Piano Study", "Zen Garden", "White Noise")
        )

        val colors = listOf(
            Color(0xFFE91E63), Color(0xFF9C27B0), Color(0xFF673AB7), Color(0xFF3F51B5),
            Color(0xFF2196F3), Color(0xFF03A9F4), Color(0xFF00BCD4), Color(0xFF009688),
            Color(0xFF4CAF50), Color(0xFF8BC34A), Color(0xFFCDDC39), Color(0xFFFFEB3B),
            Color(0xFFFFC107), Color(0xFFFF9800), Color(0xFFFF5722), Color(0xFF795548)
        )

        var id = 1
        categories.forEach { category ->
            val categoryArtists = artists[category] ?: listOf("Unknown Artist")
            for (i in 1..20) {
                val artist = categoryArtists[i % categoryArtists.size]
                songs.add(
                    Song(
                        id = id++,
                        title = "$category Track $i",
                        artist = artist,
                        artistId = id.toString(),
                        albumName = "$category Album ${((i-1)/5) + 1}",
                        albumArtUrl = null,
                        albumArtColor = colors[id % colors.size],
                        audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-${(id % 15) + 1}.mp3",
                        duration = "${(2..5).random()}:${(10..59).random()}",
                        category = category,
                        releaseDate = "2023-${(1..12).random()}-${(1..28).random()}",
                        playCount = (1000..1000000).random().toString(),
                        isLiked = i % 3 == 0,
                        isExplicit = i % 7 == 0
                    )
                )
            }
        }
        return songs
    }
}
