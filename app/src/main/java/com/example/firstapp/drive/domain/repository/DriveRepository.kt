package com.example.firstapp.drive.domain.repository

import com.example.firstapp.common.util.Resource
import com.example.firstapp.drive.model.Destination
import com.example.firstapp.drive.model.RideOption
import com.example.firstapp.drive.model.Trip
import kotlinx.coroutines.flow.Flow

interface DriveRepository {
    fun getRideOptions(): Flow<Resource<List<RideOption>>>
    fun getDestinations(): Flow<Resource<List<Destination>>>
    fun getTrips(): Flow<Resource<List<Trip>>>
    fun saveDestination(name: String, address: String, latitude: Double, longitude: Double, isFavorite: Boolean, type: String): Flow<Resource<Boolean>>
    fun deleteDestination(id: String): Flow<Resource<Boolean>>
}
