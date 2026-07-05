package com.example.firstapp.common.data.api

import com.example.firstapp.media.data.api.ApiPostAuthor

data class CreateShortRequest(
    val description: String,
    val video_url: String,
    val thumbnail_url: String
)

data class CreateShortResponse(
    val id: String
)

data class CreateShortReplyRequest(
    val content: String,
    val parent_reply_id: String? = null
)

data class ApiShortReply(
    val id: String,
    val author_id: String,
    val short_id: String,
    val content: String,
    val created_at: String?,
    val likes: Long,
    val is_liked: Boolean,
    val author: ApiPostAuthor?
)

data class ShortRepliesResponse(
    val replies: List<ApiShortReply>
)
