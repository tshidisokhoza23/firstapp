package com.tpsk.tj.drive.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.tpsk.tj.drive.data.local.entity.DestinationEntity
import com.tpsk.tj.drive.data.local.entity.RideOptionEntity
import com.tpsk.tj.drive.model.Destination
import com.tpsk.tj.drive.model.RideOption

fun RideOptionEntity.toRideOption() = RideOption(
    id = id,
    name = name,
    price = price,
    currency = currency,
    arrivalTime = arrivalTime,
    estimatedArrival = estimatedArrival,
    description = description,
    iconUrl = iconUrl,
    iconColor = Color(iconColor),
    capacity = capacity
)

fun RideOption.toRideOptionEntity() = RideOptionEntity(
    id = id,
    name = name,
    price = price,
    currency = currency,
    arrivalTime = arrivalTime,
    estimatedArrival = estimatedArrival,
    description = description,
    iconUrl = iconUrl,
    iconColor = iconColor.toArgb(),
    capacity = capacity
)

fun DestinationEntity.toDestination() = Destination(
    id = id,
    name = name,
    address = address,
    latitude = latitude,
    longitude = longitude,
    iconColor = Color(iconColor),
    isFavorite = isFavorite,
    type = type,
    placeId = placeId
)

fun Destination.toDestinationEntity() = DestinationEntity(
    id = id,
    name = name,
    address = address,
    latitude = latitude,
    longitude = longitude,
    iconColor = iconColor.toArgb(),
    isFavorite = isFavorite,
    type = type,
    placeId = placeId
)
