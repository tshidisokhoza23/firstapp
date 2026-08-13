package com.example.firstapp.chat.domain.repository

import com.example.firstapp.common.util.Resource
import com.example.firstapp.chat.model.Advert
import kotlinx.coroutines.flow.Flow

interface AdRepository {
    fun getAdverts(): Flow<Resource<List<Advert>>>
    fun getViewedAdverts(): Flow<Resource<List<Advert>>>
    fun markAdvertViewed(advertId: String): Flow<Resource<Boolean>>
    fun trackAdvertClick(advertId: String): Flow<Resource<Boolean>>
    fun trackAdvertShare(advertId: String): Flow<Resource<Boolean>>
    fun toggleLikeAdvert(advertId: String): Flow<Resource<Boolean>>
    fun resetAds(): Flow<Resource<Unit>>
    suspend fun clearLocalAdverts()
}
