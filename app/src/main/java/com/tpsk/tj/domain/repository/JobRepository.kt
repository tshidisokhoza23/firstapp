package com.tpsk.tj.domain.repository

import com.tpsk.tj.chat.jobs.model.Job
import com.tpsk.tj.chat.jobs.model.JobApplication
import com.tpsk.tj.chat.jobs.model.JobProfileData
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.flow.Flow

interface JobRepository {
    fun getJobs(
        page: Int = 1,
        jobType: String? = null,
        featuredOnly: Boolean = false,
        location: String? = null,
        salaryMin: Double? = null,
        postedWithinDays: Int? = null,
        query: String? = null,
        category: String? = null
    ): Flow<Resource<List<Job>>>
    fun getJob(jobId: String): Flow<Resource<Job>>
    fun applyForJob(jobId: String): Flow<Resource<String>> // Returns conversation_id
    suspend fun setJobSaved(jobId: String, saved: Boolean): Resource<Boolean>
    suspend fun markClickedExternalLocally(jobId: String)
    fun getJobApplications(): Flow<Resource<List<JobApplication>>>
    fun getJobProfile(): Flow<Resource<JobProfileData>>
    fun withdrawApplication(applicationId: String): Flow<Resource<Boolean>>
    fun updateJobProfile(profile: JobProfileData): Flow<Resource<Boolean>>
    fun createJob(
        title: String,
        description: String,
        category: String,
        skills: List<String>,
        durationDays: Int,
        company: String? = null,
        locationCity: String? = null,
        jobType: String? = null,
        packageType: String = "basic",
        salaryMin: Double? = null,
        salaryMax: Double? = null,
        requirements: String? = null
    ): Flow<Resource<Job>>
    fun searchJobs(
        query: String,
        location: String? = null,
        salaryMin: Double? = null,
        jobType: String? = null,
        featuredOnly: Boolean = false,
        postedWithinDays: Int? = null,
        category: String? = null
    ): Flow<Resource<List<Job>>>
    suspend fun reportJob(jobId: String, reason: String, details: String = ""): Resource<Boolean>
    suspend fun anonymizeJobData(): Resource<Boolean>
    fun listMyJobs(): Flow<Resource<List<Job>>>
    suspend fun closeJob(jobId: String): Resource<Boolean>
    suspend fun renewJob(jobId: String, packageType: String = "basic"): Resource<String?>
}
