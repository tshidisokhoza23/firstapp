package com.tpsk.tj.search.model

import com.tpsk.tj.chat.model.ChatMessage
import com.tpsk.tj.chat.model.Contact
import com.tpsk.tj.chat.model.Message
import com.tpsk.tj.music.model.Song
import com.tpsk.tj.chat.jobs.model.Job
import com.tpsk.tj.chat.model.Channel

import com.tpsk.tj.media.model.Post

data class SearchResult(
    val chats: List<ChatMessage> = emptyList(),
    val contacts: List<Contact> = emptyList(),
    val messages: List<Message> = emptyList(),
    val songs: List<Song> = emptyList(),
    val artists: List<Contact> = emptyList(),
    val jobs: List<Job> = emptyList(),
    val channels: List<Channel> = emptyList(),
    val posts: List<Post> = emptyList()
)

enum class SearchType {
    CHATS, JOBS, CHANNELS, MUSIC, GLOBAL
}

enum class SearchFilter {
    ALL, PHOTOS, VIDEOS, LINKS, GIFS, AUDIO, DOCUMENTS
}
