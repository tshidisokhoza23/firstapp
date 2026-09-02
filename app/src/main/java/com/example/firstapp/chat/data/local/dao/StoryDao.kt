package com.example.firstapp.chat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.firstapp.chat.data.local.entity.StoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoryDao {
    @Query("SELECT * FROM stories")
    fun getStories(): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<StoryEntity>)

    @Query("SELECT * FROM stories WHERE userId = :userId")
    fun getStoriesByUserId(userId: String): Flow<StoryEntity?>

    @Query("DELETE FROM stories")
    suspend fun clearStories()

    /**
     * Atomically replace all cached stories so the Room flow never observes an
     * empty intermediate state (which caused stories to "show then disappear"
     * when a re-fetch was cancelled between clear and insert).
     */
    @Transaction
    suspend fun replaceStories(stories: List<StoryEntity>) {
        clearStories()
        insertStories(stories)
    }

    @Query("DELETE FROM stories WHERE lastUpdatedMillis < :threshold")
    suspend fun pruneStories(threshold: Long)
}
