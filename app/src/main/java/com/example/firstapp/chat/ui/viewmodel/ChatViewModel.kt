package com.example.firstapp.chat.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.firstapp.chat.data.*
import com.example.firstapp.chat.model.*
import com.example.firstapp.common.model.*
import com.example.firstapp.common.util.Resource
import com.example.firstapp.domain.repository.ChatRepository
import com.example.firstapp.common.data.realtime.WebSocketManager
import com.example.firstapp.account.domain.repository.AccountRepository
import com.example.firstapp.domain.repository.SettingsRepository
import com.example.firstapp.chat.data.api.IceServerConfig
import com.example.firstapp.chat.data.api.GroupInfoResponse
import java.io.File
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val userRepository: com.example.firstapp.vocab.domain.repository.UserRepository,
    private val accountRepository: AccountRepository,
    private val settingsRepository: SettingsRepository,
    private val webRtcCallManager: WebRtcCallManager,
    private val callTonePlayer: CallTonePlayer,
    private val webSocketManager: WebSocketManager,
    private val callWebSocketClient: CallWebSocketClient,
    private val sessionManager: com.example.firstapp.common.domain.auth.SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    // Settings flows
    val fontSize = settingsRepository.fontSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16f)
    val enterIsSend = settingsRepository.enterIsSend.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val animationsEnabled = settingsRepository.animationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showReadReceipts = accountRepository.getPrivacySettings()
        .map { it.data?.show_read_receipts ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Paging 3 Messages Flow - Modern high-performance observation
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messagesPaged: Flow<PagingData<Message>> = _activeChatId
        .flatMapLatest { id ->
            if (id != null) repository.getMessagesPaged(id)
            else flowOf(PagingData.empty())
        }
        .cachedIn(viewModelScope)

    // Legacy StateFlows for UI compatibility
    val chats = uiState.map { it.chats }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
    val messages = uiState.map { it.messages }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
    val contacts = repository.getContacts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
    val archivedChats = uiState.map { it.archivedChats }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
    val chatSearchResults = uiState.map { it.chatSearchResults }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchCurrentUser()
        observeLocalDatabase()
        observeRealtimeEvents()
        syncChats()
    }

    private fun fetchCurrentUser() {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { resource ->
                if (resource is Resource.Success) {
                    _uiState.update { it.copy(currentUser = resource.data) }
                }
            }
        }
    }

    private fun observeLocalDatabase() {
        repository.getChats()
            .onEach { res -> _uiState.update { it.copy(chats = res) } }
            .launchIn(viewModelScope)

        repository.getArchivedChats()
            .onEach { res -> _uiState.update { it.copy(archivedChats = res) } }
            .launchIn(viewModelScope)
    }

    private fun observeRealtimeEvents() {
        webSocketManager.events
            .onEach { envelope ->
                when (envelope.topic) {
                    "chat" -> {
                        when (envelope.event) {
                            "message", "CONVERSATION_UPDATE", "CONVERSATION_CREATE", "NEW_CHAT" -> syncChats()
                            "typing" -> {
                                val cid = envelope.payload?.get("conversation_id")?.toString()?.trim('"')
                                val state = envelope.payload?.get("is_typing")?.toString()?.toBoolean() ?: false
                                if (cid != null) {
                                    _uiState.update { s ->
                                        val updated = s.typingStatus.toMutableMap()
                                        updated[cid] = state
                                        s.copy(typingStatus = updated)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.FetchChats -> syncChats()
            is ChatIntent.FetchMessages -> selectChat(intent.chatId)
            is ChatIntent.SendMessage -> sendMessage(
                intent.chatId, intent.body, intent.messageType, intent.mediaUrls, 
                intent.duration, intent.fileName, intent.fileSize, 
                intent.contactName, intent.contactPhone, intent.latitude, intent.longitude, 
                intent.replyToId, intent.replyToText, intent.replyToSenderName
            )
            is ChatIntent.MarkAsRead -> markAsRead(intent.chatId)
            is ChatIntent.DeleteMessage -> deleteMessage(intent.messageId)
            is ChatIntent.EditMessage -> editMessage(intent.messageId, intent.newBody)
            is ChatIntent.ReactToMessage -> reactToMessage(intent.messageId, intent.emoji)
            is ChatIntent.SearchInChat -> searchInChat(intent.chatId, intent.query)
            is ChatIntent.SetSelectedContactsForGroup -> _uiState.update { it.copy(selectedContactsForGroup = intent.contacts) }
            is ChatIntent.CreateGroup -> createGroup(intent.name, intent.contactIds, intent.onSuccess)
            is ChatIntent.ArchiveChat -> archiveChat(intent.chatId, intent.isArchived)
            is ChatIntent.PinChat -> pinChat(intent.chatId, intent.isPinned)
            is ChatIntent.MuteChat -> muteChat(intent.chatId, intent.isMuted)
            is ChatIntent.ClearChat -> clearChat(intent.chatId)
            is ChatIntent.ClearAllChats -> clearAllChats()
        }
    }

    fun syncChats() {
        viewModelScope.launch {
            repository.syncChats().collect { res ->
                if (res is Resource.Error) _uiState.update { it.copy(error = res.message) }
            }
        }
    }

    fun selectChat(chatId: String) {
        _activeChatId.value = chatId
        viewModelScope.launch {
            repository.getMessages(chatId).collect { res ->
                _uiState.update { it.copy(messages = res) }
            }
        }
        markAsRead(chatId)
    }

    fun sendMessage(
        chatId: String, body: String, messageType: String = "text", mediaUrls: List<String> = emptyList(),
        duration: String? = null, fileName: String? = null, fileSize: String? = null,
        contactName: String? = null, contactPhone: String? = null, latitude: Double? = null, longitude: Double? = null,
        replyToId: String? = null, replyToText: String? = null, replyToSenderName: String? = null
    ) {
        viewModelScope.launch {
            repository.sendMessage(
                chatId, body, messageType, mediaUrls, duration, fileName, fileSize,
                contactName, contactPhone, latitude, longitude, 
                replyToId, replyToText, replyToSenderName
            ).collect { res ->
                if (res is Resource.Error) _uiState.update { it.copy(error = res.message) }
            }
        }
    }

    fun sendMediaMessage(chatId: String, file: File, type: String, name: String? = null, duration: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val finalType = if (type == "audio") "audio_message" else type
            accountRepository.uploadFile(file, "chat/$type").collect { res ->
                if (res is Resource.Success && res.data != null) {
                    sendMessage(chatId, name ?: type, finalType, listOf(res.data.url), duration, file.name)
                    file.delete()
                } else if (res is Resource.Error) {
                    _uiState.update { it.copy(error = res.message) }
                }
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun startCall(receiverId: String, callType: com.example.firstapp.chat.data.CallType, name: String? = null) {
        viewModelScope.launch {
            name?.let { GlobalCallState.updateContactInfo(receiverId, it, null) }
            repository.initiateCall(receiverId, callType.wireValue()).collect { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    val sessionId = resource.data
                    GlobalCallState.setCallActive(true, sessionId, receiverId, callType)
                    webRtcCallManager.start(callType)
                    callTonePlayer.playOutgoingRingtone()
                    callWebSocketClient.invite(receiverId, sessionId, callType)
                } else if (resource is Resource.Error) {
                    _uiState.update { it.copy(error = resource.message) }
                }
            }
        }
    }

    fun markAsRead(chatId: String) {
        viewModelScope.launch { repository.markAsRead(chatId).collect { } }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch { repository.deleteMessage(messageId).collect { } }
    }

    fun editMessage(messageId: String, newBody: String) {
        viewModelScope.launch { repository.editMessage(messageId, newBody).collect { } }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch { repository.reactToMessage(messageId, emoji).collect { } }
    }

    fun searchInChat(chatId: String, query: String) {
        if (query.isBlank()) { _uiState.update { it.copy(chatSearchResults = emptyList()) }; return }
        viewModelScope.launch { repository.searchLocalMessages(chatId, query).collect { res -> _uiState.update { it.copy(chatSearchResults = res) } } }
    }

    fun createGroup(name: String, contactIds: List<String>, onSuccess: (ChatMessage) -> Unit) {
        viewModelScope.launch {
            repository.createGroup(name, contactIds).collect { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    onSuccess(resource.data)
                    syncChats()
                } else if (resource is Resource.Error) {
                    _uiState.update { it.copy(error = resource.message) }
                }
            }
        }
    }

    fun pinChat(chatId: String, isPinned: Boolean) { viewModelScope.launch { repository.pinChat(chatId, isPinned).collect { syncChats() } } }
    fun muteChat(chatId: String, isMuted: Boolean) { viewModelScope.launch { repository.muteChat(chatId, isMuted).collect { syncChats() } } }
    fun archiveChat(chatId: String, isArchived: Boolean) { viewModelScope.launch { repository.archiveChat(chatId, isArchived).collect { syncChats() } } }
    fun clearChat(chatId: String) { viewModelScope.launch { repository.clearMessages(chatId); selectChat(chatId) } }
    fun clearAllChats() { viewModelScope.launch { repository.clearAllChats(); syncChats() } }

    fun sendTyping(isTyping: Boolean) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch { repository.updateTyping(chatId, isTyping) }
    }

    fun fetchGroupInfo(groupId: String) {
        viewModelScope.launch {
            repository.getGroupInfo(groupId).collect { res ->
                _uiState.update { it.copy(groupInfo = res) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
