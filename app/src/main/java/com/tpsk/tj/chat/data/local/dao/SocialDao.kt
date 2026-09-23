package com.tpsk.tj.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.tpsk.tj.chat.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SocialDao {

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
        LEFT JOIN contacts u ON (c.otherUserId = u.id OR c.id = u.id)
        WHERE c.isArchived = 0 AND (c.type = 'JOB_CHAT' OR c.type = 'DIRECT')
        ORDER BY c.isPinned DESC, c.timestamp DESC
    """)
    fun getSocialChats(): Flow<List<ChatEntity>>
}
