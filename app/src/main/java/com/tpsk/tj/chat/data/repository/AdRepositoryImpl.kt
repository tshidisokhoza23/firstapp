package com.tpsk.tj.chat.data.repository

import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.media.data.api.*
import com.tpsk.tj.chat.domain.repository.AdRepository
import com.tpsk.tj.chat.rewards.data.RewardsManager
import com.tpsk.tj.chat.model.Advert
import com.tpsk.tj.chat.data.local.dao.AdvertDao
import com.tpsk.tj.chat.data.local.entity.AdvertEntity
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class AdRepositoryImpl @Inject constructor(
    private val mediaApi: MediaApi,
    private val advertDao: AdvertDao
) : AdRepository {

    override fun getAdverts(): Flow<Resource<List<Advert>>> = flow {
        val cached = advertDao.getAdverts().first().map { it.toModel() }
        emit(Resource.Loading(cached))
        try {
            val response = mediaApi.getAdverts()
            if (response.isSuccessful) {
                val apiAdverts = response.body()?.adverts ?: emptyList()
                val entities = apiAdverts.map { mapApiAdvert(it).toEntity() }
                advertDao.insertAdverts(entities)
            }
        } catch (_: Exception) {}
        
        advertDao.getAdverts().collect { entities ->
            val models = entities.map { it.toModel() }
            emit(Resource.Success(models))
        }
    }

    override fun getViewedAdverts(): Flow<Resource<List<Advert>>> = flow {
        advertDao.getViewedAdverts().collect { entities ->
            emit(Resource.Success(entities.map { it.toModel() }))
        }
    }

    override fun markAdvertViewed(advertId: String): Flow<Resource<Boolean>> = flow {
        if (advertId.isBlank()) {
            emit(Resource.Error("Invalid advert ID"))
            return@flow
        }
        
        // Anti-fraud: prevent double-rewarding on the client side
        val existing = advertDao.getAdvertById(advertId)
        if (existing != null && existing.isViewed) {
            emit(Resource.Success(true))
            return@flow
        }

        try {
            // 1. Mark as viewed locally for immediate UI feedback (gray out button)
            advertDao.markAsViewed(advertId)
            
            // 2. Notify backend for tracking/analytics.
            // SECURE DESIGN: The backend will NOT award points based on this client-side call.
            // It will wait for the cryptographically signed Server-Side Verification (SSV) 
            // callback directly from Google's servers to prevent fraud.
            val apiResponse = mediaApi.markAdvertViewed(advertId)
            
            emit(Resource.Success(apiResponse.isSuccessful))
        } catch (e: Exception) { 
            emit(Resource.Error("Tracking update failed"))
        }
    }

    override fun trackAdvertClick(advertId: String): Flow<Resource<Boolean>> = flow {
        try {
            mediaApi.trackAdvertClick(advertId)
            emit(Resource.Success(true))
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun trackAdvertShare(advertId: String): Flow<Resource<Boolean>> = flow {
        try {
            mediaApi.trackAdvertShare(advertId)
            emit(Resource.Success(true))
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun toggleLikeAdvert(advertId: String): Flow<Resource<Boolean>> = flow {
        try {
            mediaApi.likeAdvert(advertId)
            emit(Resource.Success(true))
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override fun resetAds(): Flow<Resource<Unit>> = flow {
        try {
            mediaApi.resetAds()
            advertDao.clearAdverts()
            emit(Resource.Success(Unit))
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
    }

    override suspend fun clearLocalAdverts() {
        advertDao.clearAdverts()
    }
}
