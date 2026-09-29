package com.tpsk.tj.chat.jobs.data.api

import com.tpsk.tj.common.data.api.StatusResponse
import com.tpsk.tj.chat.jobs.model.JobProfileData
import retrofit2.Response
import retrofit2.http.*

interface JobApi {
    @GET("jobs")
    suspend fun getJobs(
        @Query("page") page: Int = 1,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<JobsResponse>

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

data class JobsResponse(
    val jobs: List<ApiJob>,
    val next_cursor: String? = null
)
data class JobApplicationsResponse(val applications: List<ApiJobApplication>)
data class ConversationResponse(val conversation_id: String)
data class CreateJobRequest(
    val title: String,
    val description: String,
    val job_category: String,
    val required_skills: List<String>,
    val duration_days: Int,
    val company: String? = null,
    val location_city: String? = null,
    val category: String? = null,
    val skills: List<String>? = null
)

data class ApiJob(
    val id: String,
    val title: String,
    val company_name: String,
    val company_logo_url: String?,
    val location: String,
    val salary_range: String?,
    val job_type: String?,
    val posted_time: Long,
    val description: String?,
    val requirements: List<String>?,
    val is_verified: Boolean,
    val created_by: String?,
    val is_applied: Boolean,
    val is_saved: Boolean,
    val is_external: Boolean? = false,
    val source_partner: String? = null,
    val apply_redirect_url: String? = null,
    val tracking_apply_url: String? = null,
    val apply_url: String? = null,
    val has_applied_internal: Boolean? = false,
    val has_clicked_external: Boolean? = false
) {
    fun getTrackingUrl(): String? = apply_redirect_url ?: tracking_apply_url ?: apply_url

    fun toModel(): com.tpsk.tj.chat.jobs.model.Job = com.tpsk.tj.chat.jobs.model.Job(
        id = id,
        title = title,
        companyName = company_name,
        companyLogoUrl = company_logo_url,
        location = location,
        logoColor = androidx.compose.ui.graphics.Color(0xFF2196F3), 
        salaryRange = salary_range ?: "",
        jobType = job_type ?: "Full-time",
        postedTime = posted_time,
        description = description ?: "",
        requirements = requirements ?: emptyList(),
        isVerified = is_verified,
        isApplied = is_applied || (has_applied_internal ?: false),
        isSaved = is_saved,
        isExternal = is_external ?: false,
        sourcePartner = source_partner,
        trackingApplyUrl = getTrackingUrl(),
        hasAppliedInternal = is_applied || (has_applied_internal ?: false),
        hasClickedExternal = has_clicked_external ?: false
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
    val location: String? = null,
    val conversation_id: String? = null
) {
    fun toModel(): com.tpsk.tj.chat.jobs.model.JobApplication = com.tpsk.tj.chat.jobs.model.JobApplication(
        id = id,
        jobId = job_id,
        jobTitle = title ?: "Job",
        companyName = company_name ?: "",
        location = location ?: "",
        appliedDate = applied_date,
        status = com.tpsk.tj.chat.jobs.model.ApplicationStatus.PENDING,
        logoColor = androidx.compose.ui.graphics.Color(0xFF2196F3),
        conversationId = conversation_id
    )
}
