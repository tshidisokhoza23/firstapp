package com.tpsk.tj.vocab.domain.repository

import com.tpsk.tj.vocab.model.Lesson
import kotlinx.coroutines.flow.Flow

interface LessonRepository {
    fun getLessons(): Flow<List<Lesson>>
    suspend fun getLessonById(id: String): Lesson?
    suspend fun completeLesson(userId: String, lessonId: String, score: Int)
    suspend fun getMissedItems(userId: String): List<String>
}
