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

    // Driver Features
    fun getDriverDocuments(): Flow<Resource<List<com.example.firstapp.drive.ui.viewmodel.DriveViewModel.DriverDocument>>>
    fun uploadDocument(type: String, url: String): Flow<Resource<com.example.firstapp.drive.ui.viewmodel.DriveViewModel.DriverDocument>>
    fun getAvailableRequests(lat: Double, lng: Double): Flow<Resource<List<com.example.firstapp.drive.ui.viewmodel.DriveViewModel.RideRequest>>>
    fun updateOnlineStatus(online: Boolean, latitude: Double, longitude: Double): Flow<Resource<Boolean>>
    
    // Ride Lifecycle
    fun createRideRequest(pickup: String, dest: String, pLat: Double, pLng: Double, dLat: Double, dLng: Double, fare: Double, type: String): Flow<Resource<String>>
    fun updateRideStatus(rideId: String, status: String): Flow<Resource<Boolean>>
    fun cancelRide(rideId: String, reason: String, by: String): Flow<Resource<Boolean>>

    // Vehicle Features
    fun getVehicleInfo(): Flow<Resource<com.example.firstapp.drive.ui.viewmodel.DriveViewModel.VehicleInfo>>
    fun updateVehicleInfo(vehicle: com.example.firstapp.drive.ui.viewmodel.DriveViewModel.VehicleInfo): Flow<Resource<com.example.firstapp.drive.ui.viewmodel.DriveViewModel.VehicleInfo>>
}
