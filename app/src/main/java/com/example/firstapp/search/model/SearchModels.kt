package com.example.firstapp.search.model

import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.chat.model.Message
import com.example.firstapp.music.model.Song
import com.example.firstapp.chat.jobs.model.Job
import com.example.firstapp.media.model.Channel

data class SearchResult(
    val chats: List<ChatMessage> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val messages: List<Message> = emptyList(),
    val songs: List<Song> = emptyList(),
    val artists: List<Contact> = emptyList(),
    val jobs: List<Job> = emptyList(),
    val channels: List<Channel> = emptyList()
)

enum class SearchType {
    CHATS, JOBS, CHANNELS, MUSIC, GLOBAL
}

enum class SearchFilter {
    ALL, PHOTOS, VIDEOS, LINKS, GIFS, AUDIO, DOCUMENTS
}
