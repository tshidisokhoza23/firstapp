package com.example.firstapp.chat.jobs.data.repository

import com.example.firstapp.chat.jobs.data.local.entity.JobEntity
import com.example.firstapp.chat.jobs.data.local.entity.JobApplicationEntity
import com.example.firstapp.chat.jobs.model.Job
import com.example.firstapp.chat.jobs.model.JobApplication
import com.example.firstapp.common.ui.theme.AppPrimary
import com.example.firstapp.common.ui.theme.AppAccent
import androidx.compose.ui.graphics.Color

fun Job.toEntity() = JobEntity(
    id = id,
    title = title,
    companyName = companyName,
    companyLogoUrl = companyLogoUrl,
    location = location,
    salaryRange = salaryRange,
    jobType = jobType,
    postedTime = postedTime,
    description = description,
    requirements = requirements,
    isVerified = isVerified,
    isApplied = isApplied,
    isSaved = isSaved,
)

fun JobEntity.toModel() = Job(
    id = id,
    title = title,
    companyName = companyName,
    companyLogoUrl = companyLogoUrl,
    location = location,
    logoColor = AppPrimary,
    salaryRange = salaryRange,
    jobType = jobType,
    postedTime = postedTime,
    description = description,
    requirements = requirements,
    isVerified = isVerified,
    isApplied = isApplied,
    isSaved = isSaved,
)

fun JobApplication.toEntity() = JobApplicationEntity(
    id = id,
    jobId = jobId,
    jobTitle = jobTitle,
    companyName = companyName,
    location = location,
    appliedDate = appliedDate,
    status = status,
    conversationId = conversationId
)

fun JobApplicationEntity.toModel() = JobApplication(
    id = id,
    jobId = jobId,
    jobTitle = jobTitle,
    companyName = companyName,
    location = location,
    appliedDate = appliedDate,
    status = status,
    logoColor = AppAccent,
    conversationId = conversationId
)
