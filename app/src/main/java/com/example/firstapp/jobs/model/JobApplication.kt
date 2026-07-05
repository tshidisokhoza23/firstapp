package com.example.firstapp.jobs.model

import androidx.compose.ui.graphics.Color

data class JobApplication(
    val id: String,
    val jobId: String,
    val jobTitle: String,
    val companyName: String,
    val location: String,
    val appliedDate: Long,
    val status: ApplicationStatus,
    val logoColor: Color
)

enum class ApplicationStatus(val label: String, val color: Color) {
    PENDING("Pending", Color(0xFFFFB74D)),
    REVIEWING("Reviewing", Color(0xFF64B5F6)),
    INTERVIEW("Interview", Color(0xFF81C784)),
    REJECTED("Rejected", Color(0xFFE57373)),
    OFFERED("Offered", Color(0xFFBA68C8))
}
