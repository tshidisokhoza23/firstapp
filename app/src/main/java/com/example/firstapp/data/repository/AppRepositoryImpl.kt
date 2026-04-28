package com.example.firstapp.data.repository

import android.util.Log
import com.example.firstapp.data.api.*
import com.example.firstapp.data.local.dao.*
import com.example.firstapp.data.local.entity.*
import com.example.firstapp.model.*
import com.example.firstapp.model.Short as AppShort
import com.example.firstapp.util.Resource
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.*
import retrofit2.HttpException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

class AppRepositoryImpl(
    private val chatDao: ChatDao,
    private val songDao: SongDao,
    private val jobDao: JobDao,
    private val advertDao: AdvertDao,
    private val channelDao: ChannelDao,
    private val postDao: PostDao,
    private val storyDao: StoryDao,
    private val shortDao: ShortDao,
    private val callLogDao: CallLogDao,
    private val contactDao: ContactDao,
    private val jobApplicationDao: JobApplicationDao,
    private val driveDao: DriveDao
) : AppRepository {

    private val apiService = ApiClient.apiService

    override fun getChats(): Flow<Resource<List<ChatMessage>>> = flow {
        val cached = chatDao.getChats().firstOrNull()?.map { it.toModel() } ?: emptyList()
        emit(Resource.Loading(cached))
        
        try {
            val response = apiService.getChats()
            if (response.isSuccessful) {
                val chats = response.body()?.chats?.map { mapApiChatSummary(it) } ?: emptyList()
                chatDao.insertChats(chats.map { it.toEntity() })
            }
        } catch (e: Exception) { Log.e("AppRepository", "getChats failed", e) }
        
        chatDao.getChats().collect { entities ->
            emit(Resource.Success(entities.map { it.toModel() }))
        }
    }

    override fun getCallLogs(): Flow<Resource<List<CallLog>>> = flow {
        val cached = callLogDao.getCallLogs().firstOrNull()?.map { it.toCallLog() } ?: emptyList()
        emit(Resource.Loading(cached))
        try {
            val response = apiService.getCalls()
            if (response.isSuccessful) {
                val calls = response.body()?.calls?.map { apiCall ->
                    CallLog(id = apiCall.id, sessionId = apiCall.session_id, callerId = apiCall.caller_id, calleeId = apiCall.callee_id, contactId = apiCall.contact_id, contactName = apiCall.contact_name, contactAvatar = apiCall.contact_avatar, callType = apiCall.call_type, status = apiCall.status, durationSeconds = apiCall.duration_seconds, startedAt = apiCall.started_at, endedAt = apiCall.ended_at)
                } ?: emptyList()
                callLogDao.clearCallLogs()
                callLogDao.insertCallLogs(calls.map { it.toCallLogEntity() })
            }
        } catch (e: Exception) { Log.e("AppRepository", "getCallLogs failed", e) }
        callLogDao.getCallLogs().collect { entities ->
            emit(Resource.Success(entities.map { it.toCallLog() }))
        }
    }

    override fun getMessages(chatId: Int, page: Int): Flow<Resource<List<Message>>> = flow {
        if (page == 1) {
            val cached = chatDao.getMessages(chatId).firstOrNull()?.map { it.toModel() } ?: emptyList()
            emit(Resource.Loading(cached))
            
            try {
                val response = apiService.getMessages(chatId, page)
                if (response.isSuccessful) {
                    val messages = response.body()?.messages?.map { mapApiMessage(it) } ?: emptyList()
                    chatDao.insertMessages(messages.map { it.toEntity(chatId) })
                }
            } catch (e: Exception) { Log.e("AppRepository", "getMessages sync failed", e) }
            
            chatDao.getMessages(chatId).collect { entities ->
                emit(Resource.Success(entities.map { it.toModel() }))
            }
        } else {
            emit(Resource.Loading())
            try {
                val response = apiService.getMessages(chatId, page)
                if (response.isSuccessful) {
                    val messages = response.body()?.messages?.map { mapApiMessage(it) } ?: emptyList()
                    chatDao.insertMessages(messages.map { it.toEntity(chatId) })
                    emit(Resource.Success(messages))
                } else {
                    emit(Resource.Error("Failed to load more"))
                }
            } catch (e: Exception) {
                emit(Resource.Error(e.toUserMessage()))
            }
        }
    }

    override fun markAsRead(chatId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.markAsRead(chatId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error(e.toUserMessage())) } }
    override fun deleteMessage(messageId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (apiService.deleteMessage(messageId).isSuccessful) {
                chatDao.deleteMessage(messageId)
                emit(Resource.Success(true))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }
    override fun editMessage(messageId: Int, newBody: String): Flow<Resource<Message>> = flow { try { emit(Resource.Loading()); val response = apiService.editMessage(messageId, EditMessageRequest(newBody)); if (response.isSuccessful && response.body() != null) emit(Resource.Success(mapApiMessage(response.body()!!.message))) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error(e.toUserMessage())) } }
    override fun reactToMessage(messageId: Int, emoji: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.reactToMessage(messageId, ReactionRequest(emoji)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error(e.toUserMessage())) } }

    override fun sendMessage(chatId: Int, body: String, messageType: String, mediaUrl: String?, duration: String?, fileName: String?, fileSize: String?, contactName: String?, contactPhone: String?, latitude: Double?, longitude: Double?, replyToId: Int?): Flow<Resource<Message>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.sendMessage(chatId, SendMessageRequest(body, messageType, mediaUrl, duration, fileName, fileSize, contactName, contactPhone, latitude, longitude, replyToId))
            if (response.isSuccessful && response.body() != null) {
                val msg = mapApiMessage(response.body()!!.message)
                chatDao.insertMessages(listOf(msg.toEntity(chatId)))
                emit(Resource.Success(msg))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Offline")) }
    }

    override suspend fun saveMessage(chatId: Int, message: Message) { chatDao.insertMessages(listOf(message.toEntity(chatId))) }
    override suspend fun updateMessageInCache(chatId: Int, message: Message) { chatDao.insertMessages(listOf(message.toEntity(chatId))) }
    override suspend fun updatePresence(chatId: Int, isOnline: Boolean, lastSeen: String) { chatDao.updatePresence(chatId, isOnline, lastSeen) }
    override suspend fun updateTyping(chatId: Int, isTyping: Boolean) { chatDao.updateTyping(chatId, isTyping) }
    override suspend fun updateLastMessage(chatId: Int, lastMessage: String, time: String) { chatDao.updateLastMessage(chatId, lastMessage, time) }
    override suspend fun updateWallpaper(chatId: Int, wallpaperUrl: String?) { chatDao.updateWallpaper(chatId, wallpaperUrl) }

    override fun getContacts(): Flow<Resource<List<Contact>>> = flow {
        val cached = contactDao.getContacts().firstOrNull()?.map { it.toContact() } ?: emptyList()
        emit(Resource.Loading(cached))
        try {
            val response = apiService.getContacts()
            if (response.isSuccessful) {
                val contacts = response.body()?.contacts?.map { mapApiContact(it) } ?: emptyList()
                contactDao.clearContacts(); contactDao.insertContacts(contacts.map { it.toContactEntity() })
            }
        } catch (e: Exception) { Log.e("AppRepository", "getContacts failed", e) }
        contactDao.getContacts().collect { entities ->
            emit(Resource.Success(entities.map { it.toContact() }))
        }
    }

    override fun getJobs(): Flow<Resource<List<Job>>> = flow {
        val cached = jobDao.getJobs().firstOrNull()?.map { it.toModel() } ?: emptyList()
        emit(Resource.Loading(cached))
        try {
            val response = apiService.getJobs()
            if (response.isSuccessful) {
                val jobs = response.body()?.jobs?.map { apiJob -> Job(id = apiJob.id, title = apiJob.title, companyName = apiJob.company_name, companyLogoUrl = apiJob.company_logo_url, location = apiJob.location, logoColor = com.example.firstapp.ui.theme.BoyishPurple, salaryRange = apiJob.salary_range ?: "", jobType = apiJob.job_type ?: "Full-time", postedTime = apiJob.posted_time ?: "", description = apiJob.description ?: "", requirements = apiJob.requirements ?: emptyList(), isVerified = apiJob.is_verified, isApplied = false, isSaved = false) } ?: emptyList()
                jobDao.clearJobs(); jobDao.insertJobs(jobs.map { it.toEntity() })
            }
        } catch (e: Exception) { Log.e("AppRepository", "getJobs failed", e) }
        jobDao.getJobs().collect { entities ->
            emit(Resource.Success(entities.map { it.toModel() }))
        }
    }

    override fun applyForJob(jobId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.applyForJob(jobId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }

    override fun getJobApplications(): Flow<Resource<List<JobApplication>>> = flow {
        emit(Resource.Loading())
        jobApplicationDao.getApplications().firstOrNull()?.let { entities -> if (entities.isNotEmpty()) emit(Resource.Success(entities.map { it.toModel() })) }
        try {
            val response = apiService.getJobApplications()
            if (response.isSuccessful) {
                val applications = response.body()?.applications?.map { apiApp -> JobApplication(id = apiApp.id, jobId = apiApp.job_id, jobTitle = apiApp.title ?: "Job #${apiApp.job_id}", companyName = apiApp.company_name ?: "Unknown company", appliedDate = apiApp.applied_date, status = when (apiApp.status.lowercase()) { "reviewing" -> ApplicationStatus.REVIEWING; "interview" -> ApplicationStatus.INTERVIEW; "rejected" -> ApplicationStatus.REJECTED; "offered" -> ApplicationStatus.OFFERED; else -> ApplicationStatus.PENDING }, logoColor = com.example.firstapp.ui.theme.NiceOrange) } ?: emptyList()
                jobApplicationDao.clearApplications(); jobApplicationDao.insertApplications(applications.map { it.toEntity() })
            }
        } catch (e: Exception) { Log.e("AppRepository", "getJobApps failed", e) }
        jobApplicationDao.getApplications().collect { emit(Resource.Success(it.map { it.toModel() })) }
    }

    override fun getAdverts(): Flow<Resource<List<Advert>>> = flow {
        emit(Resource.Loading())
        advertDao.getAdverts().firstOrNull()?.let { entities -> if (entities.isNotEmpty()) emit(Resource.Success(entities.map { it.toModel() })) }
        try {
            val response = apiService.getAdverts()
            if (response.isSuccessful) {
                val adverts = response.body()?.adverts?.map { mapApiAdvert(it) } ?: emptyList()
                advertDao.clearAdverts(); advertDao.insertAdverts(adverts.map { it.toEntity() })
            }
        } catch (e: Exception) { Log.e("AppRepository", "getAdverts failed", e) }
        advertDao.getAdverts().collect { emit(Resource.Success(it.map { it.toModel() })) }
    }

    override fun getChannels(): Flow<Resource<List<Channel>>> = flow {
        emit(Resource.Loading())
        channelDao.getChannels().firstOrNull()?.let { entities -> if (entities.isNotEmpty()) emit(Resource.Success(entities.map { it.toModel() })) }
        try {
            val response = apiService.getChannels()
            if (response.isSuccessful) {
                val channels = response.body()?.channels?.map { mapApiChannel(it) } ?: emptyList()
                channelDao.clearChannels(); channelDao.insertChannels(channels.map { it.toEntity() })
            }
        } catch (e: Exception) { Log.e("AppRepository", "getChannels failed", e) }
        channelDao.getChannels().collect { emit(Resource.Success(it.map { it.toModel() })) }
    }

    override fun getShorts(): Flow<Resource<List<AppShort>>> = flow {
        emit(Resource.Loading())
        shortDao.getShorts().firstOrNull()?.let { entities -> if (entities.isNotEmpty()) emit(Resource.Success(entities.map { it.toModel() })) }
        try {
            val response = apiService.getShorts()
            if (response.isSuccessful) {
                val apiShorts = response.body()?.shorts ?: emptyList()
                val shorts = apiShorts.map { apiShort -> AppShort(id = apiShort.id, creatorName = apiShort.creator_name, description = apiShort.description, likes = apiShort.likes.toString(), comments = apiShort.comments.toString(), shares = apiShort.shares.toString(), videoUrl = apiShort.video_url ?: "", thumbnailUrl = apiShort.thumbnail_url, thumbnailColor = androidx.compose.ui.graphics.Color.Black, profilePictureColor = androidx.compose.ui.graphics.Color.Gray, isLiked = apiShort.is_liked, isSubscribed = apiShort.is_subscribed) }
                shortDao.clearShorts(); shortDao.insertShorts(shorts.map { it.toEntity() })
            }
        } catch (e: Exception) { Log.e("AppRepository", "getShorts failed", e) }
        shortDao.getShorts().collect { emit(Resource.Success(it.map { it.toModel() })) }
    }

    override fun createShort(description: String, videoUrl: String, thumbnailUrl: String): Flow<Resource<Long>> = flow { try { emit(Resource.Loading()); val response = apiService.createShort(CreateShortRequest(description, videoUrl, thumbnailUrl)); if (response.isSuccessful && response.body() != null) emit(Resource.Success(response.body()!!.id)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error(e.toUserMessage())) } }
    override fun repostShort(shortId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.repostShort(shortId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun bookmarkShort(shortId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.bookmarkShort(shortId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun replyToShort(shortId: Int, content: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.replyToShort(shortId, CreateShortReplyRequest(content)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun getShortReplies(shortId: Int): Flow<Resource<List<ShortReply>>> = flow { emit(Resource.Loading()); try { val response = apiService.getShortReplies(shortId); if (response.isSuccessful) emit(Resource.Success(response.body()?.replies?.map { ShortReply(it.id, it.author?.display_name ?: "Unknown", it.author?.username ?: "", it.author?.avatar_url, androidx.compose.ui.graphics.Color.Gray, it.author?.is_verified ?: false, it.content, it.created_at ?: "", it.likes.toString(), it.is_liked) } ?: emptyList())) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }

    override fun getSongs(): Flow<Resource<List<Song>>> = flow {
        emit(Resource.Loading())
        songDao.getSongs().firstOrNull()?.let { entities -> if (entities.isNotEmpty()) emit(Resource.Success(entities.map { it.toModel() })) }
        try {
            val response = apiService.getSongs()
            if (response.isSuccessful && response.body() != null) {
                val songs = response.body()!!.songs.map { apiSong -> Song(id = apiSong.id, title = apiSong.title, artist = apiSong.artist, artistId = apiSong.artist_id ?: "", albumName = apiSong.album_name ?: "", albumArtUrl = apiSong.album_art_url, albumArtColor = androidx.compose.ui.graphics.Color(0xFF6A1B9A), audioUrl = apiSong.audio_url ?: "", duration = apiSong.duration ?: "", category = apiSong.category ?: "", lyrics = apiSong.lyrics ?: "", releaseDate = apiSong.release_date ?: "", playCount = apiSong.play_count.toString(), isLiked = apiSong.is_liked, isExplicit = apiSong.is_explicit) }
                songDao.clearSongs(); songDao.insertSongs(songs.map { it.toEntity() })
            }
        } catch (e: Exception) { Log.e("AppRepository", "getSongs failed", e) }
        songDao.getSongs().collect { emit(Resource.Success(it.map { it.toModel() })) }
    }

    override fun getPosts(): Flow<Resource<List<Post>>> = flow {
        emit(Resource.Loading())
        postDao.getPosts().firstOrNull()?.let { entities -> if (entities.isNotEmpty()) emit(Resource.Success(entities.map { it.toModel() })) }
        try {
            val response = apiService.getPosts()
            if (response.isSuccessful) {
                val posts = response.body()?.posts?.map { mapApiPost(it) } ?: emptyList()
                postDao.clearPosts(); postDao.insertPosts(posts.map { it.toEntity() })
            }
        } catch (e: Exception) { Log.e("AppRepository", "getPosts failed", e) }
        postDao.getPosts().collect { emit(Resource.Success(it.map { it.toModel() })) }
    }

    override fun createPost(content: String, mediaUrls: List<String>, postType: String?, tags: List<String>): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.createPost(CreatePostRequest(content, mediaUrls, postType ?: "text_only", null, tags)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun likePost(postId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.likePost(postId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun repostPost(postId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.repostPost(postId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun bookmarkPost(postId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.bookmarkPost(postId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun createPostReply(postId: Int, content: String, parentReplyId: Int?): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.createPostReply(postId, CreatePostReplyRequest(content, parentReplyId)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun likePostReply(replyId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.likePostReply(replyId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun repostPostReply(replyId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.repostPostReply(replyId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun bookmarkPostReply(replyId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.bookmarkPostReply(replyId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }

    override fun getStories(): Flow<Resource<List<UserStory>>> = flow {
        emit(Resource.Loading())
        storyDao.getStories().firstOrNull()?.let { entities -> if (entities.isNotEmpty()) emit(Resource.Success(entities.map { it.toModel() })) }
        try {
            val response = apiService.getStories()
            if (response.isSuccessful) {
                val stories = response.body()?.stories?.map { mapApiStory(it) } ?: emptyList()
                storyDao.clearStories(); storyDao.insertStories(stories.map { it.toEntity() })
            }
        } catch (e: Exception) { Log.e("AppRepository", "getStories failed", e) }
        storyDao.getStories().collect { emit(Resource.Success(it.map { it.toModel() })) }
    }

    override fun createStory(content: String, contentUrl: String?, type: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.createStory(CreateStoryRequest(content, contentUrl, type)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }

    override fun markStoryAsViewed(userId: Int, storyId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            // Local update first
            val stories = storyDao.getStories().first()
            val entity = stories.find { it.id == userId }
            if (entity != null) {
                val updatedItems = entity.stories.map {
                    if (it.id == storyId) it.copy(isViewed = true) else it
                }
                storyDao.insertStories(listOf(entity.copy(stories = updatedItems)))
            }
            // Remote update
            apiService.markStoryViewed(storyId)
            emit(Resource.Success(true))
        } catch (e: Exception) { emit(Resource.Error("Local update failed")) }
    }

    override fun muteStory(userId: Int, isMuted: Boolean): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            // Local update
            val stories = storyDao.getStories().first()
            val entity = stories.find { it.id == userId }
            if (entity != null) {
                storyDao.insertStories(listOf(entity.copy(isMuted = isMuted)))
            }
            // Remote update
            apiService.muteUser(userId, MuteRequest(isMuted))
            emit(Resource.Success(true))
        } catch (e: Exception) { emit(Resource.Error("Mute failed")) }
    }

    override fun syncContacts(phoneNumbers: List<String>): Flow<Resource<List<Contact>>> = flow { try { emit(Resource.Loading()); val response = apiService.syncContacts(SyncContactsRequest(phoneNumbers)); if (response.isSuccessful) emit(Resource.Success(response.body()?.contacts?.map { mapApiContact(it) } ?: emptyList())) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override suspend fun createChat(contactId: Int) { try { apiService.createChat(CreateChatRequest(contactId)) } catch (_: Exception) {} }
    override suspend fun createGroup(name: String, contactIds: List<Int>): Flow<Resource<ChatMessage>> = flow { try { emit(Resource.Loading()); val response = apiService.createGroup(CreateGroupRequest(name, contactIds)); if (response.isSuccessful && response.body() != null) { val chat = mapApiChatSummary(response.body()!!); chatDao.insertChats(listOf(chat.toEntity())); emit(Resource.Success(chat)) } else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun pinChat(chatId: Int, isPinned: Boolean): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.pinChat(chatId, PinRequest(isPinned)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun muteChat(chatId: Int, isMuted: Boolean): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.muteChat(chatId, MuteRequest(isMuted)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun deleteChat(chatId: Int): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (apiService.deleteChat(chatId).isSuccessful) { chatDao.clearMessages(chatId); emit(Resource.Success(true)) } else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override suspend fun clearMessages(chatId: Int) { chatDao.clearMessages(chatId) }

    override fun search(query: String): Flow<Resource<SearchResult>> = flow {
        emit(Resource.Loading())
        val localChats = chatDao.searchChats(query).firstOrNull()?.map { it.toModel() } ?: emptyList()
        val localMessages = chatDao.searchMessagesAcrossAll(query).firstOrNull()?.map { it.toModel() } ?: emptyList()
        val localContacts = contactDao.searchContacts(query).firstOrNull()?.map { it.toContact() } ?: emptyList()
        emit(Resource.Success(SearchResult(localChats, localContacts, localMessages)))
        try {
            val response = apiService.search(query)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                emit(Resource.Success(SearchResult(
                    body.chats.map { mapApiChatSummary(it) }, 
                    body.contacts.map { mapApiContact(it) }, 
                    body.messages.map { mapApiMessage(it) }
                )))
            }
        } catch (_: Exception) {}
    }

    override fun searchLocalMessages(chatId: Int, query: String): Flow<List<Message>> = chatDao.searchMessages(chatId, query).map { it.map { m -> m.toModel() } }
    override fun searchLocalChats(query: String): Flow<List<ChatMessage>> = chatDao.searchChats(query).map { it.map { c -> c.toModel() } }
    override fun searchLocalContacts(query: String): Flow<List<Contact>> = contactDao.searchContacts(query).map { it.map { c -> c.toContact() } }
    override fun searchShorts(query: String): Flow<Resource<List<AppShort>>> = flow { emit(Resource.Loading()); try { val response = apiService.searchShorts(query); if (response.isSuccessful) emit(Resource.Success(response.body()?.shorts?.map { apiShort -> AppShort(id = apiShort.id, creatorName = apiShort.creator_name, description = apiShort.description, likes = apiShort.likes.toString(), comments = apiShort.comments.toString(), shares = apiShort.shares.toString(), videoUrl = apiShort.video_url ?: "", thumbnailUrl = apiShort.thumbnail_url, thumbnailColor = androidx.compose.ui.graphics.Color.Black, profilePictureColor = androidx.compose.ui.graphics.Color.Gray, isLiked = apiShort.is_liked, isSubscribed = apiShort.is_subscribed) } ?: emptyList())) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun getRideOptions(): Flow<Resource<List<RideOption>>> = flow {
        emit(Resource.Loading())
        driveDao.getRideOptions().firstOrNull()?.let { entities -> if (entities.isNotEmpty()) emit(Resource.Success(entities.map { it.toModel() })) }
        try {
            val response = apiService.getRideOptions()
            if (response.isSuccessful && response.body() != null) {
                val options = response.body()!!.ride_options
                driveDao.clearRideOptions(); driveDao.insertRideOptions(options.map { it.toEntity() })
            }
        } catch (_: Exception) {}
        driveDao.getRideOptions().collect { emit(Resource.Success(it.map { it.toModel() })) }
    }

    override fun getDestinations(): Flow<Resource<List<Destination>>> = flow {
        emit(Resource.Loading())
        driveDao.getDestinations().firstOrNull()?.let { entities -> if (entities.isNotEmpty()) emit(Resource.Success(entities.map { it.toModel() })) }
        try {
            val response = apiService.getDestinations()
            if (response.isSuccessful && response.body() != null) {
                val destinations = response.body()!!.destinations
                driveDao.insertDestinations(destinations.map { it.toEntity() })
            }
        } catch (_: Exception) {}
        driveDao.getDestinations().collect { emit(Resource.Success(it.map { it.toModel() })) }
    }

    override fun viewPost(postId: Int): Flow<Resource<Boolean>> = flow { try { apiService.viewPost(postId); emit(Resource.Success(true)) } catch (_: Exception) {} }
    override fun deletePost(postId: Int): Flow<Resource<Boolean>> = flow { try { apiService.deletePost(postId); emit(Resource.Success(true)) } catch (_: Exception) {} }

    override fun saveDestination(name: String, address: String, latitude: Double, longitude: Double, isFavorite: Boolean, type: String): Flow<Resource<Boolean>> = flow {
        val dest = Destination(0, name, address, latitude, longitude, androidx.compose.ui.graphics.Color.Gray, isFavorite, try { DestinationType.valueOf(type.uppercase()) } catch (_: Exception) { DestinationType.RECENT })
        driveDao.insertDestinations(listOf(dest.toEntity()))
        emit(Resource.Success(true))
        try { apiService.saveDestination(SaveDestinationRequest(name, address, latitude, longitude, isFavorite, type)) } catch (_: Exception) {}
    }

    override fun withdrawApplication(applicationId: Int): Flow<Resource<Boolean>> = flow { try { if (apiService.withdrawApplication(applicationId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun subscribeToChannel(channelId: Int): Flow<Resource<Boolean>> = flow { try { if (apiService.subscribeToChannel(channelId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { emit(Resource.Error("Offline")) } }
    override fun incrementSongPlay(songId: Int): Flow<Resource<Boolean>> = flow { try { apiService.incrementSongPlay(songId); emit(Resource.Success(true)) } catch (_: Exception) {} }
    override fun likeSong(songId: Int): Flow<Resource<Boolean>> = flow { try { apiService.likeSong(songId); emit(Resource.Success(true)) } catch (_: Exception) {} }

    override fun uploadFile(file: java.io.File): Flow<Resource<UploadResponse>> = flow {
        emit(Resource.Loading())
        try {
            val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
            val body = okhttp3.MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = apiService.uploadMedia(body)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Upload failed"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun starMessage(messageId: Int, isStarred: Boolean): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            // Local update first
            chatDao.starMessage(messageId, isStarred)
            // Optional: call API if server supports starring
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to star message"))
        }
    }

    private fun mapApiContact(apiUser: ApiUser) = Contact(apiUser.id, apiUser.username, apiUser.display_name, apiUser.avatar_url, apiUser.phone_number ?: "", apiUser.phone_number != null, true, apiUser.is_verified)
    private fun mapApiChatSummary(chat: ChatSummary) = ChatMessage(id = chat.contact_id, senderName = chat.contact_name, lastMessage = chat.last_message, time = chat.time, unreadCount = chat.unread_count, avatarColor = com.example.firstapp.ui.theme.BoyishPurple, avatarUrl = chat.avatar_url, isOnline = false, isMuted = false, isPinned = false, isTyping = false, isGroup = chat.is_group)
    private fun mapApiMessage(apiMessage: ApiMessage) = Message(id = apiMessage.id, text = apiMessage.body, senderId = apiMessage.sender_id ?: 0, senderName = apiMessage.sender_name ?: "Unknown", time = apiMessage.created_at, isFromMe = apiMessage.direction == "outgoing", type = when (apiMessage.message_type?.lowercase()) { "image" -> MessageType.IMAGE; "video" -> MessageType.VIDEO; "audio" -> MessageType.AUDIO_MESSAGE; "voice_call" -> MessageType.VOICE_CALL; "location" -> MessageType.LOCATION; "contact" -> MessageType.CONTACT; "document" -> MessageType.DOCUMENT; else -> MessageType.TEXT }, duration = apiMessage.duration, mediaUri = apiMessage.media_url, fileName = apiMessage.file_name, fileSize = apiMessage.file_size, contactName = apiMessage.contact_name, contactPhone = apiMessage.contact_phone, latitude = apiMessage.latitude, longitude = apiMessage.longitude, isRead = apiMessage.read_at != null, replyToId = apiMessage.parent_id, replyToText = apiMessage.parent_body, replyToSenderName = apiMessage.parent_sender_name, senderAvatarUrl = null, isStarred = false, reactions = apiMessage.reactions ?: emptyMap())
    private fun mapApiPost(apiPost: ApiPost) = Post(id = apiPost.id, authorId = apiPost.author_id, authorName = apiPost.author?.display_name ?: "Unknown", authorHandle = apiPost.author?.username ?: "", authorAvatarUrl = apiPost.author?.avatar_url, authorAvatarColor = com.example.firstapp.ui.theme.BoyishPurple, isVerifiedAuthor = apiPost.author?.is_verified ?: false, content = apiPost.content, mediaUrls = apiPost.media_urls ?: emptyList(), timestamp = apiPost.created_at ?: "", likes = apiPost.likes.toString(), repliesCount = apiPost.replies_count.toString(), shares = apiPost.shares.toString(), views = apiPost.views.toString(), type = when (apiPost.post_type) { "image_with_text" -> PostType.IMAGE_WITH_TEXT; "video_with_text" -> PostType.VIDEO_WITH_TEXT; "article" -> PostType.ARTICLE; "poll" -> PostType.POLL; "song" -> PostType.SONG; else -> PostType.TEXT_ONLY }, mediaPlaceholderColor = com.example.firstapp.ui.theme.BoyishPurple.copy(0.2f), hasThread = apiPost.has_thread, threadCount = apiPost.thread_count, isLiked = apiPost.is_liked, isReposted = apiPost.is_reposted, isBookmarked = apiPost.is_bookmarked, replies = apiPost.replies?.map { mapApiPostReply(it) } ?: emptyList(), location = apiPost.location, tags = apiPost.tags ?: emptyList())
    private fun mapApiPostReply(apiReply: ApiPostReply): PostReply = PostReply(id = apiReply.id, authorName = apiReply.author?.display_name ?: "Unknown", authorHandle = apiReply.author?.username ?: "", authorAvatarUrl = apiReply.author?.avatar_url, authorAvatarColor = androidx.compose.ui.graphics.Color(0xFF90CAF9), isVerifiedAuthor = apiReply.author?.is_verified ?: false, content = apiReply.content, timestamp = apiReply.created_at ?: "", likes = apiReply.likes.toString(), isLiked = apiReply.is_liked, isReposted = apiReply.is_reposted, isBookmarked = false, nestedReplies = apiReply.nested_replies?.map { mapApiPostReply(it) } ?: emptyList())
    private fun mapApiStory(apiStory: ApiStoryResponse) = UserStory(id = apiStory.user_id, userName = apiStory.user_name, userAvatarUrl = apiStory.avatar_url, userAvatarColor = androidx.compose.ui.graphics.Color(0xFF455A64), stories = apiStory.stories.map { StoryItem(id = it.id, content = it.content ?: "", contentUrl = it.content_url, type = when (it.story_type) { "video" -> StoryType.VIDEO; "image" -> StoryType.IMAGE; else -> StoryType.TEXT }, timestamp = it.created_at ?: "", backgroundColor = androidx.compose.ui.graphics.Color(0xFF546E7A).copy(0.75f), isViewed = it.is_viewed ?: false, durationMs = it.duration_ms ?: 5000) }, lastUpdated = apiStory.stories.firstOrNull()?.created_at ?: "recently", isMuted = false)
    private fun mapApiAdvert(apiAd: ApiAdvert) = Advert(id = apiAd.id, advertiserName = apiAd.advertiser_name, advertiserId = apiAd.advertiser_id ?: "", title = apiAd.title ?: "", time = apiAd.created_at ?: apiAd.time ?: "", avatarUrl = apiAd.avatar_url, avatarColor = androidx.compose.ui.graphics.Color(0xFF283593), contentUrl = apiAd.content_url, contentColor = androidx.compose.ui.graphics.Color(0xFFE8EAF6), contentText = apiAd.content_text, actionText = apiAd.action_text ?: "Learn More", targetUrl = apiAd.target_url ?: "", isSponsored = apiAd.is_sponsored, impressions = apiAd.impressions.toString(), clicks = apiAd.clicks.toString(), isNativeAd = apiAd.is_native_ad, isViewed = apiAd.is_viewed ?: false)
    private fun mapApiChannel(apiChannel: ApiChannel) = Channel(id = apiChannel.id, name = apiChannel.name, description = apiChannel.description ?: "", followers = (apiChannel.followers).toString(), avatarUrl = apiChannel.avatar_url, avatarColor = androidx.compose.ui.graphics.Color(0xFF1A237E), isVerified = apiChannel.is_verified, isSubscribed = apiChannel.is_subscribed, category = apiChannel.category ?: "Community", lastPostText = apiChannel.last_post_text ?: "", lastPostImageUrl = apiChannel.last_post_image_url, lastPostImageColor = androidx.compose.ui.graphics.Color(0xFFE8EAF6), lastPostTime = apiChannel.last_post_time ?: "")

    private fun ChatMessage.toEntity() = ChatEntity(id, senderName, lastMessage, time, unreadCount, avatarUrl, isOnline, isMuted, isPinned, isTyping, isGroup, wallpaperUrl)
    private fun ChatEntity.toModel() = ChatMessage(id = id, senderName = senderName, lastMessage = lastMessage, time = time, unreadCount = unreadCount, avatarColor = com.example.firstapp.ui.theme.BoyishPurple, avatarUrl = avatarUrl, isOnline = isOnline, isMuted = isMuted, isPinned = isPinned, isTyping = isTyping, isGroup = isGroup, wallpaperUrl = wallpaperUrl)
    private fun Message.toEntity(chatId: Int) = MessageEntity(id, chatId, text, senderId, senderName, time, isFromMe, type, duration, mediaUri, fileName, fileSize, contactName, contactPhone, latitude, longitude, isRead, replyToId, replyToText, replyToSenderName, senderAvatarUrl, isStarred)
    private fun MessageEntity.toModel() = Message(id = id, text = text, senderId = senderId, senderName = senderName, time = time, isFromMe = isFromMe, type = type, duration = duration, mediaUri = mediaUri, fileName = fileName, fileSize = fileSize, contactName = contactName, contactPhone = contactPhone, latitude = latitude, longitude = longitude, isRead = isRead, replyToId = replyToId, replyToText = replyToText, replyToSenderName = replyToSenderName, senderAvatarUrl = senderAvatarUrl, isStarred = isStarred, reactions = emptyMap())
    private fun Song.toEntity() = SongEntity(id, title, artist, artistId, albumName, albumArtUrl, audioUrl, duration, category, playCount, isLiked, isExplicit)
    private fun SongEntity.toModel() = Song(id = id, title = title, artist = artist, artistId = artistId, albumName = albumName, albumArtUrl = albumArtUrl, albumArtColor = androidx.compose.ui.graphics.Color(0xFF6A1B9A), audioUrl = audioUrl, duration = duration, category = category, isLiked = isLiked, isExplicit = isExplicit, playCount = playCount)
    private fun Job.toEntity() = JobEntity(id, title, companyName, companyLogoUrl, location, salaryRange, jobType, postedTime, description, isVerified, isApplied, isSaved)
    private fun JobEntity.toModel() = Job(id = id, title = title, companyName = companyName, companyLogoUrl = companyLogoUrl, location = location, logoColor = androidx.compose.ui.graphics.Color(0xFF1565C0), salaryRange = salaryRange, jobType = jobType, postedTime = postedTime, description = description, requirements = emptyList(), isVerified = isVerified, isApplied = isApplied, isSaved = isSaved)
    private fun Advert.toEntity() = AdvertEntity(id, advertiserName, advertiserId, title, time, avatarUrl, avatarColor.toArgb(), contentUrl, contentColor.toArgb(), contentText, actionText, targetUrl, isSponsored, impressions, clicks, isNativeAd, isViewed)
    private fun AdvertEntity.toModel() = Advert(id, advertiserName, advertiserId, title, time, avatarUrl, Color(avatarColor), contentUrl, Color(contentColor), contentText, actionText, targetUrl, isSponsored, impressions, clicks, isNativeAd, isViewed)
    private fun Channel.toEntity() = ChannelEntity(id, name, description, followers, avatarUrl, avatarColor.toArgb(), isVerified, isSubscribed, category, lastPostText, lastPostImageUrl, lastPostImageColor.toArgb(), lastPostTime)
    private fun ChannelEntity.toModel() = Channel(id, name, description, followers, avatarUrl, Color(avatarColor), isVerified, isSubscribed, category, lastPostText, lastPostImageUrl, Color(lastPostImageColor), lastPostTime)
    private fun Post.toEntity() = PostEntity(id, authorId, authorName, authorHandle, authorAvatarUrl, authorAvatarColor.toArgb(), isVerifiedAuthor, content, mediaUrls, timestamp, likes, repliesCount, shares, views, type, mediaPlaceholderColor?.toArgb(), hasThread, threadCount, isLiked, isReposted, isBookmarked, replies, location, tags)
    private fun PostEntity.toModel() = Post(id, authorId, authorName, authorHandle, authorAvatarUrl, Color(authorAvatarColor), isVerifiedAuthor, content, mediaUrls, timestamp, likes, repliesCount, shares, views, type, mediaPlaceholderColor?.let { Color(it) }, hasThread, threadCount, isLiked, isReposted, isBookmarked, replies, location, tags)
    private fun UserStory.toEntity() = StoryEntity(id, userId, userName, userAvatarUrl, userAvatarColor.toArgb(), stories, lastUpdated, isMuted)
    private fun StoryEntity.toModel() = UserStory(id, userId, userName, userAvatarUrl, Color(userAvatarColor), stories, lastUpdated, isMuted)
    private fun AppShort.toEntity() = ShortEntity(id, creatorName, description, likes, comments, shares, videoUrl, thumbnailUrl, thumbnailColor.toArgb(), profilePictureColor.toArgb(), isLiked, isSubscribed)
    private fun ShortEntity.toModel() = AppShort(id, creatorName, description, likes, comments, shares, videoUrl, thumbnailUrl, Color(thumbnailColor), Color(profilePictureColor), isLiked, isSubscribed)
    private fun CallLog.toCallLogEntity() = CallLogEntity(id, sessionId, callerId, calleeId, contactId, contactName, contactAvatar, callType, status, durationSeconds, startedAt, endedAt)
    private fun CallLogEntity.toCallLog() = CallLog(id, sessionId, callerId, calleeId, contactId, contactName, contactAvatar, callType, status, durationSeconds, startedAt, endedAt)
    private fun Contact.toContactEntity() = ContactEntity(id, username, displayName, avatarUrl, phoneNumber, isRegistered, isAuthenticated, isVerified)
    private fun ContactEntity.toContact() = Contact(id, username, displayName, avatarUrl, phoneNumber, isRegistered, isAuthenticated, isVerified)
    private fun JobApplication.toEntity() = JobApplicationEntity(id, jobId, jobTitle, companyName, appliedDate, status)
    private fun JobApplicationEntity.toModel() = JobApplication(id, jobId, jobTitle, companyName, appliedDate, status, androidx.compose.ui.graphics.Color(0xFF6200EE))
    private fun RideOption.toEntity() = RideOptionEntity(id, name, price, currency, arrivalTime, estimatedArrival, description, iconUrl, iconColor.toArgb(), capacity)
    private fun RideOptionEntity.toModel() = RideOption(id, name, price, currency, arrivalTime, estimatedArrival, description, iconUrl, Color(iconColor), capacity)
    private fun Destination.toEntity() = DestinationEntity(id, name, address, latitude, longitude, iconColor.toArgb(), isFavorite, type, placeId)
    private fun DestinationEntity.toModel() = Destination(id, name, address, latitude, longitude, Color(iconColor), isFavorite, type, placeId)

    private fun Throwable.toUserMessage(): String = when (this) { is ConnectException, is UnknownHostException, is SocketTimeoutException -> "Offline"; is HttpException -> "Error"; else -> "Error" }
}
