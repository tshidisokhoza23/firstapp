package com.example.firstapp.chat.data.repository

import android.content.Context
import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.example.firstapp.chat.data.api.*
import com.example.firstapp.chat.data.local.dao.*
import com.example.firstapp.chat.data.local.entity.ChatEntity
import com.example.firstapp.chat.data.local.entity.MessageEntity
import com.example.firstapp.chat.data.local.entity.SyncCursorEntity
import com.example.firstapp.chat.data.paging.MessageRemoteMediator
import com.example.firstapp.chat.model.*
import com.example.firstapp.chat.rewards.data.RewardsManager
import com.example.firstapp.common.data.api.AccountContactDTO
import com.example.firstapp.common.data.api.ApiUser
import com.example.firstapp.common.data.api.SyncAccountContactsRequest
import com.example.firstapp.common.data.api.SyncContactsRequest
import com.example.firstapp.common.data.api.UserApi
import com.example.firstapp.common.data.local.AppDatabase
import com.example.firstapp.common.data.realtime.WSEnvelope
import com.example.firstapp.common.data.realtime.WebSocketManager
import com.example.firstapp.common.data.worker.ChatOutboxWorker
import com.example.firstapp.common.domain.auth.SessionManager
import com.example.firstapp.common.util.ContactUtils
import com.example.firstapp.common.util.DisplayNameResolver
import com.example.firstapp.common.util.Resource
import com.example.firstapp.domain.repository.ChatRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.example.firstapp.media.data.api.SocialApi
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.util.UUID
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

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
                    val db = dbProvider.get()
                    if (db.isOpen) {
                        val chatDao = chatDaoProvider.get()
                        val chats = chatDao.getChatsWithDisappearingMessages()
                        val now = System.currentTimeMillis()

                        db.withTransaction {
                            chats.forEach { chat ->
                                val expiryTime = now - (chat.disappearingMessagesDuration * 1000L)
                                chatDao.deleteMessagesBefore(chat.id, expiryTime)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatRepository", "Error pruning disappearing messages", e)
                }
                delay(60_000)
            }
        }
    }

    private fun observeWebSocketEvents() {
        webSocketManager.events
            .onEach { envelope -> handleIncomingEvent(envelope) }
            .catch { e -> Log.e("ChatRepository", "WS Flow Exception", e) }
            .launchIn(scope)
    }

    private fun observeConnectionState() {
        webSocketManager.connectionState
            .onEach { state ->
                if (state == WebSocketManager.ConnectionState.CONNECTED) {
                    subscribeToTopics()
                    // Atomic sync catch-up on connect
                    scope.launch { syncDelta().collect {} }
                }
            }
            .launchIn(scope)
    }

    /**
     * Batch subscribes to all active conversation channels in one shot
     * to eliminate socket congestion and buffer overflow on reconnect.
     */
    private suspend fun subscribeToTopics() {
        val myUserId = sessionManager.getUserId() ?: return
        try {
            val chatDao = chatDaoProvider.get()
            val channelDao = channelDaoProvider.get()

            val chatIds = chatDao.getAllChatIds()
            val channelConvIds = channelDao.getChannels().first()
                .filter { it.isSubscribed }
                .mapNotNull { it.conversationId ?: it.id }

            val allIds = (chatIds + channelConvIds).distinct()

            // 1. Subscribe to personal routing channels
            webSocketManager.send("subscribe", buildJsonObject { put("topic", "user:$myUserId") })
            webSocketManager.send("subscribe", buildJsonObject { put("topic", "presence") })
            webSocketManager.send("subscribe", buildJsonObject { put("topic", "chat") })

            // 2. Join all active chat rooms
            allIds.forEach { id ->
                webSocketManager.send("subscribe", buildJsonObject { put("topic", "chat:$id") })
            }
            Log.d("ChatRepository", "Subscribed to ${allIds.size} total active chat topics")
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to execute batch subscription", e)
        }
    }

    private suspend fun handleIncomingEvent(envelope: WSEnvelope) {
        val db = try { dbProvider.get() } catch (e: Exception) { return }
        if (!db.isOpen) return

        val chatDao = chatDaoProvider.get()
        val event = envelope.type ?: ""

        Log.d("ChatRepository", "handleIncomingEvent: type='$event'")

        when (event) {
            "message_new" -> {
                envelope.payload?.let { payload ->
                    try {
                        val apiMsg = parseIncomingApiMessage(payload)
                        val myUserId = sessionManager.getUserId()
                        val privKey = sessionManager.getPrivateKey()
                        var message = mapApiMessage(apiMsg, privKey, myUserId)

                        if (message.content.isBlank()) {
                            val altContent = payload["content"]?.jsonPrimitive?.contentOrNull
                                ?: payload["text"]?.jsonPrimitive?.contentOrNull
                                ?: payload["body"]?.jsonPrimitive?.contentOrNull ?: ""
                            if (altContent.isNotBlank()) {
                                message = message.copy(content = altContent)
                            }
                        }

                        if (message.conversationId.isBlank() || message.id.isBlank()) {
                            Log.w("ChatRepository", "Dropping message_new with blank id/conversationId")
                            return@let
                        }

                        db.withTransaction {
                            saveMessage(message.conversationId, message)

                            val phone = apiMsg.contact_phone ?: ""
                            val senderName = apiMsg.sender_name ?: ""
                            val resolvedName = displayNameResolver.resolve(
                                com.example.firstapp.common.model.User(id = apiMsg.sender_id, displayName = senderName),
                                phone
                            )

                            val displaySummary = formatLastMessageSummary(message.content, message.type)
                            val existingChat = chatDao.searchChatsById(message.conversationId)

                            if (existingChat != null) {
                                chatDao.updateLastMessage(
                                    message.conversationId,
                                    displaySummary,
                                    message.timestamp,
                                    message.isFromMe,
                                    message.isRead
                                )
                                if (!message.isFromMe) {
                                    chatDao.incrementUnreadCount(message.conversationId)
                                }
                            } else {
                                // The server now tags every message with the conversation's real
                                // type, so a brand-new local chat entity can be created correctly
                                // on the first message instead of guessing from the sender id.
                                val conversationType = (payload["conversation_type"]?.jsonPrimitive?.contentOrNull)
                                    .toConversationTypeOrNull() ?: com.example.firstapp.common.model.ConversationType.DIRECT

                                val newChat = if (conversationType == com.example.firstapp.common.model.ConversationType.CHANNEL) {
                                    val channel = channelDaoProvider.get().getChannels().first()
                                        .find { it.conversationId == message.conversationId }
                                    ChatEntity(
                                        id = message.conversationId,
                                        type = com.example.firstapp.common.model.ConversationType.CHANNEL,
                                        senderName = channel?.name ?: "Channel",
                                        lastMessage = displaySummary,
                                        timestamp = message.timestamp,
                                        unreadCount = if (!message.isFromMe) 1 else 0,
                                        avatarUrl = channel?.avatarUrl,
                                        otherUserId = null,
                                        isOnline = false,
                                        isMuted = channel?.isMuted ?: false,
                                        isPinned = false,
                                        isTyping = false,
                                        isAdmin = channel?.isAdmin ?: false,
                                        isSubscribed = channel?.isSubscribed ?: false,
                                        adminsCanPost = channel?.adminsCanPost ?: false,
                                        createdBy = channel?.createdBy,
                                        phoneNumber = "",
                                        lastMessageIsFromMe = message.isFromMe,
                                        lastMessageRead = message.isRead
                                    )
                                } else if (conversationType == com.example.firstapp.common.model.ConversationType.GROUP) {
                                    ChatEntity(
                                        id = message.conversationId,
                                        type = com.example.firstapp.common.model.ConversationType.GROUP,
                                        senderName = "Group",
                                        lastMessage = displaySummary,
                                        timestamp = message.timestamp,
                                        unreadCount = if (!message.isFromMe) 1 else 0,
                                        avatarUrl = null,
                                        otherUserId = null,
                                        isOnline = false,
                                        isMuted = false,
                                        isPinned = false,
                                        isTyping = false,
                                        phoneNumber = "",
                                        lastMessageIsFromMe = message.isFromMe,
                                        lastMessageRead = message.isRead
                                    )
                                } else {
                                    ChatEntity(
                                        id = message.conversationId,
                                        type = com.example.firstapp.common.model.ConversationType.DIRECT,
                                        senderName = resolvedName.ifBlank { senderName.ifBlank { "New Chat" } },
                                        lastMessage = displaySummary,
                                        timestamp = message.timestamp,
                                        unreadCount = if (!message.isFromMe) 1 else 0,
                                        avatarUrl = null,
                                        otherUserId = apiMsg.sender_id,
                                        isOnline = false,
                                        isMuted = false,
                                        isPinned = false,
                                        isTyping = false,
                                        phoneNumber = phone,
                                        lastMessageIsFromMe = message.isFromMe,
                                        lastMessageRead = message.isRead
                                    )
                                }
                                chatDao.insertChats(listOf(newChat))
                            }
                        }

                        if (!message.isFromMe) {
                            sendDeliveryAck(message.conversationId, message.id)
                        }
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Failed to decode WS message payload", e)
                    }
                }
            }

            "message_ack" -> {
                envelope.payload?.let { payload ->
                    val clientUuid = payload["client_msg_id"]?.jsonPrimitive?.contentOrNull
                    val serverId = payload["message_id"]?.jsonPrimitive?.contentOrNull
                    val timestamp = payload["created_at"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
                    if (clientUuid != null && serverId != null) {
                        val msg = chatDao.getMessageById(clientUuid)
                        if (msg != null) {
                            chatDao.confirmSentMessage(clientUuid, msg.copy(id = serverId, timestamp = timestamp, status = MessageStatus.SENT))
                        }
                    }
                }
            }

            "read_receipt", "read_ack" -> {
                envelope.payload?.let { payload ->
                    val convId = payload["conversation_id"]?.jsonPrimitive?.contentOrNull
                    if (convId != null) {
                        db.withTransaction {
                            chatDao.markOutgoingAsRead(convId)
                            chatDao.markOutgoingLastMessageAsRead(convId)
                        }
                    }
                }
            }

            "delivery_receipt", "delivery_ack" -> {
                envelope.payload?.let { payload ->
                    val convId = payload["conversation_id"]?.jsonPrimitive?.contentOrNull
                    if (convId != null) {
                        chatDao.markOutgoingAsDelivered(convId)
                    }
                }
            }

            "message_deleted" -> {
                envelope.payload?.let { payload ->
                    val messageId = payload["message_id"]?.jsonPrimitive?.contentOrNull
                    if (messageId != null) {
                        db.withTransaction { chatDao.deleteMessage(messageId) }
                    }
                }
            }

            "message_edited" -> {
                envelope.payload?.let { payload ->
                    val messageId = payload["message_id"]?.jsonPrimitive?.contentOrNull
                    val newText = payload["message_text"]?.jsonPrimitive?.contentOrNull
                        ?: payload["content"]?.jsonPrimitive?.contentOrNull
                    if (messageId != null && !newText.isNullOrBlank()) {
                        db.withTransaction { chatDao.updateMessageContent(messageId, newText) }
                    }
                }
            }

            "message_reaction" -> {
                envelope.payload?.let { payload ->
                    val messageId = payload["message_id"]?.jsonPrimitive?.contentOrNull ?: return@let
                    val reactionsEl = payload["reactions"]
                    val reactions = if (reactionsEl is JsonObject) {
                        reactionsEl.mapNotNull { (emoji, countEl) ->
                            val count = countEl.jsonPrimitive.intOrNull ?: return@mapNotNull null
                            emoji to count
                        }.toMap()
                    } else {
                        emptyMap()
                    }
                    if (reactions.isNotEmpty()) {
                        db.withTransaction { chatDao.updateMessageReactions(messageId, reactions) }
                    }
                }
            }

            "conversation_deleted" -> {
                envelope.payload?.let { payload ->
                    val chatId = payload["conversation_id"]?.jsonPrimitive?.contentOrNull
                    if (chatId != null) {
                        db.withTransaction {
                            chatDao.deleteChat(chatId)
                            chatDao.clearMessages(chatId)
                        }
                    }
                }
            }

            "conversation_cleared" -> {
                envelope.payload?.let { payload ->
                    val chatId = payload["conversation_id"]?.jsonPrimitive?.contentOrNull
                    if (chatId != null) {
                        db.withTransaction { chatDao.clearMessages(chatId) }
                    }
                }
            }

            "typing" -> {
                envelope.payload?.let { payload ->
                    val chatId = payload["conversation_id"]?.jsonPrimitive?.contentOrNull
                    val stateStr = payload["state"]?.jsonPrimitive?.contentOrNull
                    val isTyping = stateStr == "START"
                    chatId?.let { chatDao.updateTyping(it, isTyping) }
                }
            }

            "presence" -> {
                envelope.payload?.let { payload ->
                    val userId = payload["user_id"]?.jsonPrimitive?.contentOrNull
                    val statusStr = (payload["status"] ?: payload["is_online"])?.jsonPrimitive?.contentOrNull?.lowercase()
                    val isOnline = statusStr == "online" || statusStr == "true"
                    val lastSeen = payload["last_seen"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
                    if (userId != null) {
                        chatDao.updatePresence(userId, isOnline, lastSeen)
                    }
                }
            }

            "channel_subscribed" -> {
                envelope.payload?.let { payload ->
                    val channelId = payload["channel_id"]?.jsonPrimitive?.contentOrNull ?: return@let
                    val isSubscribed = payload["is_subscribed"]?.jsonPrimitive?.booleanOrNull ?: true
                    channelDaoProvider.get().updateSubscription(channelId, isSubscribed)
                    chatDao.updateSubscribed(channelId, isSubscribed)
                }
            }

            "channel_muted" -> {
                envelope.payload?.let { payload ->
                    val channelId = payload["channel_id"]?.jsonPrimitive?.contentOrNull ?: return@let
                    val isMuted = payload["is_muted"]?.jsonPrimitive?.booleanOrNull ?: return@let
                    channelDaoProvider.get().updateMuted(channelId, isMuted)
                    chatDao.updateMuted(channelId, isMuted)
                }
            }

            "channel_admin_posting" -> {
                envelope.payload?.let { payload ->
                    val channelId = payload["channel_id"]?.jsonPrimitive?.contentOrNull ?: return@let
                    val allowed = payload["admins_can_post"]?.jsonPrimitive?.booleanOrNull ?: return@let
                    channelDaoProvider.get().updateAdminsCanPost(channelId, allowed)
                    chatDao.updateAdminsCanPost(channelId, allowed)
                }
            }

            "channel_admin_added" -> {
                envelope.payload?.let { payload ->
                    val channelId = payload["channel_id"]?.jsonPrimitive?.contentOrNull ?: return@let
                    channelDaoProvider.get().updateAdmin(channelId, true)
                    chatDao.updateAdmin(channelId, true)
                }
            }

            "channel_admin_removed" -> {
                envelope.payload?.let { payload ->
                    val channelId = payload["channel_id"]?.jsonPrimitive?.contentOrNull ?: return@let
                    channelDaoProvider.get().updateAdmin(channelId, false)
                    chatDao.updateAdmin(channelId, false)
                }
            }

            "message_pinned" -> {
                envelope.payload?.let { payload ->
                    val chatId = payload["conversation_id"]?.jsonPrimitive?.contentOrNull ?: return@let
                    val pinnedId = payload["pinned_message_id"]?.jsonPrimitive?.contentOrNull
                    chatDao.updatePinnedMessage(chatId, pinnedId?.takeIf { it.isNotBlank() })
                }
            }

            "reward_update" -> {
                envelope.payload?.get("balance")?.jsonPrimitive?.doubleOrNull?.let {
                    RewardsManager.onBalanceUpdate(it)
                }
            }
        }
    }

    /**
     * Pure Offline-First Send Pipeline:
     * 1. Writes to Room with PENDING status.
     * 2. UI emits instantly via Room Flow.
     * 3. Dispatches asynchronously via OutboxSyncWorker.
     */
    override fun sendMessage(
        chatId: String, body: String, messageType: String, mediaUrls: List<String>,
        duration: String?, fileName: String?, fileSize: String?,
        contactName: String?, contactPhone: String?, latitude: Double?, longitude: Double?,
        replyToId: String?, replyToText: String?, replyToSenderName: String?,
        clientId: String?, scheduledAt: Long?
    ): Flow<Resource<Message>> = flow {
        val clientUuid = clientId ?: UUID.randomUUID().toString()
        val myUserId = sessionManager.getUserId() ?: "0"
        val type = try { MessageType.valueOf(messageType.uppercase()) } catch (_: Exception) { MessageType.TEXT }

        val pendingMsg = Message(
            id = clientUuid, conversationId = chatId, content = body, senderId = myUserId,
            timestamp = System.currentTimeMillis(), isFromMe = true, status = MessageStatus.PENDING,
            clientId = clientUuid, type = type, mediaUrls = mediaUrls, duration = duration,
            fileName = fileName, fileSize = fileSize, contactName = contactName,
            contactPhone = contactPhone, latitude = latitude, longitude = longitude,
            replyToId = replyToId, replyToText = replyToText, replyToSenderName = replyToSenderName
        )

        val chatDao = chatDaoProvider.get()
        val db = dbProvider.get()

        db.withTransaction {
            chatDao.insertMessages(listOf(pendingMsg.toEntity(chatId)))
            val displaySummary = formatLastMessageSummary(body, type)
            chatDao.updateLastMessage(chatId, displaySummary, pendingMsg.timestamp, isFromMe = true, isRead = false)
        }

        // Live text goes over the socket. Do NOT also enqueue the outbox for
        // that path — REST SendMessage would persist+broadcast a second copy.
        val sentLive = scheduledAt == null && mediaUrls.isEmpty() && webSocketManager.send(
            "send_message",
            buildJsonObject {
                put("client_msg_id", clientUuid)
                put("conversation_id", chatId)
                put("content", body)
                put("media_type", messageType.uppercase())
            }
        )
        if (sentLive) {
            chatDao.updateMessageStatus(clientUuid, MessageStatus.SENDING)
        }

        emit(Resource.Success(pendingMsg))

        if (!sentLive) {
            ChatOutboxWorker.enqueue(context)
        }
    }

    /**
     * Atomic Delta Sync: Processes catch-up stream inside a single Room transaction
     * to prevent lock contention and UI micro-stutters.
     */
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

                val db = dbProvider.get()
                val chatDao = chatDaoProvider.get()

                val entitiesToInsert = mutableListOf<MessageEntity>()

                for (jsonEl in body.messages) {
                    try {
                        val apiMsg = gson.fromJson(jsonEl.toString(), ApiMessage::class.java)
                        val message = mapApiMessage(apiMsg, privKey, myUserId)
                        entitiesToInsert.add(message.toEntity(message.conversationId))
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Skipping malformed message entity in delta sync", e)
                    }
                }

                // Atomic batch persistence
                db.withTransaction {
                    if (entitiesToInsert.isNotEmpty()) {
                        chatDao.insertMessages(entitiesToInsert)

                        // Update summaries for affected conversations
                        entitiesToInsert.groupBy { it.conversationId }.forEach { (convId, msgs) ->
                            val latest = msgs.maxByOrNull { it.timestamp }
                            if (latest != null) {
                                chatDao.updateLastMessage(
                                    convId,
                                    latest.content,
                                    latest.timestamp,
                                    latest.senderId == myUserId,
                                    latest.isRead
                                )
                            }
                        }
                    }

                    dao.upsertCursor(
                        SyncCursorEntity(
                            entityType = "messages",
                            lastSyncedSequence = body.latestSequence,
                            lastSyncedAt = System.currentTimeMillis()
                        )
                    )
                }

                emit(Resource.Success(body.latestSequence))
            } else {
                emit(Resource.Error("Delta sync failed with code: ${response.code()}"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("ChatRepository", "Delta sync failed", e)
            emit(Resource.Error(e.message ?: "Delta sync failure"))
        }
    }

    override fun getChats(): Flow<Resource<List<ChatMessage>>> =
        chatDaoProvider.get().getChats().map { list ->
            val deduplicated = list.distinctBy { entity ->
                if (entity.type == com.example.firstapp.common.model.ConversationType.DIRECT && !entity.otherUserId.isNullOrBlank()) {
                    "direct_${entity.otherUserId}"
                } else {
                    entity.id
                }
            }
            Resource.Success(deduplicated.map { it.toModel() })
        }.distinctUntilChanged()

    override fun getChatById(chatId: String): Flow<Resource<ChatMessage>> =
        chatDaoProvider.get().getChatById(chatId).map { entity ->
            if (entity != null) Resource.Success(entity.toModel())
            else Resource.Error("Chat not found")
        }.distinctUntilChanged()

    override fun getMessages(chatId: String, page: Int): Flow<Resource<List<Message>>> =
        chatDaoProvider.get().getMessages(chatId).map { list ->
            Resource.Success(list.map { it.toModel() })
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

    override fun fetchMessages(chatId: String, page: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.getMessages(chatId, cursor = null, limit = 50)
            if (response.isSuccessful && response.body() != null) {
                val apiMessages = response.body()!!.messages
                val myUserId = sessionManager.getUserId()
                val privKey = sessionManager.getPrivateKey()
                val chatDao = chatDaoProvider.get()
                val starredIds = chatDao.getStarredMessageIds().toSet()
                val entities = apiMessages.map { api ->
                    val mapped = mapApiMessage(api, privKey, myUserId).toEntity(chatId)
                    if (mapped.id in starredIds) mapped.copy(isStarred = true) else mapped
                }

                val db = dbProvider.get()
                db.withTransaction {
                    chatDao.insertMessages(entities)
                }
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to fetch messages: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Sync error"))
        }
    }

    override fun syncChats(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = withTimeout(30000) { chatApi.getChats() }
            if (response.isSuccessful && response.body() != null) {
                val apiChats = response.body()!!.chats
                val contactDao = contactDaoProvider.get()
                val chatDao = chatDaoProvider.get()
                val db = dbProvider.get()

                val entities = apiChats.map { chat ->
                    val mapped = mapApiChatSummary(chat)
                    var finalName = ""

                    if (mapped.type == com.example.firstapp.common.model.ConversationType.DIRECT) {
                        val phone = chat.phone_number ?: ""
                        val title = chat.title ?: ""
                        val otherId = chat.contact_id ?: ""
                        val localContact = if (otherId.isNotBlank()) contactDao.getContactById(otherId) else null
                        val localName = localContact?.displayName?.takeIf { it.isNotBlank() }

                        finalName = localName ?: displayNameResolver.resolve(
                            com.example.firstapp.common.model.User(id = otherId, displayName = title),
                            phone
                        )
                        if (finalName.isBlank()) {
                            finalName = phone.ifBlank { localContact?.username ?: "" }
                        }
                    } else {
                        finalName = if (chat.title.isNotBlank() && chat.title.lowercase() != "unknown") {
                            chat.title
                        } else {
                            if (mapped.type == com.example.firstapp.common.model.ConversationType.CHANNEL) "Channel" else "Group"
                        }
                    }
                    mapped.copy(senderName = finalName, phoneNumber = chat.phone_number).toEntity()
                }.distinctBy { it.id }

                val localChats = chatDao.getChats().first().associateBy { it.id }
                val finalEntities = entities.map { serverEntity ->
                    val local = localChats[serverEntity.id]
                    val latestMsg = chatDao.getLatestMessage(serverEntity.id)
                    var resultEntity = serverEntity

                    if (latestMsg != null) {
                        val displaySummary = formatLastMessageSummary(latestMsg.content, latestMsg.type)
                        if (latestMsg.timestamp >= serverEntity.timestamp || serverEntity.lastMessage.isBlank()) {
                            resultEntity = resultEntity.copy(
                                lastMessage = displaySummary,
                                timestamp = maxOf(serverEntity.timestamp, latestMsg.timestamp),
                                lastMessageIsFromMe = latestMsg.isFromMe,
                                lastMessageRead = latestMsg.isRead
                            )
                        }
                    } else if (local != null && serverEntity.lastMessage.isBlank() && local.lastMessage.isNotBlank()) {
                        resultEntity = resultEntity.copy(
                            lastMessage = local.lastMessage,
                            timestamp = maxOf(serverEntity.timestamp, local.timestamp),
                            lastMessageIsFromMe = local.lastMessageIsFromMe,
                            lastMessageRead = local.lastMessageRead
                        )
                    }

                    if (local != null && serverEntity.type == com.example.firstapp.common.model.ConversationType.CHANNEL) {
                        resultEntity.copy(
                            isSubscribed = local.isSubscribed || serverEntity.isSubscribed,
                            isAdmin = local.isAdmin || serverEntity.isAdmin,
                            isMuted = local.isMuted,
                            adminsCanPost = local.adminsCanPost || serverEntity.adminsCanPost,
                            createdBy = serverEntity.createdBy ?: local.createdBy,
                            pinnedMessageId = serverEntity.pinnedMessageId ?: local.pinnedMessageId
                        )
                    } else if (local != null) {
                        resultEntity.copy(pinnedMessageId = serverEntity.pinnedMessageId ?: local.pinnedMessageId)
                    } else {
                        resultEntity
                    }
                }

                db.withTransaction {
                    chatDao.insertChats(finalEntities)
                }

                val chatsNeedingHistory = finalEntities.filter { entity ->
                    chatDao.getLatestMessage(entity.id) == null
                }
                for (entity in chatsNeedingHistory.take(40)) {
                    try {
                        val history = chatApi.getMessages(entity.id, cursor = null, limit = 50)
                        if (history.isSuccessful && history.body() != null) {
                            val myUserId = sessionManager.getUserId()
                            val privKey = sessionManager.getPrivateKey()
                            val starredIds = chatDao.getStarredMessageIds().toSet()
                            val messages = history.body()!!.messages.map {
                                val mapped = mapApiMessage(it, privKey, myUserId).toEntity(entity.id)
                                if (mapped.id in starredIds) mapped.copy(isStarred = true) else mapped
                            }
                            if (messages.isNotEmpty()) {
                                db.withTransaction {
                                    chatDao.insertMessages(messages)
                                    val latest = messages.maxByOrNull { it.timestamp }
                                    if (latest != null) {
                                        chatDao.updateLastMessage(
                                            entity.id,
                                            formatLastMessageSummary(latest.content, latest.type),
                                            latest.timestamp,
                                            latest.isFromMe,
                                            latest.isRead
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        Log.w("ChatRepository", "hydrate messages failed for ${entity.id}", e)
                    }
                }
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Chat sync failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Chat sync error"))
        }
    }

    override suspend fun saveMessage(chatId: String, message: Message) {
        val chatDao = chatDaoProvider.get()
        val entity = message.toEntity(chatId)

        val clientId = entity.clientId
        if (clientId != null && entity.id != clientId) {
            val pending = chatDao.getMessageById(clientId)
            if (pending != null) {
                var finalEntity = entity
                if (finalEntity.mediaUrls.isEmpty() && pending.mediaUrls.isNotEmpty()) {
                    finalEntity = finalEntity.copy(mediaUrls = pending.mediaUrls)
                }
                chatDao.confirmSentMessage(clientId, finalEntity)
                val displaySummary = formatLastMessageSummary(message.content, message.type)
                chatDao.updateLastMessage(chatId, displaySummary, message.timestamp, message.isFromMe, message.isRead)
                return
            }
        }
        chatDao.insertMessages(listOf(entity))
        val displaySummary = formatLastMessageSummary(message.content, message.type)
        chatDao.updateLastMessage(chatId, displaySummary, message.timestamp, message.isFromMe, message.isRead)
    }

    override fun sendDeliveryAck(chatId: String, messageId: String) {
        webSocketManager.send("delivery_ack", buildJsonObject {
            put("message_id", messageId)
            put("conversation_id", chatId)
        })
    }

    override fun markAsRead(chatId: String): Flow<Resource<Boolean>> = flow {
        try {
            val chatDao = chatDaoProvider.get()
            val db = dbProvider.get()

            db.withTransaction {
                chatDao.markIncomingAsRead(chatId)
                chatDao.updateUnreadCount(chatId, 0)
            }

            webSocketManager.send("read_ack", buildJsonObject {
                put("conversation_id", chatId)
                put("timestamp", System.currentTimeMillis())
            })

            chatApi.markAsRead(chatId, ReadReceiptRequest(""))
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to mark read"))
        }
    }

    override fun deleteChat(chatId: String): Flow<Resource<Boolean>> = flow {
        try {
            val chatDao = chatDaoProvider.get()
            val db = dbProvider.get()

            db.withTransaction {
                chatDao.deleteChat(chatId)
                chatDao.clearMessages(chatId)
            }

            val response = chatApi.deleteConversation(chatId)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Server failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Deletion error"))
        }
    }

    private fun formatLastMessageSummary(content: String, type: MessageType? = null): String {
        val mType = type?.name?.uppercase() ?: ""
        return when {
            mType.contains("LOCATION") || content.startsWith("loc:") -> "📍 Location"
            mType.contains("VIDEO_CALL") -> "📹 Video call"
            mType.contains("VOICE_CALL") || mType.contains("CALL") -> "📞 Voice call"
            mType.contains("IMAGE") -> if (content.isNotBlank() && !content.startsWith("http")) content else "📷 Photo"
            mType.contains("VIDEO") -> if (content.isNotBlank() && !content.startsWith("http")) content else "🎥 Video"
            mType.contains("AUDIO") || mType.contains("VOICE") -> if (content.isNotBlank() && !content.startsWith("http")) content else "🎵 Audio"
            mType.contains("FILE") || mType.contains("DOCUMENT") -> if (content.isNotBlank() && !content.startsWith("http")) content else "📄 Document"
            content.isBlank() && mType.isNotBlank() -> mType.lowercase().replaceFirstChar { it.uppercase() }
            else -> content
        }
    }

    private fun parseIncomingApiMessage(payload: JsonObject): ApiMessage {
        val parsed = try {
            gson.fromJson(payload.toString(), ApiMessage::class.java)
        } catch (_: Exception) {
            ApiMessage()
        }
        val text = parsed.message_text.ifBlank {
            payload["content"]?.jsonPrimitive?.contentOrNull
                ?: payload["body"]?.jsonPrimitive?.contentOrNull
                ?: ""
        }
        val id = parsed.id.ifBlank {
            payload["message_id"]?.jsonPrimitive?.contentOrNull ?: ""
        }
        val clientUuid = parsed.client_uuid.ifBlank {
            payload["client_msg_id"]?.jsonPrimitive?.contentOrNull ?: ""
        }
        return parsed.copy(
            id = id,
            client_uuid = clientUuid,
            message_text = text,
            conversation_id = parsed.conversation_id.ifBlank {
                payload["conversation_id"]?.jsonPrimitive?.contentOrNull ?: ""
            },
            sender_id = parsed.sender_id.ifBlank {
                payload["sender_id"]?.jsonPrimitive?.contentOrNull ?: ""
            }
        )
    }

    private fun String?.toConversationTypeOrNull(): com.example.firstapp.common.model.ConversationType? = when (this?.trim()?.lowercase()) {
        "group" -> com.example.firstapp.common.model.ConversationType.GROUP
        "channel", "hiring_channel" -> com.example.firstapp.common.model.ConversationType.CHANNEL
        "job_chat", "job_application" -> com.example.firstapp.common.model.ConversationType.JOB_CHAT
        "direct", "direct_message" -> com.example.firstapp.common.model.ConversationType.DIRECT
        else -> null
    }

    override fun getContacts(): Flow<Resource<List<Contact>>> =
        contactDaoProvider.get().getContacts().map { list -> Resource.Success(list.map { it.toModel() }) }

    private val syncContactsMutex = Mutex()

    override fun syncContacts(phoneNumbers: List<String>): Flow<Resource<List<Contact>>> = flow {
        emit(Resource.Loading())
        syncContactsMutex.withLock {
            try {
                val deviceContacts = ContactUtils.getPhoneContactsWithNames(context)
                val savedContacts = userApi.getSavedContacts().body().orEmpty()
                if (deviceContacts.isEmpty() && phoneNumbers.isEmpty() && savedContacts.isEmpty()) {
                    val existing = contactDaoProvider.get().getContacts().first()
                    emit(Resource.Success(existing.map { it.toModel() }))
                    return@withLock
                }

                val localByPhone = deviceContacts.associateBy { it.phoneNumber }
                userApi.syncAccountContacts(
                    SyncAccountContactsRequest(
                        deviceContacts.map { AccountContactDTO(name = it.displayName, phone_number = it.phoneNumber) }
                    )
                )

                val savedByPhone = savedContacts.associateBy { it.phone_number }
                val numbersToSync = (phoneNumbers + deviceContacts.map { it.phoneNumber } + savedContacts.map { it.phone_number }).distinct()
                val response = userApi.syncContacts(SyncContactsRequest(numbersToSync))

                if (response.isSuccessful && response.body() != null) {
                    val apiUsers = response.body()!!.contacts
                    val registeredContacts = apiUsers.map { apiUser ->
                        val localMatch = apiUser.phone_number?.let { localByPhone[it] }
                        val savedMatch = apiUser.phone_number?.let { savedByPhone[it] }
                        Contact(
                            id = apiUser.id,
                            username = apiUser.username,
                            displayName = localMatch?.displayName ?: savedMatch?.name ?: apiUser.display_name,
                            avatarUrl = apiUser.avatar_url,
                            phoneNumber = apiUser.phone_number ?: "",
                            isRegistered = true
                        )
                    }

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

                    val existingDbContacts = contactDaoProvider.get().getContacts().first().map { it.toModel() }
                    val mergedContacts = (registeredContacts + nonRegisteredContacts + existingDbContacts).distinctBy { it.id }

                    contactDaoProvider.get().insertContacts(mergedContacts.map { it.toEntity() })
                    emit(Resource.Success(mergedContacts))
                } else {
                    emit(Resource.Error("Contact sync failed"))
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                emit(Resource.Error(e.message ?: "Error syncing contacts"))
            }
        }
    }

    override suspend fun refreshLocalChatNames() {
        val chatDao = chatDaoProvider.get()
        val contactDao = contactDaoProvider.get()
        val allChats = chatDao.getChats().first()
        val localContacts = contactDao.getContacts().first().associateBy { it.id }
        val updated = ArrayList<ChatEntity>()

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
                val contact = contactDaoProvider.get().getContactById(contactId)
                val chatDao = chatDaoProvider.get()

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
                chatDao.insertChats(listOf(newChat))
                webSocketManager.send("subscribe", buildJsonObject { put("topic", "chat:$conversationId") })
                emit(Resource.Success(conversationId))
            } else {
                emit(Resource.Error("Failed to initiate DM"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override suspend fun syncPendingMessages() {
        val chatDao = chatDaoProvider.get()
        val pending = chatDao.getPendingMessages()
        if (pending.isEmpty()) return

        Log.d("ChatRepository", "Syncing ${pending.size} pending messages from OutboxWorker")
        for (entity in pending) {
            try {
                chatDao.updateMessageStatus(entity.clientId ?: entity.id, MessageStatus.SENDING)
                val response = chatApi.sendMessage(
                    conversationID = entity.conversationId,
                    request = SendMessageRequest(
                        client_uuid = entity.clientId ?: entity.id,
                        message_text = entity.content,
                        media_type = entity.type.name.uppercase(),
                        media_urls = entity.mediaUrls,
                        duration = entity.duration,
                        file_name = entity.fileName,
                        file_size = entity.fileSize,
                        contact_name = entity.contactName,
                        contact_phone = entity.contactPhone,
                        latitude = entity.latitude,
                        longitude = entity.longitude,
                        reply_to_id = entity.replyToId,
                        reply_to_text = entity.replyToText,
                        reply_to_sender_name = entity.replyToSenderName,
                        scheduled_at = entity.scheduledAt,
                        link_preview = entity.linkPreview,
                        audio_amplitudes = entity.audioAmplitudes
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val privKey = sessionManager.getPrivateKey()
                    val myUserId = sessionManager.getUserId()
                    val serverMsg = mapApiMessage(response.body()!!.message, privKey, myUserId)
                    chatDao.confirmSentMessage(entity.clientId ?: entity.id, serverMsg.toEntity(entity.conversationId))
                } else {
                    chatDao.updateMessageStatus(entity.clientId ?: entity.id, MessageStatus.FAILED)
                }
            } catch (e: Exception) {
                Log.e("ChatRepository", "Failed to sync pending message id=${entity.id}", e)
                chatDao.updateMessageStatus(entity.clientId ?: entity.id, MessageStatus.FAILED)
                throw e
            }
        }
    }

    override suspend fun updateTyping(chatId: String, isTyping: Boolean) {
        chatDaoProvider.get().updateTyping(chatId, isTyping)
        webSocketManager.send("typing", buildJsonObject {
            put("conversation_id", chatId)
            put("state", if (isTyping) "START" else "STOP")
        })
    }

    override suspend fun updatePresence(chatId: String, isOnline: Boolean, lastSeen: Long) {
        chatDaoProvider.get().updatePresence(chatId, isOnline, lastSeen)
    }

    override suspend fun saveChat(chat: ChatMessage) { chatDaoProvider.get().insertChats(listOf(chat.toEntity())) }
    override suspend fun updateMessageInCache(chatId: String, message: Message) { chatDaoProvider.get().insertMessages(listOf(message.toEntity(chatId))) }
    override suspend fun updateMessageReactions(messageId: String, reactions: Map<String, Int>) { chatDaoProvider.get().updateMessageReactions(messageId, reactions) }
    override suspend fun deleteMessageLocal(messageId: String) { chatDaoProvider.get().deleteMessage(messageId) }
    override suspend fun markChatAsReadLocal(chatId: String) { chatDaoProvider.get().markIncomingAsRead(chatId) }
    override suspend fun incrementUnreadCount(chatId: String) { chatDaoProvider.get().incrementUnreadCount(chatId) }
    override suspend fun updateLastMessage(chatId: String, lastMessage: String, time: Long, isFromMe: Boolean, isRead: Boolean) {
        chatDaoProvider.get().updateLastMessage(chatId, formatLastMessageSummary(lastMessage), time, isFromMe, isRead)
    }
    override suspend fun clearMessages(chatId: String) {
        chatApi.clearConversation(chatId)
        chatDaoProvider.get().clearMessages(chatId)
    }
    override suspend fun clearAllChats() {
        val db = dbProvider.get()
        db.clearAllTables()
    }
    override fun getArchivedChats(): Flow<Resource<List<ChatMessage>>> = chatDaoProvider.get().getArchivedChats().map { list -> Resource.Success(list.map { it.toModel() }) }
    override fun getCallLogs(): Flow<Resource<List<CallLog>>> = callLogDaoProvider.get().getCallLogs().map { list -> Resource.Success(list.map { it.toModel() }) }
    override fun getSharedMedia(chatId: String): Flow<Resource<List<Message>>> = chatDaoProvider.get().getMessages(chatId).map { list -> Resource.Success(list.filter { it.type == MessageType.IMAGE || it.type == MessageType.VIDEO }.map { it.toModel() }) }
    override fun getStarredMessages(): Flow<Resource<List<Message>>> = flow {
        try {
            val response = chatApi.getStarredMessages()
            if (response.isSuccessful) {
                val privKey = sessionManager.getPrivateKey()
                val myUserId = sessionManager.getUserId()
                val entities = response.body()?.messages.orEmpty().map { api ->
                    mapApiMessage(api, privKey, myUserId).toEntity(api.conversation_id).copy(isStarred = true)
                }
                if (entities.isNotEmpty()) {
                    chatDaoProvider.get().insertMessages(entities)
                }
            }
        } catch (_: Exception) {
        }
        emitAll(chatDaoProvider.get().getStarredMessages().map { list -> Resource.Success(list.map { it.toModel() }) })
    }
    override fun starMessage(messageId: String, isStarred: Boolean): Flow<Resource<Boolean>> = flow {
        val chatDao = chatDaoProvider.get()
        chatDao.updateMessageStarred(messageId, isStarred)
        try {
            if (isStarred) {
                chatApi.starMessage(messageId, StarMessageRequest(starred = true))
            } else {
                chatApi.unstarMessage(messageId)
            }
        } catch (_: Exception) {
        }
        emit(Resource.Success(true))
    }
    override fun pinChat(chatId: String, isPinned: Boolean): Flow<Resource<Boolean>> = flow {
        chatDaoProvider.get().updatePinned(chatId, isPinned)
        try {
            chatApi.updateConversationPrefs(chatId, ConversationPrefsRequest(is_pinned = isPinned))
        } catch (_: Exception) {
        }
        emit(Resource.Success(true))
    }
    override fun muteChat(chatId: String, isMuted: Boolean): Flow<Resource<Boolean>> = flow {
        chatDaoProvider.get().updateMuted(chatId, isMuted)
        try {
            chatApi.updateConversationPrefs(chatId, ConversationPrefsRequest(is_muted = isMuted))
        } catch (_: Exception) {
        }
        emit(Resource.Success(true))
    }
    override fun archiveChat(chatId: String, isArchived: Boolean): Flow<Resource<Boolean>> = flow {
        chatDaoProvider.get().updateArchived(chatId, isArchived)
        try {
            chatApi.updateConversationPrefs(chatId, ConversationPrefsRequest(is_archived = isArchived))
        } catch (_: Exception) {
        }
        emit(Resource.Success(true))
    }
    override suspend fun updateWallpaper(chatId: String, wallpaperUrl: String?) { chatDaoProvider.get().updateWallpaper(chatId, wallpaperUrl) }
    override suspend fun updateCustomNotificationTone(chatId: String, tone: String?) { chatDaoProvider.get().updateCustomNotificationTone(chatId, tone) }
    override suspend fun updateMediaVisibility(chatId: String, visible: Boolean) { chatDaoProvider.get().updateMediaVisibility(chatId, visible) }
    override suspend fun updateDisappearingMessages(chatId: String, duration: Int) { chatDaoProvider.get().updateDisappearingMessages(chatId, duration) }
    override suspend fun updateChatAvatar(chatId: String, avatarUrl: String?) { chatDaoProvider.get().updateAvatar(chatId, avatarUrl) }
    override fun searchLocalMessages(chatId: String, query: String): Flow<List<Message>> = chatDaoProvider.get().getMessages(chatId).map { it.filter { m -> m.content.contains(query, true) }.map { e -> e.toModel() } }
    override fun searchLocalChats(query: String): Flow<List<ChatMessage>> = chatDaoProvider.get().getChats().map { it.filter { c -> c.senderName.contains(query, true) }.map { e -> e.toModel() } }
    override fun searchLocalContacts(query: String): Flow<List<Contact>> = contactDaoProvider.get().searchContacts(query).map { list -> list.map { it.toModel() } }
    override fun searchMessagesAcrossAll(query: String, filter: com.example.firstapp.search.model.SearchFilter): Flow<List<Message>> = chatDaoProvider.get().searchMessagesAcrossAll(query).map { it.map { e -> e.toModel() } }
    override fun getAllFiles(): Flow<Resource<List<Message>>> = chatDaoProvider.get().getAllFiles().map { list -> Resource.Success(list.map { it.toModel() }) }

    override suspend fun createGroup(name: String, contactIds: List<String>): Flow<Resource<ChatMessage>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.createGroup(CreateGroupRequest(name, contactIds))
            if (response.isSuccessful && response.body() != null) {
                val convId = response.body()!!.conversation_id
                val group = ChatMessage(convId, com.example.firstapp.common.model.ConversationType.GROUP, name, isGroup = true)
                chatDaoProvider.get().insertChats(listOf(group.toEntity()))
                emit(Resource.Success(group))
            } else emit(Resource.Error("Failed to create group"))
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

    override fun editMessage(messageId: String, newBody: String): Flow<Resource<Message>> = flow {
        emit(Resource.Loading())
        try {
            val chatDao = chatDaoProvider.get()
            val chatId = chatDao.getChatIdForMessage(messageId) ?: ""
            chatDao.updateMessageContent(messageId, newBody)
            val response = chatApi.editMessage(chatId, messageId, EditMessageRequest(newBody))
            if (response.isSuccessful && response.body() != null) {
                val privKey = sessionManager.getPrivateKey()
                val myUserId = sessionManager.getUserId()
                emit(Resource.Success(mapApiMessage(response.body()!!.message, privKey, myUserId)))
            } else emit(Resource.Error("Failed to edit on server"))
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Edit error")) }
    }

    override fun reactToMessage(messageId: String, emoji: String): Flow<Resource<Boolean>> = flow {
        val chatDao = chatDaoProvider.get()
        val existing = chatDao.getMessageById(messageId)
        val merged = (existing?.reactions ?: emptyMap()).toMutableMap()
        merged[emoji] = (merged[emoji] ?: 0) + 1
        chatDao.updateMessageReactions(messageId, merged)
        try {
            val chatId = chatDao.getChatIdForMessage(messageId)
            if (!chatId.isNullOrBlank()) {
                val response = chatApi.reactToMessage(chatId, messageId, ReactMessageRequest(emoji))
                if (response.isSuccessful) {
                    response.body()?.reactions?.takeIf { it.isNotEmpty() }?.let {
                        chatDao.updateMessageReactions(messageId, it)
                    }
                    emit(Resource.Success(true))
                } else {
                    emit(Resource.Error("Failed to react"))
                }
            } else {
                emit(Resource.Success(true))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to react"))
        }
    }

    override fun forwardMessage(messageId: String, receiverIds: List<String>, groupIds: List<String>): Flow<Resource<Boolean>> = flow {
        val source = chatDaoProvider.get().getMessageById(messageId)
        if (source == null) {
            emit(Resource.Error("Message not found"))
            return@flow
        }
        val targets = (receiverIds + groupIds).distinct().filter { it.isNotBlank() }
        if (targets.isEmpty()) {
            emit(Resource.Error("No destinations selected"))
            return@flow
        }
        var failed = false
        for (targetId in targets) {
            var chatId = targetId
            val existing = chatDaoProvider.get().searchChatsById(targetId)
            if (existing == null && receiverIds.contains(targetId)) {
                try {
                    initiateDM(targetId).collect { res ->
                        if (res is Resource.Success && !res.data.isNullOrBlank()) {
                            chatId = res.data
                        } else if (res is Resource.Error) {
                            failed = true
                        }
                    }
                } catch (_: Exception) {
                    failed = true
                    continue
                }
            }
            sendMessage(
                chatId = chatId,
                body = source.content,
                messageType = source.type.name.lowercase(),
                mediaUrls = source.mediaUrls,
                duration = source.duration,
                fileName = source.fileName,
                fileSize = source.fileSize,
                contactName = source.contactName,
                contactPhone = source.contactPhone,
                latitude = source.latitude,
                longitude = source.longitude
            ).collect { res ->
                if (res is Resource.Error) failed = true
            }
        }
        if (failed) emit(Resource.Error("Some forwards failed"))
        else emit(Resource.Success(true))
    }
    override fun fetchOlderMessages(chatId: String, beforeTs: Long, limit: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.getMessages(chatId, cursor = beforeTs.toString(), limit = limit)
            if (response.isSuccessful && response.body() != null) {
                val apiMessages = response.body()!!.messages
                val myUserId = sessionManager.getUserId()
                val privKey = sessionManager.getPrivateKey()
                val chatDao = chatDaoProvider.get()
                val starredIds = chatDao.getStarredMessageIds().toSet()
                val entities = apiMessages.map { api ->
                    val mapped = mapApiMessage(api, privKey, myUserId).toEntity(chatId)
                    if (mapped.id in starredIds) mapped.copy(isStarred = true) else mapped
                }
                dbProvider.get().withTransaction {
                    chatDao.insertMessages(entities)
                }
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to fetch older messages: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to fetch older messages"))
        }
    }
    override fun getGroupInfo(groupId: String): Flow<Resource<GroupInfoResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.getGroupInfo(groupId)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to load group"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load group"))
        }
    }

    override fun addGroupMember(groupId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.addChatGroupMember(groupId, AddMemberRequest(userId))
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Only admins can add members"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to add member"))
        }
    }

    override fun removeGroupMember(groupId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.removeChatGroupMember(groupId, AddMemberRequest(userId))
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Only admins can remove members"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to remove member"))
        }
    }

    override fun promoteToAdmin(groupId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.promoteChatGroupAdmin(groupId, AddMemberRequest(userId))
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Only admins can promote members"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to promote"))
        }
    }

    override fun demoteAdmin(groupId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.demoteChatGroupAdmin(groupId, AddMemberRequest(userId))
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Only admins can dismiss admins"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to demote"))
        }
    }

    override fun updateGroupAvatar(groupId: String, avatarUrl: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.updateGroupAvatar(groupId, AvatarUrlRequest(avatarUrl))
            if (response.isSuccessful) {
                runCatching { chatDaoProvider.get().updateAvatar(groupId, avatarUrl) }
                emit(Resource.Success(true))
            } else emit(Resource.Error("Only admins can update the group icon"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update icon"))
        }
    }

    override fun updateChannelAvatar(channelId: String, avatarUrl: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.updateChannelAvatar(channelId, AvatarUrlRequest(avatarUrl))
            if (response.isSuccessful) {
                runCatching { chatDaoProvider.get().updateAvatar(channelId, avatarUrl) }
                emit(Resource.Success(true))
            } else emit(Resource.Error("Only admins can update the channel icon"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update icon"))
        }
    }

    override fun leaveGroup(groupId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.leaveChatGroup(groupId)
            if (response.isSuccessful) {
                try { chatDaoProvider.get().deleteChat(groupId) } catch (_: Exception) {}
                emit(Resource.Success(true))
            } else emit(Resource.Error("Failed to leave group"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to leave group"))
        }
    }

    override fun generateGroupInviteCode(groupId: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.generateGroupInviteCode(groupId)
            val code = response.body()?.invite_code
            if (response.isSuccessful && !code.isNullOrBlank()) emit(Resource.Success(code))
            else emit(Resource.Error("Only admins can create invite links"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to create invite link"))
        }
    }

    override fun joinGroupByInvite(inviteCode: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.joinGroupByInvite(inviteCode)
            val body = response.body()
            val joinedId = body?.conversation_id?.takeIf { it.isNotBlank() }
                ?: body?.group_id?.takeIf { it.isNotBlank() }
            if (response.isSuccessful && !joinedId.isNullOrBlank()) emit(Resource.Success(joinedId))
            else emit(Resource.Error("Invalid invite link"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to join group"))
        }
    }
    override fun pinMessage(chatId: String, messageId: String?): Flow<Resource<Boolean>> = flow {
        val chatDao = chatDaoProvider.get()
        chatDao.updatePinnedMessage(chatId, messageId)
        try {
            val response = chatApi.pinMessage(chatId, PinMessageRequest(message_id = messageId))
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Failed to pin message"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to pin message"))
        }
    }
    override fun getMessageReaders(messageId: String): Flow<Resource<List<ApiReader>>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.getMessageReaders(messageId)
            if (response.isSuccessful) emit(Resource.Success(response.body() ?: emptyList()))
            else emit(Resource.Error("Failed to load readers"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load readers"))
        }
    }
    override fun reportMessage(messageId: String, reason: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.reportMessage(messageId, ReportMessageRequest(reason))
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Failed to report message"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to report message"))
        }
    }
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
    override fun updateJobApplicationStatus(chatId: String, status: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.updateApplicationStatus(chatId, StatusUpdateRequest(status))
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Failed to update application status"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update application status"))
        }
    }
    override fun getConversationMembers(chatId: String): Flow<Resource<List<ApiUser>>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.getMembers(chatId)
            if (response.isSuccessful) emit(Resource.Success(response.body() ?: emptyList()))
            else emit(Resource.Error("Failed to load members"))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to load members"))
        }
    }

    override fun subscribeToChannel(channelId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.subscribeToChannel(channelId)
            if (response.isSuccessful) {
                val channelDao = channelDaoProvider.get()
                val chatDao = chatDaoProvider.get()
                val entity = channelDao.getChannelsOnce()
                    .find { it.id == channelId || it.conversationId == channelId }
                val subscribed = if (entity != null) !entity.isSubscribed else true
                if (entity != null) {
                    channelDao.updateSubscription(entity.id, subscribed)
                }
                val conversationId = entity?.conversationId ?: channelId
                chatDao.updateSubscribed(conversationId, subscribed)
                if (entity != null && entity.id != conversationId) {
                    chatDao.updateSubscribed(entity.id, subscribed)
                }
                if (subscribed) {
                    webSocketManager.send("subscribe", buildJsonObject { put("topic", "chat:$conversationId") })
                }
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to update subscription"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update subscription"))
        }
    }

    override suspend fun getICEServers(): Resource<List<IceServerConfig>> {
        return try {
            val response = chatApi.getICEServers()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.ice_servers)
            } else {
                Resource.Error("Failed to load ICE servers")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to load ICE servers")
        }
    }

    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {}
}
