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

data class Trip(
    val id: String,
    val date: String,
    val from: String,
    val to: String,
    val price: String,
    val car: String,
    val status: String = "Completed"
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
    val licensePlate: String = "GP 123 456"
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
    val paymentMethod: String = "Wallet"
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
    val url: String? = null
)

data class RideRequest(
    val id: String,
    val passengerName: String,
    val pickupAddress: String,
    val destinationAddress: String,
    val fare: Double,
    val distance: String,
    val passengerRating: Double = 4.9,
    val pickupLat: Double = 0.0,
    val pickupLng: Double = 0.0,
    val destLat: Double = 0.0,
    val destLng: Double = 0.0
)

data class VehicleInfo(
    val make: String = "",
    val model: String = "",
    val year: String = "",
    val color: String = "",
    val license_plate: String = ""
)

data class DriverStats(
    val totalTrips: Int = 42,
    val onlineHours: Double = 5.5,
    val acceptanceRate: Double = 98.0,
    val weeklyEarnings: Double = 2450.0
)
