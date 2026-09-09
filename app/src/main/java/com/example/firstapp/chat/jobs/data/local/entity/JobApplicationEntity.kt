package com.example.firstapp.chat.jobs.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.chat.jobs.model.ApplicationStatus

@Entity(tableName = "job_applications")
data class JobApplicationEntity(
    @PrimaryKey val id: String,
    val jobId: String,
    val jobTitle: String,
    val companyName: String,
    val location: String,
    val appliedDate: Long,
    val status: ApplicationStatus,
    val conversationId: String? = null
)
