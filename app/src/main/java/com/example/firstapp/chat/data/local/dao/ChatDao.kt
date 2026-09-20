package com.example.firstapp.chat.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.example.firstapp.chat.data.local.entity.ChatEntity
import com.example.firstapp.chat.data.local.entity.MessageEntity
import com.example.firstapp.chat.model.MessageStatus
import com.example.firstapp.common.model.ConversationType
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    // ==========================================
    // CHAT STREAMS & MUTATIONS
    // ==========================================

    @Query("""
        SELECT 
            c.id AS id,
            c.type AS type,
            COALESCE(NULLIF(u.displayName, ''), NULLIF(c.senderName, ''), NULLIF(u.phoneNumber, ''), NULLIF(c.phoneNumber, ''), '') AS senderName,
            COALESCE(c.description, '') AS description,
            COALESCE(c.lastMessage, '') AS lastMessage,
            c.timestamp AS timestamp,
            c.unreadCount AS unreadCount,
            c.lastSeen AS lastSeen,
            COALESCE(u.avatarUrl, c.avatarUrl) AS avatarUrl,
            c.isOnline AS isOnline,
            c.isMuted AS isMuted,
            c.isPinned AS isPinned,
            c.pinnedMessageId AS pinnedMessageId,
            c.isTyping AS isTyping,
            c.isArchived AS isArchived,
            c.isVerified AS isVerified,
            c.isAdmin AS isAdmin,
            c.adminsCanPost AS adminsCanPost,
            c.isGroup AS isGroup,
            c.isSubscribed AS isSubscribed,
            c.createdBy AS createdBy,
            c.followers AS followers,
            c.category AS category,
            c.wallpaperUrl AS wallpaperUrl,
            c.customNotificationTone AS customNotificationTone,
            c.mediaVisibility AS mediaVisibility,
            c.disappearingMessagesDuration AS disappearingMessagesDuration,
            c.lastMessageIsFromMe AS lastMessageIsFromMe,
            c.lastMessageRead AS lastMessageRead,
            COALESCE(u.publicKey, c.publicKey) AS publicKey,
            COALESCE(NULLIF(u.phoneNumber, ''), NULLIF(c.phoneNumber, '')) AS phoneNumber,
            c.otherUserId AS otherUserId
        FROM chats c
        LEFT JOIN contacts u ON (
            c.otherUserId = u.id 
            OR c.id = u.id 
            OR (
                c.phoneNumber IS NOT NULL 
                AND u.phoneNumber IS NOT NULL 
                AND LENGTH(c.phoneNumber) >= 9 
                AND LENGTH(u.phoneNumber) >= 9 
                AND SUBSTR(REPLACE(c.phoneNumber, ' ', ''), -9) = SUBSTR(REPLACE(u.phoneNumber, ' ', ''), -9)
            )
        )
        WHERE c.isArchived = 0 AND (c.type IS NULL OR c.type != 'MINGLE')
        ORDER BY c.isPinned DESC, c.timestamp DESC
    """)
    fun getChats(): Flow<List<ChatEntity>>

    @Query("""
        SELECT 
            c.id AS id,
            c.type AS type,
            COALESCE(NULLIF(u.displayName, ''), NULLIF(c.senderName, ''), NULLIF(u.phoneNumber, ''), NULLIF(c.phoneNumber, ''), '') AS senderName,
            COALESCE(c.description, '') AS description,
            COALESCE(c.lastMessage, '') AS lastMessage,
            c.timestamp AS timestamp,
            c.unreadCount AS unreadCount,
            c.lastSeen AS lastSeen,
            COALESCE(u.avatarUrl, c.avatarUrl) AS avatarUrl,
            c.isOnline AS isOnline,
            c.isMuted AS isMuted,
            c.isPinned AS isPinned,
            c.pinnedMessageId AS pinnedMessageId,
            c.isTyping AS isTyping,
            c.isArchived AS isArchived,
            c.isVerified AS isVerified,
            c.isAdmin AS isAdmin,
            c.adminsCanPost AS adminsCanPost,
            c.isGroup AS isGroup,
            c.isSubscribed AS isSubscribed,
            c.createdBy AS createdBy,
            c.followers AS followers,
            c.category AS category,
            c.wallpaperUrl AS wallpaperUrl,
            c.customNotificationTone AS customNotificationTone,
            c.mediaVisibility AS mediaVisibility,
            c.disappearingMessagesDuration AS disappearingMessagesDuration,
            c.lastMessageIsFromMe AS lastMessageIsFromMe,
            c.lastMessageRead AS lastMessageRead,
            COALESCE(u.publicKey, c.publicKey) AS publicKey,
            COALESCE(NULLIF(u.phoneNumber, ''), NULLIF(c.phoneNumber, '')) AS phoneNumber,
            c.otherUserId AS otherUserId
        FROM chats c
        LEFT JOIN contacts u ON (
            c.otherUserId = u.id 
            OR c.id = u.id 
            OR (
                c.phoneNumber IS NOT NULL 
                AND u.phoneNumber IS NOT NULL 
                AND LENGTH(c.phoneNumber) >= 9 
                AND LENGTH(u.phoneNumber) >= 9 
                AND SUBSTR(REPLACE(c.phoneNumber, ' ', ''), -9) = SUBSTR(REPLACE(u.phoneNumber, ' ', ''), -9)
            )
        )
        WHERE c.isArchived = 0 AND c.type = :type
        ORDER BY c.isPinned DESC, c.timestamp DESC
    """)
    fun getChatsByType(type: ConversationType): Flow<List<ChatEntity>>

    @Query("""
        SELECT 
            c.id AS id,
            c.type AS type,
            COALESCE(NULLIF(u.displayName, ''), NULLIF(c.senderName, ''), NULLIF(u.phoneNumber, ''), NULLIF(c.phoneNumber, ''), '') AS senderName,
            COALESCE(c.description, '') AS description,
            COALESCE(c.lastMessage, '') AS lastMessage,
            c.timestamp AS timestamp,
            c.unreadCount AS unreadCount,
            c.lastSeen AS lastSeen,
            COALESCE(u.avatarUrl, c.avatarUrl) AS avatarUrl,
            c.isOnline AS isOnline,
            c.isMuted AS isMuted,
            c.isPinned AS isPinned,
            c.pinnedMessageId AS pinnedMessageId,
            c.isTyping AS isTyping,
            c.isArchived AS isArchived,
            c.isVerified AS isVerified,
            c.isAdmin AS isAdmin,
            c.adminsCanPost AS adminsCanPost,
            c.isGroup AS isGroup,
            c.isSubscribed AS isSubscribed,
            c.createdBy AS createdBy,
            c.followers AS followers,
            c.category AS category,
            c.wallpaperUrl AS wallpaperUrl,
            c.customNotificationTone AS customNotificationTone,
            c.mediaVisibility AS mediaVisibility,
            c.disappearingMessagesDuration AS disappearingMessagesDuration,
            c.lastMessageIsFromMe AS lastMessageIsFromMe,
            c.lastMessageRead AS lastMessageRead,
            COALESCE(u.publicKey, c.publicKey) AS publicKey,
            COALESCE(NULLIF(u.phoneNumber, ''), NULLIF(c.phoneNumber, '')) AS phoneNumber,
            c.otherUserId AS otherUserId
        FROM chats c
        LEFT JOIN contacts u ON (
            c.otherUserId = u.id 
            OR c.id = u.id 
            OR (
                c.phoneNumber IS NOT NULL 
                AND u.phoneNumber IS NOT NULL 
                AND LENGTH(c.phoneNumber) >= 9 
                AND LENGTH(u.phoneNumber) >= 9 
                AND SUBSTR(REPLACE(c.phoneNumber, ' ', ''), -9) = SUBSTR(REPLACE(u.phoneNumber, ' ', ''), -9)
            )
        )
        WHERE c.isArchived = 0 AND c.type = :type
        ORDER BY c.isPinned DESC, c.timestamp DESC
    """)
    suspend fun getChatsByTypeOnce(type: ConversationType): List<ChatEntity>

    @Query("""
        SELECT 
            c.id AS id,
            c.type AS type,
            COALESCE(NULLIF(u.displayName, ''), NULLIF(c.senderName, ''), NULLIF(u.phoneNumber, ''), NULLIF(c.phoneNumber, ''), '') AS senderName,
            COALESCE(c.description, '') AS description,
            COALESCE(c.lastMessage, '') AS lastMessage,
            c.timestamp AS timestamp,
            c.unreadCount AS unreadCount,
            c.lastSeen AS lastSeen,
            COALESCE(u.avatarUrl, c.avatarUrl) AS avatarUrl,
            c.isOnline AS isOnline,
            c.isMuted AS isMuted,
            c.isPinned AS isPinned,
            c.pinnedMessageId AS pinnedMessageId,
            c.isTyping AS isTyping,
            c.isArchived AS isArchived,
            c.isVerified AS isVerified,
            c.isAdmin AS isAdmin,
            c.adminsCanPost AS adminsCanPost,
            c.isGroup AS isGroup,
            c.isSubscribed AS isSubscribed,
            c.createdBy AS createdBy,
            c.followers AS followers,
            c.category AS category,
            c.wallpaperUrl AS wallpaperUrl,
            c.customNotificationTone AS customNotificationTone,
            c.mediaVisibility AS mediaVisibility,
            c.disappearingMessagesDuration AS disappearingMessagesDuration,
            c.lastMessageIsFromMe AS lastMessageIsFromMe,
            c.lastMessageRead AS lastMessageRead,
            COALESCE(u.publicKey, c.publicKey) AS publicKey,
            COALESCE(NULLIF(u.phoneNumber, ''), NULLIF(c.phoneNumber, '')) AS phoneNumber,
            c.otherUserId AS otherUserId
        FROM chats c
        LEFT JOIN contacts u ON (
            c.otherUserId = u.id 
            OR c.id = u.id 
            OR (
                c.phoneNumber IS NOT NULL 
                AND u.phoneNumber IS NOT NULL 
                AND LENGTH(c.phoneNumber) >= 9 
                AND LENGTH(u.phoneNumber) >= 9 
                AND SUBSTR(REPLACE(c.phoneNumber, ' ', ''), -9) = SUBSTR(REPLACE(u.phoneNumber, ' ', ''), -9)
            )
        )
        WHERE c.isArchived = 1
        ORDER BY c.timestamp DESC
    """)
    fun getArchivedChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun getChatById(chatId: String): Flow<ChatEntity?>

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun searchChatsById(chatId: String): ChatEntity?

    @Query("SELECT id FROM chats")
    suspend fun getAllChatIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity): Long

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)

    @Query("DELETE FROM chats")
    suspend fun clearChats()

    // ==========================================
    // MESSAGE STREAMS & MUTATIONS
    // ==========================================

    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :chatId 
           OR (
                (SELECT type FROM chats WHERE id = :chatId LIMIT 1) = 'DIRECT'
                AND (
                    conversationId = (SELECT otherUserId FROM chats WHERE id = :chatId LIMIT 1)
                    OR conversationId = (SELECT id FROM chats WHERE otherUserId = :chatId AND type = 'DIRECT' LIMIT 1)
                )
           )
        ORDER BY timestamp ASC
    """)
    fun getMessages(chatId: String): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :chatId 
           OR (
                (SELECT type FROM chats WHERE id = :chatId LIMIT 1) = 'DIRECT'
                AND (
                    conversationId = (SELECT otherUserId FROM chats WHERE id = :chatId LIMIT 1)
                    OR conversationId = (SELECT id FROM chats WHERE otherUserId = :chatId AND type = 'DIRECT' LIMIT 1)
                )
           )
        ORDER BY timestamp DESC
    """)
    fun getMessagesPaging(chatId: String): PagingSource<Int, MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessagesRaw(messages: List<MessageEntity>)

    @Transaction
    suspend fun insertMessages(messages: List<MessageEntity>) {
        messages.forEach { incoming ->
            val existing = getMessageById(incoming.id)
            val mediaUrls = if (incoming.mediaUrls.isNotEmpty()) {
                incoming.mediaUrls
            } else {
                existing?.mediaUrls.orEmpty()
            }
            insertMessage(incoming.copy(mediaUrls = mediaUrls))
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE clientId = :clientId LIMIT 1")
    suspend fun getMessageByClientId(clientId: String): MessageEntity?

    @Query("SELECT conversationId FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getChatIdForMessage(messageId: String): String?

    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :chatId 
           OR (
                (SELECT type FROM chats WHERE id = :chatId LIMIT 1) = 'DIRECT'
                AND (
                    conversationId = (SELECT otherUserId FROM chats WHERE id = :chatId LIMIT 1)
                    OR conversationId = (SELECT id FROM chats WHERE otherUserId = :chatId AND type = 'DIRECT' LIMIT 1)
                )
           )
        ORDER BY timestamp ASC LIMIT 1
    """)
    suspend fun getOldestMessage(chatId: String): MessageEntity?

    @Query("""
        SELECT * FROM messages 
        WHERE conversationId = :chatId 
           OR (
                (SELECT type FROM chats WHERE id = :chatId LIMIT 1) = 'DIRECT'
                AND (
                    conversationId = (SELECT otherUserId FROM chats WHERE id = :chatId LIMIT 1)
                    OR conversationId = (SELECT id FROM chats WHERE otherUserId = :chatId AND type = 'DIRECT' LIMIT 1)
                )
           )
        ORDER BY timestamp DESC LIMIT 1
    """)
    suspend fun getLatestMessage(chatId: String): MessageEntity?

    @Query("SELECT MAX(timestamp) FROM messages WHERE conversationId = :chatId")
    suspend fun getLastServerTimestamp(chatId: String): Long?

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages WHERE conversationId = :chatId")
    suspend fun clearMessages(chatId: String)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()

    // ==========================================
    // ATOMIC RECONCILIATION & OUTBOX
    // ==========================================

    /**
     * Reconciles an optimistic message with the server copy.
     * Prevents list flicker by deleting the client ID ONLY when it differs from the server ID.
     */
    @Transaction
    suspend fun confirmSentMessage(clientId: String, serverMsg: MessageEntity) {
        val existing = getMessageById(clientId) ?: getMessageByClientId(clientId)
        val targetConversationId = existing?.conversationId ?: serverMsg.conversationId
        val targetMediaUrls = if (serverMsg.mediaUrls.isNotEmpty()) {
            serverMsg.mediaUrls
        } else if (!existing?.mediaUrls.isNullOrEmpty()) {
            existing.mediaUrls
        } else if (serverMsg.content.isNotBlank() && (
            serverMsg.content.startsWith("http") ||
            serverMsg.content.startsWith("/") ||
            serverMsg.content.startsWith("file:") ||
            serverMsg.content.startsWith("content:") ||
            serverMsg.content.startsWith("uploads/")
        )) {
            listOf(serverMsg.content)
        } else {
            emptyList()
        }

        if (clientId != serverMsg.id && clientId.isNotBlank()) {
            deleteMessage(clientId)
        }
        val reconciledMsg = serverMsg.copy(
            conversationId = targetConversationId,
            mediaUrls = targetMediaUrls,
            clientId = if (serverMsg.clientId.isNullOrBlank()) clientId else serverMsg.clientId
        )
        insertMessage(reconciledMsg)
    }

    @Query("SELECT * FROM messages WHERE status IN ('PENDING', 'FAILED') ORDER BY timestamp ASC")
    suspend fun getPendingMessages(): List<MessageEntity>

    @Query("UPDATE messages SET status = :status WHERE clientId = :clientId OR id = :clientId")
    suspend fun updateMessageStatus(clientId: String, status: MessageStatus)

    @Query("""
        UPDATE messages 
        SET id = :serverId, timestamp = :serverTimestamp, status = 'SENT' 
        WHERE clientId = :clientId OR id = :clientId
    """)
    suspend fun markMessageSynced(clientId: String, serverId: String, serverTimestamp: Long)

    @Query("UPDATE messages SET conversationId = :newId WHERE conversationId = :oldId")
    suspend fun migrateMessagesConversationId(oldId: String, newId: String)

    // ==========================================
    // READ / DELIVERY RECEIPTS & LAST MESSAGE
    // ==========================================

    @Query("UPDATE chats SET lastMessage = :lastMessage, timestamp = :time, lastMessageIsFromMe = :isFromMe, lastMessageRead = :isRead WHERE id = :chatId OR (type = 'DIRECT' AND otherUserId = :chatId)")
    suspend fun updateLastMessage(chatId: String, lastMessage: String, time: Long, isFromMe: Boolean, isRead: Boolean)

    @Query("UPDATE chats SET unreadCount = :count WHERE id = :chatId OR (type = 'DIRECT' AND otherUserId = :chatId)")
    suspend fun updateUnreadCount(chatId: String, count: Int)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId OR (type = 'DIRECT' AND otherUserId = :chatId)")
    suspend fun clearUnreadCount(chatId: String)

    @Query("UPDATE chats SET unreadCount = unreadCount + 1 WHERE id = :chatId OR (type = 'DIRECT' AND otherUserId = :chatId)")
    suspend fun incrementUnreadCount(chatId: String)

    @Query("""
        UPDATE chats 
        SET unreadCount = (
            SELECT COUNT(*) FROM messages 
            WHERE conversationId = chats.id
            AND isRead = 0 AND isFromMe = 0
        ) 
        WHERE id = :chatId OR (type = 'DIRECT' AND otherUserId = :chatId)
    """)
    suspend fun recalculateUnreadCount(chatId: String)

    @Query("UPDATE chats SET lastMessageRead = 1 WHERE id = :chatId AND lastMessageIsFromMe = 1")
    suspend fun markOutgoingLastMessageAsRead(chatId: String)

    @Query("UPDATE chats SET lastMessageRead = 1 WHERE id = :chatId AND lastMessageIsFromMe = 0")
    suspend fun markIncomingLastMessageAsRead(chatId: String)

    @Query("""
        UPDATE messages 
        SET isRead = 1, status = 'READ' 
        WHERE (
            conversationId = :chatId
            OR (
                (SELECT type FROM chats WHERE id = :chatId LIMIT 1) = 'DIRECT'
                AND conversationId = (SELECT otherUserId FROM chats WHERE id = :chatId LIMIT 1)
            )
        )
        AND isFromMe = 0
    """)
    suspend fun markIncomingAsRead(chatId: String)

    @Query("UPDATE messages SET isRead = 1, status = 'READ' WHERE conversationId = :chatId AND isFromMe = 1")
    suspend fun markOutgoingAsRead(chatId: String)

    @Query("UPDATE messages SET isDelivered = 1, status = 'DELIVERED' WHERE conversationId = :chatId AND isFromMe = 1")
    suspend fun markOutgoingAsDelivered(chatId: String)

    @Query("UPDATE messages SET isDelivered = 1, status = 'DELIVERED' WHERE id = :messageId")
    suspend fun markAsDelivered(messageId: String)

    @Query("UPDATE messages SET isRead = 1, status = 'READ' WHERE id = :messageId")
    suspend fun markAsRead(messageId: String)

    // ==========================================
    // PRESENCE, TYPING & METADATA
    // ==========================================

    @Query("UPDATE chats SET isOnline = :isOnline, lastSeen = :lastSeen WHERE id = :chatId OR (type = 'DIRECT' AND otherUserId = :chatId)")
    suspend fun updatePresence(chatId: String, isOnline: Boolean, lastSeen: Long)

    @Query("UPDATE chats SET isTyping = :isTyping WHERE id = :chatId")
    suspend fun updateTyping(chatId: String, isTyping: Boolean)

    @Query("UPDATE chats SET isPinned = :isPinned WHERE id = :chatId")
    suspend fun updatePinned(chatId: String, isPinned: Boolean)

    @Query("UPDATE chats SET pinnedMessageId = :messageId WHERE id = :chatId")
    suspend fun updatePinnedMessage(chatId: String, messageId: String?)

    @Query("SELECT id FROM messages WHERE isStarred = 1")
    suspend fun getStarredMessageIds(): List<String>

    @Query("UPDATE chats SET isMuted = :isMuted WHERE id = :chatId")
    suspend fun updateMuted(chatId: String, isMuted: Boolean)

    @Query("UPDATE chats SET isArchived = :isArchived WHERE id = :chatId")
    suspend fun updateArchived(chatId: String, isArchived: Boolean)

    @Query("UPDATE chats SET isSubscribed = :isSubscribed WHERE id = :chatId")
    suspend fun updateSubscribed(chatId: String, isSubscribed: Boolean)

    @Query("UPDATE chats SET adminsCanPost = :allowed WHERE id = :chatId")
    suspend fun updateAdminsCanPost(chatId: String, allowed: Boolean)

    @Query("UPDATE chats SET isAdmin = :isAdmin WHERE id = :chatId")
    suspend fun updateAdmin(chatId: String, isAdmin: Boolean)

    @Query("UPDATE chats SET wallpaperUrl = :wallpaperUrl WHERE id = :chatId")
    suspend fun updateWallpaper(chatId: String, wallpaperUrl: String?)

    @Query("UPDATE chats SET customNotificationTone = :tone WHERE id = :chatId")
    suspend fun updateCustomNotificationTone(chatId: String, tone: String?)

    @Query("UPDATE chats SET mediaVisibility = :visible WHERE id = :chatId")
    suspend fun updateMediaVisibility(chatId: String, visible: Boolean)

    @Query("UPDATE chats SET disappearingMessagesDuration = :duration WHERE id = :chatId")
    suspend fun updateDisappearingMessages(chatId: String, duration: Int)

    @Query("UPDATE chats SET avatarUrl = :avatarUrl WHERE id = :chatId")
    suspend fun updateAvatar(chatId: String, avatarUrl: String?)

    @Query("UPDATE messages SET reactions = :reactions WHERE id = :messageId")
    suspend fun updateMessageReactions(messageId: String, reactions: Map<String, Int>)

    @Query("UPDATE messages SET content = :newContent WHERE id = :messageId")
    suspend fun updateMessageContent(messageId: String, newContent: String)

    @Query("UPDATE messages SET isStarred = :isStarred WHERE id = :messageId")
    suspend fun updateMessageStarred(messageId: String, isStarred: Boolean)

    // ==========================================
    // EXPIRATION, MEDIA & SEARCH QUERIES
    // ==========================================

    @Query("DELETE FROM messages WHERE conversationId = :chatId AND timestamp < :timestamp")
    suspend fun deleteMessagesBefore(chatId: String, timestamp: Long)

    @Query("SELECT * FROM chats WHERE disappearingMessagesDuration > 0")
    suspend fun getChatsWithDisappearingMessages(): List<ChatEntity>

    @Query("SELECT * FROM messages WHERE isStarred = 1 ORDER BY timestamp DESC")
    fun getStarredMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE type IN ('IMAGE', 'VIDEO', 'DOCUMENT', 'AUDIO_MESSAGE') ORDER BY timestamp DESC")
    fun getAllFiles(): Flow<List<MessageEntity>>

    @Query("""
        SELECT * FROM messages 
        WHERE id IN (SELECT rowid FROM messages_fts WHERE messages_fts MATCH :query) 
        ORDER BY timestamp DESC
    """)
    fun searchMessagesAcrossAll(query: String): Flow<List<MessageEntity>>
}
