package com.example.firstapp.model

import androidx.compose.ui.graphics.Color

data class RideOption(
    val id: Int,
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
    val id: Int,
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
