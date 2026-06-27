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
}

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
