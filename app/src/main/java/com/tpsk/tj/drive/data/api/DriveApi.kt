package com.tpsk.tj.drive.data.api

import com.tpsk.tj.common.data.api.*
import com.tpsk.tj.drive.model.*
import retrofit2.Response
import retrofit2.http.*

interface DriveApi {
    @GET("ride-options")
    suspend fun getRideOptions(): Response<RideOptionsResponse>

    @GET("trips")
    suspend fun getTrips(): Response<TripsResponse>

    @GET("destinations")
    suspend fun getDestinations(): Response<DestinationsResponse>

    @POST("destinations")
    suspend fun saveDestination(@Body request: SaveDestinationRequest): Response<SaveDestinationResponse>

    @GET("driver/documents")
    suspend fun getDriverDocuments(): Response<DriverDocumentsResponse>

    @POST("driver/documents")
    suspend fun uploadDocument(@Body request: UploadDocumentRequest): Response<UploadDocumentResponse>

    @Multipart
    @POST("driver/documents/photo")
    suspend fun uploadDocumentPhoto(
        @Part("type") type: okhttp3.RequestBody,
        @Part file: okhttp3.MultipartBody.Part
    ): Response<UploadDocumentResponse>

    @GET("ride/wallet/activity")
    suspend fun getWalletActivity(): Response<WalletActivityResponse>

    @GET("driver/requests")
    suspend fun getAvailableRequests(@Query("lat") lat: Double, @Query("lng") lng: Double): Response<RideRequestsResponse>

    @POST("driver/status")
    suspend fun updateOnlineStatus(@Body request: UpdateStatusRequest): Response<UpdateStatusResponse>

    @POST("ride/request")
    suspend fun createRideRequest(@Body request: CreateRideRequest): Response<CreateRideResponse>

    @POST("ride/presence")
    suspend fun reportPresence(@Body request: PresenceRequest): Response<com.tpsk.tj.drive.model.AreaPresence>

    @POST("driver/ride/stop")
    suspend fun markStop(@Body request: MarkStopRequest): Response<UpdateRideStatusResponse>

    @POST("driver/ride/status")
    suspend fun updateRideStatus(@Body request: UpdateRideStatusRequest): Response<UpdateRideStatusResponse>

    @POST("driver/ride/start")
    suspend fun startRide(@Body request: StartRideRequest): Response<UpdateRideStatusResponse>

    @GET("ride/active")
    suspend fun getRideState(): Response<com.tpsk.tj.drive.model.RideLiveState>

    @POST("ride/wallet/topup")
    suspend fun topUpWallet(@Body request: TopUpRequest): Response<TopUpResponse>

    @POST("ride/wallet/confirm")
    suspend fun confirmTopUp(@Body request: ConfirmTopUpRequest): Response<TopUpResponse>

    @POST("ride/wallet/voucher")
    suspend fun redeemVoucher(@Body request: VoucherRequest): Response<TopUpResponse>

    @GET("driver/payout")
    suspend fun getPayoutAccount(): Response<DriverPayoutAccount>

    @POST("driver/payout-account")
    suspend fun savePayoutAccount(@Body request: SavePayoutAccountRequest): Response<DriverPayoutAccount>

    @GET("driver/payout-banks")
    suspend fun getPayoutBanks(): Response<PayoutBanksResponse>

    @GET("driver/plan")
    suspend fun getDriverPlan(): Response<com.tpsk.tj.drive.model.DriverFeePlan>

    @POST("driver/plan")
    suspend fun setDriverPlan(@Body request: SetDriverPlanRequest): Response<com.tpsk.tj.drive.model.DriverFeePlan>

    @POST("ride/cancel")
    suspend fun cancelRide(@Body request: CancelRideRequest): Response<CancelRideResponse>

    @GET("ride/nearby-drivers")
    suspend fun getNearbyDrivers(@Query("lat") lat: Double, @Query("lng") lng: Double): Response<NearbyDriversResponse>

    @GET("driver/vehicle")
    suspend fun getVehicleInfo(): Response<VehicleInfo>

    @POST("driver/vehicle")
    suspend fun updateVehicleInfo(@Body request: VehicleInfo): Response<VehicleInfo>

    @Multipart
    @POST("driver/vehicle/photos")
    suspend fun uploadVehiclePhoto(@Part file: okhttp3.MultipartBody.Part): Response<VehicleInfo>

    @POST("driver/vehicle/photos/remove")
    suspend fun removeVehiclePhoto(@Body request: RemoveVehiclePhotoRequest): Response<VehicleInfo>

    @Multipart
    @POST("driver/profile/photo")
    suspend fun uploadDriverProfilePhoto(@Part file: okhttp3.MultipartBody.Part): Response<VehicleInfo>

    @GET("user/active-session")
    suspend fun getActiveSession(): Response<ActiveSession>
}

data class CreateRideRequest(
    val pickup_address: String,
    val destination_address: String,
    val pickup_lat: Double,
    val pickup_lng: Double,
    val dest_lat: Double,
    val dest_lng: Double,
    val fare: Double,
    val distance: String,
    val ride_type: String,
    val duration_seconds: Int = 0,
    val distance_meters: Int = 0,
    val stops: List<RideStopBody> = emptyList()
)

data class RideStopBody(val address: String, val lat: Double, val lng: Double)
data class RemoveVehiclePhotoRequest(val url: String)
data class PresenceRequest(val latitude: Double, val longitude: Double)
data class MarkStopRequest(val ride_id: String, val index: Int, val action: String)

data class CreateRideResponse(val ride_id: String)

data class UpdateRideStatusRequest(val ride_id: String, val status: String)
data class StartRideRequest(val ride_id: String, val pin: String)
data class SetDriverPlanRequest(val plan: String)
data class TopUpRequest(val amount: Double)
data class TopUpResponse(
    val wallet_balance: Double = 0.0,
    val authorization_url: String? = null,
    val reference: String? = null
)
data class ConfirmTopUpRequest(val reference: String)
data class VoucherRequest(val pin: String)
data class UpdateRideStatusResponse(val success: Boolean)

data class CancelRideRequest(val ride_id: String, val reason: String, val by: String)
data class CancelRideResponse(val success: Boolean)

data class NearbyDriversResponse(val drivers: List<DriverStatus>)

data class DriverDocumentsResponse(val documents: List<DriverDocument>)
data class UploadDocumentRequest(val type: String, val url: String)
data class UploadDocumentResponse(val document: DriverDocument)
data class RideRequestsResponse(val requests: List<RideRequest>)
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
data class TripsResponse(val trips: List<com.tpsk.tj.drive.model.Trip>)

data class WalletActivityResponse(
    val transactions: List<com.tpsk.tj.drive.model.Transaction> = emptyList(),
    @com.google.gson.annotations.SerializedName("available_payout") val availablePayout: Double = 0.0,
    @com.google.gson.annotations.SerializedName("week_earnings") val weekEarnings: Double = 0.0
)
data class DriverPayoutAccount(
    val account_name: String = "",
    val account_last4: String = "",
    val bank_name: String = "",
    val bank_code: String = "",
    val available: Double = 0.0,
    val next_payout_label: String = "",
    val last_status: String = "",
    val last_amount: Double = 0.0,
    val last_failure: String = "",
    val ready: Boolean = false
)
data class SavePayoutAccountRequest(
    val account_name: String,
    val account_number: String,
    val bank_code: String,
    val bank_name: String
)
data class PayoutBank(val code: String = "", val name: String = "")
data class PayoutBanksResponse(val banks: List<PayoutBank> = emptyList())
data class DestinationsResponse(val destinations: List<Destination>)
data class SaveDestinationResponse(val destination: Destination)
