package com.example.firstapp.chat.data.repository

import android.content.Context
import android.util.Log
import androidx.paging.*
import androidx.room.withTransaction
import com.example.firstapp.chat.data.*
import com.example.firstapp.chat.data.api.*
import com.example.firstapp.chat.data.local.dao.*
import com.example.firstapp.chat.data.local.entity.ChatEntity
import com.example.firstapp.chat.data.local.entity.SyncCursorEntity
import com.example.firstapp.chat.data.paging.MessageRemoteMediator
import com.example.firstapp.chat.model.*
import com.example.firstapp.common.data.local.AppDatabase
import com.example.firstapp.common.domain.auth.SessionManager
import com.example.firstapp.common.util.Resource
import com.example.firstapp.common.util.DisplayNameResolver
import com.example.firstapp.common.util.ContactUtils
import com.example.firstapp.domain.repository.ChatRepository
import com.example.firstapp.common.data.realtime.WebSocketManager
import com.example.firstapp.common.data.realtime.WSEnvelope
import com.example.firstapp.common.data.api.UserApi
import com.example.firstapp.common.data.api.ApiUser
import com.example.firstapp.common.data.api.SyncContactsRequest
import com.example.firstapp.common.data.api.SyncAccountContactsRequest
import com.example.firstapp.common.data.api.AccountContactDTO
import com.example.firstapp.media.data.api.SocialApi
import com.example.firstapp.chat.rewards.data.RewardsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import okhttp3.ResponseBody
import com.google.gson.Gson

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val userApi: UserApi,
    private val socialApi: SocialApi,
    private val chatDaoProvider: Provider<ChatDao>,
    private val channelDaoProvider: Provider<ChannelDao>,
    private val contactDaoProvider: Provider<ContactDao>,
    private val syncCursorDaoProvider: Provider<SyncCursorDao>,
    private val callLogDaoProvider: Provider<CallLogDao>,
    private val sessionManager: SessionManager,
    private val dbProvider: Provider<AppDatabase>,
    private val webSocketManager: WebSocketManager,
    private val displayNameResolver: DisplayNameResolver,
    @param:ApplicationContext private val context: Context
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    private val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    private val gson = Gson()

    init {
        observeWebSocketEvents()
        observeConnectionState()
        startDisappearingMessagesWorker()
    }

    private fun startDisappearingMessagesWorker() {
        scope.launch {
            while (isActive) {
                try {
                    val chatDao = chatDaoProvider.get()
                    val chats = chatDao.getChatsWithDisappearingMessages()
                    val now = System.currentTimeMillis()
                    
                    chats.forEach { chat ->
                        val expiryTime = now - (chat.disappearingMessagesDuration * 1000L)
                        chatDao.deleteMessagesBefore(chat.id, expiryTime)
                    }
                } catch (e: Exception) {
                    Log.e("ChatRepository", "Error pruning disappearing messages", e)
                }
                delay(60_000) // Every minute
            }
        }
    }

    private fun observeWebSocketEvents() {
        webSocketManager.events
            .onEach { envelope ->
                handleIncomingEvent(envelope)
            }
            .catch { e -> Log.e("ChatRepository", "WS Flow Error", e) }
            .launchIn(scope)
    }

    private fun observeConnectionState() {
        webSocketManager.connectionState
            .onEach { state ->
                if (state == WebSocketManager.ConnectionState.CONNECTED) {
                    subscribeToTopics()
                    // Automatic catch-up on reconnect
                    scope.launch { syncDelta().collect {} }
                }
            }
            .launchIn(scope)
    }

    private suspend fun subscribeToTopics() {
        Log.d("ChatRepository", "WebSocket Connected: Re-subscribing to topics")
        
        // Subscribe to global presence events
        webSocketManager.send("presence", "subscribe", op = "subscribe")
        
        // Subscribe to personal user events (Redis Pub/Sub alignment)
        sessionManager.getUserId()?.let { userId ->
            webSocketManager.send("user:$userId", "subscribe", op = "subscribe")
        }
        
        // Subscribe to all chat rooms and channels to receive real-time messages
        try {
            val chatDao = chatDaoProvider.get()
            val channelDao = channelDaoProvider.get()
            
            // DMs and Groups
            val chatIds = chatDao.getAllChatIds()
            
            // Channels use their conversationId for real-time messages
            val channelConvIds = channelDao.getChannels().first()
                .filter { it.isSubscribed }
                .mapNotNull { it.conversationId ?: it.id }
            
            val allIds = (chatIds + channelConvIds).distinct()
            
            allIds.forEach { id ->
                webSocketManager.send("chat:$id", "join", op = "subscribe")
            }
            Log.d("ChatRepository", "Subscribed to ${allIds.size} topics (${chatIds.size} chats, ${channelConvIds.size} channels)")
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to get IDs for re-subscription", e)
        }
    }

    private suspend fun handleIncomingEvent(envelope: WSEnvelope) {
        val db = try { dbProvider.get() } catch (e: Exception) { return }
        if (!db.isOpen) {
            Log.w("ChatRepository", "DB is closed, ignoring incoming event topic=${envelope.topic} event=${envelope.event}")
            return
        }
        val chatDao = chatDaoProvider.get()
        val topic = envelope.topic ?: ""
        
        when {
            topic == "chat" || topic.startsWith("chat:") -> {
                Log.v("ChatRepository", "Handling chat event: ${envelope.event} on topic: $topic")
                when (envelope.event) {
                    "message", "INCOMING_MSG" -> {
                        val payload = envelope.payload
                        if (payload != null) {
                            try {
                                val apiMsg = json.decodeFromJsonElement<ApiMessage>(payload)
                                val myUserId = sessionManager.getUserId()
                                val privKey = sessionManager.getPrivateKey()
                                val message = mapApiMessage(apiMsg, privKey, myUserId)
                                Log.d("ChatRepository", "Incoming message from WS: ${message.id}, saving to DB")
                                
                                // Step 1: Save the message itself
                                saveMessage(message.conversationId, message)
                                
                                // Step 2: Send Delivery ACK immediately
                                if (!message.isFromMe) {
                                    webSocketManager.send("chat", "DELIVERY_ACK", buildJsonObject {
                                        put("message_id", message.id)
                                        put("conversation_id", message.conversationId)
                                    })
                                }

                                // Step 3: Update the chat summary
                                val phone = apiMsg.contact_phone ?: ""
                                val senderName = apiMsg.sender_name ?: ""
                                val resolvedName = displayNameResolver.resolve(
                                    com.example.firstapp.common.model.User(id = apiMsg.sender_id, displayName = senderName),
                                    phone
                                )

                                chatDao.updateLastMessage(
                                    message.conversationId, 
                                    message.content, 
                                    message.timestamp, 
                                    message.isFromMe, 
                                    message.isRead
                                )
                                
                                // Update name if it's a DIRECT chat and name was resolved
                                val chat = chatDao.searchChatsById(message.conversationId)
                                if (chat != null && chat.type == com.example.firstapp.common.model.ConversationType.DIRECT) {
                                    if (chat.senderName != resolvedName) {
                                        chatDao.insertChats(listOf(chat.copy(senderName = resolvedName)))
                                    }
                                }
                                
                                if (!message.isFromMe) {
                                    chatDao.incrementUnreadCount(message.conversationId)
                                }

                                // Expert: Verify if chat exists in either Chats or Channels (checking both ID and ConversationID)
                                val existsInChats = chatDao.searchChatsById(message.conversationId) != null
                                val channels = channelDaoProvider.get().getChannels().first()
                                val existsInChannels = channels.any { it.id == message.conversationId || it.conversationId == message.conversationId }
                                
                                if (!existsInChats && !existsInChannels) {
                                    Log.i("ChatRepository", "New conversation detected via WS: ${message.conversationId}, syncing...")
                                    syncChats().collect {}
                                }
                            } catch (e: Exception) {
                                Log.e("ChatRepository", "Failed to decode WS message", e)
                            }
                        }
                    }
                    "REWARD_UPDATE" -> {
                        // Notify RewardViewModel via a global flow or similar
                        Log.i("ChatRepository", "Reward update received via WS topic=chat")
                    }
                    "call_ringing", "call_invite", "call_accept", "call_end", "call_reject" -> {
                        Log.d("ChatRepository", "Incoming call event via multiplexed WS: ${envelope.event}")
                        // These are handled by CallListenerService which observes the same WebSocketManager
                    }
                    "DELETE_MSG" -> {
                        val payload = envelope.payload
                        if (payload != null) {
                            try {
                                val messageId = payload["message_id"]?.toString()?.trim('"')
                                if (messageId != null) {
                                    Log.d("ChatRepository", "Deleting message via WS: $messageId")
                                    val db = dbProvider.get()
                                    db.withTransaction {
                                        chatDao.deleteMessage(messageId)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("ChatRepository", "Failed to decode DELETE_MSG event", e)
                            }
                        }
                    }
                    "DELETE_CONV" -> {
                        val payload = envelope.payload
                        if (payload != null) {
                            try {
                                val chatId = payload["conversation_id"]?.toString()?.trim('"')
                                if (chatId != null) {
                                    Log.d("ChatRepository", "Deleting conversation via WS: $chatId")
                                    val db = dbProvider.get()
                                    db.withTransaction {
                                        chatDao.deleteChat(chatId)
                                        chatDao.clearMessages(chatId)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("ChatRepository", "Failed to decode DELETE_CONV event", e)
                            }
                        }
                    }
                    "typing", "TYPING" -> {
                        val payload = envelope.payload
                        if (payload != null) {
                            try {
                                val chatId = payload["conversation_id"]?.toString()?.trim('"')
                                val isTyping = (payload["is_typing"] ?: payload["state"])?.toString()?.trim('"')
                                val state = isTyping == "true" || isTyping == "START"
                                chatId?.let { chatDao.updateTyping(it, state) }
                            } catch (e: Exception) {
                                Log.e("ChatRepository", "Failed to decode typing event", e)
                            }
                        }
                    }
                    "MSG_ACK" -> {
                        val payload = envelope.payload
                        if (payload != null) {
                            val clientUuid = payload["client_msg_id"]?.toString()?.trim('"')
                            val serverId = payload["message_id"]?.toString()?.trim('"')
                            val timestamp = payload["created_at"]?.toString()?.toLongOrNull() ?: System.currentTimeMillis()
                            if (clientUuid != null && serverId != null) {
                                val msg = chatDao.getMessageById(clientUuid)
                                if (msg != null) {
                                    chatDao.confirmSentMessage(clientUuid, msg.copy(id = serverId, timestamp = timestamp, status = MessageStatus.SENT))
                                }
                            }
                        }
                    }
                    "DELIVERY_ACK" -> {
                        val payload = envelope.payload
                        if (payload != null) {
                            val messageId = payload["message_id"]?.toString()?.trim('"')
                            if (messageId != null) {
                                chatDao.markAsDelivered(messageId)
                            }
                        }
                    }
                    "READ_ACK" -> {
                        val payload = envelope.payload
                        if (payload != null) {
                            val messageId = payload["message_id"]?.toString()?.trim('"')
                            if (messageId != null) {
                                chatDao.markAsRead(messageId)
                            }
                        }
                    }
                }
            }
            topic == "presence" -> {
                val payload = envelope.payload
                if (payload != null) {
                    try {
                        val userId = payload["user_id"]?.toString()?.trim('"')
                        val eventType = envelope.event ?: ""
                        
                        // Support multiple event formats for presence
                        val isOnline = when (eventType.uppercase()) {
                            "ONLINE" -> true
                            "OFFLINE" -> false
                            else -> payload["is_online"]?.toString()?.toBoolean() ?: false
                        }
                        
                        val lastSeen = payload["last_seen"]?.toString()?.toLongOrNull() ?: System.currentTimeMillis()
                        
                        if (userId != null) {
                            Log.d("ChatRepository", "Presence event: user=$userId, online=$isOnline, event=$eventType")
                            chatDao.updatePresence(userId, isOnline, lastSeen)
                        }
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Failed to decode presence event", e)
                    }
                }
            }
            topic == "jobs" -> {
                // Trigger job list refresh if needed, or handle specific job events
                Log.d("ChatRepository", "Received jobs event: ${envelope.event}")
            }
            topic.startsWith("user:") -> {
                when (envelope.event) {
                    "balance_update" -> {
                        val newBalance = envelope.payload?.get("balance")?.toString()?.trim('"')?.toDoubleOrNull()
                        if (newBalance != null) {
                            Log.i("ChatRepository", "Received balance update via personal WS topic: $newBalance")
                            RewardsManager.onBalanceUpdate(newBalance)
                        }
                    }
                    "REWARD_UPDATE" -> {
                        Log.i("ChatRepository", "Received reward update via personal WS topic")
                        // General notification handling
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getMessagesPaged(chatId: String): Flow<PagingData<Message>> {
        val db = dbProvider.get()
        val chatDao = chatDaoProvider.get()
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            remoteMediator = MessageRemoteMediator(chatId, chatApi, db, sessionManager),
            pagingSourceFactory = { chatDao.getMessagesPaging(chatId) }
        ).flow.map { pagingData ->
            pagingData.map { it.toModel() }
        }
    }

    override fun getMessages(chatId: String, page: Int): Flow<Resource<List<Message>>> = 
        chatDaoProvider.get().getMessages(chatId).map { list ->
            Resource.Success(list.map { it.toModel() })
        }.distinctUntilChanged()

    override fun fetchMessages(chatId: String, page: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.getMessages(chatId, cursor = null, limit = 50)
            if (response.isSuccessful && response.body() != null) {
                val apiMessages = response.body()!!.messages
                val myUserId = sessionManager.getUserId()
                val privKey = sessionManager.getPrivateKey()
                val entities = apiMessages.map { mapApiMessage(it, privKey, myUserId).toEntity(chatId) }
                
                Log.d("ChatRepository", "Syncing ${entities.size} messages for chat $chatId")
                try {
                    val db = dbProvider.get()
                    db.withTransaction {
                        chatDaoProvider.get().insertMessages(entities)
                    }
                    emit(Resource.Success(true))
                } catch (e: Exception) {
                    Log.e("ChatRepository", "DB error during message sync", e)
                    emit(Resource.Error("Database unavailable"))
                }
            } else {
                emit(Resource.Error("Failed to fetch: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error syncing messages", e)
            emit(Resource.Error(e.message ?: "Sync error"))
        }
    }

    override fun sendMessage(
        chatId: String, body: String, messageType: String, mediaUrls: List<String>,
        duration: String?, fileName: String?, fileSize: String?,
        contactName: String?, contactPhone: String?, latitude: Double?, longitude: Double?,
        replyToId: String?, replyToText: String?, replyToSenderName: String?,
        clientId: String?, scheduledAt: Long?
    ): Flow<Resource<Message>> = flow {
        val clientUuid = clientId ?: UUID.randomUUID().toString()
        val myUserId = sessionManager.getUserId() ?: "0"
        val type = try { MessageType.valueOf(messageType.uppercase()) } catch(_: Exception) { MessageType.TEXT }

        val pendingMsg = Message(
            id = clientUuid, conversationId = chatId, content = body, senderId = myUserId,
            timestamp = System.currentTimeMillis(), isFromMe = true, status = MessageStatus.PENDING,
            clientId = clientUuid, type = type, mediaUrls = mediaUrls, duration = duration,
            fileName = fileName, fileSize = fileSize, contactName = contactName,
            contactPhone = contactPhone, latitude = latitude, longitude = longitude,
            replyToId = replyToId, replyToText = replyToText, replyToSenderName = replyToSenderName
        )
        
        val chatDao = chatDaoProvider.get()
        if (scheduledAt == null) {
            chatDao.insertMessages(listOf(pendingMsg.toEntity(chatId)))
            updateLastMessage(chatId, body, pendingMsg.timestamp, true, false)
            emit(Resource.Success(pendingMsg))
        }

        try {
            val response = chatApi.sendMessage(
                conversationID = chatId,
                request = SendMessageRequest(
                    client_uuid = clientUuid, message_text = body, media_type = messageType.uppercase(),
                    media_urls = mediaUrls, duration = duration, file_name = fileName, file_size = fileSize,
                    contact_name = contactName, contact_phone = contactPhone, latitude = latitude,
                    longitude = longitude, reply_to_id = replyToId, scheduled_at = scheduledAt,
                    link_preview = pendingMsg.linkPreview, audio_amplitudes = pendingMsg.audioAmplitudes
                )
            )

            if (response.isSuccessful && response.body() != null) {
                if (scheduledAt == null) {
                    val privKey = sessionManager.getPrivateKey()
                    var serverMsg = mapApiMessage(response.body()!!.message, privKey, myUserId)
                    
                    // Preserve mediaUrls if server response doesn't echo them
                    if (serverMsg.mediaUrls.isEmpty() && pendingMsg.mediaUrls.isNotEmpty()) {
                        serverMsg = serverMsg.copy(mediaUrls = pendingMsg.mediaUrls)
                    }

                    val db = dbProvider.get()
                    db.withTransaction {
                        chatDao.deleteMessage(clientUuid)
                        chatDao.insertMessages(listOf(serverMsg.toEntity(chatId)))
                    }
                    updateLastMessage(chatId, serverMsg.content, serverMsg.timestamp, true, true)
                    emit(Resource.Success(serverMsg))
                } else {
                    emit(Resource.Success(pendingMsg))
                }
            } else {
                if (scheduledAt == null) {
                    chatDao.insertMessages(listOf(pendingMsg.copy(status = MessageStatus.FAILED).toEntity(chatId)))
                    // Only enqueue if NOT currently syncing to avoid loops
                    if (clientId == null) { 
                        com.example.firstapp.common.data.worker.ChatOutboxWorker.enqueue(context)
                    }
                }
                emit(Resource.Error("Failed to send message: ${response.code()}"))
            }
        } catch (e: Exception) {
            if (scheduledAt == null) {
                chatDao.insertMessages(listOf(pendingMsg.copy(status = MessageStatus.FAILED).toEntity(chatId)))
                if (clientId == null) {
                    com.example.firstapp.common.data.worker.ChatOutboxWorker.enqueue(context)
                }
            }
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }

    override fun getChats(): Flow<Resource<List<ChatMessage>>> = 
        chatDaoProvider.get().getChats().map { list ->
            Log.d("ChatRepository", "getChats: emitting ${list.size} chats from local DB")
            Resource.Success(list.map { it.toModel() }) 
        }.distinctUntilChanged()

    override fun getChatById(chatId: String): Flow<Resource<ChatMessage>> =
        chatDaoProvider.get().getChatById(chatId).map { entity ->
            if (entity != null) Resource.Success(entity.toModel())
            else Resource.Error("Chat not found")
        }.distinctUntilChanged()

    override fun syncChats(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = withTimeout(30000) {
                chatApi.getChats()
            }
            if (response.isSuccessful && response.body() != null) {
                val apiChats = response.body()!!.chats
                
                val entities = apiChats.map { chat ->
                    val mapped = mapApiChatSummary(chat)
                    var finalName = ""
                    
                    if (mapped.type == com.example.firstapp.common.model.ConversationType.DIRECT) {
                        val phone = chat.phone_number ?: ""
                        val title = chat.title ?: ""
                        val otherId = chat.contact_id ?: ""
                        finalName = displayNameResolver.resolve(
                            com.example.firstapp.common.model.User(id = otherId, displayName = title),
                            phone
                        )
                    } else {
                        val fallback = if (mapped.type == com.example.firstapp.common.model.ConversationType.CHANNEL) "Channel" else "Group"
                        finalName = if (chat.title.isNotBlank() && 
                            chat.title.lowercase() != "unknown" &&
                            !chat.title.matches(Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"))
                        ) {
                             chat.title
                        } else {
                             fallback
                        }
                    }
                    
                    mapped.copy(senderName = finalName, phoneNumber = chat.phone_number).toEntity()
                }.distinctBy { it.id }
                
                val db = dbProvider.get()
                val chatDao = chatDaoProvider.get()
                
                // Expert Fix: Preserve local state like isSubscribed if server doesn't provide it
                val localChats = chatDao.getChats().first().associateBy { it.id }
                
                val finalEntities = entities.map { serverEntity ->
                    val local = localChats[serverEntity.id]
                    if (local != null && serverEntity.type == com.example.firstapp.common.model.ConversationType.CHANNEL) {
                        // Preserve local truth for subscription and admin status (backend field names might vary or be missing)
                        serverEntity.copy(
                            isSubscribed = local.isSubscribed || serverEntity.isSubscribed,
                            isAdmin = local.isAdmin || serverEntity.isAdmin,
                            isMuted = local.isMuted,
                            adminsCanPost = local.adminsCanPost || serverEntity.adminsCanPost,
                            createdBy = serverEntity.createdBy ?: local.createdBy
                        )
                    } else {
                        serverEntity
                    }
                }

                try {
                    db.withTransaction {
                        chatDao.insertChats(finalEntities)
                        
                        // Expert: Also update ChannelDao with conversation metadata for followed channels
                        val channelDao = channelDaoProvider.get()
                        apiChats.forEach { apiChat ->
                            if (apiChat.type.trim().uppercase().contains("CHANNEL")) {
                                val channelId = apiChat.channel_id
                                if (channelId != null) {
                                    channelDao.updateConversationMetadata(channelId, apiChat.conversation_id)
                                }
                            }
                        }
                    }
                    emit(Resource.Success(true))
                } catch (e: Exception) {
                    Log.e("ChatRepository", "DB closed or error during chat sync", e)
                    emit(Resource.Error("Database unavailable"))
                }
            } else {
                emit(Resource.Error("Sync failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error syncing chats", e)
            emit(Resource.Error(e.message ?: "Error syncing chats")) 
        }
    }

    override suspend fun refreshLocalChatNames() {
        val chatDao = chatDaoProvider.get()
        val contactDao = contactDaoProvider.get()
        
        val allChats = chatDao.getChats().first()
        val localContacts = contactDao.getContacts().first().associateBy { it.id }
        val updated = java.util.ArrayList<ChatEntity>()
        
        for (entity in allChats) {
            if (entity.type == com.example.firstapp.common.model.ConversationType.DIRECT) {
                val targetId = entity.otherUserId ?: entity.id
                val contact = localContacts[targetId]
                val name = displayNameResolver.resolve(
                    com.example.firstapp.common.model.User(id = targetId, displayName = ""),
                    contact?.phoneNumber ?: entity.phoneNumber ?: ""
                )
                if (name.isNotBlank() && name != entity.senderName) {
                    updated.add(entity.copy(senderName = name))
                }
            }
        }
        if (updated.isNotEmpty()) chatDao.insertChats(updated)
    }

    override suspend fun initiateDM(contactId: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.initiateDM(InitiateDMRequest(contactId))
            if (response.isSuccessful && response.body() != null) {
                val conversationId = response.body()!!.conversation_id
                val contactDao = contactDaoProvider.get()
                val contact = contactDao.getContactById(contactId)
                
                val resolvedName = displayNameResolver.resolve(
                    com.example.firstapp.common.model.User(id = contactId, displayName = contact?.displayName ?: ""),
                    contact?.phoneNumber ?: ""
                )
                
                val newChat = ChatEntity(
                    id = conversationId, type = com.example.firstapp.common.model.ConversationType.DIRECT,
                    senderName = resolvedName, lastMessage = "",
                    timestamp = System.currentTimeMillis(), unreadCount = 0, avatarUrl = contact?.avatarUrl,
                    otherUserId = contactId, isOnline = false, isMuted = false, isPinned = false, isTyping = false,
                    phoneNumber = contact?.phoneNumber
                )
                chatDaoProvider.get().insertChats(listOf(newChat))
                emit(Resource.Success(conversationId))
            } else emit(Resource.Error("Failed to initiate DM"))
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun getContacts(): Flow<Resource<List<Contact>>> = 
        contactDaoProvider.get().getContacts().map { list -> Resource.Success(list.map { it.toModel() }) }

    override fun syncContacts(phoneNumbers: List<String>): Flow<Resource<List<Contact>>> = flow {
        emit(Resource.Loading())
        try {
            val deviceContacts = ContactUtils.getPhoneContactsWithNames(context)
            
            // Huawei/Privacy Safety: If we have permission but the list is empty, don't clear remote list.
            // An empty list here usually means system restriction or transient error, 
            // and syncing it would hide all stories for the user.
            val savedContacts = userApi.getSavedContacts().body().orEmpty()
            if (deviceContacts.isEmpty() && phoneNumbers.isEmpty() && savedContacts.isEmpty()) {
                Log.w("ChatRepository", "Skipping contact sync: No local contacts found")
                val existing = contactDaoProvider.get().getContacts().first()
                emit(Resource.Success(existing.map { it.toModel() }))
                return@flow
            }

            val localByPhone = deviceContacts.associateBy { it.phoneNumber }
            val persistResponse = userApi.syncAccountContacts(
                SyncAccountContactsRequest(
                    deviceContacts.map { AccountContactDTO(name = it.displayName, phone_number = it.phoneNumber) }
                )
            )
            if (!persistResponse.isSuccessful) {
                Log.w("ChatRepository", "Named contact persistence failed: ${persistResponse.code()}")
            }
            val savedByPhone = savedContacts.associateBy { it.phone_number }
            val numbersToSync = (phoneNumbers + deviceContacts.map { it.phoneNumber } + savedContacts.map { it.phone_number }).distinct()
            val response = userApi.syncContacts(SyncContactsRequest(numbersToSync))
            if (response.isSuccessful && response.body() != null) {
                val apiUsers = response.body()!!.contacts
                val registeredContacts = apiUsers.map { apiUser ->
                    val localMatch = apiUser.phone_number?.let { localByPhone[it] }
                    val savedMatch = apiUser.phone_number?.let { savedByPhone[it] }
                    Contact(
                        id = apiUser.id, username = apiUser.username,
                        displayName = localMatch?.displayName ?: savedMatch?.name ?: apiUser.display_name,
                        avatarUrl = apiUser.avatar_url, phoneNumber = apiUser.phone_number ?: "",
                        isRegistered = true
                    )
                }
                // Also keep non-registered device contacts so the full address book is shown
                // (the UI renders them with an "Invite" action for people not on the app yet).
                val registeredPhones = apiUsers.mapNotNull { it.phone_number }.toSet()
                val allSavedContacts = (deviceContacts.map { AccountContactDTO(name = it.displayName, phone_number = it.phoneNumber) } + savedContacts)
                    .distinctBy { it.phone_number }
                val nonRegisteredContacts = allSavedContacts
                    .filter { it.phone_number.isNotBlank() && it.phone_number !in registeredPhones }
                    .map { deviceContact ->
                        Contact(
                            id = "phone:${deviceContact.phone_number}",
                            username = deviceContact.phone_number,
                            displayName = deviceContact.name,
                            avatarUrl = null,
                            phoneNumber = deviceContact.phone_number,
                            isRegistered = false
                        )
                    }
                val contacts = registeredContacts + nonRegisteredContacts
                contactDaoProvider.get().insertContacts(contacts.map { it.toEntity() })
                emit(Resource.Success(contacts))
            } else emit(Resource.Error("Sync failed"))
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error syncing contacts")) }
    }

    override fun syncDelta(): Flow<Resource<Long>> = flow {
        emit(Resource.Loading())
        try {
            val dao = syncCursorDaoProvider.get()
            val since = dao.getCursor("messages")?.lastSyncedSequence ?: 0L
            val response = chatApi.syncDelta(since)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                
                val privKey = sessionManager.getPrivateKey()
                val myUserId = sessionManager.getUserId()
                var processingFailed = false

                for (jsonEl in body.messages) {
                    try {
                        val apiMsg = gson.fromJson(jsonEl.toString(), ApiMessage::class.java)
                        val message = mapApiMessage(apiMsg, privKey, myUserId)
                        saveMessage(message.conversationId, message)
                        chatDaoProvider.get().updateLastMessage(
                            message.conversationId,
                            message.content,
                            message.timestamp,
                            message.isFromMe,
                            message.isRead
                        )
                    } catch (e: Exception) {
                        processingFailed = true
                        Log.e("ChatRepository", "Failed to process delta event", e)
                    }
                }

                if (processingFailed) {
                    emit(Resource.Error("Failed to process message sync"))
                    return@flow
                }

                dao.upsertCursor(
                    SyncCursorEntity(
                        entityType = "messages",
                        lastSyncedSequence = body.latestSequence,
                        lastSyncedAt = System.currentTimeMillis()
                    )
                )
                emit(Resource.Success(body.latestSequence))
            } else {
                emit(Resource.Error("Delta sync failed"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Delta sync error"))
        }
    }

    override fun getArchivedChats(): Flow<Resource<List<ChatMessage>>> = 
        chatDaoProvider.get().getArchivedChats().map { list -> Resource.Success(list.map { it.toModel() }) }

    override fun getCallLogs(): Flow<Resource<List<CallLog>>> = 
        callLogDaoProvider.get().getCallLogs().map { list -> Resource.Success(list.map { it.toModel() }) }

    override fun fetchOlderMessages(chatId: String, beforeTs: Long, limit: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val chatDao = chatDaoProvider.get()
            val oldestMsg = chatDao.getOldestMessage(chatId)
            val cursor = oldestMsg?.id
            
            val response = chatApi.getMessages(chatId, cursor = cursor, limit = limit)
            if (response.isSuccessful && response.body() != null) {
                val apiMessages = response.body()!!.messages
                val myUserId = sessionManager.getUserId()
                val privKey = sessionManager.getPrivateKey()
                val entities = apiMessages.map { mapApiMessage(it, privKey, myUserId).toEntity(chatId) }
                chatDao.insertMessages(entities)
                emit(Resource.Success(apiMessages.isNotEmpty()))
            } else emit(Resource.Error("Failed to fetch messages"))
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun markAsRead(chatId: String): Flow<Resource<Boolean>> = flow {
        try {
            val chatDao = chatDaoProvider.get()
            chatDao.markIncomingAsRead(chatId)
            chatDao.updateUnreadCount(chatId, 0)
            
            // WS Ack for real-time visibility
            webSocketManager.send("chat", "READ_ACK", buildJsonObject {
                put("conversation_id", chatId)
                put("timestamp", System.currentTimeMillis())
            })

            chatApi.markAsRead(chatId, ReadReceiptRequest(""))
            emit(Resource.Success(true))
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun deleteMessage(messageId: String, deleteForEveryone: Boolean): Flow<Resource<Boolean>> = flow {
        try {
            val chatDao = chatDaoProvider.get()
            val chatId = chatDao.getChatIdForMessage(messageId)
            
            chatDao.deleteMessage(messageId)
            if (deleteForEveryone && chatId != null) {
                chatApi.deleteMessage(chatId, messageId)
            }
            emit(Resource.Success(true))
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun forwardMessage(messageId: String, receiverIds: List<String>, groupIds: List<String>): Flow<Resource<Boolean>> = flow { emit(Resource.Success(true)) }
    override fun archiveChat(chatId: String, isArchived: Boolean): Flow<Resource<Boolean>> = flow { chatDaoProvider.get().updateArchived(chatId, isArchived); emit(Resource.Success(true)) }

    override fun getGroupInfo(groupId: String): Flow<Resource<GroupInfoResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.getGroupInfo(groupId)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                val membersResp = chatApi.getMembers(groupId)
                if (membersResp.isSuccessful && membersResp.body() != null) {
                    emit(Resource.Success(GroupInfoResponse(groupId, "Group", membersResp.body()!!.map { it.id }, emptyList(), 0)))
                } else emit(Resource.Error("Failed to load group info"))
            }
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun addGroupMember(groupId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try { 
            val response = chatApi.addGroupMember(groupId, AddMemberRequest(userId))
            if (response.isSuccessful) {
                // Refresh group info to update local state/UI
                getGroupInfo(groupId).collect {}
                emit(Resource.Success(true))
            } else {
                val error = response.errorBody()?.string() ?: "Failed to add member"
                emit(Resource.Error(error))
            }
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun removeGroupMember(groupId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try { 
            val response = chatApi.removeGroupMember(groupId, userId)
            if (response.isSuccessful) {
                getGroupInfo(groupId).collect {}
                emit(Resource.Success(true)) 
            } else {
                emit(Resource.Error("Failed to remove member"))
            }
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun promoteToAdmin(groupId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try { 
            val response = chatApi.promoteToAdmin(groupId, userId)
            if (response.isSuccessful) {
                getGroupInfo(groupId).collect {}
                emit(Resource.Success(true)) 
            } else {
                emit(Resource.Error("Failed to promote member"))
            }
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun leaveGroup(groupId: String): Flow<Resource<Boolean>> = flow {
        try { chatApi.leaveGroup(groupId); chatDaoProvider.get().deleteChat(groupId); emit(Resource.Success(true)) } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun getSharedMedia(chatId: String): Flow<Resource<List<Message>>> = 
        chatDaoProvider.get().getMessages(chatId).map { list ->
            Resource.Success(list.filter { it.type == MessageType.IMAGE || it.type == MessageType.VIDEO }.map { it.toModel() })
        }

    override fun editMessage(messageId: String, newBody: String): Flow<Resource<Message>> = flow {
        emit(Resource.Loading())
        try {
            val chatDao = chatDaoProvider.get()
            val chatId = chatDao.getChatIdForMessage(messageId) ?: ""
            
            // 1. Local Update
            chatDao.updateMessageContent(messageId, newBody)
            
            // 2. Server Update
            val response = chatApi.editMessage(chatId, messageId, EditMessageRequest(newBody))
            if (response.isSuccessful && response.body() != null) {
                val privKey = sessionManager.getPrivateKey()
                val myUserId = sessionManager.getUserId()
                val serverMsg = mapApiMessage(response.body()!!.message, privKey, myUserId)
                emit(Resource.Success(serverMsg))
            } else {
                emit(Resource.Error("Failed to edit on server"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Edit error"))
        }
    }
    override fun reactToMessage(messageId: String, emoji: String): Flow<Resource<Boolean>> = flow { chatDaoProvider.get().updateMessageReactions(messageId, mapOf(emoji to 1)); emit(Resource.Success(true)) }

    override suspend fun saveMessage(chatId: String, message: Message) {
        val chatDao = chatDaoProvider.get()
        val entity = message.toEntity(chatId)
        
        // Expert Fix: Check for duplicate/pending message using clientId
        val clientId = entity.clientId
        if (clientId != null && entity.id != clientId) {
            val pending = chatDao.getMessageById(clientId)
            if (pending != null) {
                Log.d("ChatRepository", "Detected pending message for clientId=$clientId, confirming with serverId=${entity.id}")
                
                // Preserve local media URLs if the server didn't provide any (optimistic UI persistence)
                var finalEntity = entity
                if (finalEntity.mediaUrls.isEmpty() && pending.mediaUrls.isNotEmpty()) {
                    finalEntity = finalEntity.copy(mediaUrls = pending.mediaUrls)
                }
                
                chatDao.confirmSentMessage(clientId, finalEntity)
                return
            }
        }
        
        chatDao.insertMessages(listOf(entity)) 
    }
    override suspend fun updateMessageInCache(chatId: String, message: Message) { chatDaoProvider.get().insertMessages(listOf(message.toEntity(chatId))) }
    override suspend fun updateMessageReactions(messageId: String, reactions: Map<String, Int>) { chatDaoProvider.get().updateMessageReactions(messageId, reactions) }
    override suspend fun deleteMessageLocal(messageId: String) { chatDaoProvider.get().deleteMessage(messageId) }
    override suspend fun saveChat(chat: ChatMessage) { chatDaoProvider.get().insertChats(listOf(chat.toEntity())) }
    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) { /* Implement */ }
    override suspend fun markChatAsReadLocal(chatId: String) { chatDaoProvider.get().markIncomingAsRead(chatId) }
    override suspend fun updatePresence(chatId: String, isOnline: Boolean, lastSeen: Long) { chatDaoProvider.get().updatePresence(chatId, isOnline, lastSeen) }
    override suspend fun updateTyping(chatId: String, isTyping: Boolean) { 
        chatDaoProvider.get().updateTyping(chatId, isTyping)
        webSocketManager.send("chat", "typing", buildJsonObject { 
            put("conversation_id", chatId)
            put("is_typing", isTyping)
        })
    }
    override suspend fun incrementUnreadCount(chatId: String) { chatDaoProvider.get().incrementUnreadCount(chatId) }
    override suspend fun updateLastMessage(chatId: String, lastMessage: String, time: Long, isFromMe: Boolean, isRead: Boolean) { chatDaoProvider.get().updateLastMessage(chatId, lastMessage, time, isFromMe, isRead) }

    override suspend fun createGroup(name: String, contactIds: List<String>): Flow<Resource<ChatMessage>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.createGroup(CreateGroupRequest(name, contactIds))
            if (response.isSuccessful && response.body() != null) {
                val conversationId = response.body()!!.conversation_id
                val group = ChatMessage(conversationId, com.example.firstapp.common.model.ConversationType.GROUP, name, isGroup = true)
                chatDaoProvider.get().insertChats(listOf(group.toEntity()))
                emit(Resource.Success(group))
            } else emit(Resource.Error("Failed to create group"))
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun pinChat(chatId: String, isPinned: Boolean): Flow<Resource<Boolean>> = flow { chatDaoProvider.get().updatePinned(chatId, isPinned); emit(Resource.Success(true)) }
    override fun muteChat(chatId: String, isMuted: Boolean): Flow<Resource<Boolean>> = flow { chatDaoProvider.get().updateMuted(chatId, isMuted); emit(Resource.Success(true)) }
    override fun deleteChat(chatId: String): Flow<Resource<Boolean>> = flow {
        try {
            val chatDao = chatDaoProvider.get()
            val db = dbProvider.get()
            
            // Critical: Immediate local wipe so UI reacts instantly
            db.withTransaction {
                chatDao.deleteChat(chatId)
                chatDao.clearMessages(chatId)
            }
            
            val response = chatApi.deleteConversation(chatId)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                // If API fails, sync back might happen, but local is gone for now
                emit(Resource.Error("Server failed to delete: ${response.code()}"))
            }
        } catch (e: Exception) { 
            Log.e("ChatRepository", "Error deleting chat locally", e)
            emit(Resource.Error(e.message ?: "Error")) 
        }
    }
    override suspend fun clearMessages(chatId: String) { chatDaoProvider.get().clearMessages(chatId) }
    override suspend fun clearAllChats() { val chatDao = chatDaoProvider.get(); chatDao.clearChats(); chatDao.clearAllMessages() }
    override fun starMessage(messageId: String, isStarred: Boolean): Flow<Resource<Boolean>> = flow {
        try {
            chatDaoProvider.get().updateMessageStarred(messageId, isStarred)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }
    override fun getStarredMessages(): Flow<Resource<List<Message>>> = 
        chatDaoProvider.get().getStarredMessages().map { list ->
            Resource.Success(list.map { it.toModel() })
        }
    override suspend fun updateWallpaper(chatId: String, wallpaperUrl: String?) { chatDaoProvider.get().updateWallpaper(chatId, wallpaperUrl) }
    override suspend fun updateCustomNotificationTone(chatId: String, tone: String?) { chatDaoProvider.get().updateCustomNotificationTone(chatId, tone) }
    override suspend fun updateMediaVisibility(chatId: String, visible: Boolean) { chatDaoProvider.get().updateMediaVisibility(chatId, visible) }
    override suspend fun updateDisappearingMessages(chatId: String, duration: Int) { chatDaoProvider.get().updateDisappearingMessages(chatId, duration) }
    override suspend fun updateChatAvatar(chatId: String, avatarUrl: String?) { chatDaoProvider.get().updateAvatar(chatId, avatarUrl) }
    override suspend fun syncPendingMessages() {
        val chatDao = chatDaoProvider.get()
        val pending = chatDao.getPendingMessages()
        if (pending.isEmpty()) return

        Log.d("ChatRepository", "Syncing ${pending.size} pending messages")
        for (entity in pending) {
            val msg = entity.toModel()
            sendMessage(
                chatId = entity.conversationId,
                body = entity.content,
                messageType = entity.type.name,
                mediaUrls = entity.mediaUrls,
                duration = entity.duration,
                fileName = entity.fileName,
                fileSize = entity.fileSize,
                contactName = entity.contactName,
                contactPhone = entity.contactPhone,
                latitude = entity.latitude,
                longitude = entity.longitude,
                replyToId = entity.replyToId,
                replyToText = entity.replyToText,
                replyToSenderName = entity.replyToSenderName,
                clientId = entity.clientId
            ).collect {}
        }
    }
    override fun pinMessage(chatId: String, messageId: String?): Flow<Resource<Boolean>> = flow { emit(Resource.Success(true)) }
    override fun getMessageReaders(messageId: String): Flow<Resource<List<ApiReader>>> = flow {
        emit(Resource.Loading())
        try {
            // This would normally call an endpoint like GET conversations/{cid}/messages/{mid}/readers
            // For now, we return empty list if not implemented on backend, or we can add it to ChatApi
            emit(Resource.Success(emptyList()))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }
    override fun reportMessage(messageId: String, reason: String): Flow<Resource<Boolean>> = flow { emit(Resource.Success(true)) }
    override fun initiateCall(receiverId: String, callType: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.initiateCall(InitiateCallRequest(receiverId, callType))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.session_id))
            } else {
                emit(Resource.Error("Failed to initiate call: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error initiating call", e)
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }
    override fun searchLocalMessages(chatId: String, query: String): Flow<List<Message>> = chatDaoProvider.get().getMessages(chatId).map { it.filter { m -> m.content.contains(query, true) }.map { e -> e.toModel() } }
    override fun searchLocalChats(query: String): Flow<List<ChatMessage>> = chatDaoProvider.get().getChats().map { it.filter { c -> c.senderName.contains(query, true) }.map { e -> e.toModel() } }
    override fun searchLocalContacts(query: String): Flow<List<Contact>> = contactDaoProvider.get().searchContacts(query).map { list -> list.map { it.toModel() } }
    override fun searchMessagesAcrossAll(query: String, filter: com.example.firstapp.search.model.SearchFilter): Flow<List<Message>> = chatDaoProvider.get().searchMessagesAcrossAll(query).map { it.map { e -> e.toModel() } }
    override fun getAllFiles(): Flow<Resource<List<Message>>> = 
        chatDaoProvider.get().getAllFiles().map { list ->
            Resource.Success(list.map { it.toModel() })
        }
    override fun updateJobApplicationStatus(chatId: String, status: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.updateApplicationStatus(chatId, StatusUpdateRequest(status))
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to update status: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }
    override fun getConversationMembers(chatId: String): Flow<Resource<List<ApiUser>>> = flow { emit(Resource.Success(emptyList())) }
    override suspend fun getICEServers(): Resource<List<IceServerConfig>> {
        return try {
            val response = chatApi.getICEServers()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.ice_servers)
            } else {
                Log.e("ChatRepository", "Failed to fetch ICE servers: ${response.code()}")
                Resource.Success(emptyList()) // Fallback to STUN defaults in WebRtcManager
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error fetching ICE servers", e)
            Resource.Success(emptyList())
        }
    }

    override fun subscribeToChannel(channelId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.subscribeToChannel(channelId)
            if (response.isSuccessful) {
                // 1. Join WebSocket topic immediately for real-time messages
                webSocketManager.send("chat:$channelId", "join", op = "subscribe")

                // 2. Update local ChatEntity subscription status
                val chatDao = chatDaoProvider.get()
                val chat = chatDao.searchChatsById(channelId)
                if (chat != null) {
                    // Endpoint is typically a toggle
                    chatDao.insertChats(listOf(chat.copy(isSubscribed = true)))
                } else {
                    // 3. If chat entity doesn't exist, fetch details from server
                    try {
                        val detailsResp = chatApi.getChatDetails(channelId)
                        if (detailsResp.isSuccessful && detailsResp.body() != null) {
                            val newChat = mapApiChatSummary(detailsResp.body()!!).toEntity()
                            // Force subscribed to true since we just succeeded
                            chatDao.insertChats(listOf(newChat.copy(isSubscribed = true)))
                        }
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Failed to fetch chat details after subscribe", e)
                    }
                }
                
                // Background sync chats to reflect membership
                syncChats().collect {}
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to follow channel"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Offline"))
        }
    }

    override fun sendDeliveryAck(chatId: String, messageId: String) {
        webSocketManager.send("chat", "DELIVERY_ACK", buildJsonObject {
            put("message_id", messageId)
            put("conversation_id", chatId)
        })
    }
}
