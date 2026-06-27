package com.example.firstapp.domain.repository

import com.example.firstapp.chat.data.api.GroupInfoResponse
import com.example.firstapp.chat.model.*
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChats(): Flow<Resource<List<ChatMessage>>>
    fun getContacts(): Flow<Resource<List<Contact>>>
    fun getArchivedChats(): Flow<Resource<List<ChatMessage>>>
    fun getCallLogs(): Flow<Resource<List<CallLog>>>
    fun getMessages(chatId: Int, page: Int = 1): Flow<Resource<List<Message>>>
    fun markAsRead(chatId: Int): Flow<Resource<Boolean>>
    fun deleteMessage(messageId: String, deleteForEveryone: Boolean = false): Flow<Resource<Boolean>>
    fun forwardMessage(messageId: String, receiverIds: List<Int>, groupIds: List<Int>): Flow<Resource<Boolean>>
    fun archiveChat(chatId: Int, isArchived: Boolean): Flow<Resource<Boolean>>
    fun getGroupInfo(groupId: Int): Flow<Resource<GroupInfoResponse>>
    fun addGroupMember(groupId: Int, userId: Int): Flow<Resource<Boolean>>
    fun removeGroupMember(groupId: Int, userId: Int): Flow<Resource<Boolean>>
    fun promoteToAdmin(groupId: Int, userId: Int): Flow<Resource<Boolean>>
    fun leaveGroup(groupId: Int): Flow<Resource<Boolean>>
    fun getSharedMedia(chatId: Int): Flow<Resource<List<Message>>>
    fun editMessage(messageId: String, newBody: String): Flow<Resource<Message>>
    fun reactToMessage(messageId: String, emoji: String): Flow<Resource<Boolean>>
    fun sendMessage(
        chatId: Int,
        body: String,
        messageType: String = "text",
        mediaUrl: String? = null,
        duration: String? = null,
        fileName: String? = null,
        fileSize: String? = null,
        contactName: String? = null,
        contactPhone: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        replyToId: String? = null
    ): Flow<Resource<Message>>
    suspend fun saveMessage(chatId: Int, message: Message)
    suspend fun updateMessageInCache(chatId: Int, message: Message)
    suspend fun updateMessageReactions(messageId: String, reactions: Map<String, Int>)
    suspend fun deleteMessageLocal(messageId: String)
    suspend fun saveChat(chat: ChatMessage)
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)
    suspend fun markChatAsReadLocal(chatId: Int)
    suspend fun updatePresence(chatId: Int, isOnline: Boolean, lastSeen: Long)
    suspend fun updateTyping(chatId: Int, isTyping: Boolean)
    suspend fun incrementUnreadCount(chatId: Int)
    suspend fun updateLastMessage(chatId: Int, lastMessage: String, time: Long, isFromMe: Boolean, isRead: Boolean)
    suspend fun createChat(contactId: Int)
    suspend fun createGroup(name: String, contactIds: List<Int>): Flow<Resource<ChatMessage>>
    fun pinChat(chatId: Int, isPinned: Boolean): Flow<Resource<Boolean>>
    fun muteChat(chatId: Int, isMuted: Boolean): Flow<Resource<Boolean>>
    fun deleteChat(chatId: Int): Flow<Resource<Boolean>>
    suspend fun clearMessages(chatId: Int)
    suspend fun clearAllChats()
    fun starMessage(messageId: String, isStarred: Boolean): Flow<Resource<Boolean>>
    fun getStarredMessages(): Flow<Resource<List<Message>>>
    suspend fun updateWallpaper(chatId: Int, wallpaperUrl: String?)
    suspend fun updateCustomNotificationTone(chatId: Int, tone: String?)
    suspend fun updateMediaVisibility(chatId: Int, visible: Boolean)
    suspend fun updateDisappearingMessages(chatId: Int, duration: Int)
    suspend fun syncPendingMessages()
    fun pinMessage(chatId: Int, messageId: String?): Flow<Resource<Boolean>>
    fun getMessageReaders(messageId: String): Flow<Resource<List<com.example.firstapp.chat.data.api.ApiReader>>>
    fun reportMessage(messageId: String, reason: String): Flow<Resource<Boolean>>
    fun initiateCall(receiverId: Int, callType: String): Flow<Resource<String>>
    fun searchLocalMessages(chatId: Int, query: String): Flow<List<Message>>
    fun searchLocalChats(query: String): Flow<List<ChatMessage>>
    fun searchLocalContacts(query: String): Flow<List<Contact>>
    fun searchMessagesAcrossAll(query: String): Flow<List<Message>>
    fun getAllFiles(): Flow<Resource<List<Message>>>
}
