package com.example.firstapp.common.data.api

import com.example.firstapp.media.data.api.ApiPostAuthor

data class CreateShortRequest(
    val description: String,
    val video_url: String,
    val thumbnail_url: String
)

data class CreateShortResponse(
    val id: Long
)

data class CreateShortReplyRequest(
    val content: String
)

data class ApiShortReply(
    val id: Int,
    val author_id: Int,
    val short_id: Int,
    val content: String,
    val created_at: String?,
    val likes: Long,
    val is_liked: Boolean,
    val author: ApiPostAuthor?
)

data class ShortRepliesResponse(
    val replies: List<ApiShortReply>
)
