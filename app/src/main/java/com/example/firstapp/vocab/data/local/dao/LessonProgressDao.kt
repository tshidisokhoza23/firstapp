package com.example.firstapp.vocab.data.local.dao

import androidx.room.*
import com.example.firstapp.vocab.data.local.entity.LessonProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonProgressDao {
    @Query("SELECT * FROM lesson_progress WHERE userId = :userId")
    fun getAllProgress(userId: String): Flow<List<LessonProgressEntity>>

    @Query("SELECT * FROM lesson_progress WHERE userId = :userId AND lessonId = :lessonId")
    suspend fun getProgress(userId: String, lessonId: String): LessonProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: LessonProgressEntity)

    @Query("UPDATE lesson_progress SET isCompleted = 1, score = :score, lastAttemptTimestamp = :timestamp WHERE userId = :userId AND lessonId = :lessonId")
    suspend fun markCompleted(userId: String, lessonId: String, score: Int, timestamp: Long)
}
