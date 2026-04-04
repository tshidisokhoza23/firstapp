package com.example.firstapp.model

import androidx.compose.ui.graphics.Color

data class Advert(
    val id: Int,
    val advertiserName: String,
    val time: String,
    val avatarColor: Color,
    val contentColor: Color,
    val contentText: String = ""
)
