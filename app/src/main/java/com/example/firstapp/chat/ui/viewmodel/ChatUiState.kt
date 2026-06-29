package com.example.firstapp.chat.ui.viewmodel

import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.chat.model.Message
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.model.User
import com.example.firstapp.common.util.Resource

data class ChatUiState(
    val currentUser: User? = null,
    val chats: Resource<List<ChatMessage>> = Resource.Loading(),
    val archivedChats: Resource<List<ChatMessage>> = Resource.Loading(),
    val messages: Resource<List<Message>> = Resource.Loading(),
    val isChatsFetching: Boolean = false,
    val starredMessages: Resource<List<Message>> = Resource.Loading(),
    val chatSearchResults: List<Message> = emptyList(),
    val selectedContactsForGroup: List<Contact> = emptyList(),
    val error: String? = null
)

sealed class ChatIntent {
    object FetchChats : ChatIntent()
    data class FetchMessages(val chatId: Int) : ChatIntent()
    data class SendMessage(
        val chatId: Int, 
        val body: String, 
        val messageType: String = "text",
        val mediaUrl: String? = null,
        val duration: String? = null,
        val fileName: String? = null,
        val fileSize: String? = null,
        val contactName: String? = null,
        val contactPhone: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val replyToId: String? = null
    ) : ChatIntent()
    data class MarkAsRead(val chatId: Int) : ChatIntent()
    data class DeleteMessage(val messageId: String) : ChatIntent()
    data class EditMessage(val messageId: String, val newBody: String) : ChatIntent()
    data class ReactToMessage(val messageId: String, val emoji: String) : ChatIntent()
    data class SearchInChat(val chatId: Int, val query: String) : ChatIntent()
    data class SetSelectedContactsForGroup(val contacts: List<Contact>) : ChatIntent()
    data class CreateGroup(val name: String, val contactIds: List<Int>, val onSuccess: (ChatMessage) -> Unit) : ChatIntent()
    data class ArchiveChat(val chatId: Int, val isArchived: Boolean) : ChatIntent()
    data class PinChat(val chatId: Int, val isPinned: Boolean) : ChatIntent()
    data class MuteChat(val chatId: Int, val isMuted: Boolean) : ChatIntent()
    data class ClearChat(val chatId: Int) : ChatIntent()
    object ClearAllChats : ChatIntent()
}
