package com.example.firstapp.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.firstapp.chat.data.local.entity.ChatEntity
import com.example.firstapp.chat.data.local.entity.MessageEntity
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
    suspend fun getAllChatIds(): List<String>

    @Query("SELECT * FROM messages WHERE conversationId = :chatId ORDER BY timestamp ASC")
    fun getMessages(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :chatId ORDER BY timestamp DESC")
    fun getMessagesPaging(chatId: String): androidx.paging.PagingSource<Int, MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE conversationId = :chatId")
    suspend fun clearMessages(chatId: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)

    @Query("UPDATE messages SET reactions = :reactions WHERE id = :messageId")
    suspend fun updateMessageReactions(messageId: String, reactions: Map<String, Int>)

    @Query("SELECT * FROM messages WHERE clientId = :clientId LIMIT 1")
    suspend fun getMessageByClientId(clientId: String): MessageEntity?

    @Query("SELECT * FROM chats WHERE id = :chatId")
    suspend fun searchChatsById(chatId: String): ChatEntity?

    @Query("UPDATE chats SET isOnline = :isOnline, lastSeen = :lastSeen WHERE id = :chatId OR otherUserId = :chatId")
    suspend fun updatePresence(chatId: String, isOnline: Boolean, lastSeen: Long)

    @Query("UPDATE chats SET isTyping = :isTyping WHERE id = :chatId")
    suspend fun updateTyping(chatId: String, isTyping: Boolean)

    @Query("UPDATE chats SET lastMessage = :lastMessage, timestamp = :time, lastMessageIsFromMe = :isFromMe, lastMessageRead = :isRead WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, lastMessage: String, time: Long, isFromMe: Boolean, isRead: Boolean)

    @Query("UPDATE chats SET unreadCount = :count WHERE id = :chatId")
    suspend fun updateUnreadCount(chatId: String, count: Int)

    @Query("UPDATE chats SET unreadCount = unreadCount + 1 WHERE id = :chatId")
    suspend fun incrementUnreadCount(chatId: String)

    @Query("UPDATE chats SET lastMessageRead = 1 WHERE id = :chatId AND lastMessageIsFromMe = 1")
    suspend fun markOutgoingLastMessageAsRead(chatId: String)

    @Query("UPDATE chats SET lastMessageRead = 1 WHERE id = :chatId AND lastMessageIsFromMe = 0")
    suspend fun markIncomingLastMessageAsRead(chatId: String)

    @Query("UPDATE messages SET isRead = 1, status = 'READ' WHERE conversationId = :chatId AND isFromMe = 0")
    suspend fun markIncomingAsRead(chatId: String)

    @Query("UPDATE messages SET isRead = 1, status = 'READ' WHERE conversationId = :chatId AND isFromMe = 1")
    suspend fun markOutgoingAsRead(chatId: String)

    @Query("UPDATE messages SET isDelivered = 1, status = 'DELIVERED' WHERE id = :messageId")
    suspend fun markAsDelivered(messageId: String)

    @Query("UPDATE messages SET isRead = 1, status = 'READ' WHERE id = :messageId")
    suspend fun markAsRead(messageId: String)

    @Query("SELECT conversationId FROM messages WHERE id = :messageId")
    suspend fun getChatIdForMessage(messageId: String): String?

    @Query("SELECT * FROM messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @androidx.room.Transaction
    suspend fun confirmSentMessage(clientId: String, serverMsg: MessageEntity) {
        deleteMessage(clientId)
        insertMessages(listOf(serverMsg))
    }
    
    @Query("UPDATE chats SET isPinned = :isPinned WHERE id = :chatId")
    suspend fun updatePinned(chatId: String, isPinned: Boolean)

    @Query("UPDATE chats SET isMuted = :isMuted WHERE id = :chatId")
    suspend fun updateMuted(chatId: String, isMuted: Boolean)

    @Query("UPDATE chats SET isArchived = :isArchived WHERE id = :chatId")
    suspend fun updateArchived(chatId: String, isArchived: Boolean)

    @Query("UPDATE chats SET wallpaperUrl = :wallpaperUrl WHERE id = :chatId")
    suspend fun updateWallpaper(chatId: String, wallpaperUrl: String?)

    @Query("UPDATE chats SET customNotificationTone = :tone WHERE id = :chatId")
    suspend fun updateCustomNotificationTone(chatId: String, tone: String?)

    @Query("UPDATE chats SET mediaVisibility = :visible WHERE id = :chatId")
    suspend fun updateMediaVisibility(chatId: String, visible: Boolean)

    @Query("UPDATE chats SET disappearingMessagesDuration = :duration WHERE id = :chatId")
    suspend fun updateDisappearingMessages(chatId: String, duration: Int)
    
    @Query("SELECT * FROM messages WHERE id IN (SELECT rowid FROM messages_fts WHERE messages_fts MATCH :query) ORDER BY timestamp DESC")
    fun searchMessagesAcrossAll(query: String): Flow<List<MessageEntity>>

    @Query("DELETE FROM messages WHERE conversationId = :chatId AND id NOT IN (SELECT id FROM messages WHERE conversationId = :chatId ORDER BY timestamp DESC LIMIT :limit)")
    suspend fun pruneMessages(chatId: String, limit: Int)

    @Query("SELECT * FROM chats WHERE id = :chatId")
    fun getChatById(chatId: String): Flow<ChatEntity?>
}
