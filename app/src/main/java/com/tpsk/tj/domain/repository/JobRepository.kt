package com.tpsk.tj.domain.repository

import com.tpsk.tj.chat.jobs.model.Job
import com.tpsk.tj.chat.jobs.model.JobApplication
import com.tpsk.tj.chat.jobs.model.JobProfileData
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.flow.Flow

interface JobRepository {
    fun getJobs(page: Int = 1): Flow<Resource<List<Job>>>
    fun applyForJob(jobId: String): Flow<Resource<String>> // Returns conversation_id
    fun getJobApplications(): Flow<Resource<List<JobApplication>>>
    fun getJobProfile(): Flow<Resource<JobProfileData>>
    fun withdrawApplication(applicationId: String): Flow<Resource<Boolean>>
    fun updateJobProfile(profile: JobProfileData): Flow<Resource<Boolean>>
    fun createJob(title: String, description: String, category: String, skills: List<String>, durationDays: Int): Flow<Resource<Job>>
    fun searchJobs(query: String): Flow<Resource<List<Job>>>
}
