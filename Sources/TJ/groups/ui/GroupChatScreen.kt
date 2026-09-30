package com.tpsk.tj.groups.ui

import androidx.compose.runtime.Composable
import com.tpsk.tj.chat.model.ChatMessage
import com.tpsk.tj.chat.ui.screens.ChatMessagesScreen
import com.tpsk.tj.chat.ui.viewmodel.ChatViewModel
import com.tpsk.tj.common.model.ConversationType

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
