package com.example.firstapp.common.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.drive.model.DestinationType

@Entity(tableName = "ride_options")
data class RideOptionEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val price: String,
    val currency: String,
    val arrivalTime: String,
    val estimatedArrival: String,
    val description: String,
    val iconUrl: String?,
    val iconColor: Int,
    val capacity: Int
)

@Entity(tableName = "destinations")
data class DestinationEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val iconColor: Int,
    val isFavorite: Boolean,
    val type: DestinationType,
    val placeId: String?
)
