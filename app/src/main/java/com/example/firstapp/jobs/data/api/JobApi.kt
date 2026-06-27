package com.example.firstapp.jobs.data.api

import com.example.firstapp.common.data.api.*
import com.example.firstapp.jobs.model.JobProfileData
import retrofit2.Response
import retrofit2.http.*

interface JobApi {
    @GET("jobs")
    suspend fun getJobs(@Query("page") page: Int = 1): Response<JobsResponse>

    @POST("jobs/{jobId}/apply")
    suspend fun applyForJob(@Path("jobId") jobId: Int): Response<StatusResponse>

    @GET("applications")
    suspend fun getJobApplications(): Response<JobApplicationsResponse>

    @DELETE("applications/{applicationId}")
    suspend fun withdrawApplication(@Path("applicationId") applicationId: Int): Response<StatusResponse>

    @PUT("users/me/job-profile")
    suspend fun updateJobProfile(@Body request: JobProfileData): Response<StatusResponse>
}

data class ApiJob(
    val id: Int,
    val title: String,
    val company_name: String,
    val company_logo_url: String?,
    val location: String,
    val salary_range: String?,
    val job_type: String?,
    val posted_time: String?,
    val description: String?,
    val requirements: List<String>?,
    val is_verified: Boolean,
    val created_by: Int?,
    val is_applied: Boolean,
    val is_saved: Boolean
)

data class JobsResponse(val jobs: List<ApiJob>, val page: Int)

data class ApiJobApplication(
    val id: Int,
    val user_id: Int,
    val job_id: Int,
    val applied_date: Long,
    val status: String,
    val title: String? = null,
    val company_name: String? = null,
    val location: String? = null
)

data class JobApplicationsResponse(val applications: List<ApiJobApplication>)
