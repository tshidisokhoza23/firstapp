package com.tpsk.tj.chat.jobs.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.chat.jobs.model.Job
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.tpsk.tj.domain.repository.JobRepository

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

    fun reportJob(jobId: String, reason: String) {
        viewModelScope.launch { repository.reportJob(jobId, reason) }
    }

    fun loadJob(jobId: String) {
        viewModelScope.launch {
            repository.getJob(jobId).collect { _job.value = it }
        }
    }

    fun toggleSave() {
        val current = (_job.value as? Resource.Success)?.data ?: return
        viewModelScope.launch {
            val next = !current.isSaved
            if (repository.setJobSaved(current.id, next) is Resource.Success) {
                _job.value = Resource.Success(current.copy(isSaved = next))
            }
        }
    }

    fun applyForJob(jobId: String, jobTitle: String, companyName: String, coverNote: String = "") {
        viewModelScope.launch {
            _lastApplyContext.value = ApplyContext(jobId, jobTitle, companyName)
            repository.applyForJob(jobId, coverNote).collect { result ->
                _applyResult.value = result
            }
        }
    }

    fun markClickedExternal(jobId: String) {
        viewModelScope.launch {
            repository.markClickedExternalLocally(jobId)
        }
    }

    fun resetApplyResult() {
        _applyResult.value = null
    }
}
