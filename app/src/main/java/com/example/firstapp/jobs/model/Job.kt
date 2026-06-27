package com.example.firstapp.jobs.model

import androidx.compose.ui.graphics.Color

data class Job(
    val id: Int,
    val title: String,
    val companyName: String,
    val companyLogoUrl: String? = null,
    val location: String,
    val logoColor: Color,
    val salaryRange: String = "",
    val jobType: String = "Full-time", // e.g., Full-time, Contract, Remote
    val postedTime: String = "",
    val description: String = "",
    val requirements: List<String> = emptyList(),
    val isVerified: Boolean = true,
    val isApplied: Boolean = false,
    val isSaved: Boolean = false
)
