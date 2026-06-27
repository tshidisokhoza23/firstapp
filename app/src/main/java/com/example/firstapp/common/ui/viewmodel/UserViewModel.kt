package com.example.firstapp.common.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.common.data.TokenManager
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.model.User
import com.example.firstapp.common.util.CryptoUtils
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

import com.example.firstapp.vocab.domain.repository.UserRepository
import com.example.firstapp.account.domain.repository.AccountRepository
import com.example.firstapp.domain.repository.ChatRepository
import com.example.firstapp.common.domain.repository.SystemRepository

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository,
    private val chatRepository: ChatRepository,
    private val systemRepository: SystemRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        fetchCurrentUser()
        fetchContacts()
        ensureE2EEKeys()
    }

    private fun ensureE2EEKeys() {
        viewModelScope.launch {
            val existingPriv = TokenManager.getPrivateKey()
            val existingPub = TokenManager.getPublicKey()
            
            if (existingPriv == null || existingPub == null) {
                val pair = CryptoUtils.generateKeyPair()
                val pub = pair.first
                val priv = pair.second
                TokenManager.setPrivateKey(priv)
                TokenManager.setPublicKey(pub)
                accountRepository.setPublicKey(pub)
            } else {
                // Periodically sync public key to server just in case
                accountRepository.setPublicKey(existingPub)
            }
        }
    }

    fun fetchCurrentUser() {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { resource ->
                _currentUser.value = resource.data
            }
        }
    }

    fun fetchContacts() {
        viewModelScope.launch {
            chatRepository.getContacts().collect { result ->
                val data = result.data ?: emptyList()
                _contacts.value = data
                data.forEach { contact ->
                    com.example.firstapp.chat.data.GlobalCallState.updateContactInfo(contact.id, contact.displayName, contact.phoneNumber)
                }
            }
        }
    }

    fun updateProfile(displayName: String?, bio: String?, location: String?, avatarUrl: String?, bannerUrl: String? = null, isArtist: Boolean? = null) {
        viewModelScope.launch {
            userRepository.updateUser(displayName, bio, location, avatarUrl, bannerUrl, isArtist).collect { resource ->
                if (resource is Resource.Success) {
                    _currentUser.value = resource.data
                    _toastMessage.value = "Profile updated successfully"
                } else if (resource is Resource.Error) {
                    _toastMessage.value = resource.message ?: "Failed to update profile"
                }
            }
        }
    }

    fun uploadProfilePicture(file: java.io.File, isBanner: Boolean = false) {
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
                    TokenManager.clear()
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
                    val synced = result.data ?: emptyList()
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
                    res.data?.forEach { 
                        if (it.isRegistered) chatRepository.createChat(it.id) 
                    }
                    _toastMessage.value = "Contacts registered as chats successfully"
                    fetchContacts() // Refresh contacts list
                }
            }
        }
    }

    fun blockUser(userId: Int) {
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

    fun reportUser(userId: Int, reason: String) {
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
}
