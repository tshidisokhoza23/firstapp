package com.example.firstapp.vocab.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lesson_progress", primaryKeys = ["userId", "lessonId"])
data class LessonProgressEntity(
    val userId: Int,
    val lessonId: String,
    val isCompleted: Boolean,
    val score: Int,
    val lastAttemptTimestamp: Long
)
