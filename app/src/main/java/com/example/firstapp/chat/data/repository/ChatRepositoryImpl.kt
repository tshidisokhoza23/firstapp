package com.example.firstapp.chat.data.repository

import com.example.firstapp.chat.data.api.ChatApi
import com.example.firstapp.common.data.local.dao.ChatDao
import com.example.firstapp.common.data.local.dao.CallLogDao
import com.example.firstapp.common.data.local.dao.ContactDao
import com.example.firstapp.common.data.local.entity.ChatEntity
import com.example.firstapp.common.util.Resource
import com.example.firstapp.domain.repository.ChatRepository
import com.example.firstapp.chat.model.*
import com.example.firstapp.chat.data.GlobalCallState
import com.example.firstapp.chat.data.ChatWebSocketClient
import com.example.firstapp.common.ui.theme.BoyishPurple
import kotlinx.coroutines.flow.*
import android.util.Log
import com.example.firstapp.chat.data.api.*
import com.example.firstapp.common.data.api.*

import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val chatDao: ChatDao,
    private val callLogDao: CallLogDao,
    private val contactDao: ContactDao,
    private val accountRepository: com.example.firstapp.account.domain.repository.AccountRepository
) : ChatRepository {

    override fun getChats(): Flow<Resource<List<ChatMessage>>> = flow {
        // Repair missing chats from messages
        try {
            val messageChatIds = chatDao.getAllChatIdsFromMessages()
            val existingChatIds = chatDao.getAllChatIds().toSet()
            val missingIds = messageChatIds.filter { it !in existingChatIds }
            
            if (missingIds.isNotEmpty()) {
                val newStubs = missingIds.mapNotNull { id ->
                    val lastMsg = chatDao.getLastMessageForChat(id) ?: return@mapNotNull null
                    val name = GlobalCallState.getContactName(id) ?: id.toString()
                    
                    ChatEntity(
                        id = id,
                        senderName = name,
                        lastMessage = lastMsg.content,
                        timestamp = lastMsg.timestamp,
                        unreadCount = 0,
                        avatarUrl = null,
                        isOnline = false,
                        isMuted = false,
                        isPinned = false,
                        isTyping = false,
                        isGroup = false,
                        lastMessageIsFromMe = lastMsg.isFromMe,
                        lastMessageRead = lastMsg.isRead
                    )
                }
                if (newStubs.isNotEmpty()) chatDao.insertChats(newStubs)
            }
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "Failed to repair missing chats", e)
        }

        val cached = chatDao.getChats().first().map { it.toModel() }
        emit(Resource.Loading(cached))
        
        try {
            val response = chatApi.getChats()
            if (response.isSuccessful) {
                val chats = response.body()?.chats?.map { mapApiChatSummary(it) } ?: emptyList()
                chatDao.insertChats(chats.map { it.toEntity() })
            }
        } catch (e: Exception) { 
            Log.e("ChatRepositoryImpl", "getChats API failed", e)
        }
        
        chatDao.getChats().collect { entities ->
            emit(Resource.Success(entities.map { it.toModel() }))
        }
    }

    override fun getContacts(): Flow<Resource<List<Contact>>> = flow {
        val cached = contactDao.getContacts().first().map { it.toModel() }
        emit(Resource.Loading(cached))
        try {
            val response = chatApi.getContacts()
            if (response.isSuccessful) {
                val contacts = response.body()?.contacts?.map { apiUser ->
                    Contact(
                        id = apiUser.id,
                        username = apiUser.username,
                        displayName = apiUser.display_name,
                        avatarUrl = apiUser.avatar_url,
                        phoneNumber = apiUser.phone_number ?: "",
                        isRegistered = true,
                        isAuthenticated = false,
                        isVerified = apiUser.is_verified
                    )
                } ?: emptyList()
                contactDao.clearContacts()
                contactDao.insertContacts(contacts.map { it.toEntity() })
            }
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "getContacts failed", e)
        }
        contactDao.getContacts().collect { entities ->
            emit(Resource.Success(entities.map { it.toModel() }))
        }
    }

    override fun getArchivedChats(): Flow<Resource<List<ChatMessage>>> = flow {
        val cached = chatDao.getArchivedChats().first().map { it.toModel() }
        emit(Resource.Loading(cached))
        chatDao.getArchivedChats().collect { entities ->
            emit(Resource.Success(entities.map { it.toModel() }))
        }
    }

    override fun getCallLogs(): Flow<Resource<List<CallLog>>> = flow {
        val cached = callLogDao.getCallLogs().first().map { it.toModel() }
        emit(Resource.Loading(cached))
        try {
            val response = chatApi.getCalls()
            if (response.isSuccessful) {
                val calls = response.body()?.calls?.map { mapApiCallLog(it) } ?: emptyList()
                callLogDao.clearCallLogs()
                callLogDao.insertCallLogs(calls.map { it.toEntity() })
            }
        } catch (e: Exception) { 
            Log.e("ChatRepositoryImpl", "getCallLogs failed", e)
        }
        callLogDao.getCallLogs().collect { entities ->
            emit(Resource.Success(entities.map { it.toModel() }))
        }
    }

    override fun getMessages(chatId: Int, page: Int): Flow<Resource<List<Message>>> = flow {
        // According to new rules: 
        // 1. Groups messages are stored locally only.
        // 2. 1-on-1 chats and messages are stored locally only.
        // So we do NOT fetch messages from the backend for any chat/group.
        
        val cached = chatDao.getMessages(chatId).first().map { it.toModel() }
        emit(Resource.Loading(cached))
        
        chatDao.getMessages(chatId).collect { entities ->
            emit(Resource.Success(entities.map { it.toModel() }))
        }
    }

    override fun markAsRead(chatId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            chatDao.markIncomingAsRead(chatId)
            chatDao.markIncomingLastMessageAsRead(chatId)
            chatDao.updateUnreadCount(chatId, 0)
            ChatWebSocketClient.sendRead(chatId)
            if (chatApi.markAsRead(chatId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun deleteMessage(messageId: String, deleteForEveryone: Boolean): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val chatId = chatDao.getChatIdForMessage(messageId) ?: 0
            if (chatId != 0 && deleteForEveryone) {
                ChatWebSocketClient.sendDelete(chatId, messageId)
            }
            chatDao.deleteMessage(messageId)
            if (chatApi.deleteMessage(messageId, deleteForEveryone).isSuccessful) {
                emit(Resource.Success(true))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun forwardMessage(messageId: String, receiverIds: List<Int>, groupIds: List<Int>): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (chatApi.forwardMessage(messageId, ForwardMessageRequest(receiverIds, groupIds)).isSuccessful) {
                emit(Resource.Success(true))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun archiveChat(chatId: Int, isArchived: Boolean): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            chatDao.updateArchived(chatId, isArchived)
            if (chatApi.archiveChat(chatId, ArchiveRequest(isArchived)).isSuccessful) {
                emit(Resource.Success(true))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun getGroupInfo(groupId: Int): Flow<Resource<GroupInfoResponse>> = flow {
        try {
            emit(Resource.Loading())
            val response = chatApi.getGroupInfo(groupId)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun addGroupMember(groupId: Int, userId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (chatApi.addGroupMember(groupId, GroupMemberRequest(userId)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun removeGroupMember(groupId: Int, userId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (chatApi.removeGroupMember(groupId, GroupMemberRequest(userId)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun promoteToAdmin(groupId: Int, userId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (chatApi.promoteToAdmin(groupId, GroupMemberRequest(userId)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun leaveGroup(groupId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (chatApi.leaveGroup(groupId).isSuccessful) {
                chatDao.deleteChat(groupId)
                emit(Resource.Success(true))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun getSharedMedia(chatId: Int): Flow<Resource<List<Message>>> = flow {
        val cached = chatDao.getMessages(chatId).first()
            .map { it.toModel() }
            .filter { it.type in listOf(MessageType.IMAGE, MessageType.VIDEO, MessageType.DOCUMENT) }
        emit(Resource.Loading(cached))
        chatDao.getMessages(chatId).collect { entities ->
            val media = entities.map { it.toModel() }.filter { it.type in listOf(MessageType.IMAGE, MessageType.VIDEO, MessageType.DOCUMENT) }
            emit(Resource.Success(media))
        }
    }

    override fun editMessage(messageId: String, newBody: String): Flow<Resource<Message>> = flow {
        try {
            emit(Resource.Loading())
            val chatId = chatDao.getChatIdForMessage(messageId) ?: 0
            if (chatId != 0) {
                ChatWebSocketClient.sendEdit(chatId, messageId, newBody)
            }
            val response = chatApi.editMessage(messageId, EditMessageRequest(newBody))
            if (response.isSuccessful && response.body() != null) emit(Resource.Success(mapApiMessage(response.body()!!.message))) 
            else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun reactToMessage(messageId: String, emoji: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val chatId = chatDao.getChatIdForMessage(messageId) ?: 0
            if (chatId != 0) {
                ChatWebSocketClient.sendReaction(chatId, messageId, emoji)
            }
            if (chatApi.reactToMessage(messageId, ReactionRequest(emoji)).isSuccessful) emit(Resource.Success(true)) 
            else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun sendMessage(chatId: Int, body: String, messageType: String, mediaUrl: String?, duration: String?, fileName: String?, fileSize: String?, contactName: String?, contactPhone: String?, latitude: Double?, longitude: Double?, replyToId: String?): Flow<Resource<Message>> = flow {
        val uuid = java.util.UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        
        val pendingMsg = Message(
            id = uuid,
            chatId = chatId,
            content = body,
            senderId = 0,
            senderName = "Me",
            timestamp = timestamp,
            isFromMe = true,
            type = when(messageType.lowercase()) { 
                "image" -> MessageType.IMAGE
                "video" -> MessageType.VIDEO
                "audio" -> MessageType.AUDIO_MESSAGE
                "location" -> MessageType.LOCATION
                "contact" -> MessageType.CONTACT
                "document" -> MessageType.DOCUMENT
                else -> MessageType.TEXT 
            },
            mediaUri = mediaUrl,
            fileName = fileName,
            fileSize = fileSize,
            duration = duration,
            contactName = contactName,
            contactPhone = contactPhone,
            latitude = latitude,
            longitude = longitude,
            replyToId = replyToId,
            status = MessageStatus.PENDING,
            clientId = uuid
        )
        chatDao.insertMessages(listOf(pendingMsg.toEntity(chatId)))
        emit(Resource.Success(pendingMsg))

        try {
            var encryptedBody = body
            var isEncrypted = false
            
            // Try to get public key from cache first
            var pubKey = chatDao.getChatPublicKey(chatId)
            
            if (pubKey == null) {
                // Fetch from API
                pubKey = accountRepository.getPublicKey(chatId)
                if (pubKey != null) {
                    chatDao.updatePublicKey(chatId, pubKey)
                }
            }
            
            if (pubKey != null) {
                try {
                    encryptedBody = "ENC:" + com.example.firstapp.common.util.CryptoUtils.encrypt(body, pubKey)
                    isEncrypted = true
                } catch (e: Exception) {
                    Log.e("ChatRepositoryImpl", "Encryption failed", e)
                }
            }

            val response = chatApi.sendMessage(
                contactId = chatId,
                request = SendMessageRequest(
                    client_id = uuid,
                    body = encryptedBody,
                    message_type = messageType, 
                    media_url = mediaUrl, 
                    duration = duration, 
                    file_name = fileName, 
                    file_size = fileSize, 
                    contact_name = contactName, 
                    contact_phone = contactPhone, 
                    latitude = latitude, 
                    longitude = longitude, 
                    parent_id = replyToId
                )
            )
            if (response.isSuccessful && response.body() != null) {
                val serverMsg = mapApiMessage(response.body()!!.message)
                chatDao.deleteMessage(uuid)
                chatDao.insertMessages(listOf(serverMsg.toEntity(chatId)))
                chatDao.updateLastMessage(chatId, serverMsg.content, serverMsg.timestamp, serverMsg.isFromMe, serverMsg.isRead)
                emit(Resource.Success(serverMsg))
            }
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "sendMessage failed", e)
        }
    }

    override suspend fun saveMessage(chatId: Int, message: Message) { 
        if (message.clientId != null) {
            val existing = chatDao.getMessageByClientId(message.clientId)
            if (existing != null && existing.id != message.id) {
                chatDao.deleteMessage(existing.id)
            }
        }
        chatDao.insertMessages(listOf(message.toEntity(chatId))) 
    }

    override suspend fun updateMessageInCache(chatId: Int, message: Message) { chatDao.insertMessages(listOf(message.toEntity(chatId))) }
    override suspend fun updateMessageReactions(messageId: String, reactions: Map<String, Int>) { chatDao.updateMessageReactions(messageId, reactions) }
    override suspend fun deleteMessageLocal(messageId: String) { chatDao.deleteMessage(messageId) }
    override suspend fun saveChat(chat: ChatMessage) { chatDao.insertChats(listOf(chat.toEntity())) }
    override suspend fun updateMessageStatus(messageId: String, status: MessageStatus) {
        if (status == MessageStatus.DELIVERED) chatDao.markAsDelivered(messageId)
    }
    override suspend fun markChatAsReadLocal(chatId: Int) {
        chatDao.markOutgoingAsRead(chatId)
        chatDao.markOutgoingLastMessageAsRead(chatId)
        chatDao.updateUnreadCount(chatId, 0)
    }
    override suspend fun updatePresence(chatId: Int, isOnline: Boolean, lastSeen: Long) { chatDao.updatePresence(chatId, isOnline, lastSeen) }
    override suspend fun updateTyping(chatId: Int, isTyping: Boolean) { chatDao.updateTyping(chatId, isTyping) }
    override suspend fun incrementUnreadCount(chatId: Int) { chatDao.incrementUnreadCount(chatId) }
    override suspend fun updateLastMessage(chatId: Int, lastMessage: String, time: Long, isFromMe: Boolean, isRead: Boolean) { 
        val existing = chatDao.searchChatsById(chatId)
        if (existing == null) {
            val contact = contactDao.getContactById(chatId)
            val name = contact?.displayName ?: GlobalCallState.getContactName(chatId) ?: "User $chatId"
            chatDao.insertChats(listOf(
                ChatEntity(
                    id = chatId,
                    senderName = name,
                    lastMessage = lastMessage,
                    timestamp = time,
                    unreadCount = 0,
                    avatarUrl = contact?.avatarUrl,
                    isOnline = false,
                    isMuted = false,
                    isPinned = false,
                    isTyping = false,
                    isGroup = false, // We might need to know if it's a group, but usually this is for 1-on-1 here
                    lastMessageIsFromMe = isFromMe,
                    lastMessageRead = isRead
                )
            ))
        } else {
            chatDao.updateLastMessage(chatId, lastMessage, time, isFromMe, isRead)
        }
    }
    
    override suspend fun createChat(contactId: Int) { 
        try { 
            val contact = contactDao.getContactById(contactId)
            val name = contact?.displayName ?: "$contactId"
            val stub = ChatMessage(id = contactId, senderName = name, lastMessage = "", timestamp = System.currentTimeMillis(), avatarColor = BoyishPurple, isGroup = false)
            chatDao.insertChats(listOf(stub.toEntity()))
            chatApi.createChat(CreateChatRequest(contactId))
        } catch (_: Exception) {} 
    }

    override suspend fun createGroup(name: String, contactIds: List<Int>): Flow<Resource<ChatMessage>> = flow { 
        emit(Resource.Loading())
        try { 
            val response = chatApi.createGroup(CreateGroupRequest(name, contactIds))
            if (response.isSuccessful && response.body() != null) { 
                val chat = mapApiChatSummary(response.body()!!)
                chatDao.insertChats(listOf(chat.toEntity()))
                emit(Resource.Success(chat)) 
            }
        } catch (e: Exception) { emit(Resource.Error("Error")) } 
    }

    override fun pinChat(chatId: Int, isPinned: Boolean): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            chatDao.updatePinned(chatId, isPinned)
            if (chatApi.pinChat(chatId, PinRequest(isPinned)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun muteChat(chatId: Int, isMuted: Boolean): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            chatDao.updateMuted(chatId, isMuted)
            if (chatApi.muteChat(chatId, MuteRequest(isMuted)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun deleteChat(chatId: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            chatDao.clearMessages(chatId)
            chatDao.deleteChat(chatId)
            if (chatApi.deleteChat(chatId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override suspend fun clearAllChats() {
        chatDao.clearAllMessages()
        chatDao.clearChats()
    }

    override suspend fun clearMessages(chatId: Int) { chatDao.clearMessages(chatId) }

    override fun starMessage(messageId: String, isStarred: Boolean): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            chatDao.starMessage(messageId, isStarred)
            if (chatApi.starMessage(messageId, StarMessageRequest(isStarred)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun getStarredMessages(): Flow<Resource<List<Message>>> = flow {
        chatDao.getStarredMessages().collect { entities -> emit(Resource.Success(entities.map { it.toModel() })) }
    }

    override suspend fun updateWallpaper(chatId: Int, wallpaperUrl: String?) {
        chatDao.updateWallpaper(chatId, wallpaperUrl)
        try { chatApi.updateWallpaper(chatId, UpdateWallpaperRequest(wallpaperUrl)) } catch (_: Exception) {}
    }

    override suspend fun updateCustomNotificationTone(chatId: Int, tone: String?) { chatDao.updateCustomNotificationTone(chatId, tone) }
    override suspend fun updateMediaVisibility(chatId: Int, visible: Boolean) { chatDao.updateMediaVisibility(chatId, visible) }
    override suspend fun updateDisappearingMessages(chatId: Int, duration: Int) { chatDao.updateDisappearingMessages(chatId, duration) }

    override suspend fun syncPendingMessages() {
        // Logic for syncing pending messages could be moved here if needed
    }

    override fun pinMessage(chatId: Int, messageId: String?): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (chatApi.pinMessageInChat(chatId, PinMessageRequest(messageId)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun getMessageReaders(messageId: String): Flow<Resource<List<ApiReader>>> = flow {
        emit(Resource.Loading())
        try {
            val res = chatApi.getMessageReaders(messageId)
            if (res.isSuccessful) emit(Resource.Success(res.body()?.readers ?: emptyList())) else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun reportMessage(messageId: String, reason: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (chatApi.reportMessage(ReportMessageRequest(messageId, reason)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun initiateCall(receiverId: Int, callType: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        val sessionId = java.util.UUID.randomUUID().toString()
        emit(Resource.Success(sessionId))
    }

    override fun searchLocalMessages(chatId: Int, query: String): Flow<List<Message>> = chatDao.searchMessages(chatId, query).map { it.map { m -> m.toModel() } }

    override fun searchLocalChats(query: String): Flow<List<ChatMessage>> = 
        chatDao.searchChats(query).map { it.map { c -> c.toModel() } }

    override fun searchLocalContacts(query: String): Flow<List<Contact>> = 
        contactDao.searchContacts(query).map { list -> list.map { it.toModel() } }

    override fun searchMessagesAcrossAll(query: String): Flow<List<Message>> = 
        chatDao.searchMessagesAcrossAll(query).map { it.map { m -> m.toModel() } }

    override fun getAllFiles(): Flow<Resource<List<Message>>> = flow {
        emit(Resource.Loading())
        chatDao.getAllFiles().collect { entities ->
            emit(Resource.Success(entities.map { it.toModel() }))
        }
    }
}
