package com.tpsk.tj.chat.jobs.data.repository

import com.tpsk.tj.chat.jobs.data.api.*
import com.tpsk.tj.chat.jobs.data.local.dao.JobApplicationDao
import com.tpsk.tj.chat.jobs.data.local.dao.JobDao
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.domain.repository.JobRepository
import com.tpsk.tj.chat.jobs.model.Job
import com.tpsk.tj.chat.jobs.model.JobApplication
import com.tpsk.tj.chat.jobs.model.JobProfileData
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map as flowMap
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

class JobRepositoryImpl @Inject constructor(
    private val jobApi: JobApi,
    private val jobDao: JobDao,
    private val jobApplicationDao: JobApplicationDao,
) : JobRepository {

    override fun getJobs(
        page: Int,
        jobType: String?,
        featuredOnly: Boolean,
        location: String?,
        salaryMin: Double?,
        postedWithinDays: Int?,
        query: String?,
        category: String?
    ): Flow<Resource<List<Job>>> = flow {
        val filtering = !jobType.isNullOrBlank() || featuredOnly || !location.isNullOrBlank() || salaryMin != null || postedWithinDays != null || !query.isNullOrBlank() || !category.isNullOrBlank()
        val cachedEntities = jobDao.getJobs().first()
        val cached = cachedEntities.map { it.toModel() }
        if (!filtering && cached.isNotEmpty()) {
            emit(Resource.Loading(cached))
        } else {
            emit(Resource.Loading())
        }

        try {
            val response = withTimeout(12_000) {
                jobApi.getJobs(
                    page = page,
                    jobType = jobType,
                    featured = if (featuredOnly) true else null,
                    location = location,
                    salaryMin = salaryMin,
                    postedWithinDays = postedWithinDays,
                    query = query,
                    category = category,
                    sort = if (query.isNullOrBlank()) "match" else "relevance"
                )
            }
            if (response.isSuccessful) {
                val apiJobs = response.body()?.jobs ?: emptyList<ApiJob>()
                Log.d("JobRepo", "Fetched ${apiJobs.size} jobs from network")
                val jobs = apiJobs.map { aj: ApiJob -> aj.toModel() }
                if (filtering) {
                    emit(Resource.Success(jobs))
                    return@flow
                }
                if (page == 1) jobDao.clearJobs()
                jobDao.insertJobs(jobs.map { it.toEntity() })
            } else {
                Log.e("JobRepo", "getJobs network failed: ${response.code()} ${response.errorBody()?.string()}")
                if (filtering) {
                    emit(Resource.Error("Could not load jobs", cached))
                    return@flow
                }
            }
        } catch (e: Exception) { 
            Log.e("JobRepo", "getJobs failed with exception", e)
            if (filtering) {
                emit(Resource.Error(e.localizedMessage ?: "Network error", cached))
                return@flow
            }
        }

        emitAll(jobDao.getJobs().flowMap { entities ->
            Resource.Success(entities.map { it.toModel() })
        })
    }

    override fun getJob(jobId: String): Flow<Resource<Job>> = flow {
        emit(Resource.Loading())
        try {
            val response = jobApi.getJob(jobId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                runCatching { jobApi.recordView(jobId) }
                val job = body.toModel().copy(viewsCount = (body.views_count ?: 0L) + 1)
                jobDao.insertJobs(listOf(job.toEntity()))
                emit(Resource.Success(job))
                return@flow
            }
        } catch (e: Exception) {
            Log.e("JobRepo", "getJob failed", e)
        }
        val cached = jobDao.getJobs().first().map { it.toModel() }.find { it.id == jobId }
        if (cached != null) emit(Resource.Success(cached))
        else emit(Resource.Error("Could not load this job"))
    }

    override suspend fun setJobSaved(jobId: String, saved: Boolean): Resource<Boolean> {
        return try {
            val response = if (saved) jobApi.saveJob(jobId) else jobApi.unsaveJob(jobId)
            if (response.isSuccessful) {
                jobDao.updateSavedStatus(jobId, saved)
                Resource.Success(true)
            } else {
                Resource.Error("Could not update saved job")
            }
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Could not update saved job")
        }
    }

    override fun applyForJob(jobId: String, coverNote: String): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            val response = jobApi.applyForJob(jobId, com.tpsk.tj.chat.jobs.data.api.ApplyJobRequest(coverNote))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                jobDao.updateAppliedStatus(jobId = jobId, isApplied = true)
                emit(Resource.Success(body.conversation_id))
            } else {
                val detail = response.errorBody()?.string()?.trim().orEmpty()
                emit(Resource.Error(detail.ifBlank { "Application failed (${response.code()})" }))
            }
        } catch (e: Exception) { emit(Resource.Error("Network error: ${e.message}")) }
    }

    override suspend fun markClickedExternalLocally(jobId: String) {
        try {
            jobDao.markClickedExternalLocally(jobId)
        } catch (e: Exception) {
            Log.e("JobRepo", "Failed to mark external click locally", e)
        }
    }

    override fun getJobApplications(): Flow<Resource<List<JobApplication>>> = flow {
        val cached = jobApplicationDao.getApplications().first().map { it.toModel() }
        if (cached.isNotEmpty()) {
            emit(Resource.Loading(cached))
        } else {
            emit(Resource.Loading())
        }
        try {
            val response = jobApi.getJobApplications()
            if (response.isSuccessful) {
                val apiApps = response.body()?.applications ?: emptyList<ApiJobApplication>()
                val applications = apiApps.map { aa: ApiJobApplication -> aa.toModel() }
                jobApplicationDao.clearApplications()
                jobApplicationDao.insertApplications(applications.map { it.toEntity() })
            }
        } catch (e: Exception) {
            Log.e("JobRepo", "getJobApplications failed", e)
        }
        emitAll(jobApplicationDao.getApplications().flowMap { entities ->
            Resource.Success(entities.map { it.toModel() })
        })
    }

    override fun getJobProfile(): Flow<Resource<JobProfileData>> = flow {
        emit(Resource.Loading())
        try {
            val response = jobApi.getJobProfile()
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun withdrawApplication(applicationId: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (jobApi.withdrawApplication(applicationId).isSuccessful) {
                jobApplicationDao.deleteApplication(applicationId)
                emit(Resource.Success(true))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun updateJobProfile(profile: JobProfileData): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = jobApi.updateJobProfile(profile)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                val detail = response.errorBody()?.string()?.takeIf { it.isNotBlank() } ?: "Could not save profile (${response.code()})"
                Log.e("JobRepo", "updateJobProfile failed: $detail")
                emit(Resource.Error(detail))
            }
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun createJob(
        title: String,
        description: String,
        category: String,
        skills: List<String>,
        durationDays: Int,
        company: String?,
        locationCity: String?,
        jobType: String?,
        packageType: String,
        salaryMin: Double?,
        salaryMax: Double?,
        requirements: String?
    ): Flow<Resource<Job>> = flow {
        emit(Resource.Loading())
        try {
            val req = CreateJobRequest(
                title = title,
                description = description,
                job_category = category.ifBlank { "General" },
                required_skills = skills,
                duration_days = durationDays,
                company = company,
                location_city = locationCity,
                job_type = jobType,
                package_type = packageType,
                salary_min = salaryMin,
                salary_max = salaryMax,
                requirements = requirements,
                category = category.ifBlank { "General" },
                skills = skills
            )
            val response = jobApi.createJob(req)
            val apiJob = response.body()
            if (response.isSuccessful && apiJob != null) {
                val job = apiJob.toModel()
                jobDao.insertJobs(listOf(job.toEntity()))
                emit(Resource.Success(job))
            } else {
                val detail = response.errorBody()?.string()?.trim().orEmpty()
                emit(Resource.Error(detail.ifBlank { "Failed to create job (${response.code()})" }))
            }
        } catch (e: Exception) { emit(Resource.Error("Network error: ${e.message}")) }
    }

    override fun searchJobs(
        query: String,
        location: String?,
        salaryMin: Double?,
        jobType: String?,
        featuredOnly: Boolean,
        postedWithinDays: Int?,
        category: String?
    ): Flow<Resource<List<Job>>> = flow {
        emit(Resource.Loading())
        try {
            val response = jobApi.search(
                query = query,
                location = location,
                salaryMin = salaryMin,
                jobType = jobType,
                featured = if (featuredOnly) true else null,
                postedWithinDays = postedWithinDays,
                category = category
            )
            if (response.isSuccessful) {
                emit(Resource.Success(response.body()?.jobs.orEmpty().map { it.toModel() }))
            } else {
                emit(Resource.Error("Search failed"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Search failed"))
        }
    }

    override suspend fun reportJob(jobId: String, reason: String, details: String): Resource<Boolean> {
        return try {
            val response = jobApi.reportJob(jobId, ReportJobRequest(reason, details))
            if (response.isSuccessful) Resource.Success(true) else Resource.Error("Could not report job")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Could not report job")
        }
    }

    override suspend fun anonymizeJobData(): Resource<Boolean> {
        return try {
            val response = jobApi.anonymizeJobData()
            if (response.isSuccessful) Resource.Success(true) else Resource.Error("Could not anonymize job data")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Could not anonymize job data")
        }
    }

    override fun listMyJobs(): Flow<Resource<List<Job>>> = flow {
        emit(Resource.Loading())
        try {
            val response = jobApi.getMyJobs()
            if (response.isSuccessful) emit(Resource.Success(response.body()?.jobs.orEmpty().map { it.toModel() }))
            else emit(Resource.Error("Could not load your jobs"))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Could not load your jobs"))
        }
    }

    override suspend fun closeJob(jobId: String): Resource<Boolean> {
        return try {
            val response = jobApi.closeJob(jobId)
            if (response.isSuccessful) Resource.Success(true) else Resource.Error("Could not close job")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Could not close job")
        }
    }

    override suspend fun renewJob(jobId: String, packageType: String): Resource<String?> {
        return try {
            val response = jobApi.renewJob(jobId, RenewJobRequest(packageType))
            if (response.isSuccessful) Resource.Success(response.body()?.authorization_url)
            else Resource.Error("Could not renew job")
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Could not renew job")
        }
    }
}

