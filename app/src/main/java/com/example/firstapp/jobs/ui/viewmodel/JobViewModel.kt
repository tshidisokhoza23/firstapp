package com.example.firstapp.jobs.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.jobs.model.*
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.domain.repository.JobRepository

@HiltViewModel
class JobViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _jobs = MutableStateFlow<Resource<List<Job>>>(Resource.Loading())
    val jobs: StateFlow<Resource<List<Job>>> = _jobs.asStateFlow()

    private val _jobProfile = MutableStateFlow<JobProfileData?>(null)
    val jobProfile: StateFlow<JobProfileData?> = _jobProfile.asStateFlow()

    private val _applications = MutableStateFlow<List<JobApplication>>(emptyList())
    val applications: StateFlow<List<JobApplication>> = _applications.asStateFlow()

    init {
        fetchJobs()
        fetchJobApplications()
        fetchJobProfile()
    }

    fun fetchJobs() {
        viewModelScope.launch { repository.getJobs().collect { _jobs.value = it } }
    }

    fun fetchJobApplications() {
        viewModelScope.launch { repository.getJobApplications().collect { _applications.value = it.data ?: emptyList() } }
    }

    fun fetchJobProfile() {
        viewModelScope.launch {
            repository.getJobProfile().collect { result ->
                if (result is Resource.Success) {
                    _jobProfile.value = result.data
                }
            }
        }
    }

    fun applyForJob(jobId: Int) {
        viewModelScope.launch {
            repository.applyForJob(jobId).collect { result ->
                if (result is Resource.Success && (result.data == true)) {
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

    fun withdrawApplication(applicationId: Int) {
        _applications.value = _applications.value.filter { it.id != applicationId }
        viewModelScope.launch { repository.withdrawApplication(applicationId).collect { } }
    }
}
