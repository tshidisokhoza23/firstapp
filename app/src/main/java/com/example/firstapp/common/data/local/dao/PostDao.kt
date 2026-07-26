package com.example.firstapp.common.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.firstapp.common.data.local.entity.PostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts WHERE feedType = :feedType ORDER BY timestamp DESC")
    fun getPosts(feedType: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE feedType = :feedType ORDER BY timestamp DESC")
    fun getPostsPaging(feedType: String): PagingSource<Int, PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("DELETE FROM posts WHERE feedType = :feedType")
    suspend fun clearPosts(feedType: String)

    @Query("UPDATE posts SET likes = :likes, isLiked = :isLiked WHERE id = :postId")
    suspend fun updateLikeStatus(postId: String, likes: Long, isLiked: Boolean)

    @Query("UPDATE posts SET isReposted = :isReposted, shares = :shares WHERE id = :postId")
    suspend fun updateRepostStatus(postId: String, shares: Long, isReposted: Boolean)

    @Query("UPDATE posts SET isBookmarked = :isBookmarked WHERE id = :postId")
    suspend fun updateBookmarkStatus(postId: String, isBookmarked: Boolean)
}
