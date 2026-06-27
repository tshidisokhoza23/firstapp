package com.example.firstapp.domain.repository

import com.example.firstapp.jobs.model.*
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.Flow

interface JobRepository {
    fun getJobs(): Flow<Resource<List<Job>>>
    fun applyForJob(jobId: Int): Flow<Resource<Boolean>>
    fun getJobApplications(): Flow<Resource<List<JobApplication>>>
    fun withdrawApplication(applicationId: Int): Flow<Resource<Boolean>>
    fun getJobProfile(): Flow<Resource<JobProfileData>>
    fun updateJobProfile(profile: JobProfileData): Flow<Resource<Boolean>>
    fun searchJobs(query: String): Flow<List<Job>>
}
