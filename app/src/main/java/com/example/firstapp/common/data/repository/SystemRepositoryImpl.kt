package com.example.firstapp.common.data.repository

import com.example.firstapp.common.data.api.SystemApi
import com.example.firstapp.common.data.api.DevResetRequest
import com.example.firstapp.common.data.api.DevResetResponse
import com.example.firstapp.common.data.local.dao.*
import com.example.firstapp.music.data.local.dao.*
import com.example.firstapp.common.domain.repository.SystemRepository
import com.example.firstapp.common.util.ContactUtils
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

import javax.inject.Inject

class SystemRepositoryImpl @Inject constructor(
    private val systemApi: SystemApi,
    private val chatDao: ChatDao,
    private val songDao: SongDao,
    private val jobDao: JobDao,
    private val advertDao: AdvertDao,
    private val channelDao: ChannelDao,
    private val postDao: PostDao,
    private val storyDao: StoryDao,
    private val callLogDao: CallLogDao,
    private val contactDao: ContactDao,
    private val jobApplicationDao: JobApplicationDao,
    private val driveDao: DriveDao,
    private val playlistDao: PlaylistDao
) : SystemRepository {

    override suspend fun clearCache() {
        chatDao.clearChats()
        chatDao.clearAllMessages()
        callLogDao.clearCallLogs()
        jobDao.clearJobs()
        songDao.clearSongs()
        postDao.clearPosts()
        storyDao.clearStories()
        advertDao.clearAdverts()
        channelDao.clearChannels()
        playlistDao.clearPlaylists()
    }

    override suspend fun clearAllData() {
        clearCache()
        contactDao.clearContacts()
        jobApplicationDao.clearApplications()
        driveDao.clearRideOptions()
        driveDao.clearDestinations()
    }

    override suspend fun devResetAndSeed(contacts: List<ContactUtils.ContactInfo>): Flow<Resource<DevResetResponse>> = flow {
        emit(Resource.Loading())
        try {
            val res = systemApi.devResetAndSeed(DevResetRequest(contacts))
            if (res.isSuccessful && res.body() != null) {
                clearAllData()
                emit(Resource.Success(res.body()!!))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error"))
        }
    }
}
