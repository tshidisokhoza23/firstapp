package com.tpsk.tj.media.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tpsk.tj.media.data.local.entity.PostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts WHERE feedType = :feedType ORDER BY timestamp DESC")
    fun getPosts(feedType: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE feedType = :feedType ORDER BY timestamp DESC")
    fun getPostsPaging(feedType: String): PagingSource<Int, PostEntity>

    @Query("SELECT * FROM posts WHERE feedType = :feedType")
    suspend fun getPostsList(feedType: String): List<PostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("DELETE FROM posts WHERE feedType = :feedType")
    suspend fun clearPosts(feedType: String)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    @Query("DELETE FROM posts")
    suspend fun clearAllPosts()

    @Query("UPDATE posts SET likes = :likes, isLiked = :isLiked WHERE id = :postId")
    suspend fun updateLikeStatus(postId: String, likes: Long, isLiked: Boolean)

    @Query("UPDATE posts SET likes = :likes WHERE id = :postId")
    suspend fun updateLikeCountOnly(postId: String, likes: Long)

    @Query("UPDATE posts SET isReposted = :isReposted, repostCount = :repostCount WHERE id = :postId")
    suspend fun updateRepostStatus(postId: String, repostCount: Long, isReposted: Boolean)

    @Query("UPDATE posts SET repostCount = :repostCount WHERE id = :postId")
    suspend fun updateRepostCountOnly(postId: String, repostCount: Long)

    @Query("UPDATE posts SET shares = :shares WHERE id = :postId")
    suspend fun updateShareStatus(postId: String, shares: Long)

    @Query("UPDATE posts SET views = :views WHERE id = :postId")
    suspend fun updateViewCount(postId: String, views: Long)

    @Query("UPDATE posts SET repliesCount = :repliesCount WHERE id = :postId")
    suspend fun updateRepliesCount(postId: String, repliesCount: Long)

    @Query("UPDATE posts SET repliesCount = repliesCount + 1 WHERE id = :postId")
    suspend fun incrementRepliesCount(postId: String)

    @Query("UPDATE posts SET isBookmarked = :isBookmarked WHERE id = :postId")
    suspend fun updateBookmarkStatus(postId: String, isBookmarked: Boolean)

    @Query("UPDATE posts SET repliesLocked = :repliesLocked WHERE id = :postId")
    suspend fun updateRepliesLockStatus(postId: String, repliesLocked: Boolean)

    @Query("UPDATE posts SET isAuthorFollowed = :isFollowed WHERE authorId = :authorId")
    suspend fun updateAuthorFollowStatus(authorId: String, isFollowed: Boolean)

    @Query("UPDATE posts SET views = views + 1 WHERE id = :postId")
    suspend fun incrementViewCount(postId: String)

    @Query("SELECT * FROM posts WHERE content LIKE :query OR authorName LIKE :query ORDER BY timestamp DESC")
    fun searchPostsInCache(query: String): Flow<List<PostEntity>>
}
