package com.tpsk.tj.drive.domain.repository

import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.drive.model.*
import kotlinx.coroutines.flow.Flow

interface DriveRepository {
    fun getRideOptions(): Flow<Resource<List<RideOption>>>
    fun getDestinations(): Flow<Resource<List<Destination>>>
    fun getTrips(): Flow<Resource<List<Trip>>>
    fun saveDestination(name: String, address: String, latitude: Double, longitude: Double, isFavorite: Boolean, type: String): Flow<Resource<Boolean>>
    fun deleteDestination(id: String): Flow<Resource<Boolean>>

    // Driver Features
    fun getDriverDocuments(): Flow<Resource<List<DriverDocument>>>
    fun uploadDocument(type: String, url: String): Flow<Resource<DriverDocument>>
    fun uploadDocumentPhoto(type: String, file: okhttp3.MultipartBody.Part): Flow<Resource<DriverDocument>>
    fun getWalletActivity(): Flow<Resource<WalletActivity>>
    fun getAvailableRequests(lat: Double, lng: Double): Flow<Resource<List<RideRequest>>>
    fun updateOnlineStatus(online: Boolean, latitude: Double, longitude: Double): Flow<Resource<Boolean>>
    
    // Ride Lifecycle
    fun createRideRequest(pickup: String, dest: String, pLat: Double, pLng: Double, dLat: Double, dLng: Double, fare: Double, distance: String, type: String, meters: Int = 0, seconds: Int = 0, stops: List<com.tpsk.tj.drive.data.api.RideStopBody> = emptyList()): Flow<Resource<String>>
    fun reportPresence(lat: Double, lng: Double): Flow<Resource<AreaPresence>>
    fun markStop(rideId: String, index: Int, action: String): Flow<Resource<Boolean>>
    fun updateRideStatus(rideId: String, status: String): Flow<Resource<Boolean>>
    fun cancelRide(rideId: String, reason: String, by: String): Flow<Resource<Boolean>>

    // Vehicle Features
    fun getVehicleInfo(): Flow<Resource<VehicleInfo>>
    fun updateVehicleInfo(vehicle: VehicleInfo): Flow<Resource<VehicleInfo>>
    fun uploadVehiclePhoto(file: okhttp3.MultipartBody.Part): Flow<Resource<VehicleInfo>>
    fun removeVehiclePhoto(url: String): Flow<Resource<VehicleInfo>>
    fun uploadDriverProfilePhoto(file: okhttp3.MultipartBody.Part): Flow<Resource<VehicleInfo>>
    
    // Nearby Search
    fun getNearbyDrivers(lat: Double, lng: Double): Flow<Resource<List<DriverStatus>>>

    // Real-time & Session
    fun getRideEvents(): kotlinx.coroutines.flow.SharedFlow<com.tpsk.tj.drive.data.network.RideEvent>
    fun connectRideSocket()
    fun disconnectRideSocket()
    fun getActiveSession(): Flow<Resource<ActiveSession>>
    fun getRideState(): Flow<Resource<RideLiveState>>
    fun getDriverPlan(): Flow<Resource<DriverFeePlan>>
    fun setDriverPlan(plan: String): Flow<Resource<DriverFeePlan>>
    fun startRide(rideId: String, pin: String): Flow<Resource<Boolean>>
    fun topUpWallet(amount: Double): Flow<Resource<com.tpsk.tj.drive.data.api.TopUpResponse>>
    fun confirmTopUp(reference: String): Flow<Resource<Double>>
    fun redeemVoucher(pin: String): Flow<Resource<Double>>
    fun getPayoutAccount(): Flow<Resource<com.tpsk.tj.drive.data.api.DriverPayoutAccount>>
    fun savePayoutAccount(name: String, number: String, bankCode: String, bankName: String): Flow<Resource<com.tpsk.tj.drive.data.api.DriverPayoutAccount>>
    fun getPayoutBanks(): Flow<Resource<List<com.tpsk.tj.drive.data.api.PayoutBank>>>
}
