package com.tpsk.tj.drive.model

import androidx.compose.ui.graphics.Color

data class RideOption(
    val id: String,
    val name: String,
    val price: String,
    val currency: String = "R",
    val arrivalTime: String, // e.g., "5 min"
    val estimatedArrival: String = "", // e.g., "14:30"
    val description: String,
    val iconUrl: String? = null,
    val iconColor: Color,
    val capacity: Int = 4,
    val isSelected: Boolean = false
)

data class Destination(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val iconColor: Color = Color.Gray,
    val isFavorite: Boolean = false,
    val type: DestinationType = DestinationType.RECENT,
    val placeId: String? = null
)

enum class DestinationType {
    HOME, WORK, FAVORITE, RECENT
}

val RequiredDriverDocuments = listOf(
    "ID Document",
    "Driver's License",
    "Professional Driving Permit",
    "Vehicle Licence Disc",
    "Roadworthy Certificate",
    "Profile Photo",
    "Car Image"
)

data class Trip(
    val id: String,
    val date: String,
    val from: String,
    val to: String,
    val price: String,
    val car: String,
    val status: String = "Completed",
    val role: String = "",
    @com.google.gson.annotations.SerializedName("payment_method") val paymentMethod: String = "Wallet",
    @com.google.gson.annotations.SerializedName("payment_status") val paymentStatus: String = "",
    val fare: Double = 0.0,
    @com.google.gson.annotations.SerializedName("platform_fee") val platformFee: Double = 0.0,
    @com.google.gson.annotations.SerializedName("driver_earning") val driverEarning: Double = 0.0,
    val distance: String = ""
)

data class WalletActivity(
    val transactions: List<Transaction> = emptyList(),
    val availablePayout: Double = 0.0,
    val weekEarnings: Double = 0.0
)

data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val date: String,
    val isDebit: Boolean
)

data class TrafficSegment(
    val points: List<com.google.android.gms.maps.model.LatLng>,
    val color: androidx.compose.ui.graphics.Color
)

enum class BookingStatus {
    NONE, SEARCHING, EN_ROUTE, ARRIVED, IN_PROGRESS, COMPLETED
}

data class NearbyDriver(
    val id: String,
    val position: com.google.android.gms.maps.model.LatLng,
    val rotation: Float,
    val name: String = "Mpho",
    val rating: Double = 4.8,
    val vehicleModel: String = "BMW 3 Series",
    val vehicleColor: String = "White",
    val licensePlate: String = "GP 123 456",
    val photoUrls: List<String> = emptyList(),
    val profilePhoto: String = ""
)

data class ActiveSession(
    val mode: String,
    val is_online: Boolean,
    val ride_id: String? = null,
    val status: BookingStatus = BookingStatus.NONE,
    val destination: Destination? = null,
    val assigned_driver: NearbyDriver? = null
)

data class TripSummary(
    val fare: Double,
    val distance: String,
    val duration: String,
    val date: String = "Today, 14:20",
    val passengerName: String,
    val paymentMethod: String = "Wallet",
    val platformFee: Double = 0.0,
    val driverEarning: Double = 0.0
)

data class DriverStatus(
    val user_id: String,
    val is_online: Boolean,
    val last_lat: Double,
    val last_lng: Double,
    val updated_at: Long
)

data class DriverDocument(
    val id: String,
    val type: String,
    val status: String, // "PENDING", "APPROVED", "REJECTED"
    val url: String? = null,
    @com.google.gson.annotations.SerializedName("rejection_reason") val rejectionReason: String? = null
)

data class RideRequest(
    val id: String,
    @com.google.gson.annotations.SerializedName("passenger_name") val passengerName: String = "",
    @com.google.gson.annotations.SerializedName("pickup_address") val pickupAddress: String = "",
    @com.google.gson.annotations.SerializedName("destination_address") val destinationAddress: String = "",
    val fare: Double = 0.0,
    val distance: String = "",
    @com.google.gson.annotations.SerializedName("passenger_rating") val passengerRating: Double = 4.9,
    @com.google.gson.annotations.SerializedName("pickup_lat") val pickupLat: Double = 0.0,
    @com.google.gson.annotations.SerializedName("pickup_lng") val pickupLng: Double = 0.0,
    @com.google.gson.annotations.SerializedName("dest_lat") val destLat: Double = 0.0,
    @com.google.gson.annotations.SerializedName("dest_lng") val destLng: Double = 0.0
)

data class DriverFeePlan(
    val plan: String = "",
    val weekly_until: Long = 0,
    val chosen_at: Long = 0,
    val choice_until: Long = 0,
    val choice_active: Boolean = true,
    val can_choose: Boolean = false,
    val on_trip: Boolean = false,
    val active_plan: String = "",
    val weekly_active: Boolean = false,
    val weekly_price: Double = 349.0,
    val commission_rate: Double = 0.20,
    val wallet_balance: Double = 0.0,
    val week_earnings: Double = 0.0
)

data class AreaPresence(
    val people: Int = 0,
    val clients: Int = 0,
    val drivers: Int = 0,
    val radius_km: Double = 5.0,
    val window_minutes: Int = 15
)

data class ActiveStop(
    val index: Int = 0,
    val address: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val arrived_at: Long = 0,
    val left_at: Long = 0
)

data class RideLiveState(
    val ride_id: String? = null,
    val role: String? = null,
    val status: String? = null,
    val fare: Double = 0.0,
    val distance: String? = null,
    val pickup_address: String? = null,
    val destination_address: String? = null,
    val safety_pin: String? = null,
    val payment_status: String? = null,
    val fee_plan: String? = null,
    val driver_id: String? = null,
    val driver_name: String? = null,
    val vehicle: String? = null,
    val plate: String? = null,
    val driver_lat: Double = 0.0,
    val driver_lng: Double = 0.0,
    val wallet_balance: Double = 0.0,
    val weekly_until: Long = 0,
    val weekly_active: Boolean = false,
    val driver_earning: Double = 0.0,
    val platform_fee: Double = 0.0,
    val arrived_at: Long = 0,
    val wait_fee: Double = 0.0,
    val pickup_lat: Double = 0.0,
    val pickup_lng: Double = 0.0,
    val dest_lat: Double = 0.0,
    val dest_lng: Double = 0.0,
    val duration_seconds: Int = 0,
    val stops: List<ActiveStop>? = null,
    val vehicle_photos: List<String>? = null,
    val driver_photo: String? = null
)

data class VehicleInfo(
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val color: String = "",
    val license_plate: String = "",
    val photo_urls: List<String>? = emptyList(),
    val profile_photo_url: String? = null
)

data class DriverStats(
    val totalTrips: Int = 0,
    val onlineHours: Double = 0.0,
    val acceptanceRate: Double = 0.0,
    val weeklyEarnings: Double = 0.0
)
