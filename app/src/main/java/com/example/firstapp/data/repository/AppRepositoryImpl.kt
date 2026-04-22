package com.example.firstapp.data.repository

import android.util.Log
import com.example.firstapp.data.api.*
import com.example.firstapp.model.*
import com.example.firstapp.model.Short as AppShort
import com.example.firstapp.util.Resource
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException

class AppRepositoryImpl : AppRepository {
    override fun createShort(description: String, videoUrl: String, thumbnailUrl: String): Flow<Resource<Long>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.createShort(
                com.example.firstapp.data.api.CreateShortRequest(
                    description = description,
                    video_url = videoUrl,
                    thumbnail_url = thumbnailUrl
                )
            )
            if (response.isSuccessful) {
                val id = response.body()?.id
                if (id != null) {
                    emit(Resource.Success(id))
                } else {
                    emit(Resource.Error("Invalid create short response"))
                }
            } else {
                emit(Resource.Error("Failed to create short"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun repostShort(shortId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.repostShort(shortId)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to repost short"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun bookmarkShort(shortId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.bookmarkShort(shortId)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to bookmark short"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun replyToShort(shortId: Int, content: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.replyToShort(shortId, com.example.firstapp.data.api.CreateShortReplyRequest(content))
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to reply to short"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun getShortReplies(shortId: Int): Flow<Resource<List<ShortReply>>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.getShortReplies(shortId)
            if (response.isSuccessful) {
                val replies = response.body()?.replies?.map { apiReply ->
                    ShortReply(
                        id = apiReply.id,
                        authorName = apiReply.author?.display_name ?: "Unknown",
                        authorHandle = apiReply.author?.username ?: "",
                        authorAvatarUrl = apiReply.author?.avatar_url,
                        authorAvatarColor = androidx.compose.ui.graphics.Color(0xFF90CAF9),
                        isVerifiedAuthor = apiReply.author?.is_verified ?: false,
                        content = apiReply.content,
                        timestamp = apiReply.created_at ?: "",
                        likes = apiReply.likes.toString(),
                        isLiked = apiReply.is_liked
                    )
                } ?: emptyList()
                emit(Resource.Success(replies))
            } else {
                emit(Resource.Error("Failed to load replies"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    private val apiService = ApiClient.apiService

    override fun getChats(): Flow<Resource<List<ChatMessage>>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.getChats()
            if (response.isSuccessful) {
                val chats = response.body()?.chats?.map { chat ->
                    ChatMessage(
                        id = chat.contact_id,
                        senderName = chat.contact_name,
                        lastMessage = chat.last_message,
                        time = chat.time,
                        unreadCount = chat.unread_count,
                        avatarColor = androidx.compose.ui.graphics.Color(0xFF607D8B),
                        avatarUrl = chat.avatar_url,
                        isOnline = false,
                        isMuted = false,
                        isPinned = false,
                        isTyping = false
                    )
                } ?: emptyList()
                emit(Resource.Success(chats))
            } else {
                emit(Resource.Error("Failed to load chats"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun getCallLogs(): Flow<Resource<List<CallLog>>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.getCalls()
            if (response.isSuccessful) {
                val calls = response.body()?.calls?.map { apiCall ->
                    CallLog(
                        id = apiCall.id,
                        sessionId = apiCall.session_id,
                        callerId = apiCall.caller_id,
                        calleeId = apiCall.callee_id,
                        contactId = apiCall.contact_id,
                        contactName = apiCall.contact_name,
                        contactAvatar = apiCall.contact_avatar,
                        callType = apiCall.call_type,
                        status = apiCall.status,
                        durationSeconds = apiCall.duration_seconds,
                        startedAt = apiCall.started_at,
                        endedAt = apiCall.ended_at
                    )
                } ?: emptyList()
                emit(Resource.Success(calls))
            } else {
                emit(Resource.Error("Failed to load call logs"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun getMessages(chatId: Int): Flow<Resource<List<Message>>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.getMessages(chatId)
            if (response.isSuccessful) {
                val messages = response.body()?.messages?.map { apiMessage -> mapApiMessage(apiMessage) } ?: emptyList()
                emit(Resource.Success(messages))
            } else {
                emit(Resource.Error("Failed to load messages"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun sendMessage(
        chatId: Int,
        body: String,
        messageType: String,
        mediaUrl: String?,
        duration: String?,
        fileName: String?,
        fileSize: String?,
        contactName: String?,
        contactPhone: String?,
        latitude: Double?,
        longitude: Double?
    ): Flow<Resource<Message>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.sendMessage(
                chatId,
                com.example.firstapp.data.api.SendMessageRequest(
                    body = body,
                    message_type = messageType,
                    media_url = mediaUrl,
                    duration = duration,
                    file_name = fileName,
                    file_size = fileSize,
                    contact_name = contactName,
                    contact_phone = contactPhone,
                    latitude = latitude,
                    longitude = longitude
                )
            )
            if (response.isSuccessful) {
                val apiMessage = response.body()?.message
                if (apiMessage != null) {
                    emit(Resource.Success(mapApiMessage(apiMessage)))
                } else {
                    emit(Resource.Error("Invalid message response"))
                }
            } else {
                emit(Resource.Error("Failed to send message"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun getContacts(): Flow<Resource<List<Contact>>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.getContacts()
            if (response.isSuccessful) {
                val contacts = response.body()?.contacts?.map { apiUser ->
                    Contact(
                        id = apiUser.id,
                        username = apiUser.username,
                        displayName = apiUser.display_name,
                        avatarUrl = apiUser.avatar_url,
                        phoneNumber = apiUser.phone_number ?: "",
                        isAuthenticated = true,
                        isVerified = apiUser.is_verified,
                        isRegistered = apiUser.phone_number != null && apiUser.phone_number.isNotBlank()
                    )
                } ?: emptyList()
                emit(Resource.Success(contacts))
            } else {
                emit(Resource.Error("Failed to load contacts"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun syncContacts(phoneNumbers: List<String>): Flow<Resource<List<Contact>>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.syncContacts(SyncContactsRequest(phoneNumbers))
            if (response.isSuccessful) {
                val contacts = response.body()?.contacts?.map { mapApiContact(it) } ?: emptyList()
                emit(Resource.Success(contacts))
            } else {
                emit(Resource.Error("Failed to sync contacts"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override suspend fun createChat(contactId: Int) {
        try {
            apiService.createChat(CreateChatRequest(contactId))
        } catch (_: Exception) {}
    }

    override fun search(query: String): Flow<Resource<SearchResult>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.search(query)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    val results = SearchResult(
                        chats = body.chats.map { mapApiChatSummary(it) },
                        contacts = body.contacts.map { mapApiContact(it) }
                    )
                    emit(Resource.Success(results))
                } else {
                    emit(Resource.Error("Invalid search response"))
                }
            } else {
                emit(Resource.Error("Failed to search"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun searchShorts(query: String): Flow<Resource<List<AppShort>>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.searchShorts(query)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val shorts = body.shorts.map { apiShort ->
                    AppShort(
                        id = apiShort.id,
                        creatorName = apiShort.creator_name,
                        description = apiShort.description,
                        likes = apiShort.likes.toString(),
                        comments = apiShort.comments.toString(),
                        shares = apiShort.shares.toString(),
                        videoUrl = apiShort.video_url ?: "",
                        thumbnailUrl = apiShort.thumbnail_url,
                        thumbnailColor = androidx.compose.ui.graphics.Color(0xFF000000),
                        profilePictureColor = androidx.compose.ui.graphics.Color(0xFF607D8B),
                        isLiked = apiShort.is_liked,
                        isSubscribed = apiShort.is_subscribed
                    )
                }
                emit(Resource.Success(shorts))
            } else {
                emit(Resource.Error("Failed to search shorts"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    private fun mapApiChatSummary(chat: ChatSummary): ChatMessage = ChatMessage(
        id = chat.contact_id,
        senderName = chat.contact_name,
        lastMessage = chat.last_message,
        time = chat.time,
        unreadCount = chat.unread_count,
        avatarColor = androidx.compose.ui.graphics.Color(0xFF607D8B),
        avatarUrl = chat.avatar_url,
        isOnline = false,
        isMuted = false,
        isPinned = false,
        isTyping = false
    )

    private fun mapApiContact(apiUser: ApiUser): Contact = Contact(
        id = apiUser.id,
        username = apiUser.username,
        displayName = apiUser.display_name,
        avatarUrl = apiUser.avatar_url,
        phoneNumber = apiUser.phone_number ?: "",
        isAuthenticated = true,
        isVerified = apiUser.is_verified,
        isRegistered = apiUser.phone_number != null && apiUser.phone_number.isNotBlank()
    )

    override fun getJobs(): Flow<Resource<List<Job>>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.getJobs()
            if (response.isSuccessful) {
                val jobs = response.body()?.jobs?.map { apiJob ->
                    Job(
                        id = apiJob.id,
                        title = apiJob.title,
                        companyName = apiJob.company_name,
                        companyLogoUrl = apiJob.company_logo_url,
                        location = apiJob.location,
                        logoColor = androidx.compose.ui.graphics.Color(0xFF1565C0),
                        salaryRange = apiJob.salary_range ?: "",
                        jobType = apiJob.job_type ?: "Full-time",
                        postedTime = apiJob.posted_time ?: "",
                        description = apiJob.description ?: "",
                        requirements = apiJob.requirements ?: emptyList(),
                        isVerified = apiJob.is_verified,
                        isApplied = false,
                        isSaved = false
                    )
                } ?: emptyList()
                emit(Resource.Success(jobs))
            } else {
                emit(Resource.Error("Failed to load jobs"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun applyForJob(jobId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.applyForJob(jobId)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to apply for job"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun getJobApplications(): Flow<Resource<List<JobApplication>>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.getJobApplications()
            if (response.isSuccessful) {
                val applications = response.body()?.applications?.map { apiApp ->
                    JobApplication(
                        id = apiApp.id,
                        jobId = apiApp.job_id,
                        jobTitle = apiApp.title ?: "Job #${apiApp.job_id}",
                        companyName = apiApp.company_name ?: "Unknown company",
                        appliedDate = apiApp.applied_date,
                        status = when (apiApp.status.lowercase()) {
                            "reviewing" -> ApplicationStatus.REVIEWING
                            "interview" -> ApplicationStatus.INTERVIEW
                            "rejected" -> ApplicationStatus.REJECTED
                            "offered" -> ApplicationStatus.OFFERED
                            else -> ApplicationStatus.PENDING
                        },
                        logoColor = androidx.compose.ui.graphics.Color(0xFF6200EE)
                    )
                } ?: emptyList()
                emit(Resource.Success(applications))
            } else {
                emit(Resource.Error("Failed to load applications"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun getAdverts(): Flow<Resource<List<Advert>>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.getAdverts()
            if (response.isSuccessful) {
                val adverts = response.body()?.adverts?.map { apiAd ->
                    Advert(
                        id = apiAd.id,
                        advertiserName = apiAd.advertiser_name,
                        advertiserId = apiAd.advertiser_id ?: "",
                        title = apiAd.title ?: "",
                        time = apiAd.created_at ?: apiAd.time ?: "",
                        avatarUrl = apiAd.avatar_url,
                        avatarColor = androidx.compose.ui.graphics.Color(0xFF283593),
                        contentUrl = apiAd.content_url,
                        contentColor = androidx.compose.ui.graphics.Color(0xFFE8EAF6),
                        contentText = apiAd.content_text,
                        actionText = apiAd.action_text ?: "Learn More",
                        targetUrl = apiAd.target_url ?: "",
                        isSponsored = apiAd.is_sponsored,
                        impressions = apiAd.impressions.toString(),
                        clicks = apiAd.clicks.toString(),
                        isNativeAd = apiAd.is_native_ad,
                        isViewed = apiAd.is_viewed
                    )
                } ?: emptyList()
                emit(Resource.Success(adverts))
            } else {
                emit(Resource.Error("Failed to load adverts"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun getChannels(): Flow<Resource<List<Channel>>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.getChannels()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val channels = body.channels.map { apiChannel ->
                    Channel(
                        id = apiChannel.id,
                        name = apiChannel.name,
                        description = apiChannel.description ?: "",
                        followers = apiChannel.followers.toString(),
                        avatarUrl = apiChannel.avatar_url,
                        avatarColor = androidx.compose.ui.graphics.Color(0xFF1A237E),
                        isVerified = apiChannel.is_verified,
                        isSubscribed = apiChannel.is_subscribed,
                        category = apiChannel.category ?: "Community",
                        lastPostText = apiChannel.last_post_text ?: "",
                        lastPostImageUrl = apiChannel.last_post_image_url,
                        lastPostImageColor = androidx.compose.ui.graphics.Color(0xFFE8EAF6),
                        lastPostTime = apiChannel.last_post_time ?: ""
                    )
                }
                emit(Resource.Success(channels))
            } else {
                emit(Resource.Error("Failed to load channels"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun getShorts(): Flow<Resource<List<AppShort>>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.getShorts()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val shorts = body.shorts.map { apiShort ->
                    AppShort(
                        id = apiShort.id,
                        creatorName = apiShort.creator_name,
                        description = apiShort.description,
                        likes = apiShort.likes.toString(),
                        comments = apiShort.comments.toString(),
                        shares = apiShort.shares.toString(),
                        videoUrl = apiShort.video_url ?: "",
                        thumbnailUrl = apiShort.thumbnail_url,
                        thumbnailColor = androidx.compose.ui.graphics.Color(0xFF000000),
                        profilePictureColor = androidx.compose.ui.graphics.Color(0xFF607D8B),
                        isLiked = apiShort.is_liked,
                        isSubscribed = apiShort.is_subscribed
                    )
                }
                emit(Resource.Success(shorts))
            } else {
                emit(Resource.Error("Failed to load shorts"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun getSongs(): Flow<Resource<List<Song>>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.getSongs()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val songs = body.songs.map { apiSong ->
                    Song(
                        id = apiSong.id,
                        title = apiSong.title,
                        artist = apiSong.artist,
                        artistId = apiSong.artist_id ?: "",
                        albumName = apiSong.album_name ?: "",
                        albumArtUrl = apiSong.album_art_url,
                        albumArtColor = androidx.compose.ui.graphics.Color(0xFF6A1B9A),
                        audioUrl = apiSong.audio_url ?: "",
                        duration = apiSong.duration ?: "",
                        category = apiSong.category ?: "",
                        lyrics = apiSong.lyrics,
                        releaseDate = apiSong.release_date ?: "",
                        playCount = apiSong.play_count.toString(),
                        isLiked = apiSong.is_liked,
                        isExplicit = apiSong.is_explicit
                    )
                }
                emit(Resource.Success(songs))
            } else {
                emit(Resource.Error("Failed to load songs"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun getPosts(): Flow<Resource<List<Post>>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.getPosts()
            if (response.isSuccessful) {
                val posts = response.body()?.posts?.map { apiPost ->
                    Post(
                        id = apiPost.id,
                        authorId = apiPost.author_id,
                        authorName = apiPost.author?.display_name ?: "Unknown",
                        authorHandle = apiPost.author?.username ?: "",
                        authorAvatarUrl = apiPost.author?.avatar_url,
                        authorAvatarColor = androidx.compose.ui.graphics.Color(0xFF0277BD),
                        isVerifiedAuthor = apiPost.author?.is_verified ?: false,
                        content = apiPost.content,
                        mediaUrls = apiPost.media_urls ?: emptyList(),
                        timestamp = apiPost.created_at ?: "",
                        likes = apiPost.likes.toString(),
                        repliesCount = apiPost.replies_count.toString(),
                        shares = apiPost.shares.toString(),
                        views = apiPost.views.toString(),
                        type = when (apiPost.post_type) {
                            "image_with_text" -> PostType.IMAGE_WITH_TEXT
                            "video_with_text" -> PostType.VIDEO_WITH_TEXT
                            "article" -> PostType.ARTICLE
                            "poll" -> PostType.POLL
                            else -> PostType.TEXT_ONLY
                        },
                        mediaPlaceholderColor = androidx.compose.ui.graphics.Color(0xFFB3E5FC),
                        hasThread = apiPost.has_thread,
                        threadCount = apiPost.thread_count,
                        isLiked = apiPost.is_liked,
                        isReposted = apiPost.is_reposted,
                        isBookmarked = apiPost.is_bookmarked,
                        replies = apiPost.replies?.map { apiReply -> mapApiPostReply(apiReply) } ?: emptyList(),
                        location = apiPost.location,
                        tags = apiPost.tags ?: emptyList()
                    )
                } ?: emptyList()
                emit(Resource.Success(posts))
            } else {
                emit(Resource.Error("Failed to load posts"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun createPost(content: String, mediaUrls: List<String>): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            
            // Log media URL count
            Log.d("createPost", "URL count: ${mediaUrls.size}")
            
            // Log the URLs
            Log.d("createPost", "URLs: $mediaUrls")
            
            // Log request timestamp
            Log.d("createPost", "Request being sent at ${System.currentTimeMillis()}")
            
            val trimmed = content.trim()
            if (trimmed.isEmpty() && mediaUrls.isEmpty()) {
                emit(Resource.Error("Post content or media is required"))
                return@flow
            }

            val response = apiService.createPost(
                CreatePostRequest(
                    content = trimmed,
                    media_urls = mediaUrls,
                    post_type = if (mediaUrls.isEmpty()) "text_only" else "image_with_text"
                )
            )
            
            // Log response received
            Log.d("createPost", "Response received: ${response.code()} - Success: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to create post"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun likePost(postId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.likePost(postId)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to like post"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun repostPost(postId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.repostPost(postId)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to repost post"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun bookmarkPost(postId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.bookmarkPost(postId)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to bookmark post"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun createPostReply(postId: Int, content: String, parentReplyId: Int?): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.createPostReply(
                postId = postId,
                request = com.example.firstapp.data.api.CreatePostReplyRequest(
                    content = content,
                    parent_reply_id = parentReplyId
                )
            )
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to create reply"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun likePostReply(replyId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.likePostReply(replyId)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to like reply"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun repostPostReply(replyId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.repostPostReply(replyId)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to repost reply"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun bookmarkPostReply(replyId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.bookmarkPostReply(replyId)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to bookmark reply"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun getStories(): Flow<Resource<List<UserStory>>> = flow {
        try {
            emit(Resource.Loading())
            val response = apiService.getStories()
            if (response.isSuccessful) {
                val stories = response.body()?.stories?.map { apiStory ->
                    UserStory(
                        id = apiStory.user_id,
                        userName = apiStory.user_name,
                        userAvatarUrl = apiStory.avatar_url,
                        userAvatarColor = androidx.compose.ui.graphics.Color(0xFF455A64),
                        stories = apiStory.stories.map { storyItem ->
                            StoryItem(
                                id = storyItem.id,
                                content = storyItem.content ?: "",
                                contentUrl = storyItem.content_url,
                                type = when (storyItem.story_type) {
                                    "video" -> StoryType.VIDEO
                                    "image" -> StoryType.IMAGE
                                    else -> StoryType.TEXT
                                },
                                timestamp = storyItem.created_at ?: "",
                                backgroundColor = androidx.compose.ui.graphics.Color(0xFF546E7A).copy(alpha = 0.75f),
                                isViewed = storyItem.is_viewed ?: false,
                                durationMs = storyItem.duration_ms ?: 5000
                            )
                        },
                        lastUpdated = apiStory.stories.firstOrNull()?.created_at ?: "recently",
                        isMuted = false
                    )
                } ?: emptyList()
                emit(Resource.Success(stories))
            } else {
                emit(Resource.Error("Failed to load stories"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun createStory(content: String, contentUrl: String?, type: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.createStory(
                com.example.firstapp.data.api.CreateStoryRequest(
                    content = content,
                    content_url = contentUrl,
                    story_type = type
                )
            )
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Failed to post story"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun viewPost(postId: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.viewPost(postId)
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Failed to record view"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun deletePost(postId: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.deletePost(postId)
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Failed to delete post"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun withdrawApplication(applicationId: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.withdrawApplication(applicationId)
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Failed to withdraw application"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun subscribeToChannel(channelId: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.subscribeToChannel(channelId)
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Failed to subscribe"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun incrementSongPlay(songId: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.incrementSongPlay(songId)
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Failed to track play"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun likeSong(songId: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.likeSong(songId)
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Failed to like song"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    override fun getCurrentUser(): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.user))
            } else {
                emit(Resource.Error("Failed to fetch user profile"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun saveDestination(name: String, address: String, latitude: Double, longitude: Double, isFavorite: Boolean, type: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.saveDestination(SaveDestinationRequest(
                name = name,
                address = address,
                latitude = latitude,
                longitude = longitude,
                is_favorite = isFavorite,
                destination_type = type
            ))
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Failed to save destination"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Unknown error"))
        }
    }

    private fun mapApiPostReply(apiReply: ApiPostReply): PostReply {
        return PostReply(
            id = apiReply.id,
            authorName = apiReply.author?.display_name ?: "Unknown",
            authorHandle = apiReply.author?.username ?: "",
            authorAvatarUrl = apiReply.author?.avatar_url,
            authorAvatarColor = androidx.compose.ui.graphics.Color(0xFF90CAF9),
            isVerifiedAuthor = apiReply.author?.is_verified ?: false,
            content = apiReply.content,
            timestamp = apiReply.created_at ?: "",
            likes = apiReply.likes.toString(),
            isLiked = apiReply.is_liked,
            isReposted = apiReply.is_reposted,
            isBookmarked = false, // Assuming bookmark status might be added later to reply model if API supports it
            nestedReplies = apiReply.nested_replies?.map { mapApiPostReply(it) } ?: emptyList()
        )
    }

    private fun mapApiMessage(apiMessage: ApiMessage): Message {
        return Message(
            id = apiMessage.id,
            text = apiMessage.body,
            time = apiMessage.created_at,
            isFromMe = apiMessage.direction == "outgoing",
            type = when (apiMessage.message_type?.lowercase()) {
                "image" -> MessageType.IMAGE
                "video" -> MessageType.VIDEO
                "audio" -> MessageType.AUDIO_MESSAGE
                "voice_call" -> MessageType.VOICE_CALL
                "location" -> MessageType.LOCATION
                "contact" -> MessageType.CONTACT
                "document" -> MessageType.DOCUMENT
                else -> MessageType.TEXT
            },
            duration = apiMessage.duration,
            mediaUri = apiMessage.media_url,
            fileName = apiMessage.file_name,
            fileSize = apiMessage.file_size,
            contactName = apiMessage.contact_name,
            contactPhone = apiMessage.contact_phone,
            latitude = apiMessage.latitude,
            longitude = apiMessage.longitude
        )
    }

    private fun Throwable.toUserMessage(): String {
        return when (this) {
            is ConnectException,
            is UnknownHostException,
            is SocketTimeoutException -> "Can't reach the server. Check that the backend is running and try again."
            is HttpException -> "Server error (${code()}). Please try again."
            else -> localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Something went wrong. Please try again."
        }
    }
}
