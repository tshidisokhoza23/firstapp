package com.tpsk.tj.vocab.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lesson_progress", primaryKeys = ["userId", "lessonId"])
data class LessonProgressEntity(
    val userId: String,
    val lessonId: String,
    val isCompleted: Boolean,
    val score: Int,
    val lastAttemptTimestamp: Long
)
