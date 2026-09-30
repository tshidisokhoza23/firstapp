package com.tpsk.tj.chat.jobs.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.chat.jobs.model.JobProfileData
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.tpsk.tj.domain.repository.JobRepository

@HiltViewModel
class JobProfileViewModel @Inject constructor(
    private val repository: JobRepository
) : ViewModel() {

    private val _profile = MutableStateFlow<Resource<JobProfileData>>(Resource.Loading())
    val profile: StateFlow<Resource<JobProfileData>> = _profile.asStateFlow()

    private val _saveResult = MutableStateFlow<Resource<Boolean>?>(null)
    val saveResult: StateFlow<Resource<Boolean>?> = _saveResult.asStateFlow()

    init {
        fetchProfile()
    }

    fun fetchProfile() {
        viewModelScope.launch {
            repository.getJobProfile().collect {
                _profile.value = it
            }
        }
    }

    fun updateProfile(profile: JobProfileData) {
        viewModelScope.launch {
            repository.updateJobProfile(profile).collect {
                _saveResult.value = it
            }
        }
    }

    fun resetState() {
        _saveResult.value = null
    }
}
