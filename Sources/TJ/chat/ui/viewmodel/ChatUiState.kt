package com.tpsk.tj.chat.ui.viewmodel

import com.tpsk.tj.chat.model.ChatMessage
import com.tpsk.tj.chat.model.Message
import com.tpsk.tj.chat.model.Contact
import com.tpsk.tj.common.model.User
import com.tpsk.tj.common.util.Resource

data class ChatUiState(
    val currentUser: User? = null,
    val chats: Resource<List<ChatMessage>> = Resource.Loading(emptyList()),
    val archivedChats: Resource<List<ChatMessage>> = Resource.Loading(emptyList()),
    val messages: Resource<List<Message>> = Resource.Loading(emptyList()),
    val isChatsFetching: Boolean = false,
    val starredMessages: Resource<List<Message>> = Resource.Loading(),
    val chatSearchResults: List<Message> = emptyList(),
    val selectedContactsForGroup: List<Contact> = emptyList(),
    val closedConversationIds: Set<String> = emptySet(),
    val typingStatus: Map<String, Boolean> = kotlin.collections.emptyMap(),
    val groupInfo: Resource<com.tpsk.tj.chat.data.api.GroupInfoResponse> = Resource.Loading(),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class ChatIntent {
    object FetchChats : ChatIntent()
    data class FetchMessages(val chatId: String) : ChatIntent()
    data class SendMessage(
        val chatId: String, 
        val body: String, 
        val messageType: String = "text",
        val mediaUrls: List<String> = emptyList(),
        val duration: String? = null,
        val fileName: String? = null,
        val fileSize: String? = null,
        val contactName: String? = null,
        val contactPhone: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val replyToId: String? = null,
        val replyToText: String? = null,
        val replyToSenderName: String? = null
    ) : ChatIntent()
    data class MarkAsRead(val chatId: String) : ChatIntent()
    data class DeleteMessage(val messageId: String, val deleteForEveryone: Boolean = false) : ChatIntent()
    data class EditMessage(val messageId: String, val newBody: String) : ChatIntent()
    data class ReactToMessage(val messageId: String, val emoji: String) : ChatIntent()
    data class SearchInChat(val chatId: String, val query: String) : ChatIntent()
    data class SetSelectedContactsForGroup(val contacts: List<Contact>) : ChatIntent()
    data class CreateGroup(val name: String, val contactIds: List<String>, val onSuccess: (ChatMessage) -> Unit) : ChatIntent()
    data class ArchiveChat(val chatId: String, val isArchived: Boolean) : ChatIntent()
    data class PinChat(val chatId: String, val isPinned: Boolean) : ChatIntent()
    data class MuteChat(val chatId: String, val isMuted: Boolean) : ChatIntent()
    data class ClearChat(val chatId: String) : ChatIntent()
    data class StarMessage(val messageId: String, val isStarred: Boolean) : ChatIntent()
    object ClearAllChats : ChatIntent()
}
