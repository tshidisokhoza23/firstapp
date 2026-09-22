package com.tpsk.tj.domain.repository

import com.tpsk.tj.drive.model.*
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.flow.Flow

interface TransportRepository {
    fun getRideOptions(): Flow<Resource<List<RideOption>>>
    fun getDestinations(): Flow<Resource<List<Destination>>>
    fun saveDestination(name: String, address: String, latitude: Double, longitude: Double, isFavorite: Boolean, type: String): Flow<Resource<Boolean>>
}
