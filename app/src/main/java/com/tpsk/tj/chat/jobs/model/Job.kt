package com.tpsk.tj.chat.jobs.model

import androidx.compose.ui.graphics.Color

data class Job(
    val id: String,
    val title: String,
    val companyName: String,
    val companyLogoUrl: String? = null,
    val location: String,
    val logoColor: Color,
    val salaryRange: String = "",
    val jobType: String = "Full-time", // e.g., Full-time, Contract, Remote
    val postedTime: Long = 0,
    val description: String = "",
    val requirements: List<String> = emptyList(),
    val isVerified: Boolean = true,
    val isApplied: Boolean = false,
    val isSaved: Boolean = false,
    val createdBy: String? = null,
    val isExternal: Boolean = false,
    val sourcePartner: String? = null,
    val trackingApplyUrl: String? = null,
    val hasAppliedInternal: Boolean = false,
    val hasClickedExternal: Boolean = false
)
