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
    suspend fun getAvailableRequests(): Response<RideRequestsResponse>

    @POST("driver/status")
    suspend fun updateOnlineStatus(@Body request: UpdateStatusRequest): Response<UpdateStatusResponse>
}

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
