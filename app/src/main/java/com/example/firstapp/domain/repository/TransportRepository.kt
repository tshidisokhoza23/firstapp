package com.example.firstapp.domain.repository

import com.example.firstapp.drive.model.*
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.Flow

interface TransportRepository {
    fun getRideOptions(): Flow<Resource<List<RideOption>>>
    fun getDestinations(): Flow<Resource<List<Destination>>>
    fun saveDestination(name: String, address: String, latitude: Double, longitude: Double, isFavorite: Boolean, type: String): Flow<Resource<Boolean>>
}
