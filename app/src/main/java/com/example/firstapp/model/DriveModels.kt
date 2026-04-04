package com.example.firstapp.model

import androidx.compose.ui.graphics.Color

data class RideOption(
    val id: Int,
    val name: String,
    val price: String,
    val arrivalTime: String,
    val description: String,
    val iconColor: Color
)

data class Destination(
    val id: Int,
    val name: String,
    val address: String,
    val iconColor: Color = Color.Gray
)
