package com.example.firstapp.media.domain.repository

import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.model.ShortMedia
import com.example.firstapp.media.model.ShortReply
import com.example.firstapp.media.data.api.ShortLikeResponse
import com.example.firstapp.media.data.api.ShortSubscribeResponse
import kotlinx.coroutines.flow.Flow

interface ShortsRepository {
    fun getShorts(): Flow<Resource<List<ShortMedia>>>
    fun searchShorts(query: String): Flow<Resource<List<ShortMedia>>>
    fun createShort(description: String, videoUrl: String, thumbnailUrl: String): Flow<Resource<Long>>
    fun repostShort(shortId: Int): Flow<Resource<Boolean>>
    fun bookmarkShort(shortId: Int): Flow<Resource<Boolean>>
    fun replyToShort(shortId: Int, content: String): Flow<Resource<Boolean>>
    fun getShortReplies(shortId: Int): Flow<Resource<List<ShortReply>>>
    fun likeShort(shortId: Int): Flow<Resource<ShortLikeResponse>>
    fun subscribeToShortCreator(shortId: Int): Flow<Resource<ShortSubscribeResponse>>
}
