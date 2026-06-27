package com.example.firstapp.media.data.repository

import com.example.firstapp.media.domain.repository.AdRepository
import com.example.firstapp.media.data.api.MediaApi
import com.example.firstapp.common.data.local.dao.AdvertDao
import com.example.firstapp.common.data.repository.*
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.model.Advert
import kotlinx.coroutines.flow.*

import javax.inject.Inject

class AdRepositoryImpl @Inject constructor(
    private val mediaApi: MediaApi,
    private val advertDao: AdvertDao
) : AdRepository {

    override fun getAdverts(): Flow<Resource<List<Advert>>> = flow {
        // 1. Emit cached data first as Loading
        val cached = advertDao.getAdverts().first().map { it.toModel() }
        emit(Resource.Loading(cached))

        // 2. Perform network sync
        try {
            val response = mediaApi.getAdverts()
            if (response.isSuccessful) {
                val apiAdverts = response.body()?.adverts ?: emptyList()
                android.util.Log.d("AdRepository", "API returned ${apiAdverts.size} adverts: ${apiAdverts.map { it.id }}")
                val entities = apiAdverts.map { mapApiAdvert(it).toEntity() }
                advertDao.insertAdverts(entities)
            }
        } catch (e: Exception) {
            android.util.Log.e("AdRepository", "Sync failed", e)
        }

        // 3. Emit the final state from DB as Success
        emitAll(advertDao.getAdverts().map { entities ->
            Resource.Success(entities.map { it.toModel() })
        })
    }

    override fun getViewedAdverts(): Flow<Resource<List<Advert>>> = flow {
        val cached = advertDao.getViewedAdverts().first().map { it.toModel() }
        emit(Resource.Loading(cached))

        try {
            val response = mediaApi.getViewedAdverts()
            if (response.isSuccessful) {
                val apiAdverts = response.body()?.adverts ?: emptyList()
                val entities = apiAdverts.map { mapApiAdvert(it).toEntity().copy(isViewed = true) }
                advertDao.insertAdverts(entities)
            }
        } catch (_: Exception) {}

        emitAll(advertDao.getViewedAdverts().map { entities ->
            Resource.Success(entities.map { it.toModel() })
        })
    }

    override suspend fun clearLocalAdverts() {
        advertDao.clearAdverts()
    }

    override fun markAdvertViewed(advertId: Int): Flow<Resource<Boolean>> = flow {
        try {
            val ad = advertDao.getAdvertById(advertId)
            if (ad?.isViewed == true) {
                emit(Resource.Success(false)) // Success but false means "already viewed, no points"
                return@flow
            }

            if (mediaApi.markAdvertViewed(advertId).isSuccessful) {
                advertDao.markAsViewed(advertId)
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (_: Exception) {
            emit(Resource.Error("Fail"))
        }
    }
}
