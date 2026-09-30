package com.tpsk.tj.common.data.repository

import com.tpsk.tj.common.data.api.SystemApi
import com.tpsk.tj.common.data.api.DevResetRequest
import com.tpsk.tj.common.data.api.DevResetResponse
import com.tpsk.tj.chat.data.local.dao.*
import com.tpsk.tj.media.data.local.dao.*
import com.tpsk.tj.chat.jobs.data.local.dao.*
import com.tpsk.tj.drive.data.local.dao.*
import com.tpsk.tj.music.data.local.dao.*
import com.tpsk.tj.common.domain.repository.SystemRepository
import com.tpsk.tj.common.util.ContactUtils
import com.tpsk.tj.common.util.Resource
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
        postDao.clearAllPosts()
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
