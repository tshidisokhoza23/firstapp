package com.example.firstapp.jobs.data.repository

import com.example.firstapp.jobs.data.api.*
import com.example.firstapp.common.data.api.UserApi
import com.example.firstapp.common.data.local.dao.JobDao
import com.example.firstapp.common.util.Resource
import com.example.firstapp.domain.repository.JobRepository
import com.example.firstapp.jobs.model.Job
import com.example.firstapp.jobs.model.JobApplication
import com.example.firstapp.jobs.model.JobProfileData
import com.example.firstapp.jobs.model.ApplicationStatus
import com.example.firstapp.common.ui.theme.AppPrimary
import com.example.firstapp.common.ui.theme.AppAccent
import kotlinx.coroutines.flow.*
import android.util.Log

import javax.inject.Inject

class JobRepositoryImpl @Inject constructor(
    private val jobApi: JobApi,
    private val userApi: UserApi,
    private val jobDao: JobDao,
) : JobRepository {

    override fun getJobs(): Flow<Resource<List<Job>>> = flow {
        val cached = jobDao.getJobs().first().map { it.toModel() }
        emit(Resource.Loading(cached))
        try {
            val response = jobApi.getJobs()
            if (response.isSuccessful) {
                val jobs = response.body()?.jobs?.map { apiJob -> 
                    Job(
                        id = apiJob.id, 
                        title = apiJob.title, 
                        companyName = apiJob.company_name, 
                        companyLogoUrl = apiJob.company_logo_url, 
                        location = apiJob.location, 
                        logoColor = AppPrimary, 
                        salaryRange = apiJob.salary_range ?: "", 
                        jobType = apiJob.job_type ?: "Full-time", 
                        postedTime = apiJob.posted_time ?: "", 
                        description = apiJob.description ?: "", 
                        requirements = apiJob.requirements ?: emptyList(), 
                        isVerified = apiJob.is_verified, 
                        isApplied = apiJob.is_applied, 
                        isSaved = apiJob.is_saved,
                    ) 
                } ?: emptyList()
                jobDao.clearJobs()
                jobDao.insertJobs(jobs.map { it.toEntity() })
            }
        } catch (e: Exception) {
            Log.e("JobRepositoryImpl", "getJobs failed", e)
        }
        jobDao.getJobs().collect { entities ->
            val models = entities.map { it.toModel() }
            emit(Resource.Success(models))
        }
    }

    override fun applyForJob(jobId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = jobApi.applyForJob(jobId)
            if (response.isSuccessful) {
                jobDao.updateAppliedStatus(jobId = jobId, isApplied = true)
                emit(Resource.Success(data = true))
            } else {
                emit(Resource.Error("Application failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("JobRepositoryImpl", "applyForJob failed", e)
            emit(Resource.Error("Network error: ${e.localizedMessage}"))
        }
    }

    override fun getJobApplications(): Flow<Resource<List<JobApplication>>> = flow {
        emit(Resource.Loading())
        try {
            val response = jobApi.getJobApplications()
            if (response.isSuccessful) {
                val applications = response.body()?.applications?.map { apiApp -> 
                    JobApplication(
                        id = apiApp.id, 
                        jobId = apiApp.job_id, 
                        jobTitle = apiApp.title ?: "Job #${apiApp.job_id}", 
                        companyName = apiApp.company_name ?: "Unknown company", 
                        location = apiApp.location ?: "",
                        appliedDate = apiApp.applied_date, 
                        status = when (apiApp.status.lowercase()) { 
                            "reviewing" -> ApplicationStatus.REVIEWING
                            "interview" -> ApplicationStatus.INTERVIEW
                            "rejected" -> ApplicationStatus.REJECTED
                            "offered" -> ApplicationStatus.OFFERED
                            else -> ApplicationStatus.PENDING 
                        }, 
                        logoColor = AppAccent,
                    ) 
                } ?: emptyList()
                emit(Resource.Success(applications))
                
                // Still update the local applied status of jobs if we have them
                applications.forEach { 
                    jobDao.updateAppliedStatus(it.jobId, true)
                }
            } else {
                emit(Resource.Error("Failed to fetch applications from server"))
            }
        } catch (e: Exception) {
            Log.e("JobRepositoryImpl", "getJobApplications failed", e)
            emit(Resource.Error("Network error: ${e.localizedMessage}"))
        }
    }

    override fun withdrawApplication(applicationId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = jobApi.withdrawApplication(applicationId)
            if (response.isSuccessful) {
                emit(Resource.Success(data = true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            Log.e("JobRepositoryImpl", "withdrawApplication failed", e)
            emit(Resource.Error("Offline"))
        }
    }

    override fun getJobProfile(): Flow<Resource<JobProfileData>> = flow {
        emit(Resource.Loading())
        try {
            val response = userApi.getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!.user
                val profile = JobProfileData(
                    fullName = user.jobFullName ?: "",
                    email = user.jobEmail ?: "",
                    phone = user.jobPhone ?: "",
                    location = user.jobLocation ?: "",
                    education = user.jobEducation ?: "",
                    experience = user.jobExperience ?: "",
                    skills = user.jobSkills ?: "",
                    aboutMe = user.jobAboutMe ?: "",
                    linkedinUrl = user.jobLinkedinUrl ?: "",
                    portfolioUrl = user.jobPortfolioUrl ?: "",
                    workPreference = user.jobWorkPreference ?: "Remote",
                    availability = user.jobAvailability ?: "Immediately",
                    expectedSalary = user.jobExpectedSalary ?: ""
                )
                emit(Resource.Success(profile))
            } else {
                emit(Resource.Error("Failed to fetch profile"))
            }
        } catch (e: Exception) {
            Log.e("JobRepositoryImpl", "getJobProfile failed", e)
            emit(Resource.Error("Network error: ${e.localizedMessage}"))
        }
    }

    override fun updateJobProfile(profile: JobProfileData): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = jobApi.updateJobProfile(profile)
            if (response.isSuccessful) {
                emit(Resource.Success(data = true))
            } else {
                emit(Resource.Error("Server update failed"))
            }
        } catch (e: Exception) {
            Log.e("JobRepositoryImpl", "updateJobProfile failed", e)
            emit(Resource.Error("Offline: ${e.localizedMessage}"))
        }
    }

    override fun searchJobs(query: String): Flow<List<Job>> = 
        jobDao.searchJobs(query).map { list -> list.map { it.toModel() } }
}
