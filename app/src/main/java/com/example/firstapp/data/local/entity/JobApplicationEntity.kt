package com.example.firstapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.model.ApplicationStatus

@Entity(tableName = "job_applications")
data class JobApplicationEntity(
    @PrimaryKey val id: Int,
    val jobId: Int,
    val jobTitle: String,
    val companyName: String,
    val appliedDate: String,
    val status: ApplicationStatus
)
