package com.example.firstapp.chat.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.chat.data.*
import com.example.firstapp.chat.model.*
import com.example.firstapp.common.model.*
import com.example.firstapp.common.util.Resource
import com.example.firstapp.domain.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map as flowMap
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val webRtcCallManager: WebRtcCallManager,
    private val callTonePlayer: CallTonePlayer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Compatibility properties for current UI code
    val chats = uiState.flowMap { it.chats }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
    val messages = uiState.flowMap { it.messages }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
    val archivedChats = uiState.flowMap { it.archivedChats }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
    val starredMessages = uiState.flowMap { it.starredMessages }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())
    val chatSearchResults = uiState.flowMap { it.chatSearchResults }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val selectedContactsForGroup = uiState.flowMap { it.selectedContactsForGroup }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var activeChatId: Int? = null
    private var messagesFetchJob: kotlinx.coroutines.Job? = null
    private val groupNameCache = mutableMapOf<Int, String>()

    init {
        handleIntent(ChatIntent.FetchChats)
        connectChatRealtime()
    }

    fun handleIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.FetchChats -> fetchChats()
            is ChatIntent.FetchMessages -> fetchMessages(intent.chatId)
            is ChatIntent.SendMessage -> sendMessage(intent.chatId, intent.body, intent.messageType, intent.mediaUrl, intent.duration, intent.fileName, intent.fileSize, intent.contactName, intent.contactPhone, intent.latitude, intent.longitude, intent.replyToId)
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
                            if (newName.isNotBlank() && !newName.startsWith("User ") && newName.lowercase() != "unknown") {
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

    fun fetchMessages(chatId: Int) {
        activeChatId = chatId
        messagesFetchJob?.cancel()
        messagesFetchJob = viewModelScope.launch {
            repository.getMessages(chatId).collect { resource ->
                _uiState.update { it.copy(messages = resource) }
            }
        }
    }

    private fun connectChatRealtime() {
        ChatWebSocketClient.connect(-1, object : ChatSocketListener {
            override fun onOpen() {}
            override fun onMessage(contactId: Int, message: Message, isGroup: Boolean) {
                viewModelScope.launch { repository.saveMessage(contactId, message) }
                if (!message.isFromMe && activeChatId == contactId) {
                    ChatWebSocketClient.sendRead(contactId)
                    markAsRead(contactId)
                } else if (!message.isFromMe) {
                    viewModelScope.launch { repository.incrementUnreadCount(contactId) }
                }
                updateChatLastMessage(contactId, message)
            }
            override fun onMessageDeleted(contactId: Int, messageId: String) {
                viewModelScope.launch {
                    repository.deleteMessageLocal(messageId)
                    fetchChats()
                }
            }
            override fun onMessageEdited(contactId: Int, message: Message) {
                viewModelScope.launch { repository.updateMessageInCache(contactId, message) }
                fetchChats()
            }
            override fun onReaction(contactId: Int, messageId: String, reactions: Map<String, Int>) {
                viewModelScope.launch { repository.updateMessageReactions(messageId, reactions) }
            }
            override fun onTyping(contactId: Int, isTyping: Boolean) { updateChatTyping(contactId, isTyping) }
            override fun onRead(contactId: Int) {
                viewModelScope.launch {
                    repository.markChatAsReadLocal(contactId)
                    fetchChats()
                }
            }
            override fun onDelivered(contactId: Int, messageId: String) {
                viewModelScope.launch { repository.updateMessageStatus(messageId, MessageStatus.DELIVERED) }
            }
            override fun onPresence(contactId: Int, isOnline: Boolean, lastSeen: Long) { updateChatPresence(contactId, isOnline, lastSeen) }
            override fun onError(error: String) {}
            override fun onNewChat(chat: ChatMessage) {
                if (chat.isGroup && chat.senderName.isNotBlank()) groupNameCache[chat.id] = chat.senderName
                viewModelScope.launch {
                    repository.saveChat(chat)
                    fetchChats()
                }
            }
        })
    }

    fun sendMessage(chatId: Int, body: String, messageType: String = "text", mediaUrl: String? = null, duration: String? = null, fileName: String? = null, fileSize: String? = null, contactName: String? = null, contactPhone: String? = null, latitude: Double? = null, longitude: Double? = null, replyToId: String? = null) {
        viewModelScope.launch {
            repository.sendMessage(chatId, body, messageType, mediaUrl, duration, fileName, fileSize, contactName, contactPhone, latitude, longitude, replyToId).collect { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    updateChatLastMessage(chatId, resource.data)
                }
            }
        }
    }

    fun initiateCall(receiverId: Int, callType: CallType) {
        viewModelScope.launch {
            repository.initiateCall(receiverId, callType.wireValue()).collect { resource ->
                if (resource is Resource.Success && resource.data != null) {
                    val sessionId = resource.data
                    GlobalCallState.setCallActive(true, sessionId, receiverId, callType)
                    webRtcCallManager.start(callType)
                    callTonePlayer.playOutgoingRingtone()
                    
                    // Also notify server
                    CallWebSocketClient.invite(receiverId, sessionId, callType)
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

    fun markAsRead(chatId: Int) {
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

    fun updateChatTyping(contactId: Int, isTyping: Boolean) { viewModelScope.launch { repository.updateTyping(contactId, isTyping) } }
    fun updateChatPresence(contactId: Int, isOnline: Boolean, lastSeen: Long) { 
        viewModelScope.launch { 
            repository.updatePresence(contactId, isOnline, lastSeen)
            if (!isOnline) {
                repository.updateTyping(contactId, false)
            }
        } 
    }
    fun updateChatLastMessage(contactId: Int, message: Message) { 
        viewModelScope.launch { 
            repository.updateLastMessage(contactId, message.content, message.timestamp, message.isFromMe, message.isRead) 
        } 
    }

    fun deleteChat(chatId: Int) { viewModelScope.launch { repository.deleteChat(chatId).collect { result -> if (result is Resource.Success) fetchChats() } } }
    fun clearChat(chatId: Int) {
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

    fun createGroup(name: String, contactIds: List<Int>, onSuccess: (ChatMessage) -> Unit) {
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

    fun pinChat(chatId: Int, isPinned: Boolean) { viewModelScope.launch { repository.pinChat(chatId, isPinned).collect { if (it is Resource.Success) fetchChats() } } }
    fun muteChat(chatId: Int, isMuted: Boolean) { viewModelScope.launch { repository.muteChat(chatId, isMuted).collect { if (it is Resource.Success) fetchChats() } } }
    fun archiveChat(chatId: Int, isArchived: Boolean) { viewModelScope.launch { repository.archiveChat(chatId, isArchived).collect { if (it is Resource.Success) fetchChats() } } }

    fun reactToMessage(messageId: String, emoji: String) {
        viewModelScope.launch { repository.reactToMessage(messageId, emoji).collect { } }
    }

    fun updateWallpaper(chatId: Int, wallpaperUrl: String?) {
        viewModelScope.launch { repository.updateWallpaper(chatId, wallpaperUrl); fetchChats() }
    }

    fun updateCustomNotificationTone(chatId: Int, tone: String?) {
        viewModelScope.launch { repository.updateCustomNotificationTone(chatId, tone); fetchChats() }
    }

    fun updateMediaVisibility(chatId: Int, visible: Boolean) {
        viewModelScope.launch { repository.updateMediaVisibility(chatId, visible); fetchChats() }
    }

    fun updateDisappearingMessages(chatId: Int, duration: Int) {
        viewModelScope.launch { repository.updateDisappearingMessages(chatId, duration); fetchChats() }
    }

    fun loadMoreMessages(chatId: Int, page: Int) {
        viewModelScope.launch { repository.getMessages(chatId, page).collect { } }
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

    fun searchInChat(chatId: Int, query: String) {
        if (query.isBlank()) { _uiState.update { it.copy(chatSearchResults = emptyList()) }; return }
        viewModelScope.launch { repository.searchLocalMessages(chatId, query).collect { res -> _uiState.update { it.copy(chatSearchResults = res) } } }
    }

    fun getCachedGroupName(groupId: Int): String? {
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
        ChatWebSocketClient.close()
        super.onCleared()
    }
}
