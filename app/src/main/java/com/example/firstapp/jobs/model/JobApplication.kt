package com.example.firstapp.jobs.model

import androidx.compose.ui.graphics.Color

data class JobApplication(
    val id: Int,
    val jobId: Int,
    val jobTitle: String,
    val companyName: String,
    val location: String,
    val appliedDate: Long,
    val status: ApplicationStatus,
    val logoColor: Color
)

enum class ApplicationStatus(val label: String, val color: Color) {
    PENDING("Pending", Color(0xFFFFB300)),
    REVIEWING("Under Review", Color(0xFF2196F3)),
    INTERVIEW("Interviewing", Color(0xFF9C27B0)),
    REJECTED("Not Selected", Color(0xFFF44336)),
    OFFERED("Offer Received", Color(0xFF4CAF50))
}
