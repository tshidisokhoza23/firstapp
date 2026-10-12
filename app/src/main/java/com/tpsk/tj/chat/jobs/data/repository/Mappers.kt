package com.tpsk.tj.chat.jobs.data.repository

import com.tpsk.tj.chat.jobs.data.local.entity.JobEntity
import com.tpsk.tj.chat.jobs.data.local.entity.JobApplicationEntity
import com.tpsk.tj.chat.jobs.model.Job
import com.tpsk.tj.chat.jobs.model.JobApplication
import com.tpsk.tj.common.ui.theme.AppPrimary
import com.tpsk.tj.common.ui.theme.AppAccent

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
    isExternal = isExternal,
    sourcePartner = sourcePartner,
    trackingApplyUrl = trackingApplyUrl,
    hasAppliedInternal = hasAppliedInternal,
    hasClickedExternal = hasClickedExternal,
    isFeatured = isFeatured,
    viewsCount = viewsCount
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
    isApplied = isApplied || hasAppliedInternal,
    isSaved = isSaved,
    isExternal = isExternal,
    sourcePartner = sourcePartner,
    trackingApplyUrl = trackingApplyUrl,
    hasAppliedInternal = hasAppliedInternal,
    hasClickedExternal = hasClickedExternal,
    isFeatured = isFeatured,
    viewsCount = viewsCount
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
