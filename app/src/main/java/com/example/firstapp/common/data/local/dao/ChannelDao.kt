package com.example.firstapp.common.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.firstapp.common.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY CAST(followers AS INTEGER) DESC")
    fun getChannels(): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Query("DELETE FROM channels")
    suspend fun clearChannels()

    @Query("UPDATE channels SET isSubscribed = :isSubscribed WHERE id = :channelId")
    suspend fun updateSubscription(channelId: Int, isSubscribed: Boolean)

    @Query("UPDATE channels SET isMuted = :isMuted WHERE id = :channelId")
    suspend fun updateMuted(channelId: Int, isMuted: Boolean)

    @Query("SELECT * FROM channels WHERE LOWER(name) LIKE '%' || LOWER(:query) || '%' OR LOWER(description) LIKE '%' || LOWER(:query) || '%' ORDER BY CAST(followers AS INTEGER) DESC")
    fun searchChannels(query: String): Flow<List<ChannelEntity>>
}
