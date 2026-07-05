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

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val repository: SocialRepository
) : ViewModel() {

    private val _profileUser = MutableStateFlow<Resource<User>>(Resource.Loading())
    val profileUser: StateFlow<Resource<User>> = _profileUser.asStateFlow()

    private val _userPosts = MutableStateFlow<Resource<List<Post>>>(Resource.Loading())
    val userPosts: StateFlow<Resource<List<Post>>> = _userPosts.asStateFlow()

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

    fun loadUserPosts(userId: String?) {
        viewModelScope.launch {
            repository.getPosts(1).collect { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    val filtered = if (userId == null) {
                        val current = (profileUser.value as? Resource.Success)?.data
                        resource.data.filter { it.authorId == current?.id }
                    } else {
                        resource.data.filter { it.authorId == userId }
                    }
                    _userPosts.value = Resource.Success(filtered)
                } else {
                    _userPosts.value = resource
                }
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
}
