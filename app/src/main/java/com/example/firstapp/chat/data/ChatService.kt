package com.example.firstapp.chat.data

import androidx.compose.ui.graphics.Color
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.media.model.*
import com.example.firstapp.music.model.Song
import com.example.firstapp.jobs.model.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ChatService {
    val dummyChats = emptyList<ChatMessage>()

    private val _dummyStories = MutableStateFlow<List<UserStory>>(emptyList())
    val dummyStories: StateFlow<List<UserStory>> = _dummyStories.asStateFlow()

    fun postStory(content: String, type: StoryType, backgroundColor: Color = Color.Gray) {
        // No-op for empty dummy stories
    }

    val dummyJobs = emptyList<Job>()
    val dummyWallets = emptyList<Advert>()
    val dummyShorts = emptyList<ShortMedia>()
    val dummySongs = emptyList<Song>()
    val dummyPosts = emptyList<Post>()
}
