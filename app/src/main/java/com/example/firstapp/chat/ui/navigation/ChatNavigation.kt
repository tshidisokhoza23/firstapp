package com.example.firstapp.chat.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.firstapp.chat.model.*
import com.example.firstapp.chat.ui.screens.*
import com.example.firstapp.chat.ui.viewmodel.*
import com.example.firstapp.chat.data.GlobalCallState
import com.example.firstapp.media.ui.screens.SharedMediaScreen
import com.example.firstapp.media.ui.viewmodel.SharedMediaViewModel
import com.example.firstapp.common.ui.theme.BoyishPurple
import com.example.firstapp.common.util.Resource
import com.example.firstapp.ui.navigation.Screen

fun NavGraphBuilder.chatNavGraph(navController: NavHostController) {
    composable(
        route = Screen.Chat.route,
        arguments = listOf(
            navArgument("chatId") { type = NavType.IntType },
            navArgument("isGroup") { type = NavType.BoolType; defaultValue = false }
        )
    ) { backStackEntry ->
        val chatViewModel: ChatViewModel = hiltViewModel()
        val chatId = backStackEntry.arguments?.getInt("chatId") ?: return@composable
        val isGroupParam = backStackEntry.arguments?.getBoolean("isGroup") ?: false
        val chatsState by chatViewModel.chats.collectAsState()
        val chats = chatsState.data ?: emptyList()
        val existingChat = chats.find { it.id == chatId }
        val chat = existingChat ?: run {
            val baseName = if (isGroupParam) (chatViewModel.getCachedGroupName(chatId) ?: "Group") 
                else GlobalCallState.getContactName(chatId)
                    ?: GlobalCallState.getContactPhone(chatId) 
                    ?: "User $chatId"
            
            val sanitized = baseName.takeIf { 
                it.isNotBlank() && 
                !it.startsWith("User ") && 
                it.lowercase() != "unknown" && 
                it.lowercase() != "null" && 
                it.lowercase() != "undefined" 
            } ?: "User $chatId"

            ChatMessage(
                id = chatId,
                senderName = sanitized,
                lastMessage = "",
                timestamp = 0L,
                avatarColor = BoyishPurple,
                isGroup = isGroupParam
            )
        }
        
        LaunchedEffect(chatId) {
            chatViewModel.fetchMessages(chatId)
        }

        ChatScreen(
            chat = chat,
            chatViewModel = chatViewModel,
            onBack = { navController.popBackStack() },
            onShowProfile = { userId, isGroup -> navController.navigate(Screen.UserProfile.createRoute(userId, isGroup)) }
        )
    }

    composable(Screen.ArchivedChats.route) {
        val chatViewModel: ChatViewModel = hiltViewModel()
        ArchivedChatsScreen(
            onBack = { navController.popBackStack() },
            onChatClick = { chat -> navController.navigate(Screen.Chat.createRoute(chat.id, isGroup = chat.isGroup)) },
            viewModel = chatViewModel
        )
    }
    
    composable(Screen.StarredMessages.route) {
        val starredMessagesViewModel: StarredMessagesViewModel = hiltViewModel()
        LaunchedEffect(Unit) {
            starredMessagesViewModel.fetchStarredMessages()
        }
        StarredMessagesScreen(
            messagesState = starredMessagesViewModel.starredMessages.collectAsState().value,
            onBack = { navController.popBackStack() },
            onMessageClick = { /* maybe navigate to original chat? */ }
        )
    }

    composable(
        route = Screen.ContactPicker.route,
        arguments = listOf(navArgument("isGroupMode") { type = NavType.BoolType; defaultValue = false })
    ) { backStackEntry ->
        val isGroupMode = backStackEntry.arguments?.getBoolean("isGroupMode") ?: false
        val chatViewModel: ChatViewModel = hiltViewModel()
        val contactPickerViewModel: ContactPickerViewModel = hiltViewModel()
        ContactPickerScreen(
            viewModel = contactPickerViewModel,
            initialIsGroupMode = isGroupMode,
            onBack = { navController.popBackStack() },
            onContactSelected = { contact ->
                GlobalCallState.updateContactInfo(contact.id, contact.displayName, contact.phoneNumber)
                navController.navigate(Screen.Chat.createRoute(contact.id)) {
                    popUpTo(Screen.ContactPicker.route) { inclusive = true }
                }
            },
            onInviteClicked = { /* invite logic */ },
            onGroupCreate = { contacts ->
                chatViewModel.updateSelectedContacts(contacts)
                navController.navigate(Screen.CreateGroup.route)
            }
        )
    }
    
    composable(Screen.CreateGroup.route) {
        val chatViewModel: ChatViewModel = hiltViewModel()
        val createGroupViewModel: CreateGroupViewModel = hiltViewModel()
        val selectedContacts = chatViewModel.selectedContactsForGroup.collectAsState().value
        CreateGroupScreen(
            selectedContacts = selectedContacts,
            onBack = { navController.popBackStack() },
            onRemoveContact = { contact ->
                val updated = selectedContacts.filter { it.id != contact.id }
                chatViewModel.updateSelectedContacts(updated)
            },
            onCreated = { chat ->
                navController.navigate(Screen.Chat.createRoute(chat.id, isGroup = true)) {
                    popUpTo(Screen.CreateGroup.route) { inclusive = true }
                }
            },
            viewModel = createGroupViewModel
        )
    }

    composable(Screen.DirectMessages.route) {
        val directMessagesViewModel: DirectMessagesViewModel = hiltViewModel()
        DirectMessagesScreen(
            onBack = { navController.popBackStack() },
            onChatSelect = { chat -> navController.navigate(Screen.Chat.createRoute(chat.id, isGroup = chat.isGroup)) },
            onCallSelect = { call -> 
                navController.navigate(Screen.Chat.createRoute(call.contactId))
            },
            onNewDM = { navController.navigate(Screen.ContactPicker.route) },
            viewModel = directMessagesViewModel
        )
    }

    composable(
        route = Screen.SharedMedia.route,
        arguments = listOf(navArgument("chatId") { type = NavType.IntType })
    ) { backStackEntry ->
        val sharedMediaViewModel: SharedMediaViewModel = hiltViewModel()
        val chatViewModel: ChatViewModel = hiltViewModel()
        val chatId = backStackEntry.arguments?.getInt("chatId") ?: return@composable
        
        val chatsState by chatViewModel.chats.collectAsState()
        val existingChat = (chatsState.data ?: emptyList()).find { it.id == chatId }
        val chat = (existingChat ?: ChatMessage(
            id = chatId, 
            senderName = GlobalCallState.getContactName(chatId)
                ?: GlobalCallState.getContactPhone(chatId)
                ?: "$chatId", 
            lastMessage = "", 
            timestamp = 0L, 
            avatarColor = Color.Gray
        )).let { c ->
            val sanitized = c.senderName.takeIf { 
                it.isNotBlank() && 
                !it.startsWith("User ") && 
                it.lowercase() != "unknown" && 
                it.lowercase() != "null" && 
                it.lowercase() != "undefined" 
            } ?: c.id.toString()
            c.copy(senderName = sanitized)
        }
        
        SharedMediaScreen(
            chat = chat,
            onBack = { navController.popBackStack() },
            viewModel = sharedMediaViewModel
        )
    }
}
