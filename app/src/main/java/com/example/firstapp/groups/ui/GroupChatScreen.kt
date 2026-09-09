package com.example.firstapp.groups.ui

import androidx.compose.runtime.Composable
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.chat.ui.screens.ChatMessagesScreen
import com.example.firstapp.chat.ui.viewmodel.ChatViewModel
import com.example.firstapp.common.model.ConversationType

@Composable
fun GroupChatScreen(
    chat: ChatMessage,
    chatViewModel: ChatViewModel,
    onBack: () -> Unit,
    onOpenGroupInfo: () -> Unit
) {
    val group = chat.copy(type = ConversationType.GROUP, isGroup = true)
    ChatMessagesScreen(
        chat = group,
        chatViewModel = chatViewModel,
        onBack = onBack,
        onShowProfile = { _, _ -> onOpenGroupInfo() },
        onAvatarClick = { onOpenGroupInfo() }
    )
}
