package com.example.firstapp.drive.data.repository

import androidx.compose.ui.graphics.Color
import com.example.firstapp.drive.data.api.DriveApi
import com.example.firstapp.drive.data.api.SaveDestinationRequest
import com.example.firstapp.common.data.local.dao.DriveDao
import com.example.firstapp.common.util.Resource
import com.example.firstapp.drive.domain.repository.DriveRepository
import com.example.firstapp.drive.model.Destination
import com.example.firstapp.drive.model.DestinationType
import com.example.firstapp.drive.model.RideOption
import com.example.firstapp.drive.model.Trip
import com.example.firstapp.common.data.repository.*
import kotlinx.coroutines.flow.*

import javax.inject.Inject

class DriveRepositoryImpl @Inject constructor(
    private val driveApi: DriveApi,
    private val driveDao: DriveDao
) : DriveRepository {

    override fun getRideOptions(): Flow<Resource<List<RideOption>>> = flow {
        emit(Resource.Loading())
        driveDao.getRideOptions().firstOrNull()?.let { 
            if (it.isNotEmpty()) emit(Resource.Success(it.map { entity -> entity.toModel() })) 
        }
        try {
            val res = driveApi.getRideOptions()
            if (res.isSuccessful && res.body() != null) {
                driveDao.clearRideOptions()
                driveDao.insertRideOptions(res.body()!!.ride_options.map { it.toEntity() })
            }
        } catch (_: Exception) {}
        driveDao.getRideOptions().collect { entities ->
            emit(Resource.Success(entities.map { it.toModel() }))
        }
    }

    override fun getDestinations(): Flow<Resource<List<Destination>>> = flow {
        emit(Resource.Loading())
        driveDao.getDestinations().firstOrNull()?.let { 
            if (it.isNotEmpty()) emit(Resource.Success(it.map { it.toModel() })) 
        }
        try {
            val res = driveApi.getDestinations()
            if (res.isSuccessful && res.body() != null) {
                driveDao.insertDestinations(res.body()!!.destinations.map { it.toEntity() })
            }
        } catch (_: Exception) {}
        driveDao.getDestinations().collect { entities ->
            emit(Resource.Success(entities.map { it.toModel() }))
        }
    }

    override fun getTrips(): Flow<Resource<List<Trip>>> = flow {
        emit(Resource.Loading())
        emit(Resource.Success(listOf(
            Trip(1, "Today, 10:24", "123 Innovation Drive", "Tech Park Building", "R85.50", "Toyota Corolla"),
            Trip(2, "Yesterday, 18:45", "Mall of Africa", "123 Innovation Drive", "R120.00", "VW Polo")
        )))
    }

    override fun saveDestination(
        name: String,
        address: String,
        latitude: Double,
        longitude: Double,
        isFavorite: Boolean,
        type: String
    ): Flow<Resource<Boolean>> = flow {
        val destination = Destination(
            id = 0,
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            iconColor = Color.Gray,
            isFavorite = isFavorite,
            type = try { DestinationType.valueOf(type.uppercase()) } catch (_: Exception) { DestinationType.RECENT }
        )
        driveDao.insertDestinations(listOf(destination.toEntity()))
        emit(Resource.Success(true))
        try {
            driveApi.saveDestination(SaveDestinationRequest(name, address, latitude, longitude, isFavorite, type))
        } catch (_: Exception) {}
    }

    override fun deleteDestination(id: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        driveDao.deleteDestination(id)
        emit(Resource.Success(true))
    }
}
