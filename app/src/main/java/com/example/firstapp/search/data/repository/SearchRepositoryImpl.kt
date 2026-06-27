package com.example.firstapp.search.data.repository

import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.chat.model.Message
import com.example.firstapp.common.data.api.UserApi
import com.example.firstapp.common.util.Resource
import com.example.firstapp.search.domain.repository.SearchRepository
import com.example.firstapp.search.model.SearchResult
import com.example.firstapp.search.model.SearchType
import com.example.firstapp.domain.repository.ChatRepository
import com.example.firstapp.domain.repository.JobRepository
import com.example.firstapp.media.domain.repository.ChannelRepository
import com.example.firstapp.music.domain.repository.MusicRepository
import com.example.firstapp.common.data.repository.*
import com.example.firstapp.music.model.Song
import kotlinx.coroutines.flow.*
import androidx.compose.ui.graphics.Color
import android.util.Log

import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val chatRepository: ChatRepository,
    private val musicRepository: MusicRepository,
    private val jobRepository: JobRepository,
    private val channelRepository: ChannelRepository
) : SearchRepository {

    override fun search(query: String, type: SearchType): Flow<Resource<SearchResult>> = flow {
        emit(Resource.Loading())
        
        when (type) {
            SearchType.CHATS -> {
                val localChats = chatRepository.searchLocalChats(query).firstOrNull() ?: emptyList()
                val localContacts = chatRepository.searchLocalContacts(query).firstOrNull() ?: emptyList()
                val localMessages = chatRepository.searchMessagesAcrossAll(query).firstOrNull() ?: emptyList()
                
                emit(Resource.Success(SearchResult(
                    chats = localChats,
                    contacts = localContacts,
                    messages = localMessages
                )))
                
                // Remote search for users by contact number only
                try {
                    val response = userApi.search(query)
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        // Filter remote users to only those matching query in phone_number
                        val users = body.contacts.map { mapApiContact(it) }.filter { 
                            it.phoneNumber.contains(query) 
                        }
                        emit(Resource.Success(SearchResult(
                            chats = localChats,
                            contacts = (localContacts + users).distinctBy { it.id },
                            messages = localMessages
                        )))
                    }
                } catch (_: Exception) {}
            }
            SearchType.JOBS -> {
                val localJobs = jobRepository.searchJobs(query).firstOrNull() ?: emptyList()
                emit(Resource.Success(SearchResult(jobs = localJobs)))
            }
            SearchType.CHANNELS -> {
                val localChannels = channelRepository.searchChannels(query).firstOrNull() ?: emptyList()
                emit(Resource.Success(SearchResult(channels = localChannels)))
            }
            SearchType.MUSIC -> {
                // Implement music search if needed
            }
            SearchType.GLOBAL -> {
                val localChats = chatRepository.searchLocalChats(query).firstOrNull() ?: emptyList()
                val localContacts = chatRepository.searchLocalContacts(query).firstOrNull() ?: emptyList()
                val localMessages = chatRepository.searchMessagesAcrossAll(query).firstOrNull() ?: emptyList()
                
                emit(Resource.Success(SearchResult(
                    chats = localChats,
                    contacts = localContacts,
                    messages = localMessages
                )))

                try {
                    val response = userApi.search(query)
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        emit(Resource.Success(SearchResult(
                            chats = body.chats.map { mapApiChatSummary(it) },
                            contacts = body.contacts.map { mapApiContact(it) },
                            messages = body.messages.map { mapApiMessage(it) },
                            songs = body.songs.map { mapApiSongToModel(it) },
                            artists = body.artists.map { mapApiContact(it) }
                        )))
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun mapApiSongToModel(apiSong: com.example.firstapp.music.data.api.ApiSong): Song {
        return Song(
            id = apiSong.id,
            title = apiSong.title,
            artist = apiSong.artist,
            artistId = apiSong.artist_id ?: "",
            albumName = apiSong.album_name ?: "",
            albumArtUrl = formatUrl(apiSong.album_art_url),
            albumArtColor = Color(0xFF6A1B9A),
            audioUrl = formatUrl(apiSong.audio_url),
            duration = apiSong.duration ?: "",
            category = apiSong.category ?: "",
            lyrics = apiSong.lyrics ?: "",
            releaseDate = apiSong.release_date ?: "",
            playCount = apiSong.play_count.toString(),
            isLiked = apiSong.is_liked,
            isExplicit = apiSong.is_explicit
        )
    }
}
