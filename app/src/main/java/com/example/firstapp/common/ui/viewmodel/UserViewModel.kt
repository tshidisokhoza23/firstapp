package com.example.firstapp.common.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.common.domain.auth.SessionManager
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.model.User
import com.example.firstapp.common.util.CryptoUtils
import com.example.firstapp.common.util.Resource
import com.example.firstapp.profile.model.WorkExperience
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import kotlin.collections.*
import kotlin.*

import com.example.firstapp.vocab.domain.repository.UserRepository
import com.example.firstapp.account.domain.repository.AccountRepository
import com.example.firstapp.domain.repository.ChatRepository
import com.example.firstapp.common.domain.repository.SystemRepository
import com.example.firstapp.media.domain.repository.SocialRepository
import com.example.firstapp.common.util.MediaUtils
import android.content.Context
import android.net.Uri

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val chatRepository: ChatRepository,
    private val systemRepository: SystemRepository,
    private val socialRepository: SocialRepository,
    private val sessionManager: SessionManager,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    val currentUser: StateFlow<User?> = sessionManager.currentUser
    val currentUserId: String? get() = sessionManager.getUserId()

    val isLoggedIn: StateFlow<Boolean> = sessionManager.authState

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList<Contact>())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _workExperiences = MutableStateFlow<Resource<List<WorkExperience>>>(Resource.Loading())
    val workExperiences: StateFlow<Resource<List<WorkExperience>>> = _workExperiences.asStateFlow()

    private val _updateWorkState = MutableStateFlow<Resource<Any>?>(null)
    val updateWorkState: StateFlow<Resource<Any>?> = _updateWorkState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        fetchCurrentUser()
        fetchContacts()
        ensureE2EEKeys()
        loadWorkExperiences()
    }

    private fun ensureE2EEKeys() {
        viewModelScope.launch {
            val existingPriv = sessionManager.getPrivateKey()
            val existingPub = sessionManager.getPublicKey()
            
            if (existingPriv == null || existingPub == null) {
                val pair = CryptoUtils.generateKeyPair()
                val pub = pair.first
                val priv = pair.second
                sessionManager.setPrivateKey(priv)
                sessionManager.setPublicKey(pub)
                accountRepository.setPublicKey(pub)
            } else {
                accountRepository.setPublicKey(existingPub)
            }
        }
    }

    fun fetchCurrentUser() {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    sessionManager.updateCurrentUser(resource.data!!)
                }
            }
        }
    }

    fun updateProfileLocally(user: User) {
        sessionManager.updateCurrentUser(user)
    }

    fun getUserById(userId: String): kotlinx.coroutines.flow.Flow<Resource<User>> {
        val normalizedId = if (userId == "null" || userId.length == 0) sessionManager.getUserId() ?: "" else userId
        return accountRepository.getUserById(normalizedId)
    }

    fun fetchContacts() {
        viewModelScope.launch {
            chatRepository.getContacts().collect { result ->
                val data = result.data ?: emptyList<Contact>()
                _contacts.value = data
                data.forEach { contact ->
                    com.example.firstapp.chat.data.GlobalCallState.updateContactInfo(contact.id, contact.displayName, contact.phoneNumber)
                }
            }
        }
        syncContactsWithNetwork()
    }

    private fun syncContactsWithNetwork() {
        viewModelScope.launch {
            val phoneNumbers = com.example.firstapp.common.util.ContactUtils.getPhoneContacts(context)
            chatRepository.syncContacts(phoneNumbers).collect { result ->
                if (result is Resource.Success) {
                    _contacts.value = result.data ?: emptyList<Contact>()
                }
            }
        }
    }

    fun updateProfile(displayName: String?, bio: String?, location: String?, avatarUrl: String?, bannerUrl: String? = null, isArtist: Boolean? = null) {
        viewModelScope.launch {
            userRepository.updateUser(displayName, bio, location, avatarUrl, bannerUrl, isArtist).collect { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    sessionManager.updateCurrentUser(resource.data!!)
                    _toastMessage.value = "Profile updated successfully"
                } else if (resource is Resource.Error) {
                    _toastMessage.value = resource.message ?: "Failed to update profile"
                }
            }
        }
    }

    fun uploadProfilePicture(file: File, isBanner: Boolean = false) {
        viewModelScope.launch {
            val folder = if (isBanner) "banners" else "avatars"
            accountRepository.uploadFile(file, folder).collect { resource ->
                if (resource is Resource.Success) {
                    if (isBanner) {
                        updateProfile(null, null, null, null, resource.data?.url)
                    } else {
                        updateProfile(null, null, null, resource.data?.url)
                    }
                } else if (resource is Resource.Error) {
                    _toastMessage.value = "Failed to upload image: ${resource.message}"
                }
            }
        }
    }

    fun uploadProfilePicture(uri: android.net.Uri, context: android.content.Context, isBanner: Boolean = false) {
        viewModelScope.launch {
            val folder = if (isBanner) "banners" else "avatars"
            accountRepository.uploadUri(context, uri, folder).collect { resource ->
                if (resource is Resource.Success) {
                    if (isBanner) updateProfile(null, null, null, null, resource.data?.url)
                    else updateProfile(null, null, null, resource.data?.url)
                } else if (resource is Resource.Error) {
                    _toastMessage.value = "Failed to upload image: ${resource.message}"
                }
            }
        }
    }

    fun onAvatarSelected(context: Context, uri: Uri) {
        viewModelScope.launch {
            _toastMessage.value = "Updating avatar..."
            userRepository.updateAvatar(context, uri).collect { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    val url = resource.data!!
                    val current = currentUser.value
                    if (current != null) {
                        sessionManager.updateCurrentUser(current.copy(avatarUrl = url))
                    }
                    _toastMessage.value = "Avatar updated"
                } else if (resource is Resource.Error) {
                    _toastMessage.value = resource.message ?: "Failed to upload avatar"
                }
            }
        }
    }

    fun forgotPassword(emailOrUsername: String) {
        viewModelScope.launch {
            userRepository.forgotPassword(emailOrUsername).collect { resource ->
                if (resource is Resource.Success) _toastMessage.value = "Password reset instructions sent"
                else if (resource is Resource.Error) _toastMessage.value = resource.message ?: "Request failed"
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            userRepository.deleteAccount().collect { resource ->
                if (resource is Resource.Success) {
                    sessionManager.clearSession()
                    onSuccess()
                } else if (resource is Resource.Error) {
                    _toastMessage.value = resource.message ?: "Failed to delete account"
                }
            }
        }
    }

    fun syncContacts(phoneNumbers: List<String>) {
        viewModelScope.launch {
            accountRepository.syncContacts(phoneNumbers).collect { result ->
                if (result is Resource.Success) {
                    val synced = result.data ?: emptyList<Contact>()
                    val current = _contacts.value.associateBy { it.id }.toMutableMap()
                    synced.forEach { current[it.id] = it }
                    _contacts.value = current.values.toList()
                }
            }
        }
    }

    fun resetAndSyncContacts(context: android.content.Context) {
        viewModelScope.launch {
            _toastMessage.value = "Clearing data and syncing contacts..."
            val phoneContacts = com.example.firstapp.common.util.ContactUtils.getPhoneContacts(context)
            
            systemRepository.clearAllData()
            accountRepository.syncContacts(phoneContacts).collect { res ->
                if (res is Resource.Success) {
                    res.data?.forEach { contact ->
                        if (contact.isRegistered) {
                            launch { chatRepository.initiateDM(contact.id).collect {} }
                        }
                    }
                    _toastMessage.value = "Contacts registered as chats successfully"
                    fetchContacts() 
                }
            }
        }
    }

    fun blockUser(userId: String) {
        viewModelScope.launch {
            accountRepository.blockUser(userId).collect { resource ->
                if (resource is Resource.Success) {
                    _toastMessage.value = "User blocked"
                } else if (resource is Resource.Error) {
                    _toastMessage.value = resource.message ?: "Failed to block user"
                }
            }
        }
    }

    fun followUser(userId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            // Optimistic update for social posts
            socialRepository.updateAuthorFollowStatusLocally(userId, true)
            
            accountRepository.followUser(userId).collect { resource ->
                if (resource is Resource.Success) {
                    _toastMessage.value = "Followed"
                    onComplete(true)
                } else if (resource is Resource.Error) {
                    // Rollback
                    socialRepository.updateAuthorFollowStatusLocally(userId, false)
                    _toastMessage.value = resource.message ?: "Failed to follow user"
                    onComplete(false)
                }
            }
        }
    }

    fun unfollowUser(userId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            // Optimistic update for social posts
            socialRepository.updateAuthorFollowStatusLocally(userId, false)
            
            accountRepository.unfollowUser(userId).collect { resource ->
                if (resource is Resource.Success) {
                    _toastMessage.value = "Unfollowed"
                    onComplete(true)
                } else if (resource is Resource.Error) {
                    // Rollback
                    socialRepository.updateAuthorFollowStatusLocally(userId, true)
                    _toastMessage.value = resource.message ?: "Failed to unfollow user"
                    onComplete(false)
                }
            }
        }
    }

    fun getFollowers(userId: String): kotlinx.coroutines.flow.Flow<Resource<List<Contact>>> {
        return accountRepository.getFollowers(userId)
    }

    fun getFollowing(userId: String): kotlinx.coroutines.flow.Flow<Resource<List<Contact>>> {
        return accountRepository.getFollowing(userId)
    }

    fun reportUser(userId: String, reason: String) {
        viewModelScope.launch {
            accountRepository.reportUser(userId, reason).collect { resource ->
                if (resource is Resource.Success) {
                    _toastMessage.value = "Report submitted"
                } else if (resource is Resource.Error) {
                    _toastMessage.value = resource.message ?: "Failed to report user"
                }
            }
        }
    }

    fun clearToast() { _toastMessage.value = null }

    fun loadWorkExperiences() {
        viewModelScope.launch {
            userRepository.getWorkExperiences().collect { _workExperiences.value = it }
        }
    }

    fun addWorkExperience(experience: WorkExperience) {
        viewModelScope.launch {
            userRepository.addWorkExperience(experience).collect { resource ->
                _updateWorkState.value = resource
                if (resource is Resource.Success) {
                    loadWorkExperiences()
                    _toastMessage.value = "Experience added"
                } else if (resource is Resource.Error) {
                    _toastMessage.value = resource.message ?: "Failed to add experience"
                }
            }
        }
    }

    fun updateWorkExperience(experience: WorkExperience) {
        viewModelScope.launch {
            userRepository.updateWorkExperience(experience).collect { resource ->
                _updateWorkState.value = resource
                if (resource is Resource.Success) {
                    loadWorkExperiences()
                    _toastMessage.value = "Experience updated"
                } else if (resource is Resource.Error) {
                    _toastMessage.value = resource.message ?: "Failed to update experience"
                }
            }
        }
    }

    fun deleteWorkExperience(experienceId: String) {
        viewModelScope.launch {
            userRepository.deleteWorkExperience(experienceId).collect { resource ->
                if (resource is Resource.Success) {
                    loadWorkExperiences()
                    _toastMessage.value = "Experience deleted"
                } else if (resource is Resource.Error) {
                    _toastMessage.value = resource.message ?: "Failed to delete experience"
                }
            }
        }
    }

    fun clearUpdateWorkState() {
        _updateWorkState.value = null
    }
}
