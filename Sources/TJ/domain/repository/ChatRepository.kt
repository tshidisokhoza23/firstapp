package com.tpsk.tj.domain.repository

import com.tpsk.tj.chat.data.api.GroupInfoResponse
import com.tpsk.tj.chat.model.*
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlin.collections.emptyList

interface ChatRepository {
    fun getChats(): Flow<Resource<List<ChatMessage>>>
    fun getMingleChats(): Flow<Resource<List<ChatMessage>>>
    fun getChatById(chatId: String): Flow<Resource<ChatMessage>>
    fun syncChats(): Flow<Resource<Boolean>>
    fun syncDelta(): Flow<Resource<Long>>
    fun getContacts(): Flow<Resource<List<Contact>>>
    fun syncContacts(phoneNumbers: List<String>): Flow<Resource<List<Contact>>>
    fun getArchivedChats(): Flow<Resource<List<ChatMessage>>>
    fun getCallLogs(): Flow<Resource<List<CallLog>>>
    fun getMessagesPaged(chatId: String): Flow<androidx.paging.PagingData<Message>>
    fun getMessages(chatId: String, page: Int = 1): Flow<Resource<List<Message>>>
    fun fetchMessages(chatId: String, page: Int = 1): Flow<Resource<Boolean>>
    fun fetchOlderMessages(chatId: String, beforeTs: Long, limit: Int = 50): Flow<Resource<Boolean>>
    fun markAsRead(chatId: String): Flow<Resource<Boolean>>
    fun deleteMessage(messageId: String, deleteForEveryone: Boolean = false): Flow<Resource<Boolean>>
    fun forwardMessage(messageId: String, receiverIds: List<String>, groupIds: List<String>): Flow<Resource<Boolean>>
    fun archiveChat(chatId: String, isArchived: Boolean): Flow<Resource<Boolean>>
    fun getGroupInfo(groupId: String): Flow<Resource<GroupInfoResponse>>
    fun addGroupMember(groupId: String, userId: String): Flow<Resource<Boolean>>
    fun removeGroupMember(groupId: String, userId: String): Flow<Resource<Boolean>>
    fun promoteToAdmin(groupId: String, userId: String): Flow<Resource<Boolean>>
    fun demoteAdmin(groupId: String, userId: String): Flow<Resource<Boolean>>
    fun updateGroupAvatar(groupId: String, avatarUrl: String): Flow<Resource<Boolean>>
    fun updateChannelAvatar(channelId: String, avatarUrl: String): Flow<Resource<Boolean>>
    fun leaveGroup(groupId: String): Flow<Resource<Boolean>>
    fun generateGroupInviteCode(groupId: String): Flow<Resource<String>>
    fun joinGroupByInvite(inviteCode: String): Flow<Resource<String>>
    fun getSharedMedia(chatId: String): Flow<Resource<List<Message>>>
    fun editMessage(messageId: String, newBody: String): Flow<Resource<Message>>
    fun reactToMessage(messageId: String, emoji: String): Flow<Resource<Boolean>>
    fun sendMessage(
        chatId: String,
        body: String,
        messageType: String = "text",
        mediaUrls: List<String> = emptyList(),
        duration: String? = null,
        fileName: String? = null,
        fileSize: String? = null,
        contactName: String? = null,
        contactPhone: String? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSenderName: String? = null,
        clientId: String? = null,
        scheduledAt: Long? = null
    ): Flow<Resource<Message>>
    suspend fun saveMessage(chatId: String, message: Message)
    suspend fun updateMessageInCache(chatId: String, message: Message)
    suspend fun updateMessageReactions(messageId: String, reactions: Map<String, Int>)
    suspend fun deleteMessageLocal(messageId: String)
    suspend fun saveChat(chat: ChatMessage)
    suspend fun updateMessageStatus(messageId: String, status: MessageStatus)
    suspend fun markChatAsReadLocal(chatId: String)
    suspend fun updatePresence(chatId: String, isOnline: Boolean, lastSeen: Long)
    suspend fun updateTyping(chatId: String, isTyping: Boolean)
    suspend fun incrementUnreadCount(chatId: String)
    suspend fun updateLastMessage(chatId: String, lastMessage: String, time: Long, isFromMe: Boolean, isRead: Boolean)
    suspend fun initiateDM(contactId: String): Flow<Resource<String>>
    suspend fun createGroup(name: String, contactIds: List<String>): Flow<Resource<ChatMessage>>
    fun pinChat(chatId: String, isPinned: Boolean): Flow<Resource<Boolean>>
    fun muteChat(chatId: String, isMuted: Boolean): Flow<Resource<Boolean>>
    fun deleteChat(chatId: String): Flow<Resource<Boolean>>
    suspend fun clearMessages(chatId: String)
    suspend fun clearAllChats()
    fun starMessage(messageId: String, isStarred: Boolean): Flow<Resource<Boolean>>
    fun getStarredMessages(): Flow<Resource<List<Message>>>
    suspend fun updateWallpaper(chatId: String, wallpaperUrl: String?)
    suspend fun updateCustomNotificationTone(chatId: String, tone: String?)
    suspend fun updateMediaVisibility(chatId: String, visible: Boolean)
    suspend fun updateDisappearingMessages(chatId: String, duration: Int)
    suspend fun updateChatAvatar(chatId: String, avatarUrl: String?)
    suspend fun syncPendingMessages()
    fun pinMessage(chatId: String, messageId: String?): Flow<Resource<Boolean>>
    fun getMessageReaders(messageId: String): Flow<Resource<List<com.tpsk.tj.chat.data.api.ApiReader>>>
    fun reportMessage(messageId: String, reason: String): Flow<Resource<Boolean>>
    fun initiateCall(receiverId: String, callType: String): Flow<Resource<String>>
    fun searchLocalMessages(chatId: String, query: String): Flow<List<Message>>
    fun searchLocalChats(query: String): Flow<List<ChatMessage>>
    fun searchLocalContacts(query: String): Flow<List<Contact>>
    fun searchMessagesAcrossAll(query: String, filter: com.tpsk.tj.search.model.SearchFilter = com.tpsk.tj.search.model.SearchFilter.ALL): Flow<List<Message>>
    fun getAllFiles(): Flow<Resource<List<Message>>>
    suspend fun refreshLocalChatNames()
    fun updateJobApplicationStatus(chatId: String, status: String): Flow<Resource<Boolean>>
    fun subscribeToChannel(channelId: String): Flow<Resource<Boolean>>
    fun getConversationMembers(chatId: String): Flow<Resource<List<com.tpsk.tj.common.data.api.ApiUser>>>
    suspend fun getICEServers(): Resource<List<com.tpsk.tj.chat.data.api.IceServerConfig>>
    fun sendDeliveryAck(chatId: String, messageId: String)
}
