package com.example.firstapp.chat.data.repository

import android.content.Context
import android.util.Log
import androidx.paging.*
import androidx.room.withTransaction
import com.example.firstapp.chat.data.*
import com.example.firstapp.chat.data.api.*
import com.example.firstapp.chat.data.local.dao.*
import com.example.firstapp.chat.data.local.entity.ChatEntity
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.put
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.ResponseBody

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val userApi: UserApi,
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val callLogDao: CallLogDao,
    private val sessionManager: SessionManager,
    private val db: AppDatabase,
    private val webSocketManager: WebSocketManager,
    private val displayNameResolver: DisplayNameResolver,
    @ApplicationContext private val context: Context
) : ChatRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    private val json = Json { 
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    init {
        observeWebSocketEvents()
    }

    private fun observeWebSocketEvents() {
        webSocketManager.events
            .onEach { envelope ->
                handleIncomingEvent(envelope)
            }
            .catch { e -> Log.e("ChatRepository", "WS Flow Error", e) }
            .launchIn(scope)
    }

    private suspend fun handleIncomingEvent(envelope: WSEnvelope) {
        when (envelope.topic) {
            "chat" -> {
                when (envelope.event) {
                    "message" -> {
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
                                
                                // Step 2: Update the chat summary (Rule 1: Name resolved via phone)
                                val phone = apiMsg.contact_phone ?: ""
                                val resolvedName = displayNameResolver.resolve(null, phone)

                                chatDao.updateLastMessage(
                                    message.conversationId, 
                                    message.content, 
                                    message.timestamp, 
                                    message.isFromMe, 
                                    message.isRead
                                )
                                
                                if (!message.isFromMe) {
                                    chatDao.incrementUnreadCount(message.conversationId)
                                }

                                // Expert: Verify if chat exists, if not, trigger a full sync to fetch metadata
                                val exists = chatDao.searchChatsById(message.conversationId) != null
                                if (!exists) {
                                    Log.i("ChatRepository", "New conversation detected via WS: ${message.conversationId}, syncing...")
                                    syncChats().collect {}
                                }
                            } catch (e: Exception) {
                                Log.e("ChatRepository", "Failed to decode WS message", e)
                            }
                        }
                    }
                    "typing" -> {
                        val payload = envelope.payload
                        if (payload != null) {
                            try {
                                val chatId = payload["conversation_id"]?.toString()?.trim('"')
                                val isTyping = payload["is_typing"]?.toString()?.toBoolean() ?: false
                                chatId?.let { chatDao.updateTyping(it, isTyping) }
                            } catch (e: Exception) {
                                Log.e("ChatRepository", "Failed to decode typing event", e)
                            }
                        }
                    }
                }
            }
            "presence" -> {
                val payload = envelope.payload
                if (payload != null) {
                    try {
                        val userId = payload["user_id"]?.toString()?.trim('"')
                        val isOnline = payload["is_online"]?.toString()?.toBoolean() ?: false
                        val lastSeen = payload["last_seen"]?.toString()?.toLongOrNull() ?: System.currentTimeMillis()
                        userId?.let { chatDao.updatePresence(it, isOnline, lastSeen) }
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Failed to decode presence event", e)
                    }
                }
            }
            "jobs" -> {
                // Trigger job list refresh if needed, or handle specific job events
                Log.d("ChatRepository", "Received jobs event: ${envelope.event}")
            }
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getMessagesPaged(chatId: String): Flow<PagingData<Message>> {
        return Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            remoteMediator = MessageRemoteMediator(chatId, chatApi, db, sessionManager),
            pagingSourceFactory = { chatDao.getMessagesPaging(chatId) }
        ).flow.map { pagingData ->
            pagingData.map { it.toModel() }
        }
    }

    override fun getMessages(chatId: String, page: Int): Flow<Resource<List<Message>>> = 
        chatDao.getMessages(chatId).map { list ->
            Resource.Success(list.map { it.toModel() })
        }

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
                db.withTransaction {
                    chatDao.insertMessages(entities)
                }
                emit(Resource.Success(true))
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
        replyToId: String?, replyToText: String?, replyToSenderName: String?
    ): Flow<Resource<Message>> = flow {
        val clientUuid = UUID.randomUUID().toString()
        val myUserId = sessionManager.getUserId() ?: "0"
        val type = try { MessageType.valueOf(messageType.uppercase()) } catch(_: Exception) { MessageType.TEXT }

        val pendingMsg = Message(
            id = clientUuid, conversationId = chatId, content = body, senderId = myUserId,
            timestamp = System.currentTimeMillis(), isFromMe = true, status = MessageStatus.SENDING,
            clientId = clientUuid, type = type, mediaUrls = mediaUrls, duration = duration,
            fileName = fileName, fileSize = fileSize, contactName = contactName,
            contactPhone = contactPhone, latitude = latitude, longitude = longitude,
            replyToId = replyToId, replyToText = replyToText, replyToSenderName = replyToSenderName
        )
        
        chatDao.insertMessages(listOf(pendingMsg.toEntity(chatId)))
        updateLastMessage(chatId, body, pendingMsg.timestamp, true, false)
        emit(Resource.Success(pendingMsg))

        try {
            val response = chatApi.sendMessage(
                conversationID = chatId,
                request = SendMessageRequest(
                    client_uuid = clientUuid, message_text = body, media_type = messageType.uppercase(),
                    media_urls = mediaUrls, duration = duration, file_name = fileName, file_size = fileSize,
                    contact_name = contactName, contact_phone = contactPhone, latitude = latitude,
                    longitude = longitude, reply_to_id = replyToId
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val privKey = sessionManager.getPrivateKey()
                val serverMsg = mapApiMessage(response.body()!!.message, privKey, myUserId)
                db.withTransaction {
                    chatDao.deleteMessage(clientUuid)
                    chatDao.insertMessages(listOf(serverMsg.toEntity(chatId)))
                }
                updateLastMessage(chatId, serverMsg.content, serverMsg.timestamp, true, true)
                emit(Resource.Success(serverMsg))
            } else {
                chatDao.insertMessages(listOf(pendingMsg.copy(status = MessageStatus.FAILED).toEntity(chatId)))
                emit(Resource.Error("Failed to send message"))
            }
        } catch (e: Exception) {
            chatDao.insertMessages(listOf(pendingMsg.copy(status = MessageStatus.FAILED).toEntity(chatId)))
            emit(Resource.Error(e.message ?: "Network error"))
        }
    }

    override fun getChats(): Flow<Resource<List<ChatMessage>>> = 
        chatDao.getChats().map { list ->
            Log.d("ChatRepository", "getChats: emitting ${list.size} chats from local DB")
            Resource.Success(list.map { it.toModel() }) 
        }

    override fun syncChats(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.getChats()
            if (response.isSuccessful && response.body() != null) {
                val apiChats = response.body()!!.chats
                
                val entities = apiChats.map { chat ->
                    val mapped = mapApiChatSummary(chat)
                    var finalName = ""
                    
                    if (mapped.type == com.example.firstapp.common.model.ConversationType.DIRECT) {
                        // Requirement 1: Resolve strictly via Local Contacts or Raw Phone
                        val phone = chat.phone_number ?: ""
                        finalName = displayNameResolver.resolve(null, phone)
                    } else {
                        // For Groups/Channels, use the provided title or fallback
                        finalName = if (chat.title.isNotBlank() && chat.title.lowercase() != "unknown") {
                             chat.title
                        } else {
                             "Group"
                        }
                    }
                    
                    Log.d("ChatRepository", "Sync: chat ${chat.conversation_id} resolved name to '$finalName'")
                    mapped.copy(senderName = finalName, phoneNumber = chat.phone_number).toEntity()
                }
                
                db.withTransaction {
                    // Update only current chats, don't delete everything to avoid UI flicker
                    chatDao.insertChats(entities)
                }
                emit(Resource.Success(true))
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ChatRepository", "Sync failed: ${response.code()} $errorBody")
                emit(Resource.Error("Sync failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error syncing chats", e)
            emit(Resource.Error(e.message ?: "Error syncing chats")) 
        }
    }

    override suspend fun refreshLocalChatNames() {
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
                val contact = contactDao.getContactById(contactId)
                
                // Requirement 1: Strict resolution for new DM
                val resolvedName = displayNameResolver.resolve(null, contact?.phoneNumber ?: "")
                
                val newChat = ChatEntity(
                    id = conversationId, type = com.example.firstapp.common.model.ConversationType.DIRECT,
                    senderName = resolvedName, lastMessage = "",
                    timestamp = System.currentTimeMillis(), unreadCount = 0, avatarUrl = contact?.avatarUrl,
                    otherUserId = contactId, isOnline = false, isMuted = false, isPinned = false, isTyping = false,
                    phoneNumber = contact?.phoneNumber
                )
                chatDao.insertChats(listOf(newChat))
                emit(Resource.Success(conversationId))
            } else emit(Resource.Error("Failed to initiate DM"))
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun getContacts(): Flow<Resource<List<Contact>>> = 
        contactDao.getContacts().map { list -> Resource.Success(list.map { it.toModel() }) }

    override fun syncContacts(phoneNumbers: List<String>): Flow<Resource<List<Contact>>> = flow {
        emit(Resource.Loading())
        try {
            val deviceContacts = ContactUtils.getPhoneContactsWithNames(context)
            val numbersToSync = (phoneNumbers + deviceContacts.map { it.phoneNumber }).distinct()
            val response = userApi.syncContacts(SyncContactsRequest(numbersToSync))
            if (response.isSuccessful && response.body() != null) {
                val apiUsers = response.body()!!.contacts
                val contacts = apiUsers.map { apiUser ->
                    val localMatch = deviceContacts.find { it.phoneNumber == apiUser.phone_number }
                    Contact(
                        id = apiUser.id, username = apiUser.username, 
                        displayName = localMatch?.displayName ?: apiUser.display_name,
                        avatarUrl = apiUser.avatar_url, phoneNumber = apiUser.phone_number ?: "",
                        isRegistered = true
                    )
                }
                contactDao.insertContacts(contacts.map { it.toEntity() })
                emit(Resource.Success(contacts))
            } else emit(Resource.Error("Sync failed"))
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error syncing contacts")) }
    }

    override fun getArchivedChats(): Flow<Resource<List<ChatMessage>>> = 
        chatDao.getArchivedChats().map { list -> Resource.Success(list.map { it.toModel() }) }

    override fun getCallLogs(): Flow<Resource<List<CallLog>>> = 
        callLogDao.getCallLogs().map { list -> Resource.Success(list.map { it.toModel() }) }

    override fun fetchOlderMessages(chatId: String, beforeTs: Long, limit: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
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
            chatDao.markIncomingAsRead(chatId)
            chatDao.updateUnreadCount(chatId, 0)
            chatApi.markAsRead(chatId, ReadReceiptRequest(""))
            emit(Resource.Success(true))
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun deleteMessage(messageId: String, deleteForEveryone: Boolean): Flow<Resource<Boolean>> = flow {
        try {
            chatDao.deleteMessage(messageId)
            if (deleteForEveryone) chatApi.deleteMessage(messageId)
            emit(Resource.Success(true))
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun forwardMessage(messageId: String, receiverIds: List<String>, groupIds: List<String>): Flow<Resource<Boolean>> = flow { emit(Resource.Success(true)) }
    override fun archiveChat(chatId: String, isArchived: Boolean): Flow<Resource<Boolean>> = flow { chatDao.updateArchived(chatId, isArchived); emit(Resource.Success(true)) }

    override fun getGroupInfo(groupId: String): Flow<Resource<GroupInfoResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.getGroupInfo(groupId)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                // Fallback to members only if info fails
                val membersResp = chatApi.getMembers(groupId)
                if (membersResp.isSuccessful && membersResp.body() != null) {
                    emit(Resource.Success(GroupInfoResponse(groupId, "Group", membersResp.body()!!.map { it.id }, emptyList(), 0)))
                } else emit(Resource.Error("Failed to load group info"))
            }
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun addGroupMember(groupId: String, userId: String): Flow<Resource<Boolean>> = flow {
        try { chatApi.addGroupMember(groupId, AddMemberRequest(userId)); emit(Resource.Success(true)) } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun removeGroupMember(groupId: String, userId: String): Flow<Resource<Boolean>> = flow {
        try { chatApi.removeGroupMember(groupId, userId); emit(Resource.Success(true)) } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun promoteToAdmin(groupId: String, userId: String): Flow<Resource<Boolean>> = flow {
        try { chatApi.promoteToAdmin(groupId, userId); emit(Resource.Success(true)) } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun leaveGroup(groupId: String): Flow<Resource<Boolean>> = flow {
        try { chatApi.leaveGroup(groupId); chatDao.deleteChat(groupId); emit(Resource.Success(true)) } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun getSharedMedia(chatId: String): Flow<Resource<List<Message>>> = 
        chatDao.getMessages(chatId).map { list ->
            Resource.Success(list.filter { it.type == MessageType.IMAGE || it.type == MessageType.VIDEO }.map { it.toModel() })
        }

    override fun editMessage(messageId: String, newBody: String): Flow<Resource<Message>> = flow { /* Implement */ }
    override fun reactToMessage(messageId: String, emoji: String): Flow<Resource<Boolean>> = flow { chatDao.updateMessageReactions(messageId, mapOf(emoji to 1)); emit(Resource.Success(true)) }

    override suspend fun saveMessage(chatId: String, message: Message) { chatDao.insertMessages(listOf(message.toEntity(chatId))) }
    override suspend fun updateMessageInCache(chatId: String, message: Message) { chatDao.insertMessages(listOf(message.toEntity(chatId))) }
    override suspend fun updateMessageReactions(messageId: String, reactions: Map<String, Int>) { chatDao.updateMessageReactions(messageId, reactions) }
    override suspend fun deleteMessageLocal(messageId: String) { chatDao.deleteMessage(messageId) }
    override suspend fun saveChat(chat: ChatMessage) { chatDao.insertChats(listOf(chat.toEntity())) }
    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) { /* Implement */ }
    override suspend fun markChatAsReadLocal(chatId: String) { chatDao.markIncomingAsRead(chatId) }
    override suspend fun updatePresence(chatId: String, isOnline: Boolean, lastSeen: Long) { chatDao.updatePresence(chatId, isOnline, lastSeen) }
    override suspend fun updateTyping(chatId: String, isTyping: Boolean) { 
        chatDao.updateTyping(chatId, isTyping)
        webSocketManager.send("chat", "typing", buildJsonObject { 
            put("conversation_id", chatId)
            put("is_typing", isTyping)
        })
    }
    override suspend fun incrementUnreadCount(chatId: String) { chatDao.incrementUnreadCount(chatId) }
    override suspend fun updateLastMessage(chatId: String, lastMessage: String, time: Long, isFromMe: Boolean, isRead: Boolean) { chatDao.updateLastMessage(chatId, lastMessage, time, isFromMe, isRead) }

    override suspend fun createGroup(name: String, contactIds: List<String>): Flow<Resource<ChatMessage>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.createGroup(CreateGroupRequest(name, contactIds))
            if (response.isSuccessful && response.body() != null) {
                val conversationId = response.body()!!.conversation_id
                val group = ChatMessage(conversationId, com.example.firstapp.common.model.ConversationType.GROUP, name, isGroup = true)
                chatDao.insertChats(listOf(group.toEntity()))
                emit(Resource.Success(group))
            } else emit(Resource.Error("Failed to create group"))
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun pinChat(chatId: String, isPinned: Boolean): Flow<Resource<Boolean>> = flow { chatDao.updatePinned(chatId, isPinned); emit(Resource.Success(true)) }
    override fun muteChat(chatId: String, isMuted: Boolean): Flow<Resource<Boolean>> = flow { chatDao.updateMuted(chatId, isMuted); emit(Resource.Success(true)) }
    override fun deleteChat(chatId: String): Flow<Resource<Boolean>> = flow { chatDao.deleteChat(chatId); emit(Resource.Success(true)) }
    override suspend fun clearMessages(chatId: String) { chatDao.clearMessages(chatId) }
    override suspend fun clearAllChats() { chatDao.clearChats(); chatDao.clearAllMessages() }
    override fun starMessage(messageId: String, isStarred: Boolean): Flow<Resource<Boolean>> = flow { emit(Resource.Success(true)) }
    override fun getStarredMessages(): Flow<Resource<List<Message>>> = flow { emit(Resource.Success(emptyList())) }
    override suspend fun updateWallpaper(chatId: String, wallpaperUrl: String?) { chatDao.updateWallpaper(chatId, wallpaperUrl) }
    override suspend fun updateCustomNotificationTone(chatId: String, tone: String?) { chatDao.updateCustomNotificationTone(chatId, tone) }
    override suspend fun updateMediaVisibility(chatId: String, visible: Boolean) { chatDao.updateMediaVisibility(chatId, visible) }
    override suspend fun updateDisappearingMessages(chatId: String, duration: Int) { chatDao.updateDisappearingMessages(chatId, duration) }
    override suspend fun syncPendingMessages() {}
    override fun pinMessage(chatId: String, messageId: String?): Flow<Resource<Boolean>> = flow { emit(Resource.Success(true)) }
    override fun getMessageReaders(messageId: String): Flow<Resource<List<ApiReader>>> = flow { emit(Resource.Success(emptyList())) }
    override fun reportMessage(messageId: String, reason: String): Flow<Resource<Boolean>> = flow { emit(Resource.Success(true)) }
    override fun initiateCall(receiverId: String, callType: String): Flow<Resource<String>> = flow { emit(Resource.Success(UUID.randomUUID().toString())) }
    override fun searchLocalMessages(chatId: String, query: String): Flow<List<Message>> = chatDao.getMessages(chatId).map { it.filter { m -> m.content.contains(query, true) }.map { e -> e.toModel() } }
    override fun searchLocalChats(query: String): Flow<List<ChatMessage>> = chatDao.getChats().map { it.filter { c -> c.senderName.contains(query, true) }.map { e -> e.toModel() } }
    override fun searchLocalContacts(query: String): Flow<List<Contact>> = contactDao.searchContacts(query).map { list -> list.map { it.toModel() } }
    override fun searchMessagesAcrossAll(query: String, filter: com.example.firstapp.search.model.SearchFilter): Flow<List<Message>> = chatDao.searchMessagesAcrossAll(query).map { it.map { e -> e.toModel() } }
    override fun getAllFiles(): Flow<Resource<List<Message>>> = flow { emit(Resource.Success(emptyList())) }
    override fun updateJobApplicationStatus(chatId: String, status: String): Flow<Resource<Boolean>> = flow { emit(Resource.Success(true)) }
    override fun getConversationMembers(chatId: String): Flow<Resource<List<ApiUser>>> = flow { emit(Resource.Success(emptyList())) }
    override suspend fun getICEServers(): Resource<List<IceServerConfig>> = Resource.Success(emptyList())
}
