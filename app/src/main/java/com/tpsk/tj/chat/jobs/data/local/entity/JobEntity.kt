package com.tpsk.tj.chat.jobs.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey val id: String,
    val title: String,
    val companyName: String,
    val companyLogoUrl: String?,
    val location: String,
    val salaryRange: String,
    val jobType: String,
    val postedTime: Long,
    val description: String,
    val requirements: List<String>,
    val isVerified: Boolean,
    val isApplied: Boolean,
    val isSaved: Boolean,
    val isExternal: Boolean = false,
    val sourcePartner: String? = null,
    val trackingApplyUrl: String? = null,
    val hasAppliedInternal: Boolean = false,
    val hasClickedExternal: Boolean = false
)
