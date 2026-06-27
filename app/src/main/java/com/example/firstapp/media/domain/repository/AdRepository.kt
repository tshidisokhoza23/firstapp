package com.example.firstapp.media.domain.repository

import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.model.Advert
import kotlinx.coroutines.flow.Flow

interface AdRepository {
    fun getAdverts(): Flow<Resource<List<Advert>>>
    fun getViewedAdverts(): Flow<Resource<List<Advert>>>
    fun markAdvertViewed(advertId: Int): Flow<Resource<Boolean>>
    suspend fun clearLocalAdverts()
}
