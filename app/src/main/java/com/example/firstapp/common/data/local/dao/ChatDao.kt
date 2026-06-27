package com.example.firstapp.common.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.firstapp.common.data.local.entity.ChatEntity
import com.example.firstapp.common.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats WHERE isArchived = 0 ORDER BY isPinned DESC, timestamp DESC")
    fun getChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isArchived = 1 ORDER BY timestamp DESC")
    fun getArchivedChats(): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Query("DELETE FROM chats")
    suspend fun clearChats()

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()

    @Query("SELECT id FROM chats")
    suspend fun getAllChatIds(): List<Int>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessages(chatId: Int): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearMessages(chatId: Int)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: Int)

    @Query("UPDATE messages SET isStarred = :isStarred WHERE id = :messageId")
    suspend fun starMessage(messageId: String, isStarred: Boolean)

    @Query("SELECT * FROM messages WHERE clientId = :clientId LIMIT 1")
    suspend fun getMessageByClientId(clientId: String): MessageEntity?

    @Query("DELETE FROM messages WHERE clientId = :clientId")
    suspend fun deleteMessageByClientId(clientId: String)

    @Query("SELECT * FROM messages WHERE isStarred = 1 ORDER BY timestamp DESC")
    fun getStarredMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND LOWER(content) LIKE '%' || LOWER(:query) || '%' ORDER BY timestamp DESC")
    fun searchMessages(chatId: Int, query: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE LOWER(content) LIKE '%' || LOWER(:query) || '%' ORDER BY timestamp DESC")
    fun searchMessagesAcrossAll(query: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM chats WHERE LOWER(senderName) LIKE '%' || LOWER(:query) || '%' ORDER BY timestamp DESC")
    fun searchChats(query: String): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun searchChatsById(chatId: Int): ChatEntity?

    @Query("SELECT DISTINCT chatId FROM messages")
    suspend fun getAllChatIdsFromMessages(): List<Int>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastMessageForChat(chatId: Int): MessageEntity?

    @Query("UPDATE chats SET isOnline = :isOnline, lastSeen = :lastSeen WHERE id = :chatId")
    suspend fun updatePresence(chatId: Int, isOnline: Boolean, lastSeen: Long)

    @Query("UPDATE chats SET isTyping = :isTyping WHERE id = :chatId")
    suspend fun updateTyping(chatId: Int, isTyping: Boolean)

    @Query("UPDATE chats SET lastMessage = :lastMessage, timestamp = :time, lastMessageIsFromMe = :isFromMe, lastMessageRead = :isRead WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: Int, lastMessage: String, time: Long, isFromMe: Boolean, isRead: Boolean)

    @Query("UPDATE chats SET wallpaperUrl = :wallpaperUrl WHERE id = :chatId")
    suspend fun updateWallpaper(chatId: Int, wallpaperUrl: String?)

    @Query("UPDATE chats SET customNotificationTone = :tone WHERE id = :chatId")
    suspend fun updateCustomNotificationTone(chatId: Int, tone: String?)

    @Query("UPDATE chats SET mediaVisibility = :visible WHERE id = :chatId")
    suspend fun updateMediaVisibility(chatId: Int, visible: Boolean)

    @Query("UPDATE chats SET disappearingMessagesDuration = :duration WHERE id = :chatId")
    suspend fun updateDisappearingMessages(chatId: Int, duration: Int)

    @Query("UPDATE chats SET isMuted = :isMuted WHERE id = :chatId")
    suspend fun updateMuted(chatId: Int, isMuted: Boolean)

    @Query("UPDATE chats SET isPinned = :isPinned WHERE id = :chatId")
    suspend fun updatePinned(chatId: Int, isPinned: Boolean)

    @Query("UPDATE chats SET isArchived = :isArchived WHERE id = :chatId")
    suspend fun updateArchived(chatId: Int, isArchived: Boolean)

    @Query("UPDATE chats SET unreadCount = :count WHERE id = :chatId")
    suspend fun updateUnreadCount(chatId: Int, count: Int)

    @Query("UPDATE chats SET unreadCount = unreadCount + 1 WHERE id = :chatId")
    suspend fun incrementUnreadCount(chatId: Int)

    @Query("UPDATE chats SET lastMessageRead = 1 WHERE id = :chatId AND lastMessageIsFromMe = 1")
    suspend fun markOutgoingLastMessageAsRead(chatId: Int)

    @Query("UPDATE chats SET lastMessageRead = 1 WHERE id = :chatId AND lastMessageIsFromMe = 0")
    suspend fun markIncomingLastMessageAsRead(chatId: Int)

    @Query("UPDATE messages SET isRead = 1, status = 'READ' WHERE chatId = :chatId AND isFromMe = 0")
    suspend fun markIncomingAsRead(chatId: Int)

    @Query("UPDATE messages SET isRead = 1, status = 'READ' WHERE chatId = :chatId AND isFromMe = 1")
    suspend fun markOutgoingAsRead(chatId: Int)

    @Query("UPDATE messages SET isDelivered = 1, status = 'DELIVERED' WHERE id = :messageId")
    suspend fun markAsDelivered(messageId: String)

    @Query("UPDATE messages SET reactions = :reactions WHERE id = :messageId")
    suspend fun updateMessageReactions(messageId: String, reactions: Map<String, Int>)

    @Query("SELECT * FROM messages WHERE status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingMessages(): List<MessageEntity>

    @Query("SELECT chatId FROM messages WHERE id = :messageId")
    suspend fun getChatIdForMessage(messageId: String): Int?

    @Query("SELECT * FROM messages WHERE type IN ('IMAGE', 'VIDEO', 'DOCUMENT') ORDER BY timestamp DESC")
    fun getAllFiles(): Flow<List<MessageEntity>>

    @Query("SELECT publicKey FROM chats WHERE id = :chatId")
    suspend fun getChatPublicKey(chatId: Int): String?

    @Query("UPDATE chats SET publicKey = :publicKey WHERE id = :chatId")
    suspend fun updatePublicKey(chatId: Int, publicKey: String)
}
