package com.example.firstapp.drive.data.api

import com.example.firstapp.common.data.api.*
import com.example.firstapp.drive.model.RideOption
import com.example.firstapp.drive.model.Destination
import retrofit2.Response
import retrofit2.http.*

interface DriveApi {
    @GET("ride-options")
    suspend fun getRideOptions(): Response<RideOptionsResponse>

    @GET("destinations")
    suspend fun getDestinations(): Response<DestinationsResponse>

    @POST("destinations")
    suspend fun saveDestination(@Body request: SaveDestinationRequest): Response<SaveDestinationResponse>

    @GET("driver/documents")
    suspend fun getDriverDocuments(): Response<DriverDocumentsResponse>

    @POST("driver/documents")
    suspend fun uploadDocument(@Body request: UploadDocumentRequest): Response<UploadDocumentResponse>

    @GET("driver/requests")
    suspend fun getAvailableRequests(@Query("lat") lat: Double, @Query("lng") lng: Double): Response<RideRequestsResponse>

    @POST("driver/status")
    suspend fun updateOnlineStatus(@Body request: UpdateStatusRequest): Response<UpdateStatusResponse>

    @POST("ride/request")
    suspend fun createRideRequest(@Body request: CreateRideRequest): Response<CreateRideResponse>

    @POST("driver/ride/status")
    suspend fun updateRideStatus(@Body request: UpdateRideStatusRequest): Response<UpdateRideStatusResponse>

    @POST("ride/cancel")
    suspend fun cancelRide(@Body request: CancelRideRequest): Response<CancelRideResponse>

    @GET("ride/nearby-drivers")
    suspend fun getNearbyDrivers(@Query("lat") lat: Double, @Query("lng") lng: Double): Response<NearbyDriversResponse>

    @GET("driver/vehicle")
    suspend fun getVehicleInfo(): Response<com.example.firstapp.drive.ui.viewmodel.DriveViewModel.VehicleInfo>

    @POST("driver/vehicle")
    suspend fun updateVehicleInfo(@Body request: com.example.firstapp.drive.ui.viewmodel.DriveViewModel.VehicleInfo): Response<com.example.firstapp.drive.ui.viewmodel.DriveViewModel.VehicleInfo>
}

data class CreateRideRequest(
    val pickup_address: String,
    val destination_address: String,
    val pickup_lat: Double,
    val pickup_lng: Double,
    val dest_lat: Double,
    val dest_lng: Double,
    val fare: Double,
    val ride_type: String
)

data class CreateRideResponse(val ride_id: String)

data class UpdateRideStatusRequest(val ride_id: String, val status: String)
data class UpdateRideStatusResponse(val success: Boolean)

data class CancelRideRequest(val ride_id: String, val reason: String, val by: String)
data class CancelRideResponse(val success: Boolean)

data class NearbyDriversResponse(val drivers: List<com.example.firstapp.drive.ui.viewmodel.DriveViewModel.DriverStatus>)

data class DriverDocumentsResponse(val documents: List<com.example.firstapp.drive.ui.viewmodel.DriveViewModel.DriverDocument>)
data class UploadDocumentRequest(val type: String, val url: String)
data class UploadDocumentResponse(val document: com.example.firstapp.drive.ui.viewmodel.DriveViewModel.DriverDocument)
data class RideRequestsResponse(val requests: List<com.example.firstapp.drive.ui.viewmodel.DriveViewModel.RideRequest>)
data class UpdateStatusRequest(val online: Boolean, val latitude: Double, val longitude: Double)
data class UpdateStatusResponse(val success: Boolean)

data class SaveDestinationRequest(
    val name: String,
    val address: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val is_favorite: Boolean = false,
    val destination_type: String = "recent",
    val place_id: String? = null
)

data class RideOptionsResponse(val ride_options: List<RideOption>)
data class DestinationsResponse(val destinations: List<Destination>)
data class SaveDestinationResponse(val destination: Destination)
