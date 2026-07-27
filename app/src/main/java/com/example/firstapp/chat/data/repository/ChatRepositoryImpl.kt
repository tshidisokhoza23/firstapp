package com.example.firstapp.chat.data.repository

import android.content.Context
import android.util.Log
import com.example.firstapp.chat.data.ChatWebSocketClient
import com.example.firstapp.chat.data.ChatWebSocketDedupManager
import com.example.firstapp.chat.data.GlobalCallState
import com.example.firstapp.chat.data.api.*
import com.example.firstapp.chat.model.*
import com.example.firstapp.common.data.api.*
import com.example.firstapp.common.data.local.dao.*
import com.example.firstapp.common.data.local.entity.MessageEntity
import com.example.firstapp.common.data.local.entity.ChatEntity
import com.example.firstapp.chat.data.repository.mapApiChatSummary
import com.example.firstapp.chat.data.repository.mapApiMessage
import com.example.firstapp.chat.data.repository.toEntity
import com.example.firstapp.chat.data.repository.toModel
import com.example.firstapp.common.util.Resource
import com.example.firstapp.domain.repository.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map as flowMap
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID
import kotlin.collections.*
import okhttp3.ResponseBody

class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val socialApi: com.example.firstapp.media.data.api.SocialApi,
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val callLogDao: CallLogDao,
    private val displayNameResolver: com.example.firstapp.common.util.DisplayNameResolver,
    private val sessionManager: com.example.firstapp.common.domain.auth.SessionManager,
    private val chatWebSocketClient: com.example.firstapp.chat.data.ChatWebSocketClient,
    @ApplicationContext private val context: Context
) : ChatRepository {

    private val dedupManager = ChatWebSocketDedupManager()

    fun shouldProcessIncomingMessage(message: Message): Boolean = dedupManager.shouldProcessIncoming(message)

    fun clearDedupForConversation(conversationId: String) = dedupManager.clearConversationState(conversationId)

    override fun getChats(): Flow<Resource<List<ChatMessage>>> = 
        chatDao.getChats().flowMap { list -> Resource.Success(list.map { it.toModel() }) }

    override fun syncChats(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            Log.d("ChatRepositoryImpl", "syncChats: starting sync")
            val response = chatApi.getChats()
            if (response.isSuccessful && response.body() != null) {
                val apiChats = response.body()!!.chats
                Log.d("ChatRepositoryImpl", "syncChats: API returned ${apiChats.size} chats")
                
                // Optimized: get all local chats and contacts first to avoid N queries
                val localChats = chatDao.getChats().first().associateBy { it.id }
                val localContacts = contactDao.getContacts().first().associateBy { it.id }
                
                val finalEntities = apiChats.map { cs: ChatSummary -> 
                    val chatModel = mapApiChatSummary(cs)
                    val entity = chatModel.toEntity()
                    
                    val targetId = if (entity.type == com.example.firstapp.common.model.ConversationType.DIRECT) (entity.otherUserId ?: entity.id) else entity.id
                    val contact = localContacts[targetId]

                    // Improve: Resolve name from device contacts or profile
                    val resolvedName = if (entity.type == com.example.firstapp.common.model.ConversationType.DIRECT) {
                        displayNameResolver.resolve(
                            com.example.firstapp.common.model.User(
                                id = targetId, 
                                displayName = entity.senderName,
                                username = contact?.username ?: ""
                            ),
                            contact?.phoneNumber ?: entity.phoneNumber ?: ""
                        )
                    } else {
                        entity.senderName
                    }
                    
                    val existing = localChats[entity.id]
                    entity.copy(
                        senderName = resolvedName.ifBlank { entity.senderName },
                        avatarUrl = contact?.avatarUrl ?: entity.avatarUrl,
                        phoneNumber = contact?.phoneNumber ?: entity.phoneNumber,
                        wallpaperUrl = existing?.wallpaperUrl,
                        customNotificationTone = existing?.customNotificationTone,
                        mediaVisibility = existing?.mediaVisibility ?: true,
                        disappearingMessagesDuration = existing?.disappearingMessagesDuration ?: 0
                    )
                }
                
                chatDao.insertChats(finalEntities)
                Log.d("ChatRepositoryImpl", "syncChats: successfully inserted ${finalEntities.size} chats")
                emit(Resource.Success(true))
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ChatRepositoryImpl", "syncChats: API error: ${response.code()} - $errorBody")
                emit(Resource.Error("Sync failed: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Sync error: ${e.message}"))
        }
    }

    override suspend fun initiateDM(contactId: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.initiateDM(InitiateDMRequest(contactId))
            if (response.isSuccessful && response.body() != null) {
                val conversationId = response.body()!!.conversation_id
                
                // Improve: Save the chat locally immediately with proper contact info resolved
                val contact = contactDao.getContactById(contactId)
                val resolvedName = displayNameResolver.resolve(
                    com.example.firstapp.common.model.User(
                        id = contactId,
                        displayName = contact?.displayName ?: "",
                        username = contact?.username ?: ""
                    ),
                    contact?.phoneNumber ?: ""
                )

                val newChat = ChatEntity(
                    id = conversationId,
                    type = com.example.firstapp.common.model.ConversationType.DIRECT,
                    senderName = resolvedName,
                    lastMessage = "",
                    timestamp = System.currentTimeMillis(),
                    unreadCount = 0,
                    avatarUrl = contact?.avatarUrl,
                    isOnline = false,
                    isMuted = false,
                    isPinned = false,
                    isTyping = false,
                    lastMessageIsFromMe = false,
                    lastMessageRead = true,
                    phoneNumber = contact?.phoneNumber,
                    otherUserId = contactId
                )
                chatDao.insertChats(listOf(newChat))
                
                emit(Resource.Success(conversationId))
            } else {
                emit(Resource.Error("Failed to initiate DM"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun getMessages(chatId: String, page: Int): Flow<Resource<List<Message>>> = flow {
        // First, emit what we have locally
        val cachedEntities = chatDao.getMessages(chatId).first()
        val cached = cachedEntities.map { it.toModel() }
        
        if (cached.isNotEmpty()) {
            emit(Resource.Success(cached))
        } else {
            emit(Resource.Loading())
        }

        // Then, try to sync from network
        try {
            val oldestTs = if (cached.isNotEmpty()) cached.minOfOrNull { it.timestamp } else null
            val response = if (page > 1 && oldestTs != null) {
                chatApi.getMessages(chatId, page = 1, beforeTs = oldestTs, limit = 50)
            } else {
                chatApi.getMessages(chatId, page = page)
            }
            
            if (response.isSuccessful && response.body() != null) {
                val apiMessages = response.body()!!.messages
                val privKey = sessionManager.getPrivateKey()
                val myUserId = sessionManager.getUserId()
                val messages = apiMessages.map { am: com.example.firstapp.chat.data.api.ApiMessage -> mapApiMessage(am, privKey, myUserId) }
                // Insert new messages into the database
                chatDao.insertMessages(messages.map { m: Message -> m.toEntity(chatId) })
            }
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "getMessages network sync failed", e)
        }

        // Finally, emit the latest combined local state
        emitAll(chatDao.getMessages(chatId).flowMap { entities ->
            Resource.Success(entities.map { it.toModel() })
        })
    }

    override fun fetchOlderMessages(chatId: String, beforeTs: Long, limit: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.getMessages(
                conversationID = chatId,
                page = 1,
                beforeTs = beforeTs,
                limit = limit
            )
            if (response.isSuccessful && response.body() != null) {
                val apiMessages = response.body()!!.messages
                val myUserId = sessionManager.getUserId()
                if (apiMessages.isNotEmpty()) {
                    val mapped = apiMessages.map { am: ApiMessage -> mapApiMessage(am, currentUserId = myUserId) }
                    chatDao.insertMessages(mapped.map { m: Message -> m.toEntity(chatId) })
                }
                emit(Resource.Success(apiMessages.isNotEmpty()))
            } else {
                emit(Resource.Error("Failed to load older messages"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "fetchOlderMessages failed", e)
            emit(Resource.Error(e.message ?: "Failed to load older messages"))
        }
    }

    override fun sendMessage(
        chatId: String, body: String, messageType: String, mediaUrl: String?,
        duration: String?, fileName: String?, fileSize: String?,
        contactName: String?, contactPhone: String?, latitude: Double?, longitude: Double?,
        replyToId: String?
    ): Flow<Resource<Message>> = flow {
        val clientUuid = UUID.randomUUID().toString()
        dedupManager.trackOutgoing(clientUuid, chatId)

        val type = try {
            MessageType.valueOf(messageType.uppercase())
        } catch (e: Exception) {
            when (messageType.lowercase()) {
                "audio_message" -> MessageType.AUDIO_MESSAGE
                "application_link" -> MessageType.APPLICATION_LINK
                else -> MessageType.TEXT
            }
        }

        val myUserId = sessionManager.getUserId() ?: "0"
        val chatEntity = chatDao.searchChatsById(chatId)
        val receiverId = if (chatEntity?.type == com.example.firstapp.common.model.ConversationType.DIRECT) {
            chatEntity.otherUserId
        } else null

        val pendingMsg = Message(
            id = clientUuid,
            conversationId = chatId,
            content = body,
            senderId = myUserId,
            senderName = "Me",
            receiverId = receiverId,
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = MessageStatus.PENDING,
            clientId = clientUuid,
            type = type,
            mediaUri = formatUrl(mediaUrl),
            duration = duration,
            fileName = fileName,
            fileSize = fileSize,
            contactName = contactName,
            contactPhone = contactPhone,
            latitude = latitude,
            longitude = longitude,
            replyToId = replyToId
        )
        chatDao.insertMessages(listOf(pendingMsg.toEntity(chatId)))
        chatDao.updateLastMessage(chatId, body, pendingMsg.timestamp, true, false)
        emit(Resource.Success(pendingMsg))

        try {
            Log.d("ChatRepositoryImpl", "sendMessage: sending to API for chatId=$chatId")
            val response = chatApi.sendMessage(
                conversationID = chatId,
                request = SendMessageRequest(
                    client_uuid = clientUuid,
                    message_text = body,
                    media_type = messageType.uppercase(java.util.Locale.getDefault()),
                    media_url = mediaUrl,
                    duration = duration,
                    file_name = fileName,
                    file_size = fileSize,
                    receiver_id = receiverId
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Log.d("ChatRepositoryImpl", "sendMessage: API success for clientUuid=$clientUuid")
                val myUserId = sessionManager.getUserId()
                val serverMsg = mapApiMessage(response.body()!!.message, currentUserId = myUserId).copy(
                    duration = pendingMsg.duration,
                    fileName = pendingMsg.fileName,
                    fileSize = pendingMsg.fileSize,
                    latitude = pendingMsg.latitude,
                    longitude = pendingMsg.longitude,
                    contactName = pendingMsg.contactName,
                    contactPhone = pendingMsg.contactPhone
                )
                dedupManager.confirmOutgoing(clientUuid)
                chatDao.deleteMessage(clientUuid)
                chatDao.insertMessages(listOf(serverMsg.toEntity(chatId)))
                chatDao.updateLastMessage(chatId, serverMsg.content, serverMsg.timestamp, true, true)
                emit(Resource.Success(serverMsg))
            } else {
                val errorBodyStr = response.errorBody()?.string() ?: ""
                Log.e("ChatRepositoryImpl", "sendMessage: API error: ${response.code()} - $errorBodyStr")
                dedupManager.failOutgoing(clientUuid)
                val failedMsg = pendingMsg.copy(status = MessageStatus.FAILED)
                chatDao.insertMessages(listOf(failedMsg.toEntity(chatId)))
                
                var serverMessage = ""
                if (errorBodyStr.isNotEmpty() && errorBodyStr.contains("\"error\"")) {
                    try {
                        val key = "\"error\""
                        val idx = errorBodyStr.indexOf(key)
                        val start = errorBodyStr.indexOf(':', idx)
                        if (start >= 0) {
                            serverMessage = errorBodyStr.substring(start + 1).replace("{", "").replace("}", "").trim().trim('"')
                        }
                    } catch (_: Exception) { }
                }

                emit(Resource.Error(serverMessage.ifBlank { "Failed to send message" }))
            }
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "sendMessage: exception: ${e.message}", e)
            dedupManager.failOutgoing(clientUuid)
            val failedMsg = pendingMsg.copy(status = MessageStatus.FAILED)
            chatDao.insertMessages(listOf(failedMsg.toEntity(chatId)))
            Log.e("ChatRepositoryImpl", "Send message failed", e)
            emit(Resource.Error(e.message ?: "Send message failed"))
        }
    }

    private fun parseServerError(errorBody: ResponseBody?): String {
        return try {
            val raw = errorBody?.string()?.trim().orEmpty()
            if (raw.startsWith("{") && raw.contains("\"error\"")) {
                val key = "\"error\""
                val idx = raw.indexOf(key)
                if (idx >= 0) {
                    val start = raw.indexOf(':', idx)
                    if (start >= 0) {
                        raw.substring(start + 1).trim().trim('"', '{', '}', ' ')
                    } else raw
                } else raw
            } else raw
        } catch (_: Exception) {
            ""
        }
    }

    override fun markAsRead(chatId: String): Flow<Resource<Boolean>> = flow {
        chatDao.markIncomingAsRead(chatId)
        chatDao.updateUnreadCount(chatId, 0)
        try { chatApi.markAsRead(chatId) } catch (_: Exception) {}
        emit(Resource.Success(true))
    }

    override fun deleteMessage(messageId: String, deleteForEveryone: Boolean): Flow<Resource<Boolean>> = flow {
        chatDao.deleteMessage(messageId)
        emit(Resource.Success(true))
    }

    override fun forwardMessage(messageId: String, receiverIds: List<String>, groupIds: List<String>): Flow<Resource<Boolean>> = flow {
        emit(Resource.Success(true))
    }

    override fun archiveChat(chatId: String, isArchived: Boolean): Flow<Resource<Boolean>> = flow {
        chatDao.updateArchived(chatId, isArchived)
        emit(Resource.Success(true))
    }

    override fun getGroupInfo(groupId: String): Flow<Resource<GroupInfoResponse>> = flow {
        // Mocked
    }

    override fun addGroupMember(groupId: String, userId: String): Flow<Resource<Boolean>> = flow { emit(Resource.Success(true)) }
    override fun removeGroupMember(groupId: String, userId: String): Flow<Resource<Boolean>> = flow { emit(Resource.Success(true)) }
    override fun promoteToAdmin(groupId: String, userId: String): Flow<Resource<Boolean>> = flow { emit(Resource.Success(true)) }
    override fun leaveGroup(groupId: String): Flow<Resource<Boolean>> = flow { 
        chatDao.deleteChat(groupId)
        emit(Resource.Success(true)) 
    }

    override fun getSharedMedia(chatId: String): Flow<Resource<List<Message>>> = 
        chatDao.getMessages(chatId).flowMap { entities ->
            val media = entities.filter { me: MessageEntity -> me.type == MessageType.IMAGE || me.type == MessageType.VIDEO }.map { me: MessageEntity -> me.toModel() }
            Resource.Success(media)
        }

    override fun editMessage(messageId: String, newBody: String): Flow<Resource<Message>> = flow {
        // API then local
    }

    override fun reactToMessage(messageId: String, emoji: String): Flow<Resource<Boolean>> = flow {
        chatDao.updateMessageReactions(messageId, mapOf(emoji to 1))
        emit(Resource.Success(true))
    }

    override suspend fun saveMessage(chatId: String, message: Message) {
        chatDao.insertMessages(listOf(message.toEntity(chatId)))
        chatDao.pruneMessages(chatId, 50)
    }

    override suspend fun updateMessageInCache(chatId: String, message: Message) {
        chatDao.insertMessages(listOf(message.toEntity(chatId)))
        chatDao.pruneMessages(chatId, 50)
    }

    override suspend fun updateMessageReactions(messageId: String, reactions: Map<String, Int>) {
        chatDao.updateMessageReactions(messageId, reactions)
    }

    override suspend fun deleteMessageLocal(messageId: String) {
        chatDao.deleteMessage(messageId)
    }

    override suspend fun saveChat(chat: ChatMessage) {
        chatDao.insertChats(listOf(chat.toEntity()))
    }

    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        if (status == MessageStatus.DELIVERED) chatDao.markAsDelivered(messageId)
    }

    override suspend fun markChatAsReadLocal(chatId: String) {
        chatDao.markIncomingAsRead(chatId)
        chatDao.updateUnreadCount(chatId, 0)
    }

    override suspend fun updatePresence(chatId: String, isOnline: Boolean, lastSeen: Long) {
        chatDao.updatePresence(chatId, isOnline, lastSeen)
    }

    override suspend fun updateTyping(chatId: String, isTyping: Boolean) {
        chatDao.updateTyping(chatId, isTyping)
    }

    override suspend fun incrementUnreadCount(chatId: String) {
        chatDao.incrementUnreadCount(chatId)
    }

    override suspend fun updateLastMessage(chatId: String, lastMessage: String, time: Long, isFromMe: Boolean, isRead: Boolean) {
         val existing = chatDao.getChatById(chatId).first()
        if (existing == null) {
            // If the chat doesn't exist locally, try to create it from contact info
            val targetId = chatId // For DMs, chatId IS the otherUserId sometimes, or we need to look it up
            val contact = contactDao.getContactById(targetId)
            val name = displayNameResolver.resolve(
                com.example.firstapp.common.model.User(
                    id = targetId,
                    displayName = contact?.displayName ?: "",
                    username = contact?.username ?: ""
                ),
                contact?.phoneNumber ?: ""
            )

            val newChat = ChatEntity(
                id = chatId,
                type = com.example.firstapp.common.model.ConversationType.DIRECT,
                senderName = if (name.isNotBlank()) name else targetId,
                lastMessage = lastMessage,
                timestamp = time,
                unreadCount = if (!isFromMe && !isRead) 1 else 0,
                avatarUrl = contact?.avatarUrl,
                isOnline = false,
                isMuted = false,
                isPinned = false,
                isTyping = false,
                lastMessageIsFromMe = isFromMe,
                lastMessageRead = isRead,
                phoneNumber = contact?.phoneNumber,
                otherUserId = targetId
            )
            chatDao.insertChats(listOf(newChat))
        } else {
            // Resolve name if currently empty
            if (existing.senderName.isBlank()) {
                 val targetId = if (existing.type == com.example.firstapp.common.model.ConversationType.DIRECT) (existing.otherUserId ?: existing.id) else existing.id
                 val contact = contactDao.getContactById(targetId)
                 val name = displayNameResolver.resolve(
                    com.example.firstapp.common.model.User(
                        id = targetId,
                        displayName = contact?.displayName ?: "",
                        username = contact?.username ?: ""
                    ),
                    contact?.phoneNumber ?: existing.phoneNumber ?: ""
                )
                 if (name.isNotBlank() && name != existing.senderName) {
                     chatDao.insertChats(listOf(existing.copy(senderName = name, avatarUrl = contact?.avatarUrl ?: existing.avatarUrl)))
                 }
            }
            chatDao.updateLastMessage(chatId, lastMessage, time, isFromMe, isRead)
        }
    }

    override suspend fun createGroup(name: String, contactIds: List<String>): Flow<Resource<ChatMessage>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.createGroup(CreateGroupRequest(name, contactIds))
            if (response.isSuccessful && response.body() != null) {
                val conversationId = response.body()!!.conversation_id
                val newChat = ChatMessage(
                    id = conversationId,
                    type = com.example.firstapp.common.model.ConversationType.GROUP,
                    senderName = name,
                    lastMessage = "Group created",
                    timestamp = System.currentTimeMillis(),
                    unreadCount = 0,
                    isGroup = true,
                    isAdmin = true // Assuming creator is admin
                )
                chatDao.insertChats(listOf(newChat.toEntity()))
                emit(Resource.Success(newChat))
            } else {
                emit(Resource.Error("Failed to create group"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun pinChat(chatId: String, isPinned: Boolean): Flow<Resource<Boolean>> = flow {
        chatDao.updatePinned(chatId, isPinned)
        emit(Resource.Success(true))
    }

    override fun muteChat(chatId: String, isMuted: Boolean): Flow<Resource<Boolean>> = flow {
        chatDao.updateMuted(chatId, isMuted)
        emit(Resource.Success(true))
    }

    override fun deleteChat(chatId: String): Flow<Resource<Boolean>> = flow {
        chatDao.deleteChat(chatId)
        emit(Resource.Success(true))
    }

    override suspend fun clearMessages(chatId: String) { chatDao.clearMessages(chatId) }
    override suspend fun clearAllChats() { 
        chatDao.clearChats()
        chatDao.clearAllMessages()
    }
    override fun starMessage(messageId: String, isStarred: Boolean): Flow<Resource<Boolean>> = flow { emit(Resource.Success(true)) }
    override fun getStarredMessages(): Flow<Resource<List<Message>>> = flow { emit(Resource.Success(emptyList<Message>())) }
    override suspend fun updateWallpaper(chatId: String, wallpaperUrl: String?) { chatDao.updateWallpaper(chatId, wallpaperUrl) }
    override suspend fun updateCustomNotificationTone(chatId: String, tone: String?) { chatDao.updateCustomNotificationTone(chatId, tone) }
    override suspend fun updateMediaVisibility(chatId: String, visible: Boolean) { chatDao.updateMediaVisibility(chatId, visible) }
    override suspend fun updateDisappearingMessages(chatId: String, duration: Int) { chatDao.updateDisappearingMessages(chatId, duration) }
    override suspend fun syncPendingMessages() {}
    override fun pinMessage(chatId: String, messageId: String?): Flow<Resource<Boolean>> = flow { emit(Resource.Success(true)) }
    override fun getMessageReaders(messageId: String): Flow<Resource<List<ApiReader>>> = flow { emit(Resource.Success(emptyList<ApiReader>())) }
    override fun reportMessage(messageId: String, reason: String): Flow<Resource<Boolean>> = flow { emit(Resource.Success(true)) }
    override fun initiateCall(receiverId: String, callType: String): Flow<Resource<String>> = flow { emit(Resource.Success(UUID.randomUUID().toString())) }
    override fun searchLocalMessages(chatId: String, query: String): Flow<List<Message>> = 
        chatDao.getMessages(chatId).flowMap { list ->
            list.filter { it.content.contains(query, ignoreCase = true) }.map { it.toModel() }
        }
    override fun searchLocalChats(query: String): Flow<List<ChatMessage>> = 
        chatDao.getChats().flowMap { list ->
            list.filter { it.senderName.contains(query, ignoreCase = true) || it.lastMessage.contains(query, ignoreCase = true) }.map { it.toModel() }
        }
    override fun searchLocalContacts(query: String): Flow<List<Contact>> = 
        contactDao.searchContacts(query).flowMap { list ->
            list.map { it.toModel() }
        }
    override fun searchMessagesAcrossAll(query: String, filter: com.example.firstapp.search.model.SearchFilter): Flow<List<Message>> = 
        chatDao.searchMessagesAcrossAll(query).flowMap { list ->
            list.map { it.toModel() }
        }
    override fun getAllFiles(): Flow<Resource<List<Message>>> = flow { emit(Resource.Success(emptyList<Message>())) }
    
    override suspend fun refreshLocalChatNames() {
        val allChats = chatDao.getChats().first()
        val localContacts = contactDao.getContacts().first().associateBy { it.id }
        
        val updated = java.util.ArrayList<ChatEntity>()
        for (entity in allChats) {
            if (entity.type == com.example.firstapp.common.model.ConversationType.DIRECT) {
                val contact = localContacts[entity.id]
                val name = displayNameResolver.resolve(
                    com.example.firstapp.common.model.User(id = entity.id, displayName = ""),
                    contact?.phoneNumber ?: ""
                )
                if (name != entity.senderName && name != entity.id && name.isNotBlank()) {
                    updated.add(entity.copy(senderName = name, avatarUrl = contact?.avatarUrl ?: entity.avatarUrl))
                }
            }
        }
        
        if (updated.isNotEmpty()) {
            chatDao.insertChats(updated)
        }
    }
    
    override fun getContacts(): Flow<Resource<List<Contact>>> = 
        contactDao.getContacts().flowMap { list -> 
            Resource.Success(list.map { ce: com.example.firstapp.common.data.local.entity.ContactEntity -> ce.toModel() })
        }

    override fun syncContacts(phoneNumbers: List<String>): Flow<Resource<List<Contact>>> = flow {
        emit(Resource.Loading())
        try {
            // Get device contacts with names
            val deviceContacts = com.example.firstapp.common.util.ContactUtils.getPhoneContactsWithNames(context)
            Log.d("ChatRepositoryImpl", "syncContacts: Found ${deviceContacts.size} device contacts")
            val numbersToSync = deviceContacts.map { it.phoneNumber }

            val response = if (numbersToSync.isEmpty()) {
                socialApi.getSuggestedUsers()
            } else {
                com.example.firstapp.common.data.api.ApiClient.userApi.syncContacts(SyncContactsRequest(numbersToSync))
            }

            if (response.isSuccessful && response.body() != null) {
                val apiUsers = response.body()!!.contacts
                Log.d("ChatRepositoryImpl", "syncContacts: API returned ${apiUsers.size} registered contacts")
                
                // Helper to get normalized suffix
                fun normalize(phone: String?, length: Int): String? {
                    if (phone == null) return null
                    val digits = phone.filter { it.isDigit() }
                    return if (digits.length >= length) digits.takeLast(length) else digits
                }

                // Build maps for different suffix lengths for maximum matching robustness
                val regMap9 = apiUsers.associateBy { normalize(it.phone_number, 9) }
                val regMap8 = apiUsers.associateBy { normalize(it.phone_number, 8) }
                val regMap7 = apiUsers.associateBy { normalize(it.phone_number, 7) }
                
                val allContacts = deviceContacts.map { deviceContact: com.example.firstapp.common.util.ContactUtils.ContactInfo ->
                    val clean9 = normalize(deviceContact.phoneNumber, 9)
                    val clean8 = normalize(deviceContact.phoneNumber, 8)
                    val clean7 = normalize(deviceContact.phoneNumber, 7)
                    
                    val registeredUser = regMap9[clean9] ?: regMap8[clean8] ?: regMap7[clean7]
                    
                    if (registeredUser != null) {
                        Log.d("ChatRepositoryImpl", "syncContacts: Matched registered user ${registeredUser.username} for ${deviceContact.phoneNumber}")
                        val userModel = com.example.firstapp.common.model.User(
                            id = registeredUser.id,
                            username = registeredUser.username,
                            displayName = registeredUser.display_name,
                            phoneNumber = registeredUser.phone_number ?: deviceContact.phoneNumber
                        )
                        val resolvedName = displayNameResolver.resolve(userModel, userModel.phoneNumber ?: "")
                        
                        Contact(
                            id = registeredUser.id,
                            username = registeredUser.username,
                            displayName = resolvedName,
                            avatarUrl = registeredUser.avatar_url,
                            phoneNumber = registeredUser.phone_number ?: deviceContact.phoneNumber,
                            isRegistered = true,
                            isAuthenticated = true,
                            isVerified = registeredUser.is_verified,
                            bio = registeredUser.bio,
                            publicKey = registeredUser.public_key
                        )
                    } else {
                        Contact(
                            id = "unregistered_${deviceContact.phoneNumber}",
                            username = "",
                            displayName = deviceContact.displayName,
                            avatarUrl = null,
                            phoneNumber = deviceContact.phoneNumber,
                            isRegistered = false,
                            isAuthenticated = false,
                            isVerified = false,
                            bio = null,
                            publicKey = null
                        )
                    }
                }
                
                if (allContacts.isNotEmpty()) {
                    contactDao.clearContacts()
                    contactDao.insertContacts(allContacts.map { c: Contact -> c.toEntity() })
                }
                emit(Resource.Success(allContacts))
            } else {
                emit(Resource.Error("Failed to sync contacts"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "Sync failed", e)
            emit(Resource.Error("Sync error: ${e.message}"))
        }
    }
    
    override fun getArchivedChats(): Flow<Resource<List<ChatMessage>>> = 
        chatDao.getArchivedChats().flowMap { list -> 
            Resource.Success(list.map { ce: ChatEntity -> ce.toModel() })
        }

    override fun getCallLogs(): Flow<Resource<List<CallLog>>> = 
        callLogDao.getCallLogs().flowMap { list -> 
            Resource.Success(list.map { cle: com.example.firstapp.common.data.local.entity.CallLogEntity -> cle.toModel() })
        }
}
