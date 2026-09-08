package com.example.firstapp.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.firstapp.chat.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY followers DESC")
    fun getChannels(): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Query("DELETE FROM channels")
    suspend fun clearChannels()

    @androidx.room.Transaction
    suspend fun replaceChannels(channels: List<ChannelEntity>) {
        clearChannels()
        insertChannels(channels)
    }

    @Query("UPDATE channels SET isSubscribed = :isSubscribed WHERE id = :channelId")
    suspend fun updateSubscription(channelId: String, isSubscribed: Boolean)

    @Query("UPDATE channels SET conversationId = :conversationId, isSubscribed = 1 WHERE id = :channelId")
    suspend fun updateConversationMetadata(channelId: String, conversationId: String)

    @Query("UPDATE channels SET isMuted = :isMuted WHERE id = :channelId")
    suspend fun updateMuted(channelId: String, isMuted: Boolean)

    @Query("SELECT id FROM channels WHERE isSubscribed = 1")
    suspend fun getSubscribedChannelIds(): List<String>

    @Query("SELECT * FROM channels WHERE id = :query OR conversationId = :query OR LOWER(name) LIKE '%' || LOWER(:query) || '%' OR LOWER(description) LIKE '%' || LOWER(:query) || '%' ORDER BY followers DESC")
    fun searchChannels(query: String): Flow<List<ChannelEntity>>
}
