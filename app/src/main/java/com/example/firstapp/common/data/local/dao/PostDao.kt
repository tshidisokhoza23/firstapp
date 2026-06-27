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
    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getPostsPaging(): PagingSource<Int, PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("DELETE FROM posts")
    suspend fun clearPosts()

    @Query("UPDATE posts SET likes = :likes, isLiked = :isLiked WHERE id = :postId")
    suspend fun updateLikeStatus(postId: Int, likes: String, isLiked: Boolean)
}
