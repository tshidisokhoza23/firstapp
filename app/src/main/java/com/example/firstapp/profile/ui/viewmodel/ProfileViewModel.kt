package com.example.firstapp.profile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.media.model.Post
import com.example.firstapp.common.model.User
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.collections.*
import kotlin.*

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.vocab.domain.repository.UserRepository
import com.example.firstapp.media.domain.repository.SocialRepository
import com.example.firstapp.profile.model.WorkExperience
import com.example.firstapp.common.util.MediaUtils
import android.content.Context
import android.net.Uri

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val repository: SocialRepository
) : ViewModel() {

    private val _profileUser = MutableStateFlow<Resource<User>>(Resource.Loading())
    val profileUser: StateFlow<Resource<User>> = _profileUser.asStateFlow()

    private val _userPosts = MutableStateFlow<Resource<List<Post>>>(Resource.Loading())
    val userPosts: StateFlow<Resource<List<Post>>> = _userPosts.asStateFlow()

    private val _workExperiences = MutableStateFlow<Resource<List<WorkExperience>>>(Resource.Loading())
    val workExperiences: StateFlow<Resource<List<WorkExperience>>> = _workExperiences.asStateFlow()

    private val _updateState = MutableStateFlow<Resource<Any>?>(null)
    val updateState: StateFlow<Resource<Any>?> = _updateState.asStateFlow()

    fun loadProfile(userId: String?) {
        viewModelScope.launch {
            if (userId == null) {
                userRepository.getCurrentUser().collect { _profileUser.value = it }
            } else {
                userRepository.getUserData(userId).collect { user ->
                    if (user != null) {
                        _profileUser.value = Resource.Success(user)
                    } else {
                        _profileUser.value = Resource.Error("User not found")
                    }
                }
            }
        }
    }

    fun loadWorkExperiences() {
        viewModelScope.launch {
            userRepository.getWorkExperiences().collect { _workExperiences.value = it }
        }
    }

    fun addWorkExperience(experience: WorkExperience) {
        viewModelScope.launch {
            userRepository.addWorkExperience(experience).collect { resource ->
                _updateState.value = resource
                if (resource is Resource.Success) {
                    loadWorkExperiences()
                }
            }
        }
    }

    fun updateWorkExperience(experience: WorkExperience) {
        viewModelScope.launch {
            userRepository.updateWorkExperience(experience).collect { resource ->
                _updateState.value = resource
                if (resource is Resource.Success) {
                    loadWorkExperiences()
                }
            }
        }
    }

    fun deleteWorkExperience(experienceId: String) {
        viewModelScope.launch {
            userRepository.deleteWorkExperience(experienceId).collect { resource ->
                if (resource is Resource.Success) {
                    loadWorkExperiences()
                }
            }
        }
    }

    fun clearUpdateState() {
        _updateState.value = null
    }

    fun loadUserPosts(userId: String?) {
        viewModelScope.launch {
            val uid = if (userId != null) userId else {
                val current = (profileUser.value as? Resource.Success)?.data
                current?.id ?: ""
            }
            if (uid == "") {
                _userPosts.value = Resource.Success(kotlin.collections.emptyList())
                return@launch
            }
            repository.getUserPosts(uid, "standard", 1).collect { resource ->
                _userPosts.value = resource
            }
        }
    }

    fun updateProfile(displayName: String, bio: String, location: String) {
        viewModelScope.launch {
            userRepository.updateUser(displayName, bio, location, null).collect { resource ->
                if (resource is Resource.Success) {
                    _profileUser.value = resource
                }
            }
        }
    }

    fun onAvatarSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            _updateState.value = Resource.Loading()
            
            userRepository.updateAvatar(context, uri).collect { resource ->
                _updateState.value = resource
                if (resource is Resource.Success) {
                    val current = (_profileUser.value as? Resource.Success)?.data
                    if (current != null) {
                        _profileUser.value = Resource.Success(current.copy(avatarUrl = resource.data))
                    }
                }
            }
        }
    }
}
