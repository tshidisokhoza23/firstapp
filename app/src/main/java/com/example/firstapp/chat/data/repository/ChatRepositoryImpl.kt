package com.example.firstapp.chat.data.repository

import android.content.Context
import android.util.Log
import com.example.firstapp.chat.data.ChatWebSocketClient
import com.example.firstapp.chat.data.GlobalCallState
import com.example.firstapp.chat.data.api.*
import com.example.firstapp.chat.model.*
import com.example.firstapp.common.data.api.*
import com.example.firstapp.common.data.local.dao.CallLogDao
import com.example.firstapp.common.data.local.dao.ChatDao
import com.example.firstapp.common.data.local.dao.ContactDao
import com.example.firstapp.common.data.local.entity.ChatEntity
import com.example.firstapp.common.data.local.entity.ContactEntity
import com.example.firstapp.common.data.local.entity.MessageEntity
import com.example.firstapp.common.data.local.entity.CallLogEntity
import com.example.firstapp.common.data.repository.*
import com.example.firstapp.common.model.ConversationType
import com.example.firstapp.common.ui.theme.BoyishPurple
import com.example.firstapp.common.util.ContactUtils
import com.example.firstapp.common.util.Resource
import com.example.firstapp.domain.repository.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import java.util.UUID
import java.util.ArrayList

class ChatRepositoryImpl @Inject constructor(
    private val chatApi: ChatApi,
    private val chatDao: ChatDao,
    private val callLogDao: CallLogDao,
    private val contactDao: ContactDao,
    private val accountRepository: com.example.firstapp.account.domain.repository.AccountRepository,
    @ApplicationContext private val context: Context
) : ChatRepository {

    override fun getChats(): Flow<Resource<List<ChatMessage>>> = flow {
        val cachedEntities = chatDao.getChats().first()
        val cached = ArrayList<ChatMessage>()
        for (entity in cachedEntities) {
            cached.add(entity.toModel())
        }
        emit(Resource.Loading(cached))
        
        try {
            val response = chatApi.getChats()
            if (response.isSuccessful && response.body() != null) {
                val apiChats = response.body()!!.chats
                val chats = ArrayList<ChatMessage>()
                for (apiChat in apiChats) {
                    chats.add(mapApiChatSummary(apiChat))
                }
                
                // Clear and insert all types
                chatDao.clearChats()
                val entities = ArrayList<ChatEntity>()
                for (chat in chats) {
                    entities.add(chat.toEntity())
                }
                chatDao.insertChats(entities)
            }
        } catch (e: Exception) { 
            Log.e("ChatRepositoryImpl", "getChats API failed", e)
        }
        
        chatDao.getChats().collect { entities ->
            val models = ArrayList<ChatMessage>()
            for (entity in entities) {
                models.add(entity.toModel())
            }
            emit(Resource.Success(models))
        }
    }

    override fun getContacts(): Flow<Resource<List<Contact>>> = flow {
        val cachedEntities = contactDao.getContacts().first()
        val cached = ArrayList<Contact>()
        for (entity in cachedEntities) {
            cached.add(entity.toModel())
        }
        emit(Resource.Loading(cached))
        
        syncContactsInternal()

        contactDao.getContacts().collect { entities ->
            val models = ArrayList<Contact>()
            for (entity in entities) {
                models.add(entity.toModel())
            }
            emit(Resource.Success(models))
        }
    }

    override fun syncContacts(phoneNumbers: List<String>): Flow<Resource<List<Contact>>> = flow {
        emit(Resource.Loading())
        try {
            val response = chatApi.syncContacts(SyncContactsRequest(phoneNumbers))
            if (response.isSuccessful && response.body() != null) {
                val apiContacts = response.body()!!.contacts
                val registeredContacts = ArrayList<Contact>()
                for (apiUser in apiContacts) {
                    registeredContacts.add(Contact(
                        id = apiUser.id,
                        username = apiUser.username,
                        displayName = apiUser.display_name,
                        avatarUrl = apiUser.avatar_url,
                        phoneNumber = apiUser.phone_number ?: "",
                        isRegistered = true,
                        isAuthenticated = false,
                        isVerified = apiUser.is_verified
                    ))
                }
                val entities = ArrayList<ContactEntity>()
                for (c in registeredContacts) {
                    entities.add(c.toEntity())
                }
                contactDao.insertContacts(entities)
                emit(Resource.Success(registeredContacts as List<Contact>))
            } else {
                emit(Resource.Error("Sync failed"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Sync error"))
        }
    }

    private suspend fun syncContactsInternal() {
        try {
            val phoneContacts = ContactUtils.getPhoneContactsWithNames(context)
            val phoneNumbers = ArrayList<String>()
            for (c in phoneContacts) {
                phoneNumbers.add(c.phoneNumber)
            }
            
            val response = chatApi.syncContacts(SyncContactsRequest(phoneNumbers))
            if (response.isSuccessful && response.body() != null) {
                val registeredApiContacts = response.body()?.contacts ?: ArrayList()
                val registeredMap = java.util.HashMap<String, ApiUser>()
                for (apiUser in registeredApiContacts) {
                    val phone = apiUser.phone_number
                    if (phone != null) {
                        registeredMap.put(phone, apiUser)
                    }
                }
                
                val mergedContacts = ArrayList<Contact>()
                for (local in phoneContacts) {
                    val registered = registeredMap[local.phoneNumber]
                    if (registered != null) {
                        mergedContacts.add(Contact(
                            id = registered.id,
                            username = registered.username,
                            displayName = registered.display_name,
                            avatarUrl = registered.avatar_url,
                            phoneNumber = registered.phone_number ?: local.phoneNumber,
                            isRegistered = true,
                            isVerified = registered.is_verified
                        ))
                    } else {
                        mergedContacts.add(Contact(
                            id = local.phoneNumber.hashCode(),
                            username = local.phoneNumber,
                            displayName = local.displayName,
                            phoneNumber = local.phoneNumber,
                            isRegistered = false
                        ))
                    }
                }
                
                contactDao.clearContacts()
                val entities = ArrayList<ContactEntity>()
                for (c in mergedContacts) {
                    entities.add(c.toEntity())
                }
                contactDao.insertContacts(entities)
            } else {
                val localContacts = ArrayList<Contact>()
                for (local in phoneContacts) {
                    localContacts.add(Contact(
                        id = local.phoneNumber.hashCode(),
                        username = local.phoneNumber,
                        displayName = local.displayName,
                        phoneNumber = local.phoneNumber,
                        isRegistered = false
                    ))
                }
                val entities = ArrayList<ContactEntity>()
                for (c in localContacts) {
                    entities.add(c.toEntity())
                }
                contactDao.insertContacts(entities)
            }
        } catch (e: Exception) {
            Log.e("ChatRepositoryImpl", "Internal sync failed", e)
        }
    }

    override fun getArchivedChats(): Flow<Resource<List<ChatMessage>>> = flow {
        val entities = chatDao.getArchivedChats().first()
        val cached = ArrayList<ChatMessage>()
        for (entity in entities) {
            cached.add(entity.toModel())
        }
        emit(Resource.Loading(cached))
        chatDao.getArchivedChats().collect { entitiesList ->
            val models = ArrayList<ChatMessage>()
            for (entity in entitiesList) {
                models.add(entity.toModel())
            }
            emit(Resource.Success(models))
        }
    }

    override fun getCallLogs(): Flow<Resource<List<CallLog>>> = flow {
        val entities = callLogDao.getCallLogs().first()
        val cached = ArrayList<CallLog>()
        for (entity in entities) {
            cached.add(entity.toModel())
        }
        emit(Resource.Loading(cached))
        try {
            val response = chatApi.getCalls()
            if (response.isSuccessful && response.body() != null) {
                val apiCalls = response.body()!!.calls
                val calls = ArrayList<CallLog>()
                for (apiCall in apiCalls) {
                    calls.add(mapApiCallLog(apiCall))
                }
                val callEntities = ArrayList<CallLogEntity>()
                for (call in calls) {
                    callEntities.add(call.toEntity())
                }
                callLogDao.clearCallLogs()
                callLogDao.insertCallLogs(callEntities)
            }
        } catch (e: Exception) { 
            Log.e("ChatRepositoryImpl", "getCallLogs failed", e)
        }
        callLogDao.getCallLogs().collect { entitiesList ->
            val models = ArrayList<CallLog>()
            for (entity in entitiesList) {
                models.add(entity.toModel())
            }
            emit(Resource.Success(models))
        }
    }

    override fun getMessages(chatId: Int, page: Int): Flow<Resource<List<Message>>> = flow {
        val entities = chatDao.getMessages(chatId).first()
        val cached = ArrayList<Message>()
        for (entity in entities) {
            cached.add(entity.toModel())
        }
        emit(Resource.Loading(cached))
        
        chatDao.getMessages(chatId).collect { entitiesList ->
            val models = ArrayList<Message>()
            for (entity in entitiesList) {
                models.add(entity.toModel())
            }
            emit(Resource.Success(models))
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
        val entities = chatDao.getMessages(chatId).first()
        val cached = ArrayList<Message>()
        for (entity in entities) {
            val model = entity.toModel()
            if (model.type == MessageType.IMAGE || model.type == MessageType.VIDEO || model.type == MessageType.DOCUMENT) {
                cached.add(model)
            }
        }
        emit(Resource.Loading(cached))
        chatDao.getMessages(chatId).collect { entitiesList ->
            val media = ArrayList<Message>()
            for (entity in entitiesList) {
                val model = entity.toModel()
                if (model.type == MessageType.IMAGE || model.type == MessageType.VIDEO || model.type == MessageType.DOCUMENT) {
                    media.add(model)
                }
            }
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
        val uuid = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        
        val type = if (messageType == "image") MessageType.IMAGE
                  else if (messageType == "video") MessageType.VIDEO
                  else if (messageType == "audio") MessageType.AUDIO_MESSAGE
                  else if (messageType == "location") MessageType.LOCATION
                  else if (messageType == "contact") MessageType.CONTACT
                  else if (messageType == "document") MessageType.DOCUMENT
                  else MessageType.TEXT

        val pendingMsg = Message(
            id = uuid,
            chatId = chatId,
            content = body,
            senderId = 0,
            senderName = "Me",
            timestamp = timestamp,
            isFromMe = true,
            type = type,
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
        val entities = ArrayList<MessageEntity>()
        entities.add(pendingMsg.toEntity(chatId))
        chatDao.insertMessages(entities)
        emit(Resource.Success(pendingMsg))

        try {
            var encryptedBody = body
            
            var pubKey = chatDao.getChatPublicKey(chatId)
            
            if (pubKey == null) {
                pubKey = accountRepository.getPublicKey(chatId)
                if (pubKey != null) {
                    chatDao.updatePublicKey(chatId, pubKey)
                }
            }
            
            if (pubKey != null) {
                try {
                    encryptedBody = "ENC:" + com.example.firstapp.common.util.CryptoUtils.encrypt(body, pubKey)
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
                val serverEntities = ArrayList<MessageEntity>()
                serverEntities.add(serverMsg.toEntity(chatId))
                chatDao.insertMessages(serverEntities)
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
        val entities = ArrayList<MessageEntity>()
        entities.add(message.toEntity(chatId))
        chatDao.insertMessages(entities) 
    }

    override suspend fun updateMessageInCache(chatId: Int, message: Message) { 
        val entities = ArrayList<MessageEntity>()
        entities.add(message.toEntity(chatId))
        chatDao.insertMessages(entities) 
    }
    override suspend fun updateMessageReactions(messageId: String, reactions: Map<String, Int>) { chatDao.updateMessageReactions(messageId, reactions) }
    override suspend fun deleteMessageLocal(messageId: String) { chatDao.deleteMessage(messageId) }
    override suspend fun saveChat(chat: ChatMessage) { 
        val entities = ArrayList<ChatEntity>()
        entities.add(chat.toEntity())
        chatDao.insertChats(entities) 
    }
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
    override suspend fun updateLastMessage(chatId: Int, lastMessage: String, time: Long, isFromMe: Boolean, isRead: Boolean) { 
        val existing = chatDao.searchChatsById(chatId)
        if (existing == null) {
            val contact = contactDao.getContactById(chatId)
            val name = contact?.displayName ?: GlobalCallState.getContactName(chatId) ?: "User $chatId"
            val entities = ArrayList<ChatEntity>()
            entities.add(ChatEntity(
                    id = chatId,
                    type = ConversationType.DIRECT,
                    senderName = name,
                    lastMessage = lastMessage,
                    timestamp = time,
                    unreadCount = 0,
                    avatarUrl = contact?.avatarUrl,
                    isOnline = false,
                    isMuted = false,
                    isPinned = false,
                    isTyping = false,
                    isGroup = false, 
                    lastMessageIsFromMe = isFromMe,
                    lastMessageRead = isRead
                ))
            chatDao.insertChats(entities)
        } else {
            chatDao.updateLastMessage(chatId, lastMessage, time, isFromMe, isRead)
        }
    }
    
    override suspend fun incrementUnreadCount(chatId: Int) { chatDao.incrementUnreadCount(chatId) }

    override suspend fun createChat(contactId: Int) { 
        try { 
            val contact = contactDao.getContactById(contactId)
            val name = contact?.displayName ?: "$contactId"
            val stub = ChatMessage(id = contactId, senderName = name, lastMessage = "", timestamp = System.currentTimeMillis(), avatarColor = BoyishPurple, isGroup = false)
            val entities = ArrayList<ChatEntity>()
            entities.add(stub.toEntity())
            chatDao.insertChats(entities)
            chatApi.createChat(CreateChatRequest(contactId))
        } catch (_: Exception) {} 
    }

    override suspend fun createGroup(name: String, contactIds: List<Int>): Flow<Resource<ChatMessage>> = flow { 
        emit(Resource.Loading())
        try { 
            val response = chatApi.createGroup(CreateGroupRequest(name, contactIds))
            if (response.isSuccessful && response.body() != null) { 
                val chat = mapApiChatSummary(response.body()!!)
                val entities = ArrayList<ChatEntity>()
                entities.add(chat.toEntity())
                chatDao.insertChats(entities)
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
        chatDao.getStarredMessages().collect { entities -> 
            val models = ArrayList<Message>()
            for (entity in entities) {
                models.add(entity.toModel())
            }
            emit(Resource.Success(models)) 
        }
    }

    override suspend fun updateWallpaper(chatId: Int, wallpaperUrl: String?) {
        chatDao.updateWallpaper(chatId, wallpaperUrl)
        try { chatApi.updateWallpaper(chatId, UpdateWallpaperRequest(wallpaperUrl)) } catch (_: Exception) {}
    }

    override suspend fun updateCustomNotificationTone(chatId: Int, tone: String?) { chatDao.updateCustomNotificationTone(chatId, tone) }
    override suspend fun updateMediaVisibility(chatId: Int, visible: Boolean) { chatDao.updateMediaVisibility(chatId, visible) }
    override suspend fun updateDisappearingMessages(chatId: Int, duration: Int) { chatDao.updateDisappearingMessages(chatId, duration) }

    override suspend fun syncPendingMessages() {
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
            if (res.isSuccessful && res.body() != null) {
                emit(Resource.Success(res.body()!!.readers))
            } else {
                emit(Resource.Success(ArrayList<ApiReader>()))
            }
        } catch (_: Exception) {
            emit(Resource.Error("Fail"))
        }
    }

    override fun reportMessage(messageId: String, reason: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (chatApi.reportMessage(ReportMessageRequest(messageId, reason)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun initiateCall(receiverId: Int, callType: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        val sessionId = UUID.randomUUID().toString()
        emit(Resource.Success(sessionId))
    }

    override fun searchLocalMessages(chatId: Int, query: String): Flow<List<Message>> {
        val ftsQuery = if (query.contains("*")) query else "$query*"
        return chatDao.searchMessages(chatId, ftsQuery).map { entities -> 
            val models = ArrayList<Message>()
            for (entity in entities) {
                models.add(entity.toModel())
            }
            models
        }
    }

    override fun searchLocalChats(query: String): Flow<List<ChatMessage>> = 
        chatDao.searchChats(query).map { entities -> 
            val models = ArrayList<ChatMessage>()
            for (entity in entities) {
                models.add(entity.toModel())
            }
            models
        }

    override fun searchLocalContacts(query: String): Flow<List<Contact>> = 
        contactDao.searchContacts(query).map { entities -> 
            val models = ArrayList<Contact>()
            for (entity in entities) {
                models.add(entity.toModel())
            }
            models
        }

    override fun searchMessagesAcrossAll(query: String, filter: com.example.firstapp.search.model.SearchFilter): Flow<List<Message>> {
        val ftsQuery = if (query == "") "" else (if (query.contains("*")) query else "$query*")
        
        val typeFilter = when (filter) {
            com.example.firstapp.search.model.SearchFilter.PHOTOS -> "IMAGE"
            com.example.firstapp.search.model.SearchFilter.VIDEOS -> "VIDEO"
            com.example.firstapp.search.model.SearchFilter.AUDIO -> "AUDIO_MESSAGE"
            com.example.firstapp.search.model.SearchFilter.DOCUMENTS -> "DOCUMENT"
            else -> ""
        }

        // Special handling for LINKS: we'll search for 'http' in the FTS query if not already there
        // OR we use a different approach. For now, let's just support the basic media types.
        
        return chatDao.searchMessagesComplex(ftsQuery, typeFilter).map { entities -> 
            val models = ArrayList<Message>()
            for (entity in entities) {
                val model = entity.toModel()
                
                // Manual filtering for LINKS if needed
                if (filter == com.example.firstapp.search.model.SearchFilter.LINKS) {
                    if (model.content.contains("http://") || model.content.contains("https://")) {
                        models.add(model)
                    }
                } else {
                    models.add(model)
                }
            }
            models
        }
    }

    override fun getAllFiles(): Flow<Resource<List<Message>>> = flow {
        emit(Resource.Loading())
        chatDao.getAllFiles().collect { entities ->
            val models = ArrayList<Message>()
            for (entity in entities) {
                models.add(entity.toModel())
            }
            emit(Resource.Success(models))
        }
    }
}
