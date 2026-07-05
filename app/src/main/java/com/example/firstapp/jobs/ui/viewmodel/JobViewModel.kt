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

    private val _createJobResult = MutableStateFlow<Resource<Job>?>(null)
    val createJobResult: StateFlow<Resource<Job>?> = _createJobResult.asStateFlow()

    init {
        fetchJobs()
        fetchJobApplications()
    }

    fun fetchJobs() {
        viewModelScope.launch { repository.getJobs().collect { _jobs.value = it } }
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

    fun createJob(title: String, description: String, category: String, skills: List<String>, durationDays: Int) {
        viewModelScope.launch {
            repository.createJob(title, description, category, skills, durationDays).collect { 
                _createJobResult.value = it 
            }
        }
    }
}
