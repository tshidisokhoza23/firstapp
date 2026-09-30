package com.tpsk.tj.groups.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.chat.data.api.GroupInfoResponse
import com.tpsk.tj.chat.model.ChatMessage
import com.tpsk.tj.chat.model.Contact
import com.tpsk.tj.chat.model.Message
import com.tpsk.tj.common.domain.auth.SessionManager
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.domain.repository.ChatRepository
import com.tpsk.tj.groups.domain.GroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val repository: GroupRepository,
    private val chatRepository: ChatRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val currentUserId: String? get() = sessionManager.getUserId()

    private val _chat = MutableStateFlow<Resource<ChatMessage>>(Resource.Loading())
    val chat: StateFlow<Resource<ChatMessage>> = _chat.asStateFlow()

    private val _groupInfo = MutableStateFlow<Resource<GroupInfoResponse>>(Resource.Loading())
    val groupInfo: StateFlow<Resource<GroupInfoResponse>> = _groupInfo.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _sharedMedia = MutableStateFlow<List<Message>>(emptyList())
    val sharedMedia: StateFlow<List<Message>> = _sharedMedia.asStateFlow()

    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    fun clearToast() { _toast.value = null }

    fun load(groupId: String) {
        viewModelScope.launch {
            chatRepository.getChatById(groupId).collect { _chat.value = it }
        }
        viewModelScope.launch {
            chatRepository.getContacts().collect { res ->
                _contacts.value = res.data ?: emptyList()
            }
        }
        viewModelScope.launch {
            chatRepository.getSharedMedia(groupId).collect { res ->
                _sharedMedia.value = res.data ?: emptyList()
            }
        }
        fetchGroupInfo(groupId)
    }

    fun fetchGroupInfo(groupId: String) {
        viewModelScope.launch {
            repository.getGroupInfo(groupId).collect { res ->
                _groupInfo.value = res
                if (res is Resource.Success) {
                    _inviteCode.value = res.data?.invite_code ?: _inviteCode.value
                }
            }
        }
    }

    fun mute(groupId: String, muted: Boolean) {
        viewModelScope.launch {
            chatRepository.muteChat(groupId, muted).collect {
                if (it is Resource.Success) load(groupId)
                else if (it is Resource.Error) _toast.value = it.message
            }
        }
    }

    fun addMember(groupId: String, userId: String) {
        viewModelScope.launch {
            repository.addGroupMember(groupId, userId).collect { res ->
                if (res is Resource.Success) {
                    _toast.value = "Member added"
                    fetchGroupInfo(groupId)
                } else if (res is Resource.Error) {
                    _toast.value = res.message ?: "Only admins can add members"
                }
            }
        }
    }

    fun removeMember(groupId: String, userId: String) {
        viewModelScope.launch {
            repository.removeGroupMember(groupId, userId).collect { res ->
                if (res is Resource.Success) {
                    _toast.value = "Member removed"
                    fetchGroupInfo(groupId)
                } else if (res is Resource.Error) {
                    _toast.value = res.message ?: "Only admins can remove members"
                }
            }
        }
    }

    fun promoteAdmin(groupId: String, userId: String) {
        viewModelScope.launch {
            repository.promoteToAdmin(groupId, userId).collect { res ->
                if (res is Resource.Success) {
                    _toast.value = "Admin updated"
                    fetchGroupInfo(groupId)
                } else if (res is Resource.Error) {
                    _toast.value = res.message ?: "Only admins can promote members"
                }
            }
        }
    }

    fun demoteAdmin(groupId: String, userId: String) {
        viewModelScope.launch {
            repository.demoteAdmin(groupId, userId).collect { res ->
                if (res is Resource.Success) {
                    _toast.value = "Admin dismissed"
                    fetchGroupInfo(groupId)
                } else if (res is Resource.Error) {
                    _toast.value = res.message ?: "Only admins can dismiss admins"
                }
            }
        }
    }

    fun leave(groupId: String, onLeft: () -> Unit) {
        viewModelScope.launch {
            repository.leaveGroup(groupId).collect { res ->
                if (res is Resource.Success) onLeft()
                else if (res is Resource.Error) _toast.value = res.message ?: "Could not leave group"
            }
        }
    }

    fun rename(groupId: String, name: String) {
        viewModelScope.launch {
            repository.renameGroup(groupId, name).collect { res ->
                if (res is Resource.Success) {
                    _toast.value = "Group name updated"
                    fetchGroupInfo(groupId)
                } else if (res is Resource.Error) {
                    _toast.value = res.message ?: "Only admins can rename the group"
                }
            }
        }
    }

    fun generateInvite(groupId: String) {
        viewModelScope.launch {
            repository.generateGroupInviteCode(groupId).collect { res ->
                if (res is Resource.Success && !res.data.isNullOrBlank()) {
                    _inviteCode.value = res.data
                    _toast.value = "Invite link ready"
                    fetchGroupInfo(groupId)
                } else if (res is Resource.Error) {
                    _toast.value = res.message ?: "Only admins can create invite links"
                }
            }
        }
    }

    fun joinByInvite(code: String, onJoined: (String) -> Unit) {
        viewModelScope.launch {
            repository.joinGroupByInvite(code).collect { res ->
                if (res is Resource.Success && !res.data.isNullOrBlank()) {
                    repository.syncGroups()
                    onJoined(res.data)
                } else if (res is Resource.Error) {
                    _toast.value = res.message ?: "Invalid invite link"
                }
            }
        }
    }
}
