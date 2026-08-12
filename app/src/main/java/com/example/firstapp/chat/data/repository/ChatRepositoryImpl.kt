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
import kotlin.text.*
import okhttp3.ResponseBody

class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val socialApi: com.example.firstapp.media.data.api.SocialApi,
    private val userApi: UserApi,
    private val chatDao: ChatDao,
    private val contactDao: ContactDao,
    private val callLogDao: CallLogDao,
    private val displayNameResolver: com.example.firstapp.common.util.DisplayNameResolver,
    private val sessionManager: com.example.firstapp.common.domain.auth.SessionManager,
    private val chatWebSocketClient: com.example.firstapp.chat.data.ChatWebSocketClient,
    @ApplicationContext private val context: Context
) : ChatRepository {

    private val dedupManager = ChatWebSocketDedupManager()
    private val groupMembersCache = mutableMapOf<String, List<String>>()

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
                
                val localChats = chatDao.getChats().first().associateBy { it.id }
                val localContacts = contactDao.getContacts().first().associateBy { it.id }
                
                val contactsToInsert = java.util.ArrayList<com.example.firstapp.common.data.local.entity.ContactEntity>()

                val finalEntities = apiChats.map { cs: ChatSummary -> 
                    val chatModel = mapApiChatSummary(cs)
                    val entity = chatModel.toEntity()
                    
                    val targetId = if (entity.type == com.example.firstapp.common.model.ConversationType.DIRECT) (entity.otherUserId ?: entity.id) else entity.id
                    val contact = localContacts[targetId] ?: localContacts[targetId.removePrefix("user_")]

                    // AUTO-RESTORE CONTACTS FROM CHATS:
                    // If you have a chat with someone, they MUST be in your contacts list.
                    // This fixes the issue where contacts are missing on a new device.
                    if (entity.type == com.example.firstapp.common.model.ConversationType.DIRECT || !entity.otherUserId.isNullOrBlank()) {
                        val cid = entity.otherUserId ?: targetId
                        if (contact == null && contactsToInsert.none { it.id == cid }) {
                            contactsToInsert.add(com.example.firstapp.common.data.local.entity.ContactEntity(
                                id = cid,
                                username = "", 
                                displayName = cs.title.ifBlank { cid },
                                avatarUrl = formatUrl(cs.avatar_url),
                                phoneNumber = entity.phoneNumber ?: "",
                                isRegistered = true,
                                isAuthenticated = true,
                                isVerified = false
                            ))
                        }
                    }

                    val resolvedName = if (entity.type == com.example.firstapp.common.model.ConversationType.DIRECT) {
                        val name = displayNameResolver.resolve(
                            com.example.firstapp.common.model.User(
                                id = targetId, 
                                displayName = cs.title, 
                                username = ""
                            ),
                            contact?.phoneNumber ?: entity.phoneNumber ?: ""
                        )
                        
                        if (name.isBlank() || name == targetId) {
                             contact?.phoneNumber?.takeIf { it.isNotBlank() } ?: entity.phoneNumber ?: cs.title.ifBlank { targetId }
                        } else name
                    } else {
                        entity.senderName.ifBlank { cs.title.ifBlank { entity.id } }
                    }

                    val existing = localChats[entity.id]
                    entity.copy(
                        senderName = resolvedName,
                        avatarUrl = contact?.avatarUrl ?: entity.avatarUrl,
                        phoneNumber = contact?.phoneNumber ?: entity.phoneNumber,
                        wallpaperUrl = existing?.wallpaperUrl,
                        customNotificationTone = existing?.customNotificationTone,
                        mediaVisibility = existing?.mediaVisibility ?: true,
                        disappearingMessagesDuration = existing?.disappearingMessagesDuration ?: 0
                    )
                }
                
                val apiChatIds = finalEntities.map { it.id }.toSet()
                val localIds = chatDao.getAllChatIds()
                for (id in localIds) {
                    if (!apiChatIds.contains(id)) {
                        chatDao.deleteChat(id)
                        chatDao.clearMessages(id)
                    }
                }
                
                if (contactsToInsert.isNotEmpty()) {
                    Log.d("ChatRepositoryImpl", "syncChats: Inserting ${contactsToInsert.size} recovered contacts")
                    contactDao.insertContacts(contactsToInsert)
                }
                
                chatDao.insertChats(finalEntities)
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Sync failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Sync error: ${e.message}"))
        }
    }

    override suspend fun initiateDM(contactId: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            // First check if a DM already exists with this user to avoid double chats
            val localChats = chatDao.getChats().first()
            var existing: ChatEntity? = null
            for (chat in localChats) {
                if (chat.type == com.example.firstapp.common.model.ConversationType.DIRECT && 
                    (chat.otherUserId == contactId || chat.id == contactId)) {
                    existing = chat
                    break
                }
            }
            if (existing != null) {
                emit(Resource.Success(existing.id))
                return@flow
            }
            if (existing != null) {
                emit(Resource.Success(existing.id))
                return@flow
            }

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
                    senderName = resolvedName.ifBlank { contact?.displayName ?: contactId },
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
                
                // CRITICAL: Update GlobalCallState so name appears in Calling UI immediately
                GlobalCallState.updateContactInfo(conversationId, resolvedName, contact?.phoneNumber)
                GlobalCallState.updateContactInfo(contactId, resolvedName, contact?.phoneNumber)
                
                emit(Resource.Success(conversationId))
            } else {
                emit(Resource.Error("Failed to initiate DM"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun getMessages(chatId: String, page: Int): Flow<Resource<List<Message>>> = flow {
        // First, emit what we have locally (ALWAYS emit first to avoid hangs in .first() or collect)
        val cachedEntities = chatDao.getMessages(chatId).first()
        val cached = cachedEntities.map { it.toModel() }
        emit(Resource.Success(cached))

        // Then, try to sync from network
        try {
            // If page is 1, we want the most recent messages.
            // If we have local messages, we might want to fetch anything newer than our newest,
            // or just refresh the top.
            val response = chatApi.getMessages(chatId, page = page, limit = 50)
            
            if (response.isSuccessful && response.body() != null) {
                val apiMessages = response.body()!!.messages
                val privKey = sessionManager.getPrivateKey()
                val myUserId = sessionManager.getUserId()
                val messages = apiMessages.map { am: com.example.firstapp.chat.data.api.ApiMessage -> mapApiMessage(am, privKey, myUserId) }
                
                // Cleanup logic: If we have local messages that match the client_uuid of incoming server messages,
                // we must delete the local (pending) one to avoid duplicates in the UI.
                messages.forEach { msg ->
                    if (msg.clientId != null && msg.id != msg.clientId) {
                        chatDao.deleteMessage(msg.clientId!!)
                    }
                }

                // Insert new messages into the database
                chatDao.insertMessages(messages.map { m: Message -> m.toEntity(chatId) })
                
                // If it's a new conversation, we might need to update the last message info
                if (messages.isNotEmpty()) {
                    val last = messages.first() // API usually returns descending or ascending? Let's assume most recent is here or we find it
                    val mostRecent = messages.maxByOrNull { it.timestamp }
                    mostRecent?.let {
                        chatDao.updateLastMessage(chatId, it.content, it.timestamp, it.isFromMe, it.isRead)
                    }
                }
            } else if (response.code() == 404) {
                // Conversation might have been deleted on server
                // chatDao.deleteChat(chatId)
            }
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "getMessages network sync failed for $chatId", e)
            // If we have NO cached messages and network fails, emit error
            if (cached.isEmpty()) {
                emit(Resource.Error("Could not load messages: ${e.localizedMessage}"))
            }
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
        chatId: String, body: String, messageType: String, mediaUrls: List<String>,
        duration: String?, fileName: String?, fileSize: String?,
        contactName: String?, contactPhone: String?, latitude: Double?, longitude: Double?,
        replyToId: String?, replyToText: String?, replyToSenderName: String?
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
            status = MessageStatus.SENDING,
            clientId = clientUuid,
            type = type,
            mediaUrls = mediaUrls.map { formatUrl(it) },
            duration = duration,
            fileName = fileName,
            fileSize = fileSize,
            contactName = contactName,
            contactPhone = contactPhone,
            latitude = latitude,
            longitude = longitude,
            replyToId = replyToId,
            replyToText = replyToText,
            replyToSenderName = replyToSenderName
        )
        
        Log.d("ChatRepositoryImpl", "sendMessage: Optimistic insert clientUuid=$clientUuid")
        chatDao.insertMessages(listOf(pendingMsg.toEntity(chatId)))
        chatDao.updateLastMessage(chatId, body, pendingMsg.timestamp, true, false)
        emit(Resource.Success(pendingMsg))

        try {
            Log.d("ChatRepositoryImpl", "sendMessage: calling API for clientUuid=$clientUuid")
            val response = chatApi.sendMessage(
                conversationID = chatId,
                request = SendMessageRequest(
                    client_uuid = clientUuid,
                    message_text = body,
                    media_type = messageType.uppercase(java.util.Locale.getDefault()),
                    media_urls = mediaUrls,
                    duration = duration,
                    file_name = fileName,
                    file_size = fileSize,
                    receiver_id = receiverId,
                    reply_to_id = replyToId
                )
            )
            if (response.isSuccessful && response.body() != null) {
                Log.d("ChatRepositoryImpl", "sendMessage: API success for clientUuid=$clientUuid, serverId=${response.body()!!.message.id}")
                val myUserId = sessionManager.getUserId()
                val mappedServerMsg = mapApiMessage(response.body()!!.message, currentUserId = myUserId)
                val serverMsg = mappedServerMsg.copy(
                    duration = pendingMsg.duration,
                    fileName = pendingMsg.fileName,
                    fileSize = pendingMsg.fileSize,
                    latitude = pendingMsg.latitude,
                    longitude = pendingMsg.longitude,
                    contactName = pendingMsg.contactName,
                    contactPhone = pendingMsg.contactPhone,
                    replyToId = pendingMsg.replyToId,
                    replyToText = pendingMsg.replyToText,
                    replyToSenderName = pendingMsg.replyToSenderName,
                    timestamp = pendingMsg.timestamp,
                    mediaUrls = if (mappedServerMsg.mediaUrls.isEmpty()) pendingMsg.mediaUrls else mappedServerMsg.mediaUrls
                )
                dedupManager.confirmOutgoing(clientUuid)
                chatDao.confirmSentMessage(clientUuid, serverMsg.toEntity(chatId))
                chatDao.updateLastMessage(chatId, serverMsg.content, serverMsg.timestamp, true, true)
                emit(Resource.Success(serverMsg))
            } else {
                val errorBodyStr = response.errorBody()?.string() ?: ""
                Log.e("ChatRepositoryImpl", "sendMessage: API error: ${response.code()} - $errorBodyStr")
                dedupManager.failOutgoing(clientUuid)
                val failedMsg = pendingMsg.copy(status = MessageStatus.FAILED)
                chatDao.insertMessages(listOf(failedMsg.toEntity(chatId)))
                emit(Resource.Error(parseServerError(response.errorBody())))
            }
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "sendMessage: exception: ${e.message}", e)
            dedupManager.failOutgoing(clientUuid)
            val failedMsg = pendingMsg.copy(status = MessageStatus.FAILED)
            chatDao.insertMessages(listOf(failedMsg.toEntity(chatId)))
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
        try {
            chatDao.deleteMessage(messageId)
            val response = chatApi.deleteMessage(messageId)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to delete from server"))
            }
        } catch (e: Exception) {
            // Even if offline, we delete locally for UI responsive feel
            emit(Resource.Success(true))
        }
    }

    override fun forwardMessage(messageId: String, receiverIds: List<String>, groupIds: List<String>): Flow<Resource<Boolean>> = flow {
        emit(Resource.Success(true))
    }

    override fun archiveChat(chatId: String, isArchived: Boolean): Flow<Resource<Boolean>> = flow {
        chatDao.updateArchived(chatId, isArchived)
        emit(Resource.Success(true))
    }

    override fun getGroupInfo(groupId: String): Flow<Resource<GroupInfoResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.getGroupInfo(groupId)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else if (response.code() == 404 || response.code() == 405) {
                // Backend might not have this endpoint or group info 
                // Return a mock success with basic info or cached members to avoid red error text
                Log.d("ChatRepositoryImpl", "getGroupInfo: returning fallback/cached for $groupId")
                val cachedMembers = groupMembersCache[groupId] ?: emptyList()
                emit(Resource.Success(GroupInfoResponse(
                    id = groupId,
                    title = "Group",
                    member_ids = cachedMembers,
                    admin_ids = emptyList(),
                    created_at = System.currentTimeMillis()
                )))
            } else {
                emit(Resource.Error("Failed to fetch group info: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "getGroupInfo exception", e)
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun addGroupMember(groupId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.addGroupMember(groupId, AddMemberRequest(userId))
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to add member"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun removeGroupMember(groupId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.removeGroupMember(groupId, userId)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to remove member"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun promoteToAdmin(groupId: String, userId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.promoteToAdmin(groupId, userId)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to promote to admin"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun leaveGroup(groupId: String): Flow<Resource<Boolean>> = flow { 
        emit(Resource.Loading())
        try {
            val response = chatApi.leaveGroup(groupId)
            if (response.isSuccessful) {
                chatDao.deleteChat(groupId)
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to leave group"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
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
        // ID Consistency Check: Ensure we use the same chatId throughout the message lifecycle
        val finalChatId = message.conversationId.ifBlank { chatId }
        chatDao.insertMessages(listOf(message.toEntity(finalChatId)))
        chatDao.pruneMessages(finalChatId, 50)
    }

    override suspend fun updateMessageInCache(chatId: String, message: Message) {
        val finalChatId = message.conversationId.ifBlank { chatId }
        chatDao.insertMessages(listOf(message.toEntity(finalChatId)))
        chatDao.pruneMessages(finalChatId, 50)
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
        when (status) {
            MessageStatus.DELIVERED -> chatDao.markAsDelivered(messageId)
            MessageStatus.READ -> chatDao.markAsRead(messageId)
            else -> {}
        }
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
            // If the chat doesn't exist locally, we can't reliably create it without more info (like who the other user is)
            // So we trigger a full sync instead to stay consistent with server
            syncChats().collect {}
        } else {
            // Resolve name if currently empty or generic
            if (existing.senderName.isBlank() || existing.senderName == existing.id) {
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
                 if (name.isNotBlank() && name != existing.senderName && name != targetId) {
                     chatDao.insertChats(listOf(existing.copy(senderName = name, avatarUrl = contact?.avatarUrl ?: existing.avatarUrl)))
                     // Also update global call cache for consistency
                     GlobalCallState.updateContactInfo(existing.id, name, existing.phoneNumber)
                     existing.otherUserId?.let { GlobalCallState.updateContactInfo(it, name, existing.phoneNumber) }
                 }
            }
            chatDao.updateLastMessage(chatId, lastMessage, time, isFromMe, isRead)
        }
    }

    override suspend fun createGroup(name: String, contactIds: List<String>): Flow<Resource<ChatMessage>> = flow {
        emit(Resource.Loading())
        try {
            val myUserId = sessionManager.getUserId() ?: ""
            val allMemberIds = (contactIds + myUserId).filter { it.isNotBlank() }.distinct()

            Log.d("ChatRepositoryImpl", "Creating group '$name' with ${allMemberIds.size} members")
            val response = chatApi.createGroup(CreateGroupRequest(name, allMemberIds))
            
            if (response.isSuccessful && response.body() != null) {
                val conversationId = response.body()!!.conversation_id
                Log.d("ChatRepositoryImpl", "Group created successfully. ID: $conversationId")
                
                // Cache members locally in case getGroupInfo fails
                groupMembersCache[conversationId] = allMemberIds

                // Trigger a full sync to get the proper group metadata from server
                syncChats().collect {}
                
                // Try to find the newly created group in the synced list
                val syncedChat = chatDao.getChatById(conversationId).first()?.toModel()
                
                val resultChat = syncedChat ?: ChatMessage(
                    id = conversationId,
                    type = com.example.firstapp.common.model.ConversationType.GROUP,
                    senderName = name,
                    lastMessage = "Group created",
                    timestamp = System.currentTimeMillis(),
                    unreadCount = 0,
                    isGroup = true,
                    isAdmin = true,
                    createdBy = myUserId
                )
                
                if (syncedChat == null) {
                    chatDao.insertChats(listOf(resultChat.toEntity()))
                }
                
                emit(Resource.Success(resultChat))
            } else {
                val errorMsg = parseServerError(response.errorBody())
                Log.e("ChatRepositoryImpl", "Group creation failed: $errorMsg")
                emit(Resource.Error(errorMsg.ifBlank { "Failed to create group" }))
            }
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "Group creation exception", e)
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
                if (name != entity.senderName && name != entity.id && name.isNotEmpty()) {
                    updated.add(entity.copy(senderName = name, avatarUrl = contact?.avatarUrl ?: entity.avatarUrl))
                }
            }
        }
        
        if (updated.isNotEmpty()) {
            chatDao.insertChats(updated)
        }
    }

    override suspend fun getICEServers(): Resource<List<com.example.firstapp.chat.data.api.IceServerConfig>> {
        return try {
            val response = chatApi.getICEServers()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.ice_servers)
            } else {
                Resource.Error("Failed to fetch ICE servers")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Network error")
        }
    }
    
    override fun getContacts(): Flow<Resource<List<Contact>>> = 
        contactDao.getContacts().flowMap { list -> 
            Resource.Success(list.map { ce: com.example.firstapp.common.data.local.entity.ContactEntity -> ce.toModel() })
        }

    override fun syncContacts(phoneNumbers: List<String>): Flow<Resource<List<Contact>>> = flow {
        emit(Resource.Loading())
        try {
            // 1. Get current device contacts
            val deviceContacts = com.example.firstapp.common.util.ContactUtils.getPhoneContactsWithNames(context)
            Log.d("ChatRepositoryImpl", "syncContacts: Found ${deviceContacts.size} device contacts")
            val numbersToSync = deviceContacts.map { it.phoneNumber }

            // 2. Prepare master list of contacts from multiple server sources
            val apiUsersMaster = java.util.ArrayList<ApiUser>()

            // 2a. Sync current device numbers and get persistent account contacts
            try {
                // First, pull saved names/numbers from account
                val savedRes = userApi.getSavedContacts()
                if (savedRes.isSuccessful && savedRes.body() != null) {
                    val saved = savedRes.body()!!
                    // Add these numbers to the sync list to get their profile info
                    val allNumbers = (numbersToSync + saved.map { it.phone_number }).distinct()
                    
                    val response = userApi.syncContacts(SyncContactsRequest(allNumbers))
                    if (response.isSuccessful && response.body() != null) {
                        apiUsersMaster.addAll(response.body()!!.contacts)
                    }
                } else {
                    // Fallback to just device numbers if saved fetch fails
                    val response = userApi.syncContacts(SyncContactsRequest(numbersToSync))
                    if (response.isSuccessful && response.body() != null) {
                        apiUsersMaster.addAll(response.body()!!.contacts)
                    }
                }
            } catch (e: Exception) { Log.e("ChatRepositoryImpl", "Sync API failed", e) }

            // 2b. Supplement with people from existing chats (Direct Messages)
            // This is the most reliable way to recover contacts on a new device
            val myId = sessionManager.getUserId()
            if (myId != null) {
                try {
                    val chatsRes = chatApi.getChats()
                    if (chatsRes.isSuccessful && chatsRes.body() != null) {
                        val apiChats = chatsRes.body()!!.chats
                        val chatUserIds = apiChats
                            .filter { it.type == "DIRECT" || it.type == "DIRECT_MESSAGE" }
                            .mapNotNull { it.contact_id }
                            .filter { cid -> cid.isNotBlank() && apiUsersMaster.none { it.id == cid } }
                        
                        chatUserIds.forEach { userId ->
                            try {
                                val userRes = userApi.getUserById(userId)
                                if (userRes.isSuccessful && userRes.body() != null) {
                                    val u = userRes.body()!!.user
                                    apiUsersMaster.add(ApiUser(
                                        id = u.id,
                                        username = u.username,
                                        display_name = u.displayName,
                                        email = u.email ?: "",
                                        phone_number = u.phoneNumber ?: "",
                                        avatar_url = u.avatarUrl,
                                        is_verified = u.isVerified,
                                        is_following = u.isFollowing,
                                        bio = u.bio
                                    ))
                                } else {
                                    // Fallback if full profile fetch fails
                                    apiUsersMaster.add(ApiUser(
                                        id = userId,
                                        username = "",
                                        display_name = "User $userId",
                                        email = "",
                                        phone_number = "",
                                        avatar_url = null
                                    ))
                                }
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) { Log.e("ChatRepositoryImpl", "Chat recovery failed", e) }
            }

            Log.d("ChatRepositoryImpl", "syncContacts: Total ${apiUsersMaster.size} registered contacts collected")

            // 3. Construct local Contact list with name resolution
            fun normalize(phone: String?, length: Int): String? {
                if (phone == null) return null
                val digits = phone.filter { it.isDigit() }
                return if (digits.length >= length) digits.takeLast(length) else digits
            }

            val localMap9 = deviceContacts.associateBy { normalize(it.phoneNumber, 9) }
            val localMap8 = deviceContacts.associateBy { normalize(it.phoneNumber, 8) }
            val localMap7 = deviceContacts.associateBy { normalize(it.phoneNumber, 7) }
            
            val allContacts = apiUsersMaster.map { apiUser ->
                val clean9 = normalize(apiUser.phone_number, 9)
                val clean8 = normalize(apiUser.phone_number, 8)
                val clean7 = normalize(apiUser.phone_number, 7)
                
                // Priority: Local address book name > Server profile name
                val localMatch = localMap9[clean9] ?: localMap8[clean8] ?: localMap7[clean7]
                
                val userModel = com.example.firstapp.common.model.User(
                    id = apiUser.id,
                    username = apiUser.username,
                    displayName = apiUser.display_name,
                    phoneNumber = apiUser.phone_number ?: ""
                )
                
                val resolvedName = if (localMatch != null) {
                    localMatch.displayName
                } else {
                    displayNameResolver.resolve(userModel, apiUser.phone_number ?: "")
                }
                
                Contact(
                    id = apiUser.id,
                    username = apiUser.username,
                    displayName = resolvedName,
                    avatarUrl = apiUser.avatar_url,
                    phoneNumber = apiUser.phone_number ?: "",
                    isRegistered = true,
                    isAuthenticated = true,
                    isVerified = apiUser.is_verified,
                    bio = apiUser.bio,
                    publicKey = apiUser.public_key,
                    isFollowing = apiUser.is_following
                )
            }
            
            // Add device contacts that are NOT registered yet (for Invite functionality)
            val registeredNumbers = apiUsersMaster.mapNotNull { normalize(it.phone_number, 9) }.toSet()
            val unregisteredContacts = deviceContacts.filter { 
                val norm = normalize(it.phoneNumber, 9)
                norm != null && !registeredNumbers.contains(norm)
            }.map { dc ->
                Contact(
                    id = "unregistered_${dc.phoneNumber}",
                    username = "",
                    displayName = dc.displayName,
                    avatarUrl = null,
                    phoneNumber = dc.phoneNumber,
                    isRegistered = false,
                    isAuthenticated = false,
                    isVerified = false,
                    bio = null,
                    publicKey = null
                )
            }

            val finalContactList = allContacts + unregisteredContacts
            
            if (finalContactList.isNotEmpty()) {
                contactDao.clearContacts()
                contactDao.insertContacts(finalContactList.map { c: Contact -> c.toEntity() })
            }
            emit(Resource.Success(finalContactList))
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
