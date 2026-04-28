package com.example.firstapp.vocab.domain.repository

import com.example.firstapp.vocab.model.Lesson
import kotlinx.coroutines.flow.Flow

interface LessonRepository {
    fun getLessons(): Flow<List<Lesson>>
    suspend fun getLessonById(id: String): Lesson?
    suspend fun completeLesson(userId: Int, lessonId: String, score: Int)
    suspend fun getMissedItems(userId: Int): List<String>
}
