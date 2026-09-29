package com.tpsk.tj.chat.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import kotlinx.coroutines.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.tpsk.tj.chat.data.*
import com.tpsk.tj.chat.model.*
import com.tpsk.tj.common.model.*
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.domain.repository.ChatRepository
import com.tpsk.tj.common.data.realtime.WebSocketManager
import com.tpsk.tj.account.domain.repository.AccountRepository
import com.tpsk.tj.domain.repository.SettingsRepository
import com.tpsk.tj.chat.data.api.IceServerConfig
import com.tpsk.tj.chat.data.api.GroupInfoResponse
import java.io.File
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val userRepository: com.tpsk.tj.vocab.domain.repository.UserRepository,
    private val accountRepository: AccountRepository,
    private val settingsRepository: SettingsRepository,
    private val webRtcCallManager: WebRtcCallManager,
    private val callTonePlayer: CallTonePlayer,
    private val webSocketManager: WebSocketManager,
    private val callWebSocketClient: CallWebSocketClient,
    private val sessionManager: com.tpsk.tj.common.domain.auth.SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _activeSubTab = MutableStateFlow(0) // 0: Chats, 1: Channels, 2: Jobs, 3: Rewards, 4: DM
    val activeSubTab: StateFlow<Int> = _activeSubTab.asStateFlow()

    fun setActiveSubTab(tabIndex: Int) {
        _activeSubTab.value = tabIndex
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeChat: StateFlow<ChatMessage?> = _activeChatId
        .flatMapLatest { id ->
            if (id != null) repository.getChatById(id).map { it.data }
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val groupNameCache = mutableMapOf<String, String>()

    // Settings flows
    val fontSize = settingsRepository.fontSize.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16f)
    val enterIsSend = settingsRepository.enterIsSend.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val animationsEnabled = settingsRepository.animationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val showReadReceipts = accountRepository.getPrivacySettings()
        .map { it.data?.show_read_receipts ?: true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Paging 3 Messages Flow
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val messagesPaged: Flow<PagingData<Message>> = _activeChatId
        .flatMapLatest { id ->
            if (id != null) repository.getMessagesPaged(id)
            else flowOf(PagingData.empty())
        }
        .cachedIn(viewModelScope)

    // UI Compatibility flows
    val chats = uiState.map { it.chats }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.Eagerly, Resource.Loading())
    val mingleChats = repository.getMingleChats().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
    val messages = uiState.map { it.messages }.distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
    val contacts = repository.getContacts().distinctUntilChanged().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
    val archivedChats = uiState.map { it.archivedChats }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
    val starredMessages = uiState.map { it.starredMessages }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
    val chatSearchResults = uiState.map { it.chatSearchResults }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val selectedContactsForGroup = uiState.map { it.selectedContactsForGroup }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentlyPlayingAudio = MutableStateFlow<String?>(null)
    val currentlyPlayingAudio: StateFlow<String?> = _currentlyPlayingAudio.asStateFlow()

    private val _blockedUserIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedUserIds: StateFlow<Set<String>> = _blockedUserIds.asStateFlow()

    private val _contactUser = MutableStateFlow<User?>(null)
    val contactUser: StateFlow<User?> = _contactUser.asStateFlow()

    init {
        fetchCurrentUser()
        observeLocalDatabase()
        observeRealtimeEvents()
        fetchChats()
        refreshBlockedUsers()
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
            .onEach { res -> 
                Log.d("ChatViewModel", "Observe: chats list updated, count=${res.data?.size ?: 0}")
                _uiState.update { current ->
                    // EXPERT FIX: Prefer Success state if we have any data, even if sync is still loading/error
                    val updatedChats = if (res.data != null) {
                         Resource.Success(res.data!!)
                    } else {
                         res
                    }

                    current.copy(
                        chats = updatedChats
                        // Removed isChatsFetching = false to let sync finish independently
                    ) 
                }
            }
            .launchIn(viewModelScope)

        repository.getArchivedChats()
            .onEach { res -> _uiState.update { it.copy(archivedChats = res) } }
            .launchIn(viewModelScope)

        repository.getStarredMessages()
            .onEach { res -> _uiState.update { it.copy(starredMessages = res) } }
            .launchIn(viewModelScope)
    }

    private fun observeRealtimeEvents() {
        webSocketManager.events
            .onEach { envelope ->
                when (envelope.type) {
                    "message_new", "message_ack" -> {
                        val cid = envelope.payload?.get("conversation_id")?.toString()?.trim('"')
                        val currentOpenChat = _activeChatId.value
                        if (cid != null && cid == currentOpenChat) {
                            markAsRead(cid)
                            // Trigger immediate re-fetch from Room DB for current open chat
                            repository.getMessages(cid).firstOrNull()?.let { res ->
                                _uiState.update { s -> s.copy(messages = res) }
                            }
                        }
                    }
                    "typing" -> {
                        val cid = envelope.payload?.get("conversation_id")?.toString()?.trim('"')
                        val state = envelope.payload?.get("state")?.toString()?.trim('"')
                        val isTyping = state == "START"

                        if (!cid.isNullOrBlank()) {
                            _uiState.update { s ->
                                val updated = s.typingStatus.toMutableMap()
                                updated[cid] = isTyping
                                s.copy(typingStatus = updated)
                            }
                        }
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.FetchChats -> fetchChats()
            is ChatIntent.FetchMessages -> fetchMessages(intent.chatId)
            is ChatIntent.SendMessage -> sendMessage(
                intent.chatId, intent.body, intent.messageType, intent.mediaUrls, 
                intent.duration, intent.fileName, intent.fileSize, 
                intent.contactName, intent.contactPhone, intent.latitude, intent.longitude, 
                intent.replyToId, intent.replyToText, intent.replyToSenderName
            )
            is ChatIntent.MarkAsRead -> markAsRead(intent.chatId)
            is ChatIntent.DeleteMessage -> deleteMessage(intent.messageId, intent.deleteForEveryone)
            is ChatIntent.EditMessage -> editMessage(intent.messageId, intent.newBody)
            is ChatIntent.ReactToMessage -> reactToMessage(intent.messageId, intent.emoji)
            is ChatIntent.SearchInChat -> searchInChat(intent.chatId, intent.query)
            is ChatIntent.SetSelectedContactsForGroup -> updateSelectedContacts(intent.contacts)
            is ChatIntent.CreateGroup -> createGroup(intent.name, intent.contactIds, intent.onSuccess)
            is ChatIntent.ArchiveChat -> archiveChat(intent.chatId, intent.isArchived)
            is ChatIntent.PinChat -> pinChat(intent.chatId, intent.isPinned)
            is ChatIntent.MuteChat -> muteChat(intent.chatId, intent.isMuted)
            is ChatIntent.ClearChat -> clearChat(intent.chatId)
            is ChatIntent.StarMessage -> starMessage(intent.messageId, intent.isStarred)
            is ChatIntent.ClearAllChats -> clearAllChats()
        }
    }

    fun fetchChats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isChatsFetching = true) }
            
            val syncJob = launch {
                repository.syncChats().collect { res ->
                    Log.d("ChatViewModel", "SyncChats result: ${res::class.simpleName}")
                    if (res is Resource.Error) {
                        _uiState.update { it.copy(error = res.message, isChatsFetching = false) }
                    } else if (res is Resource.Success) {
                        _uiState.update { it.copy(isChatsFetching = false) }
                    }
                }
                repository.syncDelta().collect { delta ->
                    Log.d("ChatViewModel", "SyncDelta result: ${delta::class.simpleName}")
                }
            }
            
            // Safety timeout: 15s limit for the spinner
            delay(15000)
            if (syncJob.isActive) {
                Log.w("ChatViewModel", "Sync timeout reached, hiding spinner")
                _uiState.update { it.copy(isChatsFetching = false) }
            }
        }
    }

    private var messagesJob: kotlinx.coroutines.Job? = null
    private var messagesJobChatId: String? = null

    fun fetchMessages(chatId: String) {
        _activeChatId.value = chatId
        // Must restart the collection whenever the chat actually changes -- the
        // previous guard only checked "is some job still running", which is true
        // forever once any chat has been opened, so switching to a different
        // conversation never re-subscribed and the message list stayed stuck on
        // whichever chat was opened first in this ViewModel's lifetime.
        if (messagesJobChatId != chatId) {
            messagesJob?.cancel()
            messagesJobChatId = chatId
            messagesJob = viewModelScope.launch {
                repository.getMessages(chatId)
                    .collect { res ->
                        _uiState.update { it.copy(messages = res) }
                    }
            }
        }
        
        // Trigger network sync
        viewModelScope.launch {
            repository.fetchMessages(chatId).collect { }
        }

        markAsRead(chatId)
    }

    fun clearActiveChat() {
        _activeChatId.value = null
        messagesJob?.cancel()
        messagesJob = null
        messagesJobChatId = null
        _uiState.update { it.copy(messages = Resource.Success(emptyList())) }
    }

    fun sendMessage(
        chatId: String, body: String, messageType: String = "text", mediaUrls: List<String> = emptyList(),
        duration: String? = null, fileName: String? = null, fileSize: String? = null,
        contactName: String? = null, contactPhone: String? = null, latitude: Double? = null, longitude: Double? = null,
        replyToId: String? = null, replyToText: String? = null, replyToSenderName: String? = null,
        clientId: String? = null, audioAmplitudes: List<Int>? = null, scheduledAt: Long? = null
    ) {
        viewModelScope.launch {
            // Detect and fetch link preview if it's a text message with a link
            var linkPreview: com.tpsk.tj.chat.model.LinkPreview? = null
            if (messageType.lowercase() == "text" && scheduledAt == null) {
                val matcher = android.util.Patterns.WEB_URL.matcher(body)
                if (matcher.find()) {
                    val url = matcher.group()
                    val fullUrl = if (!url.startsWith("http")) "https://$url" else url
                    linkPreview = com.tpsk.tj.chat.util.LinkMetadataParser.fetchMetadata(fullUrl)
                }
            }

            repository.sendMessage(
                chatId, body, messageType, mediaUrls, duration, fileName, fileSize,
                contactName, contactPhone, latitude, longitude, 
                replyToId, replyToText, replyToSenderName,
                clientId, scheduledAt
            ).collect { res ->
                if (res is Resource.Success && res.data != null && (linkPreview != null || audioAmplitudes != null)) {
                    // Update the message in cache with the preview or amplitudes
                    repository.updateMessageInCache(chatId, res.data.copy(linkPreview = linkPreview, audioAmplitudes = audioAmplitudes))
                }
                if (res is Resource.Error) _uiState.update { it.copy(error = res.message) }
            }
        }
    }


    fun startVoiceOrVideoCall(receiverId: String, callType: com.tpsk.tj.chat.data.CallType, name: String? = null) {
        if (receiverId.isBlank()) return
        val context = try {
            com.tpsk.tj.common.util.ContextProvider.context
        } catch (_: Exception) {
            null
        }
        if (context == null) {
            android.util.Log.w("ChatViewModel", "No context available for initiating call")
            return
        }
        val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCam = if (callType == com.tpsk.tj.chat.data.CallType.VIDEO) {
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true

        if (!hasMic || !hasCam) {
            _uiState.update { it.copy(error = "Microphone and Camera permissions are required for calling") }
            return
        }

        viewModelScope.launch {
            name?.let { GlobalCallState.updateContactInfo(receiverId, it, null) }
            
            // 1. Fetch ICE servers first for professional TURN support in outgoing calls
            val iceResource = repository.getICEServers()
            if (iceResource is Resource.Success) {
                val servers = iceResource.data?.map { config: IceServerConfig ->
                    val builder = org.webrtc.PeerConnection.IceServer.builder(config.urls)
                    if (config.username != null) builder.setUsername(config.username)
                    if (config.credential != null) builder.setPassword(config.credential)
                    builder.createIceServer()
                }
                if (servers != null) {
                    webRtcCallManager.setIceServers(servers)
                }
            }

            // 2. Initiate the call on the server
            repository.initiateCall(receiverId, callType.wireValue()).collect { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    val sessionId = resource.data
                    GlobalCallState.setCallActive(true, sessionId, receiverId, callType)
                    try {
                        webRtcCallManager.start(callType)
                    } catch (t: Throwable) {
                        android.util.Log.e("ChatViewModel", "Call start failed", t)
                        GlobalCallState.setCallActive(false, null, null, callType)
                        _uiState.update { it.copy(error = "Couldn't start the call. No call was placed.") }
                        return@collect
                    }
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

    fun deleteMessage(messageId: String, deleteForEveryone: Boolean = false) {
        viewModelScope.launch { repository.deleteMessage(messageId, deleteForEveryone).collect { } }
    }

    fun editMessage(messageId: String, newBody: String) {
        viewModelScope.launch { repository.editMessage(messageId, newBody).collect { } }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch { repository.reactToMessage(messageId, emoji).collect { } }
    }

    fun starMessage(messageId: String, isStarred: Boolean) {
        viewModelScope.launch { repository.starMessage(messageId, isStarred).collect { } }
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
                    fetchChats()
                } else if (resource is Resource.Error) {
                    _uiState.update { it.copy(error = resource.message) }
                }
            }
        }
    }

    fun pinChat(chatId: String, isPinned: Boolean) { viewModelScope.launch { repository.pinChat(chatId, isPinned).collect { fetchChats() } } }
    fun pinMessage(chatId: String, messageId: String?) { viewModelScope.launch { repository.pinMessage(chatId, messageId).collect { } } }
    fun muteChat(chatId: String, isMuted: Boolean) { viewModelScope.launch { repository.muteChat(chatId, isMuted).collect { fetchChats() } } }
    fun archiveChat(chatId: String, isArchived: Boolean) { viewModelScope.launch { repository.archiveChat(chatId, isArchived).collect { fetchChats() } } }
    fun clearChat(chatId: String) { viewModelScope.launch { repository.clearMessages(chatId); fetchMessages(chatId) } }
    fun clearAllChats() { viewModelScope.launch { repository.clearAllChats(); fetchChats() } }

    fun refreshBlockedUsers() {
        viewModelScope.launch {
            accountRepository.getBlockedUsers().collect { res ->
                if (res is Resource.Success) {
                    _blockedUserIds.value = res.data?.map { it.id }?.toSet() ?: emptySet()
                }
            }
        }
    }

    fun loadContactUser(userId: String) {
        if (userId.isBlank()) {
            _contactUser.value = null
            return
        }
        viewModelScope.launch {
            accountRepository.getUserById(userId).collect { res ->
                if (res is Resource.Success) {
                    _contactUser.value = res.data
                }
            }
        }
    }

    fun blockUser(userId: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            accountRepository.blockUser(userId).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        _blockedUserIds.update { it + userId }
                        onDone(true)
                    }
                    is Resource.Error -> onDone(false)
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun unblockUser(userId: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            accountRepository.unblockUser(userId).collect { res ->
                when (res) {
                    is Resource.Success -> {
                        _blockedUserIds.update { it - userId }
                        onDone(true)
                    }
                    is Resource.Error -> onDone(false)
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun reportUser(userId: String, reason: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            accountRepository.reportUser(userId, reason).collect { res ->
                when (res) {
                    is Resource.Success -> onDone(true)
                    is Resource.Error -> onDone(false)
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    fun updateDisappearingMessages(chatId: String, duration: Int) {
        viewModelScope.launch {
            repository.updateDisappearingMessages(chatId, duration)
            fetchChats()
        }
    }

    fun updateMediaVisibility(chatId: String, visible: Boolean) {
        viewModelScope.launch {
            repository.updateMediaVisibility(chatId, visible)
            fetchChats()
        }
    }

    fun subscribeToChannel(channelId: String) {
        viewModelScope.launch {
            repository.subscribeToChannel(channelId).collect {
                if (it is Resource.Success) {
                    fetchChats()
                    fetchMessages(channelId)
                }
            }
        }
    }

    fun setPlayingAudio(uri: String?) {
        _currentlyPlayingAudio.value = uri
    }

    fun sendTyping(isTyping: Boolean) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch { repository.updateTyping(chatId, isTyping) }
    }

    fun updateSelectedContacts(contacts: List<Contact>) {
        _uiState.update { it.copy(selectedContactsForGroup = contacts) }
    }

    suspend fun initiateDM(contactId: String): Flow<Resource<String>> {
        return repository.initiateDM(contactId)
    }

    fun getSharedMedia(chatId: String): Flow<Resource<List<Message>>> {
        return repository.getSharedMedia(chatId)
    }

    fun addGroupMember(groupId: String, userId: String) {
        viewModelScope.launch {
            repository.addGroupMember(groupId, userId).collect { res ->
                if (res is Resource.Success) fetchGroupInfo(groupId)
                else if (res is Resource.Error) _uiState.update { it.copy(error = res.message) }
            }
        }
    }

    fun removeGroupMember(groupId: String, userId: String) {
        viewModelScope.launch {
            repository.removeGroupMember(groupId, userId).collect { res ->
                if (res is Resource.Success) fetchGroupInfo(groupId)
                else if (res is Resource.Error) _uiState.update { it.copy(error = res.message) }
            }
        }
    }

    fun promoteToAdmin(groupId: String, userId: String) {
        viewModelScope.launch {
            repository.promoteToAdmin(groupId, userId).collect { res ->
                if (res is Resource.Success) fetchGroupInfo(groupId)
                else if (res is Resource.Error) _uiState.update { it.copy(error = res.message) }
            }
        }
    }

    fun demoteAdmin(groupId: String, userId: String) {
        viewModelScope.launch {
            repository.demoteAdmin(groupId, userId).collect { res ->
                if (res is Resource.Success) fetchGroupInfo(groupId)
                else if (res is Resource.Error) _uiState.update { it.copy(error = res.message) }
            }
        }
    }

    fun leaveGroup(groupId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.leaveGroup(groupId).collect { res ->
                if (res is Resource.Success) {
                    onSuccess()
                    fetchChats()
                } else if (res is Resource.Error) {
                    _uiState.update { it.copy(error = res.message) }
                }
            }
        }
    }

    fun fetchGroupInfo(groupId: String) {
        viewModelScope.launch {
            repository.getGroupInfo(groupId).collect { res ->
                _uiState.update { it.copy(groupInfo = res) }
            }
        }
    }

    fun generateGroupInvite(groupId: String, onCode: (String) -> Unit = {}) {
        viewModelScope.launch {
            repository.generateGroupInviteCode(groupId).collect { res ->
                if (res is Resource.Success && !res.data.isNullOrBlank()) {
                    onCode(res.data)
                    fetchGroupInfo(groupId)
                } else if (res is Resource.Error) {
                    _uiState.update { it.copy(error = res.message) }
                }
            }
        }
    }

    fun joinGroupByInvite(code: String, onJoined: (String) -> Unit = {}) {
        viewModelScope.launch {
            repository.joinGroupByInvite(code).collect { res ->
                if (res is Resource.Success && !res.data.isNullOrBlank()) {
                    fetchChats()
                    onJoined(res.data)
                } else if (res is Resource.Error) {
                    _uiState.update { it.copy(error = res.message) }
                }
            }
        }
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

    fun loadMoreMessages(chatId: String, beforeTs: Long) {
        viewModelScope.launch {
            repository.fetchOlderMessages(chatId, beforeTs).collect { }
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId).collect { res ->
                if (res is Resource.Success) fetchChats()
            }
        }
    }

    fun updateChatAvatar(chatId: String, uri: android.net.Uri) {
        viewModelScope.launch {
            val context = com.tpsk.tj.common.util.ContextProvider.context
            repository.updateChatAvatar(chatId, uri.toString())
                
                // If it's my own profile chat, update the global session user and local DB too
                if (chatId == sessionManager.getUserId()) {
                    sessionManager.currentUser.value?.let { current ->
                        val updated = current.copy(avatarUrl = uri.toString())
                        sessionManager.updateCurrentUser(updated)
                        userRepository.updateUserLocally(updated)
                    }
                }
                
                // 2. Network Upload
                accountRepository.uploadUri(context, uri, "avatars").collect { res ->
                    if (res is Resource.Success && res.data != null) {
                        val serverUrl = res.data.url
                        repository.updateChatAvatar(chatId, serverUrl)
                        
                        if (chatId == sessionManager.getUserId()) {
                            // Update the actual user record on the server for my profile
                            userRepository.updateUser(null, null, null, serverUrl).collect { }
                        } else {
                            val chat = repository.getChatById(chatId).firstOrNull()?.data
                            val type = chat?.type
                            if (type == ConversationType.CHANNEL) {
                                repository.updateChannelAvatar(chatId, serverUrl).collect { }
                            } else {
                                repository.updateGroupAvatar(chatId, serverUrl).collect { }
                            }
                        }
                        
                    } else if (res is Resource.Error) {
                        _uiState.update { it.copy(error = res.message) }
                    }
                }
        }
    }

    fun sendMediaMessage(chatId: String, file: java.io.File, type: String, name: String? = null, duration: String? = null, audioAmplitudes: List<Int>? = null) {
        viewModelScope.launch {
            val clientUuid = java.util.UUID.randomUUID().toString()
            val localUri = android.net.Uri.fromFile(file).toString()
            val finalType = if (type == "audio" || type == "audio_message") "audio_message" else type
            val bodyText = name ?: type.replaceFirstChar { it.uppercase() }
            
            // 1. Optimistic Update: Save to Local DB immediately with SENDING status
            val myUserId = sessionManager.getUserId() ?: "0"
            val optimisticMsg = Message(
                id = clientUuid,
                conversationId = chatId,
                content = bodyText,
                senderId = myUserId,
                senderName = "You",
                timestamp = System.currentTimeMillis(),
                isFromMe = true,
                status = MessageStatus.SENDING,
                type = try { MessageType.valueOf(finalType.uppercase()) } catch(_: Exception) { MessageType.TEXT },
                mediaUrls = listOf(localUri),
                clientId = clientUuid,
                duration = duration,
                fileName = file.name,
                audioAmplitudes = audioAmplitudes
            )
            repository.saveMessage(chatId, optimisticMsg)
            
            // 2. Upload and finalize
            _uiState.update { it.copy(isLoading = true) }
            accountRepository.uploadFile(file, "chat/$type").collect { res ->
                if (res is Resource.Success && res.data != null) {
                    val serverUrl = res.data.url
                    repository.updateMessageInCache(
                        chatId,
                        optimisticMsg.copy(mediaUrls = listOf(serverUrl))
                    )
                    // 3. Send via API with the real URL, using the SAME clientId to replace the optimistic one
                    sendMessage(chatId, bodyText, finalType, listOf(serverUrl), duration, file.name, null, null, null, null, null, null, null, null, clientUuid, audioAmplitudes)
                    file.delete()
                } else if (res is Resource.Error) {
                    _uiState.update { it.copy(error = res.message) }
                    repository.updateMessageStatus(clientUuid, MessageStatus.FAILED)
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun sendMediaMessage(chatId: String, uri: android.net.Uri, context: android.content.Context, type: String, name: String? = null, duration: String? = null) {
        if (type == "image") {
            val compressedFile = com.tpsk.tj.common.util.MediaUtils.compressImage(context, uri)
            if (compressedFile != null) {
                sendMediaMessage(chatId, compressedFile, type, name, duration)
                return
            }
        }
        val calculatedDuration = duration ?: if (type == "video") {
            val durMs = com.tpsk.tj.common.util.MediaUtils.getVideoDuration(context, uri)
            if (durMs > 0) {
                val totalSec = durMs / 1000
                val sec = totalSec % 60
                val min = totalSec / 60
                String.format(java.util.Locale.US, "%d:%02d", min, sec)
            } else null
        } else null

        viewModelScope.launch {
            val clientUuid = java.util.UUID.randomUUID().toString()
            val finalType = if (type == "audio" || type == "audio_message") "audio_message" else type
            val bodyText = name ?: type.replaceFirstChar { it.uppercase() }
            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "media"
            val myUserId = sessionManager.getUserId() ?: "0"
            val optimisticMsg = Message(
                id = clientUuid,
                conversationId = chatId,
                content = bodyText,
                senderId = myUserId,
                senderName = "You",
                timestamp = System.currentTimeMillis(),
                isFromMe = true,
                status = MessageStatus.SENDING,
                type = try { MessageType.valueOf(finalType.uppercase()) } catch(_: Exception) { MessageType.TEXT },
                mediaUrls = listOf(uri.toString()),
                clientId = clientUuid,
                duration = calculatedDuration,
                fileName = fileName
            )
            repository.saveMessage(chatId, optimisticMsg)
            _uiState.update { it.copy(isLoading = true) }
            accountRepository.uploadUri(context, uri, "chat/$type").collect { res ->
                if (res is Resource.Success && res.data != null) {
                    val serverUrl = res.data.url
                    repository.updateMessageInCache(
                        chatId,
                        optimisticMsg.copy(mediaUrls = listOf(serverUrl))
                    )
                    sendMessage(chatId, bodyText, finalType, listOf(serverUrl), calculatedDuration, fileName, null, null, null, null, null, null, null, null, clientUuid, null)
                } else if (res is Resource.Error) {
                    _uiState.update { it.copy(error = res.message) }
                    repository.updateMessageStatus(clientUuid, MessageStatus.FAILED)
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun sendMediaMessages(chatId: String, uris: List<android.net.Uri>, context: android.content.Context, type: String, caption: String? = null) {
        if (type == "image" && uris.isNotEmpty()) {
            val compressedFiles = uris.mapNotNull { com.tpsk.tj.common.util.MediaUtils.compressImage(context, it) }
            if (compressedFiles.size == uris.size) {
                sendMediaMessages(chatId, compressedFiles, type, caption)
                return
            }
        }
        uris.forEachIndexed { index, uri ->
            sendMediaMessage(chatId, uri, context, type, if (index == 0) caption else null)
        }
    }

    fun sendMediaMessages(chatId: String, files: List<java.io.File>, type: String, caption: String? = null) {
        viewModelScope.launch {
            val finalType = if (type == "audio") "audio_message" else type
            val bodyText = if (!caption.isNullOrBlank()) caption else type.replaceFirstChar { it.uppercase() }

            if (finalType == "image") {
                val clientUuid = java.util.UUID.randomUUID().toString()
                val localUris = files.map { android.net.Uri.fromFile(it).toString() }
                
                // 1. Optimistic Grid
                val myUserId = sessionManager.getUserId() ?: "0"
                val optimisticMsg = Message(
                    id = clientUuid,
                    conversationId = chatId,
                    content = bodyText,
                    senderId = myUserId,
                    senderName = "You",
                    timestamp = System.currentTimeMillis(),
                    isFromMe = true,
                    status = MessageStatus.SENDING,
                    type = MessageType.IMAGE,
                    mediaUrls = localUris,
                    clientId = clientUuid
                )
                repository.saveMessage(chatId, optimisticMsg)
                
                // 2. Upload all
                val uploadedUrls = mutableListOf<String>()
                val uploadJobs = files.map { file ->
                    launch {
                        accountRepository.uploadFile(file, "chat/$type").collect { res ->
                            if (res is Resource.Success && res.data != null) {
                                synchronized(uploadedUrls) { uploadedUrls.add(res.data.url) }
                            }
                        }
                    }
                }
                uploadJobs.joinAll()
                
                if (uploadedUrls.size == files.size) {
                    repository.updateMessageInCache(
                        chatId,
                        optimisticMsg.copy(mediaUrls = uploadedUrls.toList())
                    )
                    // 3. Send via API
                    sendMessage(chatId, bodyText, finalType, uploadedUrls, null, null, null, null, null, null, null, null, null, null, clientUuid)
                    files.forEach { it.delete() }
                } else {
                    repository.updateMessageStatus(clientUuid, MessageStatus.FAILED)
                }
            } else {
                // Separate bubbles for non-images
                files.forEach { file ->
                    sendMediaMessage(chatId, file, type, caption)
                }
            }
        }
    }

    fun getMessageReaders(messageId: String): Flow<Resource<List<com.tpsk.tj.chat.data.api.ApiReader>>> {
        return repository.getMessageReaders(messageId)
    }

    fun reportMessage(messageId: String, reason: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            repository.reportMessage(messageId, reason).collect { res ->
                when (res) {
                    is Resource.Success -> onDone(true)
                    is Resource.Error -> onDone(false)
                    is Resource.Loading -> Unit
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}
