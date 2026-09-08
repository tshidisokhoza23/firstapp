package com.example.firstapp.search.data.repository

import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.chat.model.Message
import com.example.firstapp.chat.model.*
import com.example.firstapp.common.data.api.UserApi
import com.example.firstapp.common.util.Resource
import com.example.firstapp.search.domain.repository.SearchRepository
import com.example.firstapp.search.model.SearchFilter
import com.example.firstapp.search.model.SearchResult
import com.example.firstapp.search.model.SearchType
import com.example.firstapp.domain.repository.ChatRepository
import com.example.firstapp.domain.repository.JobRepository
import com.example.firstapp.channels.domain.ChannelRepository
import com.example.firstapp.music.domain.repository.MusicRepository
import com.example.firstapp.common.data.repository.*
import com.example.firstapp.chat.data.repository.*
import com.example.firstapp.media.data.repository.*
import com.example.firstapp.music.model.Song
import com.example.firstapp.music.data.repository.toModel
import com.example.firstapp.chat.jobs.model.Job
import com.example.firstapp.common.ui.theme.AppPrimary
import kotlinx.coroutines.flow.*
import kotlin.collections.*
import androidx.compose.ui.graphics.Color

import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val chatRepository: ChatRepository,
    private val musicRepository: MusicRepository,
    private val jobRepository: JobRepository,
    private val channelRepository: ChannelRepository,
    private val sessionManager: com.example.firstapp.common.domain.auth.SessionManager
) : SearchRepository {

    override fun search(query: String, type: SearchType, filter: SearchFilter): Flow<Resource<SearchResult>> = flow {
        emit(Resource.Loading())
        
        val typeStr = when (type) {
            SearchType.CHATS -> "chats"
            SearchType.JOBS -> "jobs"
            SearchType.CHANNELS -> "channels"
            SearchType.MUSIC -> "music"
            SearchType.GLOBAL -> "global"
        }

        when (type) {
            SearchType.CHATS -> {
                val localChats = chatRepository.searchLocalChats(query).firstOrNull() ?: emptyList()
                val localContacts = chatRepository.searchLocalContacts(query).firstOrNull() ?: emptyList()
                val localMessages = chatRepository.searchMessagesAcrossAll(query, filter).firstOrNull() ?: emptyList()
                
                emit(Resource.Success(SearchResult(
                    chats = localChats,
                    contacts = localContacts,
                    messages = localMessages
                )))
                
                try {
                    val response = userApi.search(query, typeStr)
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        val remoteUsers = (body.contacts as List<com.example.firstapp.common.data.api.ApiUser>? ?: emptyList()).map { 
                            mapApiContact(it)
                        }


                        val allContacts = (localContacts + remoteUsers).associateBy { it.id }.values.toList()
                        emit(Resource.Success(SearchResult(
                            chats = localChats,
                            contacts = allContacts,
                            messages = localMessages
                        )))
                    }
                } catch (_: Exception) {}
            }
            SearchType.JOBS -> {
                val localRes = jobRepository.searchJobs(query).firstOrNull()
                val localJobs = localRes?.data ?: emptyList()
                emit(Resource.Success(SearchResult(jobs = localJobs)))

                try {
                    val response = userApi.search(query, typeStr)
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        val remoteJobs = body.jobs.map { it.toModel() }
                        val allJobs = (localJobs + remoteJobs).associateBy { it.id }.values.toList()
                        emit(Resource.Success(SearchResult(
                            jobs = allJobs
                        )))
                    }
                } catch (_: Exception) {}
            }
            SearchType.CHANNELS -> {
                val localChannels = channelRepository.searchChannels(query).firstOrNull() ?: emptyList()
                emit(Resource.Success(SearchResult(channels = localChannels)))

                try {
                    val response = userApi.search(query, typeStr)
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        val remoteChannels = body.channels.map { mapApiChannel(it) }
                        val allChannels = (localChannels + remoteChannels).associateBy { it.id }.values.toList()
                        emit(Resource.Success(SearchResult(
                            channels = allChannels
                        )))
                    }
                } catch (_: Exception) {}
            }
            SearchType.MUSIC -> {
                try {
                    val response = userApi.search(query, typeStr)
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        emit(Resource.Success(SearchResult(
                            songs = body.songs.map { it.toModel() },
                            artists = body.artists.map { mapApiContact(it) }
                        )))
                    }
                } catch (_: Exception) {}
            }
            SearchType.GLOBAL -> {
                val localChats = chatRepository.searchLocalChats(query).firstOrNull() ?: emptyList()
                val localContacts = chatRepository.searchLocalContacts(query).firstOrNull() ?: emptyList()
                val localMessages = chatRepository.searchMessagesAcrossAll(query, filter).firstOrNull() ?: emptyList()
                val localChannels = channelRepository.searchChannels(query).firstOrNull() ?: emptyList()
                
                emit(Resource.Success(SearchResult(
                    chats = localChats,
                    contacts = localContacts,
                    messages = localMessages,
                    channels = localChannels
                )))

                try {
                    val response = userApi.search(query, typeStr)
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        val myUserId = sessionManager.getUserId()
                        emit(Resource.Success(SearchResult(
                            chats = (body.chats as List<com.example.firstapp.chat.data.api.ChatSummary>? ?: emptyList()).map { mapApiChatSummary(it) },
                            contacts = (body.contacts as List<com.example.firstapp.common.data.api.ApiUser>? ?: emptyList()).map { mapApiContact(it) },
                            messages = (body.messages as List<com.example.firstapp.chat.data.api.ApiMessage>? ?: emptyList()).map { mapApiMessage(it, currentUserId = myUserId) },
                            songs = (body.songs as List<com.example.firstapp.music.data.api.ApiSong>? ?: emptyList()).map { it.toModel() },
                            artists = (body.artists as List<com.example.firstapp.common.data.api.ApiUser>? ?: emptyList()).map { mapApiContact(it) },
                            channels = (body.channels as List<com.example.firstapp.media.data.api.ApiChannel>? ?: emptyList()).map { mapApiChannel(it) },
                            jobs = (body.jobs as List<com.example.firstapp.chat.jobs.data.api.ApiJob>? ?: emptyList()).map { mapApiJob(it) },
                            posts = (body.posts as List<com.example.firstapp.media.data.api.ApiPost>? ?: emptyList()).map { mapApiPost(it) }
                        )))


                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun mapApiJob(apiJob: com.example.firstapp.chat.jobs.data.api.ApiJob): Job {
        return Job(
            id = apiJob.id,
            title = apiJob.title,
            companyName = apiJob.company_name,
            companyLogoUrl = formatUrl(apiJob.company_logo_url),
            location = apiJob.location,
            logoColor = AppPrimary,
            salaryRange = apiJob.salary_range ?: "",
            jobType = apiJob.job_type ?: "Full-time",
            postedTime = apiJob.posted_time,
            description = apiJob.description ?: "",
            requirements = apiJob.requirements ?: emptyList(),
            isVerified = apiJob.is_verified,
            isApplied = apiJob.is_applied,
            isSaved = apiJob.is_saved
        )
    }
}
