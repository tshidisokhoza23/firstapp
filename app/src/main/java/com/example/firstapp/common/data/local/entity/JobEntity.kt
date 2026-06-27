package com.example.firstapp.common.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jobs")
data class JobEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val companyName: String,
    val companyLogoUrl: String?,
    val location: String,
    val salaryRange: String,
    val jobType: String,
    val postedTime: String,
    val description: String,
    val requirements: List<String>,
    val isVerified: Boolean,
    val isApplied: Boolean,
    val isSaved: Boolean
)
