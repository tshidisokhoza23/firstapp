package com.tpsk.tj.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.tpsk.tj.chat.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MingleDao {

    @Query("""
        SELECT 
            c.id AS id,
            'MINGLE' AS type,
            COALESCE(NULLIF(c.senderName, ''), 'Mingle User') AS senderName,
            COALESCE(c.description, '') AS description,
            COALESCE(c.lastMessage, '') AS lastMessage,
            c.timestamp AS timestamp,
            c.unreadCount AS unreadCount,
            c.lastSeen AS lastSeen,
            c.avatarUrl AS avatarUrl,
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
            c.publicKey AS publicKey,
            NULL AS phoneNumber,
            c.otherUserId AS otherUserId
        FROM chats c
        WHERE c.isArchived = 0 AND (c.type = 'MINGLE' OR c.type = 'mingle' OR c.id LIKE 'mingle_%')
        ORDER BY c.isPinned DESC, c.timestamp DESC
    """)
    fun getMingleChats(): Flow<List<ChatEntity>>

    @Query("""
        SELECT 
            c.id AS id,
            'MINGLE' AS type,
            COALESCE(NULLIF(c.senderName, ''), 'Mingle User') AS senderName,
            COALESCE(c.description, '') AS description,
            COALESCE(c.lastMessage, '') AS lastMessage,
            c.timestamp AS timestamp,
            c.unreadCount AS unreadCount,
            c.lastSeen AS lastSeen,
            c.avatarUrl AS avatarUrl,
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
            c.publicKey AS publicKey,
            NULL AS phoneNumber,
            c.otherUserId AS otherUserId
        FROM chats c
        WHERE c.isArchived = 0 AND (c.type = 'MINGLE' OR c.type = 'mingle' OR c.id LIKE 'mingle_%')
        ORDER BY c.isPinned DESC, c.timestamp DESC
    """)
    suspend fun getMingleChatsOnce(): List<ChatEntity>
}
