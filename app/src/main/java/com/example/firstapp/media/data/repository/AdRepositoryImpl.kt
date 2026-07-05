package com.example.firstapp.media.data.repository

import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.data.api.*
import com.example.firstapp.media.domain.repository.AdRepository
import com.example.firstapp.media.model.Advert
import com.example.firstapp.common.data.local.dao.AdvertDao
import com.example.firstapp.media.data.repository.*
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
        try {
            advertDao.markAsViewed(advertId)
            mediaApi.markAdvertViewed(advertId)
            emit(Resource.Success(true))
        } catch (e: Exception) { emit(Resource.Error(e.message ?: "Error")) }
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
