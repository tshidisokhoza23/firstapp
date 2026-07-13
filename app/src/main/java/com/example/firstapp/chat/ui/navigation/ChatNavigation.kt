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

import com.example.firstapp.common.model.ConversationType

fun NavGraphBuilder.chatsNav(navController: NavHostController) {
    composable(
        route = Screen.Chat.route,
        arguments = listOf(
            navArgument("chatId") { type = NavType.StringType },
            navArgument("type") { type = NavType.StringType; defaultValue = "direct" },
            navArgument("jobId") { type = NavType.StringType; nullable = true; defaultValue = "" },
            navArgument("jobTitle") { type = NavType.StringType; nullable = true; defaultValue = "" },
            navArgument("companyName") { type = NavType.StringType; nullable = true; defaultValue = "" }
        )
    ) { backStackEntry ->
        val chatViewModel: ChatViewModel = hiltViewModel()
        val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
        val typeStr = backStackEntry.arguments?.getString("type") ?: "direct"
        val jobId = backStackEntry.arguments?.getString("jobId")?.takeIf { it.isNotBlank() }
        val jobTitle = backStackEntry.arguments?.getString("jobTitle")?.takeIf { it.isNotBlank() }
        val companyName = backStackEntry.arguments?.getString("companyName")?.takeIf { it.isNotBlank() }
        
        val type = if (typeStr == "group") ConversationType.GROUP 
                   else if (typeStr == "channel") ConversationType.CHANNEL 
                   else ConversationType.DIRECT
        
        val chatsState by chatViewModel.chats.collectAsState()
        val chats = chatsState.data ?: emptyList()
        val existingChat = chats.find { it.id == chatId && it.type == type }
        val chat = (existingChat ?: run {
            val baseName = if (type == ConversationType.GROUP) (chatViewModel.getCachedGroupName(chatId) ?: "Group") 
                else if (type == ConversationType.CHANNEL) "Channel"
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
                type = type,
                senderName = sanitized,
                lastMessage = "",
                timestamp = 0L,
                avatarColor = BoyishPurple,
                isGroup = type == ConversationType.GROUP,
                isAdmin = false
            )
        }).copy(type = type)
        
        LaunchedEffect(chatId) {
            chatViewModel.fetchMessages(chatId)
        }

        ChatMessagesScreen(
            chat = chat,
            chatViewModel = chatViewModel,
            onBack = { navController.popBackStack() },
            onShowProfile = { userId, isGroup -> 
                navController.navigate(Screen.UserProfile.createRoute(userId, isGroup)) 
            },
            jobId = jobId,
            jobTitle = jobTitle,
            companyName = companyName
        )
    }

    composable(
        route = Screen.DirectChat.route,
        arguments = listOf(navArgument("chatId") { type = NavType.StringType })
    ) { backStackEntry ->
        val chatViewModel: ChatViewModel = hiltViewModel()
        val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
        
        val chatsState by chatViewModel.chats.collectAsState()
        val chats = chatsState.data ?: emptyList()
        val existingChat = chats.find { it.id == chatId && it.type == ConversationType.DIRECT }
        val chat = existingChat ?: ChatMessage(
            id = chatId,
            type = ConversationType.DIRECT,
            senderName = GlobalCallState.getContactName(chatId) ?: "User $chatId",
            lastMessage = "",
            timestamp = 0L,
            unreadCount = 0,
            avatarUrl = null,
            avatarColor = BoyishPurple
        )

        ChatMessagesScreen(
            chat = chat,
            chatViewModel = chatViewModel,
            onBack = { navController.popBackStack() },
            onShowProfile = { userId, isGroup -> 
                navController.navigate(Screen.UserProfile.createRoute(userId, isGroup)) 
            }
        )
    }

    composable(Screen.ArchivedChats.route) {
        val chatViewModel: ChatViewModel = hiltViewModel()
        ArchivedChatsScreen(
            onBack = { navController.popBackStack() },
            onChatClick = { chat -> 
                val typeStr = if (chat.type == ConversationType.CHANNEL) "channel" 
                             else if (chat.type == ConversationType.GROUP) "group" 
                             else "direct"
                navController.navigate(Screen.Chat.createRoute(chat.id, type = typeStr)) 
            },
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
                navController.navigate(Screen.Chat.createRoute(chat.id, type = "group")) {
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
            onChatSelect = { chat -> 
                val typeStr = if (chat.type == ConversationType.CHANNEL) "channel" 
                             else if (chat.type == ConversationType.GROUP) "group" 
                             else "direct"
                navController.navigate(Screen.Chat.createRoute(chat.id, type = typeStr)) 
            },
            onCallSelect = { call -> 
                navController.navigate(Screen.Chat.createRoute(call.contactId))
            },
            onNewDM = { navController.navigate(Screen.ContactPicker.route) },
            viewModel = directMessagesViewModel
        )
    }

    composable(
        route = Screen.SharedMedia.route,
        arguments = listOf(navArgument("chatId") { type = NavType.StringType })
    ) { backStackEntry ->
        val sharedMediaViewModel: SharedMediaViewModel = hiltViewModel()
        val chatViewModel: ChatViewModel = hiltViewModel()
        val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
        
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
            } ?: c.id
            c.copy(senderName = sanitized)
        }
        
        SharedMediaScreen(
            chat = chat,
            onBack = { navController.popBackStack() },
            viewModel = sharedMediaViewModel
        )
    }
}
