package com.tpsk.tj.chat.jobs.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.chat.jobs.model.*
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.tpsk.tj.domain.repository.JobRepository

@HiltViewModel
class JobViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _jobs = MutableStateFlow<Resource<List<Job>>>(Resource.Loading())
    val jobs: StateFlow<Resource<List<Job>>> = _jobs.asStateFlow()
    private val _feedChip = MutableStateFlow("All")
    val feedChip: StateFlow<String> = _feedChip.asStateFlow()
    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()
    private val _salaryMin = MutableStateFlow("")
    val salaryMin: StateFlow<String> = _salaryMin.asStateFlow()
    private val _postedWithinDays = MutableStateFlow(0)
    val postedWithinDays: StateFlow<Int> = _postedWithinDays.asStateFlow()
    private val _myJobs = MutableStateFlow<Resource<List<Job>>>(Resource.Loading())
    val myJobs: StateFlow<Resource<List<Job>>> = _myJobs.asStateFlow()

    private val _jobProfile = MutableStateFlow<JobProfileData?>(null)
    val jobProfile: StateFlow<JobProfileData?> = _jobProfile.asStateFlow()

    private val _applications = MutableStateFlow<List<JobApplication>>(emptyList())
    val applications: StateFlow<List<JobApplication>> = _applications.asStateFlow()

    private val _createJobResult = MutableStateFlow<Resource<Job>?>(null)
    val createJobResult: StateFlow<Resource<Job>?> = _createJobResult.asStateFlow()

    init {
        fetchJobs()
        fetchJobApplications()
    }

    fun selectFeedChip(chip: String) {
        _feedChip.value = chip
        fetchJobs()
    }

    fun setLocation(value: String) {
        _location.value = value
        fetchJobs()
    }

    fun setSalaryMin(value: String) {
        _salaryMin.value = value
        fetchJobs()
    }

    fun setPostedWithinDays(days: Int) {
        _postedWithinDays.value = days
        fetchJobs()
    }

    fun fetchJobs() {
        val chip = _feedChip.value
        val category = if (chip == "All") null else chip
        val salary = _salaryMin.value.toDoubleOrNull()
        viewModelScope.launch {
            repository.getJobs(
                location = _location.value.trim().ifBlank { null },
                salaryMin = salary,
                postedWithinDays = _postedWithinDays.value.takeIf { it > 0 },
                category = category
            ).collect { _jobs.value = it }
        }
    }

    fun search(query: String) {
        val category = _feedChip.value.takeUnless { it == "All" }
        viewModelScope.launch {
            if (query.isBlank()) {
                fetchJobs()
            } else {
                repository.searchJobs(query, category = category).collect { _jobs.value = it }
            }
        }
    }

    fun fetchMyJobs() {
        viewModelScope.launch { repository.listMyJobs().collect { _myJobs.value = it } }
    }

    fun closeJob(jobId: String) {
        viewModelScope.launch {
            if (repository.closeJob(jobId) is Resource.Success) fetchMyJobs()
        }
    }

    fun renewJob(jobId: String, packageType: String = "basic", onCheckout: (String) -> Unit = {}) {
        viewModelScope.launch {
            val result = repository.renewJob(jobId, packageType)
            val url = (result as? Resource.Success)?.data
            if (!url.isNullOrBlank()) onCheckout(url)
        }
    }

    fun anonymizeJobData(onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            onDone(repository.anonymizeJobData() is Resource.Success)
        }
    }

    fun fetchJobApplications() {
        viewModelScope.launch { repository.getJobApplications().collect { _applications.value = it.data ?: emptyList() } }
    }

    fun applyForJob(jobId: String) {
        viewModelScope.launch {
            repository.applyForJob(jobId).collect { result ->
                if (result is Resource.Success) {
                    fetchJobs()
                    fetchJobApplications()
                }
            }
        }
    }

    fun updateJobProfile(profile: JobProfileData) {
        viewModelScope.launch {
            repository.updateJobProfile(profile).collect { result ->
                if (result is Resource.Success) _jobProfile.value = profile
            }
        }
    }

    fun withdrawApplication(applicationId: String) {
        _applications.value = _applications.value.filter { it.id != applicationId }
        viewModelScope.launch { repository.withdrawApplication(applicationId).collect { } }
    }

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
    ) {
        viewModelScope.launch {
            repository.createJob(
                title, description, category, skills, durationDays,
                company, locationCity, jobType, packageType, salaryMin, salaryMax, requirements
            ).collect { _createJobResult.value = it }
        }
    }
}
