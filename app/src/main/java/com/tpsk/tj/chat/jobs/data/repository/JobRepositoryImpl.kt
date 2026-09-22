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
import javax.inject.Inject

class JobRepositoryImpl @Inject constructor(
    private val jobApi: JobApi,
    private val jobDao: JobDao,
    private val jobApplicationDao: JobApplicationDao,
) : JobRepository {

    override fun getJobs(page: Int): Flow<Resource<List<Job>>> = flow {
        val cachedEntities = jobDao.getJobs().first()
        val cached = cachedEntities.map { it.toModel() }
        if (cached.isNotEmpty()) {
            emit(Resource.Loading(cached))
        } else {
            emit(Resource.Loading())
        }

        try {
            val response = jobApi.getJobs(page)
            if (response.isSuccessful) {
                val apiJobs = response.body()?.jobs ?: emptyList<ApiJob>()
                Log.d("JobRepo", "Fetched ${apiJobs.size} jobs from network")
                val jobs = apiJobs.map { aj: ApiJob -> aj.toModel() }
                if (page == 1) jobDao.clearJobs()
                jobDao.insertJobs(jobs.map { it.toEntity() })
            } else {
                Log.e("JobRepo", "getJobs network failed: ${response.code()} ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) { 
            Log.e("JobRepo", "getJobs failed with exception", e)
        }

        emitAll(jobDao.getJobs().flowMap { entities ->
            val models = entities.map { it.toModel() }
            Resource.Success<List<Job>>(models)
        })
    }

    override fun applyForJob(jobId: String): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            val response = jobApi.applyForJob(jobId)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                jobDao.updateAppliedStatus(jobId = jobId, isApplied = true)
                emit(Resource.Success(body.conversation_id))
            } else {
                emit(Resource.Error("Application failed"))
            }
        } catch (e: Exception) { emit(Resource.Error("Network error: ${e.message}")) }
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
            if (jobApi.updateJobProfile(profile).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun createJob(title: String, description: String, category: String, skills: List<String>, durationDays: Int): Flow<Resource<Job>> = flow {
        emit(Resource.Loading())
        try {
            val req = CreateJobRequest(title, description, category, skills, durationDays)
            val response = jobApi.createJob(req)
            val apiJob = response.body()
            if (response.isSuccessful && apiJob != null) {
                emit(Resource.Success(apiJob.toModel()))
            } else {
                emit(Resource.Error("Failed to create job"))
            }
        } catch (e: Exception) { emit(Resource.Error("Network error: ${e.message}")) }
    }

    override fun searchJobs(query: String): Flow<Resource<List<Job>>> = flow {
        emit(Resource.Loading())
        try {
            val response = jobApi.getJobs(page = 1) 
            if (response.isSuccessful) {
                val apiJobs = response.body()?.jobs ?: emptyList<ApiJob>()
                val filtered = apiJobs.filter { aj: ApiJob ->
                    aj.title.contains(query, ignoreCase = true) || aj.company_name.contains(query, ignoreCase = true)
                }.map { f: ApiJob -> f.toModel() }
                emit(Resource.Success(filtered))
            }
        } catch (_: Exception) {}
    }
}
