package com.example.firstapp.chat.jobs.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.chat.jobs.model.Job
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.domain.repository.JobRepository

@HiltViewModel
class JobDetailViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _job = MutableStateFlow<Resource<Job>>(Resource.Loading())
    val job: StateFlow<Resource<Job>> = _job.asStateFlow()

    private val _applyResult = MutableStateFlow<Resource<String>?>(null)
    val applyResult: StateFlow<Resource<String>?> = _applyResult.asStateFlow()

    data class ApplyContext(val jobId: String, val jobTitle: String, val companyName: String)
    private val _lastApplyContext = MutableStateFlow<ApplyContext?>(null)
    val lastApplyContext: StateFlow<ApplyContext?> = _lastApplyContext.asStateFlow()

    fun loadJob(jobId: String) {
        viewModelScope.launch {
            repository.getJobs().collect { resource ->
                if (resource is Resource.Success) {
                    val j = resource.data?.find { it.id == jobId }
                    if (j != null) _job.value = Resource.Success(j)
                    else _job.value = Resource.Error("Job not found")
                } else if (resource is Resource.Error) {
                    _job.value = Resource.Error(resource.message ?: "Failed to load")
                }
            }
        }
    }

    fun applyForJob(jobId: String, jobTitle: String, companyName: String) {
        viewModelScope.launch {
            _lastApplyContext.value = ApplyContext(jobId, jobTitle, companyName)
            repository.applyForJob(jobId).collect { result ->
                _applyResult.value = result
            }
        }
    }

    fun resetApplyResult() {
        _applyResult.value = null
    }
}
