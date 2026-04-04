package com.example.firstapp.model

import androidx.compose.ui.graphics.Color

data class Job(
    val id: Int,
    val title: String,
    val companyName: String,
    val location: String,
    val logoColor: Color,
    val description: String = "This is a dummy job description for $title at $companyName.",
    val isVerified: Boolean = true
)
