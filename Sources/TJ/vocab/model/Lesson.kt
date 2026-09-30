package com.tpsk.tj.vocab.model

data class Lesson(
    val id: String = "",
    val titleTranslations: Map<String, String> = emptyMap(),
    val language: String = "Spanish",
    val categoryTranslations: Map<String, String> = emptyMap(),
    val difficulty: String = "Beginner",
    val content: List<LessonContent> = emptyList(),
    val isCompleted: Boolean = false
)

data class LessonContent(
    val type: ContentType = ContentType.QUESTION,
    val translations: Map<String, String> = emptyMap(),
    val options: List<String> = emptyList(),
    val correctAnswerIndex: Int = 0,
    val explanation: String = "",
    val audioText: String = "",
    val phonetic: String = "",
    val partOfSpeech: String = ""
)

enum class ContentType {
    TEACH,
    EXAMPLE,
    QUESTION,
    SPEAKING
}
