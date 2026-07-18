package com.example.firstapp.chat.jobs.data.api

import com.example.firstapp.common.data.api.StatusResponse
import com.example.firstapp.chat.jobs.model.JobProfileData
import retrofit2.Response
import retrofit2.http.*

interface JobApi {
    @GET("jobs")
    suspend fun getJobs(@Query("page") page: Int = 1): Response<JobsResponse>

    @GET("jobs/search")
    suspend fun search(@Query("query") query: String): Response<JobsResponse>

    @POST("jobs/{jobId}/apply")
    suspend fun applyForJob(@Path("jobId") jobId: String): Response<ConversationResponse>

    @POST("jobs/{jobId}/save")
    suspend fun saveJob(@Path("jobId") jobId: String): Response<StatusResponse>

    @DELETE("jobs/{jobId}/save")
    suspend fun unsaveJob(@Path("jobId") jobId: String): Response<StatusResponse>

    @GET("users/me/applications")
    suspend fun getJobApplications(): Response<JobApplicationsResponse>

    @DELETE("job-applications/{applicationId}")
    suspend fun withdrawApplication(@Path("applicationId") applicationId: String): Response<StatusResponse>

    @GET("users/me/job-profile")
    suspend fun getJobProfile(): Response<JobProfileData>

    @PUT("users/me/job-profile")
    suspend fun updateJobProfile(@Body profile: JobProfileData): Response<StatusResponse>

    @POST("jobs")
    suspend fun createJob(@Body request: CreateJobRequest): Response<ApiJob>

    @GET("users/me/saved-jobs")
    suspend fun getSavedJobs(): Response<JobsResponse>
}

data class JobsResponse(val jobs: List<ApiJob>)
data class JobApplicationsResponse(val applications: List<ApiJobApplication>)
data class ConversationResponse(val conversation_id: String)
data class CreateJobRequest(val title: String, val description: String, val category: String, val skills: List<String>, val duration_days: Int)

data class ApiJob(
    val id: String,
    val title: String,
    val company_name: String,
    val company_logo_url: String?,
    val location: String,
    val salary_range: String?,
    val job_type: String,
    val posted_time: Long,
    val description: String,
    val requirements: List<String>?,
    val is_verified: Boolean,
    val created_by: String?,
    val is_applied: Boolean,
    val is_saved: Boolean
) {
    fun toModel(): com.example.firstapp.chat.jobs.model.Job = com.example.firstapp.chat.jobs.model.Job(
        id = id,
        title = title,
        companyName = company_name,
        companyLogoUrl = company_logo_url,
        location = location,
        logoColor = androidx.compose.ui.graphics.Color(0xFF2196F3), 
        salaryRange = salary_range ?: "",
        jobType = job_type,
        postedTime = posted_time,
        description = description,
        requirements = requirements ?: emptyList(),
        isVerified = is_verified,
        isApplied = is_applied,
        isSaved = is_saved
    )
}

data class ApiJobApplication(
    val id: String,
    val user_id: String,
    val job_id: String,
    val applied_date: Long,
    val status: String,
    val title: String? = null,
    val company_name: String? = null,
    val location: String? = null
) {
    fun toModel(): com.example.firstapp.chat.jobs.model.JobApplication = com.example.firstapp.chat.jobs.model.JobApplication(
        id = id,
        jobId = job_id,
        jobTitle = title ?: "Job",
        companyName = company_name ?: "Unknown",
        location = location ?: "",
        appliedDate = applied_date,
        status = com.example.firstapp.chat.jobs.model.ApplicationStatus.PENDING,
        logoColor = androidx.compose.ui.graphics.Color(0xFF2196F3)
    )
}
