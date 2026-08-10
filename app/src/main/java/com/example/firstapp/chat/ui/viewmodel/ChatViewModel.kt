package com.example.firstapp.chat.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.chat.data.*
import com.example.firstapp.chat.model.*
import com.example.firstapp.common.model.*
import com.example.firstapp.common.util.Resource
import com.example.firstapp.chat.data.repository.ChatRepositoryImpl
import com.example.firstapp.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import com.example.firstapp.account.domain.repository.AccountRepository
import com.example.firstapp.domain.repository.SettingsRepository
import com.example.firstapp.chat.data.api.IceServerConfig
import java.io.File
import java.util.Locale
import kotlin.collections.emptyList
import kotlin.collections.listOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map as flowMap
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val userRepository: com.example.firstapp.vocab.domain.repository.UserRepository,
    private val accountRepository: AccountRepository,
    private val settingsRepository: SettingsRepository,
    private val webRtcCallManager: WebRtcCallManager,
    private val callTonePlayer: CallTonePlayer,
    private val chatWebSocketClient: com.example.firstapp.chat.data.ChatWebSocketClient,
    private val callWebSocketClient: CallWebSocketClient,
    private val sessionManager: com.example.firstapp.common.domain.auth.SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val settings = settingsRepository // So we can access individual flows
    val fontSize = settingsRepository.fontSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16f)
    val enterIsSend = settingsRepository.enterIsSend.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val animationsEnabled = settingsRepository.animationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showReadReceipts = accountRepository.getPrivacySettings()
        .flowMap { it.data?.show_read_receipts ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Compatibility properties for current UI code
    val chats = uiState.flowMap { it.chats }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
    val messages = uiState.flowMap { it.messages }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
    val archivedChats = uiState.flowMap { it.archivedChats }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
    val starredMessages = uiState.flowMap { it.starredMessages }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
    val chatSearchResults = uiState.flowMap { it.chatSearchResults }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val selectedContactsForGroup = uiState.flowMap { it.selectedContactsForGroup }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts = repository.getContacts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())

    fun fetchContacts() {
        viewModelScope.launch {
            repository.getContacts().collect { /* just triggering a sync */ }
        }
    }

    private var activeChatId: String? = null
    private var messagesFetchJob: kotlinx.coroutines.Job? = null
    private val groupNameCache = mutableMapOf<String, String>()
    private val loadingOlderChats = mutableSetOf<String>()
    private val exhaustedCursorByChat = mutableMapOf<String, Long>()

    init {
        handleIntent(ChatIntent.FetchChats)
        fetchCurrentUser()
        
        // Connect and observe auth state for reconnection
        viewModelScope.launch {
            sessionManager.authState.collect { isLoggedIn ->
                if (isLoggedIn) {
                    connectChatRealtime()
                } else {
                    chatWebSocketClient.disconnect()
                }
            }
        }
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

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.FetchChats -> fetchChats()
            is ChatIntent.FetchMessages -> fetchMessages(intent.chatId)
            is ChatIntent.SendMessage -> sendMessage(
                intent.chatId, intent.body, intent.messageType, intent.mediaUrls, 
                intent.duration, intent.fileName, intent.fileSize, 
                intent.contactName, intent.contactPhone, intent.latitude, intent.longitude, 
                intent.replyToId
            )
            is ChatIntent.MarkAsRead -> markAsRead(intent.chatId)
            is ChatIntent.DeleteMessage -> deleteMessage(intent.messageId)
            is ChatIntent.EditMessage -> editMessage(intent.messageId, intent.newBody)
            is ChatIntent.ReactToMessage -> reactToMessage(intent.messageId, intent.emoji)
            is ChatIntent.SearchInChat -> searchInChat(intent.chatId, intent.query)
            is ChatIntent.SetSelectedContactsForGroup -> updateSelectedContacts(intent.contacts)
            is ChatIntent.CreateGroup -> createGroup(intent.name, intent.contactIds, intent.onSuccess)
            is ChatIntent.ArchiveChat -> archiveChat(intent.chatId, intent.isArchived)
            is ChatIntent.PinChat -> pinChat(intent.chatId, intent.isPinned)
            is ChatIntent.MuteChat -> muteChat(intent.chatId, intent.isMuted)
            is ChatIntent.ClearChat -> clearChat(intent.chatId)
            is ChatIntent.ClearAllChats -> clearAllChats()
        }
    }

    fun fetchChats() {
        if (_uiState.value.isChatsFetching) return
        viewModelScope.launch {
            _uiState.update { it.copy(isChatsFetching = true) }
            try {
                // Sync from network
                launch {
                    repository.syncChats().collect { }
                }

                launch {
                    repository.getArchivedChats().collect { res ->
                        _uiState.update { it.copy(archivedChats = res) }
                    }
                }
                repository.getChats().collect { resource ->
                    _uiState.update { it.copy(chats = resource) }
                    if (resource is Resource.Success) {
                        resource.data?.forEach { chat ->
                            val currentName = GlobalCallState.getContactName(chat.id)
                            val newName = chat.senderName
                            if (newName.isNotBlank() && newName.lowercase() != "unknown") {
                                GlobalCallState.updateContactInfo(chat.id, newName, null)
                            } else if (currentName == null && newName.isNotBlank()) {
                                GlobalCallState.updateContactInfo(chat.id, newName, null)
                            }

                            if (chat.isGroup && chat.senderName.isNotBlank()) {
                                groupNameCache[chat.id] = chat.senderName
                            }
                        }
                    }
                }
            } finally {
                _uiState.update { it.copy(isChatsFetching = false) }
            }
        }
    }

    fun syncChats() {
        viewModelScope.launch {
            repository.syncChats().collect { }
        }
    }

    fun fetchMessages(chatId: String) {
        android.util.Log.d("ChatViewModel", "fetchMessages: chatId=$chatId, activeChatId=$activeChatId")
        val currentMessages = _uiState.value.messages.data
        if (activeChatId == chatId && currentMessages != null && currentMessages.isNotEmpty()) {
             // We already have messages for this chat, don't clear them
             // Just trigger a refresh from network via repository
             viewModelScope.launch {
                 repository.getMessages(chatId, 1).collect { /* background sync */ }
             }
             return
        }
        
        activeChatId = chatId
        exhaustedCursorByChat.remove(chatId)
        (repository as? ChatRepositoryImpl)?.clearDedupForConversation(chatId)
        
        messagesFetchJob?.cancel()
        messagesFetchJob = viewModelScope.launch {
            // Step 1: Check cache immediately and update UI if data exists
            repository.getMessages(chatId).first().let { resource ->
                if (resource is Resource.Success && resource.data?.isNotEmpty() == true) {
                    _uiState.update { it.copy(messages = resource) }
                } else {
                    _uiState.update { it.copy(messages = Resource.Loading()) }
                }
            }
            
            // Step 2: Observe for all future updates (network sync results + real-time)
            repository.getMessages(chatId).collect { resource ->
                _uiState.update { it.copy(messages = resource) }
            }
        }
        
        // Refresh chat list to update metadata
        viewModelScope.launch {
            repository.syncChats().collect { }
        }
    }

    private fun connectChatRealtime() {
        chatWebSocketClient.connect()
        viewModelScope.launch {
            chatWebSocketClient.messageFlow.collect { message ->
                val shouldProcess = (repository as? ChatRepositoryImpl)?.shouldProcessIncomingMessage(message) ?: true
                if (!shouldProcess) {
                    return@collect
                }
                
                // SAVE TO DB: This triggers the Room Flow in getMessages(chatId) 
                // which automatically updates the UI messages list.
                repository.saveMessage(message.conversationId, message)
                
                if (activeChatId == message.conversationId) {
                    if (!message.isFromMe) {
                        markAsRead(message.conversationId)
                    }
                } else if (!message.isFromMe) {
                    repository.incrementUnreadCount(message.conversationId)
                }

                updateChatLastMessage(message.conversationId, message)
                fetchChats()
            }
        }

        viewModelScope.launch {
            chatWebSocketClient.eventFlow.collect { frame ->
                val op = frame.op
                val payload = frame.payload?.asJsonObject ?: return@collect
                
                when (op) {
                    "PRESENCE" -> {
                        val uid = payload.get("user_id")?.asString
                        val status = payload.get("status")?.asString
                        if (uid != null) {
                            updateChatPresence(uid, status == "online", System.currentTimeMillis())
                            fetchChats()
                        }
                    }
                    "TYPING" -> {
                        val cid = payload.get("conversation_id")?.asString
                        val state = payload.get("state")?.asString
                        if (cid != null) {
                            updateChatTyping(cid, state == "START")
                        }
                    }
                    "DELIVERY_ACK" -> {
                        val mid = payload.get("message_id")?.asString
                        if (mid != null) {
                            repository.updateMessageStatus(mid, MessageStatus.DELIVERED)
                        }
                    }
                    "READ_ACK" -> {
                        val mid = payload.get("message_id")?.asString
                        if (mid != null) {
                            repository.updateMessageStatus(mid, MessageStatus.READ)
                        }
                    }
                }
            }
        }
    }

    fun sendMessage(
        chatId: String, 
        body: String, 
        messageType: String = "text", 
        mediaUrls: List<String> = emptyList(), 
        duration: String? = null, 
        fileName: String? = null, 
        fileSize: String? = null, 
        contactName: String? = null, 
        contactPhone: String? = null, 
        latitude: Double? = null, 
        longitude: Double? = null, 
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSenderName: String? = null
    ) {
        viewModelScope.launch {
            repository.sendMessage(
                chatId, body, messageType, mediaUrls, duration, fileName, fileSize,
                contactName, contactPhone, latitude, longitude, 
                replyToId, replyToText, replyToSenderName
            ).collect { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    val newMsg = resource.data!!
                    // NOTE: We don't manually update _uiState.messages here anymore.
                    // The repository saves to Room, which triggers the Flow in fetchMessages.
                    updateChatLastMessage(chatId, newMsg)
                } else if (resource is Resource.Error) {
                    val msg = resource.message.orEmpty()
                    val isClosed = msg.contains("conversation is closed", ignoreCase = true) ||
                        msg.contains("closed", ignoreCase = true)
                    _uiState.update { state ->
                        state.copy(
                            closedConversationIds = if (isClosed) state.closedConversationIds + chatId else state.closedConversationIds,
                            error = msg.ifBlank { state.error }
                        )
                    }
                }
            }
        }
    }

    fun sendMediaMessage(chatId: String, file: File, type: String, name: String? = null, duration: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val fileSize = (file.length() / 1024).toString() + " KB"
            val finalType = if (type == "audio") "audio_message" else type
            val folder = when(type) {
                "image" -> "chat/images"
                "video" -> "chat/videos"
                "audio" -> "chat/voice"
                else -> "chat/files"
            }
            
            accountRepository.uploadFile(file, folder).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    sendMessage(
                        chatId = chatId,
                        body = name ?: type.replaceFirstChar { it.uppercase() },
                        messageType = finalType,
                        mediaUrls = listOf(res.data.url),
                        duration = duration,
                        fileName = name ?: file.name,
                        fileSize = fileSize
                    )
                    file.delete()
                    _uiState.update { it.copy(isLoading = false) }
                } else if (res is Resource.Error) {
                    _uiState.update { it.copy(isLoading = false, error = "Upload failed: ${res.message}") }
                }
            }
        }
    }

    fun startVoiceOrVideoCall(receiverId: String, callType: com.example.firstapp.chat.data.CallType, name: String? = null) {
        viewModelScope.launch {
            // Update GlobalCallState with name so it shows immediately
            name?.let { GlobalCallState.updateContactInfo(receiverId, it, null) }

            // Fetch ICE servers for professional TURN support
            val iceResource = repository.getICEServers()
            if (iceResource is Resource.Success) {
                val servers = iceResource.data?.map { config: IceServerConfig ->
                    org.webrtc.PeerConnection.IceServer.builder(config.urls)
                        .setUsername(config.username)
                        .setPassword(config.credential)
                        .createIceServer()
                }
                if (servers != null) {
                    webRtcCallManager.setIceServers(servers)
                }
            }

            repository.initiateCall(receiverId, callType.wireValue()).collect { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    val sessionId = resource.data
                    GlobalCallState.setCallActive(true, sessionId, receiverId, callType)
                    webRtcCallManager.start(callType)
                    callTonePlayer.playOutgoingRingtone()
                    
                    // Also notify server
                    callWebSocketClient.invite(receiverId, sessionId, callType)
                }
            }
        }
    }

    fun editMessage(messageId: String, newBody: String) {
        viewModelScope.launch {
            repository.editMessage(messageId, newBody).collect { resource ->
                if (resource is Resource.Success) {
                    _uiState.update { state ->
                        val current = state.messages.data ?: return@update state
                        val updated = current.map { m: Message -> if (m.id == messageId) m.copy(content = newBody) else m }
                        state.copy(messages = Resource.Success(updated))
                    }
                    fetchChats()
                }
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId).collect { result ->
                if (result is Resource.Success && result.data == true) {
                    _uiState.update { state ->
                        val current = state.messages.data ?: return@update state
                        val filtered = current.filter { m: Message -> m.id != messageId }
                        state.copy(messages = Resource.Success(filtered))
                    }
                }
            }
        }
    }

    fun markAsRead(chatId: String) {
        viewModelScope.launch {
            repository.markAsRead(chatId).collect { resource ->
                if (resource is Resource.Success) {
                    _uiState.update { state ->
                        val currentChats = state.chats.data ?: return@update state
                        val updated = currentChats.map { c: ChatMessage -> if (c.id == chatId) c.copy(unreadCount = 0) else c }
                        state.copy(chats = Resource.Success(updated))
                    }
                }
            }
        }
    }

    fun updateChatTyping(contactId: String, isTyping: Boolean) { 
        viewModelScope.launch { 
            _uiState.update { state ->
                val updated = java.util.HashMap(state.typingStatus)
                updated[contactId] = isTyping
                state.copy(typingStatus = updated)
            }
            repository.updateTyping(contactId, isTyping) 
        } 
    }
    fun updateChatPresence(contactId: String, isOnline: Boolean, lastSeen: Long) { 
        viewModelScope.launch { 
            repository.updatePresence(contactId, isOnline, lastSeen)
            if (!isOnline) {
                repository.updateTyping(contactId, false)
            }
        } 
    }

    fun sendTyping(isTyping: Boolean) {
        activeChatId?.let { chatWebSocketClient.sendTyping(it, isTyping) }
    }

    fun updateChatLastMessage(contactId: String, message: Message) { 
        viewModelScope.launch { 
            repository.updateLastMessage(contactId, message.content, message.timestamp, message.isFromMe, message.isRead) 
        } 
    }

    fun deleteChat(chatId: String) { viewModelScope.launch { repository.deleteChat(chatId).collect { result -> if (result is Resource.Success) fetchChats() } } }
    
    suspend fun initiateDM(contactId: String): Flow<Resource<String>> {
        return repository.initiateDM(contactId)
    }

    fun clearChat(chatId: String) {
        viewModelScope.launch {
            repository.clearMessages(chatId)
            _uiState.update { it.copy(messages = Resource.Success(emptyList())) }
            fetchChats()
        }
    }

    fun clearAllChats() {
        viewModelScope.launch {
            repository.clearAllChats()
            _uiState.update { it.copy(messages = Resource.Success(emptyList())) }
            fetchChats()
        }
    }

    fun createGroup(name: String, contactIds: List<String>, onSuccess: (ChatMessage) -> Unit) {
        _uiState.update { it.copy(selectedContactsForGroup = emptyList()) }
        viewModelScope.launch {
            repository.createGroup(name, contactIds).collect { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    onSuccess(resource.data)
                    fetchChats()
                }
            }
        }
    }

    fun updateSelectedContacts(contacts: List<Contact>) {
        _uiState.update { it.copy(selectedContactsForGroup = contacts) }
    }

    fun pinChat(chatId: String, isPinned: Boolean) { viewModelScope.launch { repository.pinChat(chatId, isPinned).collect { if (it is Resource.Success) fetchChats() } } }
    fun muteChat(chatId: String, isMuted: Boolean) { viewModelScope.launch { repository.muteChat(chatId, isMuted).collect { if (it is Resource.Success) fetchChats() } } }
    fun archiveChat(chatId: String, isArchived: Boolean) { viewModelScope.launch { repository.archiveChat(chatId, isArchived).collect { if (it is Resource.Success) fetchChats() } } }

    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch { repository.reactToMessage(messageId, emoji).collect { } }
    }

    fun updateWallpaper(chatId: String, wallpaperUrl: String?) {
        viewModelScope.launch { repository.updateWallpaper(chatId, wallpaperUrl); fetchChats() }
    }

    fun updateCustomNotificationTone(chatId: String, tone: String?) {
        viewModelScope.launch { repository.updateCustomNotificationTone(chatId, tone); fetchChats() }
    }

    fun updateMediaVisibility(chatId: String, visible: Boolean) {
        viewModelScope.launch { repository.updateMediaVisibility(chatId, visible); fetchChats() }
    }

    fun updateDisappearingMessages(chatId: String, duration: Int) {
        viewModelScope.launch { repository.updateDisappearingMessages(chatId, duration); fetchChats() }
    }

    fun loadMoreMessages(chatId: String, beforeTs: Long) {
        if (loadingOlderChats.contains(chatId)) return
        if (exhaustedCursorByChat[chatId] == beforeTs) return

        viewModelScope.launch {
            loadingOlderChats.add(chatId)
            try {
                repository.fetchOlderMessages(chatId, beforeTs).collect { resource ->
                    when (resource) {
                        is Resource.Success -> {
                            if (resource.data == false) {
                                exhaustedCursorByChat[chatId] = beforeTs
                            }
                        }
                        is Resource.Error -> {
                            _uiState.update { it.copy(error = resource.message ?: it.error) }
                        }
                        is Resource.Loading -> Unit
                    }
                }
            } finally {
                loadingOlderChats.remove(chatId)
            }
        }
    }

    fun starMessage(messageId: String, isStarred: Boolean) {
        viewModelScope.launch {
            repository.starMessage(messageId, isStarred).collect {
                _uiState.update { state ->
                    val current = state.messages.data ?: return@update state
                    val updated = current.map { m: Message -> if (m.id == messageId) m.copy(isStarred = isStarred) else m }
                    state.copy(messages = Resource.Success(updated))
                }
            }
        }
    }

    fun searchInChat(chatId: String, query: String) {
        if (query.isBlank()) { _uiState.update { it.copy(chatSearchResults = emptyList()) }; return }
        viewModelScope.launch { repository.searchLocalMessages(chatId, query).collect { res -> _uiState.update { it.copy(chatSearchResults = res) } } }
    }

    fun getSharedMedia(chatId: String): Flow<Resource<List<Message>>> {
        return repository.getSharedMedia(chatId)
    }

    fun getCachedGroupName(groupId: String): String? {
        val cached = groupNameCache[groupId]
        if (cached != null) return cached
        val chatsState = _uiState.value.chats
        if (chatsState is Resource.Success) {
            val chat = chatsState.data?.find { it.id == groupId }
            if (chat != null && chat.isGroup && chat.senderName.isNotBlank()) {
                groupNameCache[groupId] = chat.senderName
                return chat.senderName
            }
        }
        return null
    }

    override fun onCleared() {
        chatWebSocketClient.disconnect()
        super.onCleared()
    }
}
