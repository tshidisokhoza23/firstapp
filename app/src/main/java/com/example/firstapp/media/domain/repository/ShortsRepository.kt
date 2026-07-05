package com.example.firstapp.media.domain.repository

import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.model.ShortMedia
import com.example.firstapp.media.model.ShortReply
import com.example.firstapp.media.data.api.ShortLikeResponse
import com.example.firstapp.media.data.api.ShortSubscribeResponse
import kotlinx.coroutines.flow.Flow

interface ShortsRepository {
    fun getShorts(): Flow<Resource<List<ShortMedia>>>
    fun getUserShorts(userId: String): Flow<Resource<List<ShortMedia>>>
    fun searchShorts(query: String): Flow<Resource<List<ShortMedia>>>
    fun createShort(description: String, videoUrl: String, thumbnailUrl: String): Flow<Resource<String>>
    fun repostShort(shortId: String): Flow<Resource<Boolean>>
    fun bookmarkShort(shortId: String): Flow<Resource<Boolean>>
    fun trackShortShare(shortId: String): Flow<Resource<Boolean>>
    fun replyToShort(shortId: String, content: String, parentReplyId: String? = null): Flow<Resource<Boolean>>
    fun getShortReplies(shortId: String): Flow<Resource<List<ShortReply>>>
    fun likeShort(shortId: String): Flow<Resource<ShortLikeResponse>>
    fun likeShortReply(replyId: String): Flow<Resource<Boolean>>
    fun subscribeToShortCreator(shortId: String): Flow<Resource<ShortSubscribeResponse>>
}
