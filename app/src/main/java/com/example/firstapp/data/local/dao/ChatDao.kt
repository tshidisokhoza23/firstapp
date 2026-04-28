package com.example.firstapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.firstapp.data.local.entity.ChatEntity
import com.example.firstapp.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, time DESC")
    fun getChats(): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Query("DELETE FROM chats")
    suspend fun clearChats()

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY time ASC")
    fun getMessages(chatId: Int): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearMessages(chatId: Int)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: Int)

    @Query("UPDATE messages SET isStarred = :isStarred WHERE id = :messageId")
    suspend fun starMessage(messageId: Int, isStarred: Boolean)

    @Query("SELECT * FROM messages WHERE isStarred = 1 ORDER BY time DESC")
    fun getStarredMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND LOWER(text) LIKE '%' || LOWER(:query) || '%' ORDER BY time DESC")
    fun searchMessages(chatId: Int, query: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE LOWER(text) LIKE '%' || LOWER(:query) || '%' ORDER BY time DESC")
    fun searchMessagesAcrossAll(query: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM chats WHERE LOWER(senderName) LIKE '%' || LOWER(:query) || '%' ORDER BY time DESC")
    fun searchChats(query: String): Flow<List<ChatEntity>>

    @Query("UPDATE chats SET isOnline = :isOnline, time = :lastSeen WHERE id = :chatId")
    suspend fun updatePresence(chatId: Int, isOnline: Boolean, lastSeen: String)

    @Query("UPDATE chats SET isTyping = :isTyping WHERE id = :chatId")
    suspend fun updateTyping(chatId: Int, isTyping: Boolean)

    @Query("UPDATE chats SET lastMessage = :lastMessage, time = :time WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: Int, lastMessage: String, time: String)

    @Query("UPDATE chats SET wallpaperUrl = :wallpaperUrl WHERE id = :chatId")
    suspend fun updateWallpaper(chatId: Int, wallpaperUrl: String?)
}
