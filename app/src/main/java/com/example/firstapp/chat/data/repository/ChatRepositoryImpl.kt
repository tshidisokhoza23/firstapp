package com.example.firstapp.chat.data.repository

import android.content.Context
import android.util.Log
import com.example.firstapp.chat.data.ChatWebSocketClient
import com.example.firstapp.chat.data.ChatWebSocketDedupManager
import com.example.firstapp.chat.data.api.*
import com.example.firstapp.chat.model.*
import com.example.firstapp.common.data.api.*
import com.example.firstapp.common.data.local.dao.*
import com.example.firstapp.common.data.local.entity.MessageEntity
import com.example.firstapp.common.data.local.entity.ChatEntity
import com.example.firstapp.common.data.repository.*
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
            val response = chatApi.getChats()
            if (response.isSuccessful && response.body() != null) {
                val apiChats = response.body()!!.chats
                val chats = apiChats.map { cs: ChatSummary -> mapApiChatSummary(cs) }
                
                val newEntities = chats.map { c: ChatMessage -> c.toEntity() }
                
                // Optimized: get all local chats first to avoid N queries
                val localChats = chatDao.getChats().first().associateBy { it.id }
                
                val finalEntities = newEntities.map { entity ->
                    localChats[entity.id]?.let { local ->
                        entity.copy(
                            wallpaperUrl = local.wallpaperUrl,
                            customNotificationTone = local.customNotificationTone,
                            mediaVisibility = local.mediaVisibility,
                            disappearingMessagesDuration = local.disappearingMessagesDuration
                        )
                    } ?: entity
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
            val response = chatApi.initiateDM(InitiateDMRequest(contactId))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.conversation_id))
            } else {
                emit(Resource.Error("Failed to initiate DM"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Error"))
        }
    }

    override fun getMessages(chatId: String, page: Int): Flow<Resource<List<Message>>> = flow {
        val cachedEntities = chatDao.getMessages(chatId).first()
        val cached = cachedEntities.map { it.toModel() }
        if (cached.isNotEmpty()) {
            emit(Resource.Success(cached))
        } else {
            emit(Resource.Loading())
        }

        try {
            val oldestTs = cached.minOfOrNull { it.timestamp }
            val response = if (page > 1 && oldestTs != null) {
                chatApi.getMessages(chatId, page = 1, beforeTs = oldestTs, limit = 50)
            } else {
                chatApi.getMessages(chatId, page = page)
            }
            if (response.isSuccessful && response.body() != null) {
                val apiMessages = response.body()!!.messages
                val messages = apiMessages.map { am: ApiMessage -> mapApiMessage(am) }
                if (page == 1) chatDao.clearMessages(chatId)
                chatDao.insertMessages(messages.map { m: Message -> m.toEntity(chatId) })
            }
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "getMessages failed", e)
        }

        emitAll(chatDao.getMessages(chatId).flowMap { entities ->
            val models = entities.map { it.toModel() }
            Resource.Success<List<Message>>(models)
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
                if (apiMessages.isNotEmpty()) {
                    val mapped = apiMessages.map { am: ApiMessage -> mapApiMessage(am) }
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
        val pendingMsg = Message(
            id = clientUuid,
            conversationId = chatId,
            content = body,
            senderId = "0",
            senderName = "Me",
            timestamp = System.currentTimeMillis(),
            isFromMe = true,
            status = MessageStatus.PENDING,
            clientId = clientUuid
        )
        chatDao.insertMessages(listOf(pendingMsg.toEntity(chatId)))
        emit(Resource.Success(pendingMsg))

        try {
            val response = chatApi.sendMessage(
                conversationID = chatId,
                request = SendMessageRequest(
                    client_uuid = clientUuid,
                    message_text = body,
                    media_type = messageType.uppercase(java.util.Locale.getDefault()),
                    media_url = mediaUrl
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val serverMsg = mapApiMessage(response.body()!!.message)
                dedupManager.confirmOutgoing(clientUuid)
                chatDao.deleteMessage(clientUuid)
                chatDao.insertMessages(listOf(serverMsg.toEntity(chatId)))
                emit(Resource.Success(serverMsg))
            } else {
                dedupManager.failOutgoing(clientUuid)
                chatDao.deleteMessage(clientUuid)
                val serverMessage = parseServerError(response.errorBody())
                emit(Resource.Error(serverMessage.ifBlank { "Failed to send message" }))
            }
        } catch (e: Exception) {
            dedupManager.failOutgoing(clientUuid)
            chatDao.deleteMessage(clientUuid)
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
    }

    override suspend fun updateMessageInCache(chatId: String, message: Message) {
        chatDao.insertMessages(listOf(message.toEntity(chatId)))
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
        chatDao.updateLastMessage(chatId, lastMessage, time, isFromMe, isRead)
    }

    override suspend fun createGroup(name: String, contactIds: List<String>): Flow<Resource<ChatMessage>> = flow {
        emit(Resource.Loading())
        val response = chatApi.createGroup(CreateGroupRequest(name, contactIds))
        if (response.isSuccessful && response.body() != null) {
            // Mapping needed
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
    override suspend fun clearAllChats() { chatDao.clearChats() }
    override fun starMessage(messageId: String, isStarred: Boolean): Flow<Resource<Boolean>> = flow { emit(Resource.Success(true)) }
    override fun getStarredMessages(): Flow<Resource<List<Message>>> = flow { emit(Resource.Success(emptyList<Message>())) }
    override suspend fun updateWallpaper(chatId: String, wallpaperUrl: String?) { chatDao.updateWallpaper(chatId, wallpaperUrl) }
    override suspend fun updateCustomNotificationTone(chatId: String, tone: String?) {}
    override suspend fun updateMediaVisibility(chatId: String, visible: Boolean) {}
    override suspend fun updateDisappearingMessages(chatId: String, duration: Int) {}
    override suspend fun syncPendingMessages() {}
    override fun pinMessage(chatId: String, messageId: String?): Flow<Resource<Boolean>> = flow { emit(Resource.Success(true)) }
    override fun getMessageReaders(messageId: String): Flow<Resource<List<ApiReader>>> = flow { emit(Resource.Success(emptyList<ApiReader>())) }
    override fun reportMessage(messageId: String, reason: String): Flow<Resource<Boolean>> = flow { emit(Resource.Success(true)) }
    override fun initiateCall(receiverId: String, callType: String): Flow<Resource<String>> = flow { emit(Resource.Success(UUID.randomUUID().toString())) }
    override fun searchLocalMessages(chatId: String, query: String): Flow<List<Message>> = flow { emit(emptyList()) }
    override fun searchLocalChats(query: String): Flow<List<ChatMessage>> = flow { emit(emptyList()) }
    override fun searchLocalContacts(query: String): Flow<List<Contact>> = flow { emit(emptyList()) }
    override fun searchMessagesAcrossAll(query: String, filter: com.example.firstapp.search.model.SearchFilter): Flow<List<Message>> = flow { emit(emptyList()) }
    override fun getAllFiles(): Flow<Resource<List<Message>>> = flow { emit(Resource.Success(emptyList<Message>())) }
    
    override fun getContacts(): Flow<Resource<List<Contact>>> = 
        contactDao.getContacts().flowMap { list -> 
            Resource.Success(list.map { ce: com.example.firstapp.common.data.local.entity.ContactEntity -> ce.toModel() })
        }

    override fun syncContacts(phoneNumbers: List<String>): Flow<Resource<List<Contact>>> = flow {
        emit(Resource.Loading())
        try {
            val response = if (phoneNumbers.isEmpty()) {
                socialApi.getSuggestedUsers()
            } else {
                com.example.firstapp.common.data.api.ApiClient.userApi.syncContacts(SyncContactsRequest(phoneNumbers))
            }

            if (response.isSuccessful && response.body() != null) {
                val apiUsers = response.body()!!.contacts
                val contacts = apiUsers.map { user: com.example.firstapp.common.data.api.ApiUser ->
                    Contact(
                        id = user.id,
                        username = user.username,
                        displayName = user.display_name,
                        avatarUrl = user.avatar_url,
                        phoneNumber = user.phone_number ?: "",
                        isRegistered = true,
                        isAuthenticated = true,
                        isVerified = user.is_verified,
                        bio = user.bio,
                        publicKey = user.public_key
                    )
                }
                contactDao.insertContacts(contacts.map { c: Contact -> c.toEntity() })
                emit(Resource.Success(contacts))
            } else {
                emit(Resource.Error("Failed to sync contacts"))
            }
        } catch (e: Exception) {
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
