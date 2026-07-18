package com.example.firstapp.chat.jobs.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.chat.jobs.model.JobApplication
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.domain.repository.JobRepository

@HiltViewModel
class JobApplicationsViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _applications = MutableStateFlow<Resource<List<JobApplication>>>(Resource.Loading())
    val applications: StateFlow<Resource<List<JobApplication>>> = _applications.asStateFlow()

    init {
        fetchApplications()
    }

    fun fetchApplications() {
        viewModelScope.launch {
            repository.getJobApplications().collect {
                _applications.value = it
            }
        }
    }

    fun withdrawApplication(applicationId: String) {
        viewModelScope.launch {
            repository.withdrawApplication(applicationId).collect {
                if (it is Resource.Success<*>) fetchApplications()
            }
        }
    }
}
