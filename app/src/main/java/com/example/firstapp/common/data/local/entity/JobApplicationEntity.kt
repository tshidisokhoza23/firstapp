package com.example.firstapp.common.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.jobs.model.ApplicationStatus

@Entity(tableName = "job_applications")
data class JobApplicationEntity(
    @PrimaryKey val id: Int,
    val jobId: Int,
    val jobTitle: String,
    val companyName: String,
    val location: String,
    val appliedDate: Long,
    val status: ApplicationStatus
)
