package com.example.firstapp.common.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.firstapp.common.data.local.entity.PostRemoteKeys

@Dao
interface PostRemoteKeysDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteKey: List<PostRemoteKeys>)

    @Query("SELECT * FROM post_remote_keys WHERE postId = :postId AND feedType = :feedType")
    suspend fun remoteKeysPostId(postId: String, feedType: String): PostRemoteKeys?

    @Query("DELETE FROM post_remote_keys WHERE feedType = :feedType")
    suspend fun clearRemoteKeys(feedType: String)
}
