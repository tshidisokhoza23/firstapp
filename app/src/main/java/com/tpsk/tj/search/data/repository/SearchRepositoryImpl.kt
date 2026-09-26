package com.tpsk.tj.search.data.repository

import com.tpsk.tj.chat.model.ChatMessage
import com.tpsk.tj.chat.model.Contact
import com.tpsk.tj.chat.model.Message
import com.tpsk.tj.chat.model.*
import com.tpsk.tj.common.data.api.UserApi
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.search.domain.repository.SearchRepository
import com.tpsk.tj.search.model.SearchFilter
import com.tpsk.tj.search.model.SearchResult
import com.tpsk.tj.search.model.SearchType
import com.tpsk.tj.domain.repository.ChatRepository
import com.tpsk.tj.domain.repository.JobRepository
import com.tpsk.tj.channels.domain.ChannelRepository
import com.tpsk.tj.music.domain.repository.MusicRepository
import com.tpsk.tj.common.data.repository.*
import com.tpsk.tj.chat.data.repository.*
import com.tpsk.tj.media.data.repository.*
import com.tpsk.tj.music.model.Song
import com.tpsk.tj.music.data.repository.toModel
import com.tpsk.tj.chat.jobs.model.Job
import com.tpsk.tj.common.ui.theme.AppPrimary
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
    private val sessionManager: com.tpsk.tj.common.domain.auth.SessionManager
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
                        val remoteUsers = (body.contacts as List<com.tpsk.tj.common.data.api.ApiUser>? ?: emptyList()).map { 
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
                            chats = (body.chats as List<com.tpsk.tj.chat.data.api.ChatSummary>? ?: emptyList()).map { mapApiChatSummary(it) },
                            contacts = (body.contacts as List<com.tpsk.tj.common.data.api.ApiUser>? ?: emptyList()).map { mapApiContact(it) },
                            messages = (body.messages as List<com.tpsk.tj.chat.data.api.ApiMessage>? ?: emptyList()).map { mapApiMessage(it, currentUserId = myUserId) },
                            songs = (body.songs as List<com.tpsk.tj.music.data.api.ApiSong>? ?: emptyList()).map { it.toModel() },
                            artists = (body.artists as List<com.tpsk.tj.common.data.api.ApiUser>? ?: emptyList()).map { mapApiContact(it) },
                            channels = (body.channels as List<com.tpsk.tj.chat.data.api.ApiChannel>? ?: emptyList()).map { mapApiChannel(it) },
                            jobs = (body.jobs as List<com.tpsk.tj.chat.jobs.data.api.ApiJob>? ?: emptyList()).map { mapApiJob(it) },
                            posts = (body.posts as List<com.tpsk.tj.media.data.api.ApiPost>? ?: emptyList()).map { mapApiPost(it) }
                        )))


                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun mapApiJob(apiJob: com.tpsk.tj.chat.jobs.data.api.ApiJob): Job {
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
            isSaved = apiJob.is_saved,
            isExternal = apiJob.is_external ?: false,
            sourcePartner = apiJob.source_partner,
            trackingApplyUrl = apiJob.getTrackingUrl()
        )
    }
}
