package com.example.firstapp.chat.ui.navigation

import kotlin.collections.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.chat.ui.screens.ArchivedChatsScreen
import com.example.firstapp.chat.ui.screens.ChatMessagesScreen
import com.example.firstapp.chat.ui.screens.ContactPickerScreen
import com.example.firstapp.chat.ui.screens.CreateGroupScreen
import com.example.firstapp.chat.ui.screens.StarredMessagesScreen
import com.example.firstapp.chat.ui.viewmodel.ChatViewModel
import com.example.firstapp.chat.ui.viewmodel.ContactPickerViewModel
import com.example.firstapp.chat.ui.viewmodel.CreateGroupViewModel
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
        arguments = kotlin.collections.listOf(
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
        
        var resolvedChatId by remember { mutableStateOf<String?>(if (typeStr == "initiate") null else chatId) }
        var targetUserId by remember { mutableStateOf<String?>(if (typeStr == "initiate") chatId else null) }
        
        LaunchedEffect(chatId, typeStr) {
            if (typeStr == "initiate") {
                chatViewModel.initiateDM(chatId).collect { resource ->
                    if (resource is Resource.Success && resource.data != null) {
                        resolvedChatId = resource.data
                        targetUserId = chatId
                    }
                }
            } else {
                resolvedChatId = chatId
            }
        }

        if (resolvedChatId == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = com.example.firstapp.common.ui.theme.AppAccent)
            }
            return@composable
        }

        val chatsState by chatViewModel.chats.collectAsState()
        val chats = chatsState.data ?: emptyList<ChatMessage>()
        val existingChat = chats.firstOrNull { it.id == resolvedChatId && it.type == type }
        
        val baseName = if (type == ConversationType.GROUP) {
            existingChat?.senderName ?: chatViewModel.getCachedGroupName(resolvedChatId!!) ?: ""
        } else if (type == ConversationType.CHANNEL) {
            existingChat?.senderName ?: ""
        } else {
            // Priority: Local DB Name > Contact Cache Name > Phone Number
            existingChat?.senderName?.takeIf { it.isNotBlank() && it != resolvedChatId }
                ?: GlobalCallState.getContactName(resolvedChatId!!)
                ?: GlobalCallState.getContactPhone(resolvedChatId!!) 
                ?: existingChat?.phoneNumber
                ?: ""
        }
        
        val sanitizedName = if (baseName == "" || baseName.lowercase() == "unknown" || baseName.lowercase() == "null") {
            resolvedChatId!!
        } else {
            baseName
        }

        val chat = (existingChat ?: ChatMessage(
            id = resolvedChatId!!,
            type = type,
            senderName = sanitizedName,
            lastMessage = "",
            timestamp = 0L,
            avatarUrl = null,
            avatarColor = BoyishPurple,
            isGroup = type == ConversationType.GROUP,
            isAdmin = false,
            otherUserId = targetUserId
        )).copy(type = type)
        
        LaunchedEffect(resolvedChatId) {
            chatViewModel.fetchMessages(resolvedChatId!!)
        }

        ChatMessagesScreen(
            chat = chat,
            chatViewModel = chatViewModel,
            onBack = { navController.popBackStack() },
            onShowProfile = { userId, isGroup -> 
                navController.navigate(Screen.UserProfile.createRoute(userId, isGroup)) 
            },
            onAvatarClick = { userId ->
                if (chat.avatarUrl != null) {
                    navController.navigate(Screen.MediaViewer.createRoute(chat.avatarUrl, "image", 0L))
                } else {
                    navController.navigate(Screen.UserProfile.createRoute(userId, chat.isGroup))
                }
            },
            jobId = jobId,
            jobTitle = jobTitle,
            companyName = companyName
        )
    }

    composable(
        route = Screen.DirectChat.route,
        arguments = kotlin.collections.listOf(navArgument("chatId") { type = NavType.StringType })
    ) { backStackEntry ->
        val chatViewModel: ChatViewModel = hiltViewModel()
        val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
        
        val chatsState by chatViewModel.chats.collectAsState()
        val chats = chatsState.data ?: emptyList()
        val existingChat = chats.firstOrNull { it.id == chatId && it.type == ConversationType.DIRECT }
        val chat = existingChat ?: ChatMessage(
            id = chatId,
            type = ConversationType.DIRECT,
            senderName = GlobalCallState.getContactName(chatId) ?: chatId,
            lastMessage = "",
            timestamp = 0L,
            unreadCount = 0,
            avatarUrl = null,
            avatarColor = BoyishPurple,
            otherUserId = chatId
        )

        ChatMessagesScreen(
            chat = chat,
            chatViewModel = chatViewModel,
            onBack = { navController.popBackStack() },
            onShowProfile = { userId, isGroup -> 
                navController.navigate(Screen.UserProfile.createRoute(userId, isGroup)) 
            },
            onAvatarClick = { userId ->
                if (chat.avatarUrl != null) {
                    navController.navigate(Screen.MediaViewer.createRoute(chat.avatarUrl, "image", 0L))
                } else {
                    navController.navigate(Screen.UserProfile.createRoute(userId, chat.isGroup))
                }
            }
        )
    }

    composable(Screen.ArchivedChats.route) {
        val chatViewModel: ChatViewModel = hiltViewModel()
        ArchivedChatsScreen(
            onBack = { navController.popBackStack() },
            onChatClick = { chat -> 
                val typeStr = when (chat.type) {
                    ConversationType.CHANNEL -> "channel"
                    ConversationType.GROUP -> "group"
                    else -> "direct"
                }
                navController.navigate(Screen.Chat.createRoute(chat.id, type = typeStr)) 
            },
            viewModel = chatViewModel
        )
    }

    composable(Screen.StarredMessages.route) {
        val chatViewModel: ChatViewModel = hiltViewModel()
        val starredMessages by chatViewModel.starredMessages.collectAsState()
        StarredMessagesScreen(
            messagesState = starredMessages,
            onBack = { navController.popBackStack() },
            onMessageClick = { msg ->
                navController.navigate(Screen.Chat.createRoute(msg.conversationId))
            },
            viewModel = androidx.hilt.navigation.compose.hiltViewModel()
        )
    }

    composable(
        route = Screen.ContactPicker.route,
        arguments = kotlin.collections.listOf(navArgument("isGroupMode") { type = NavType.BoolType; defaultValue = false })
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
                navController.navigate(Screen.Chat.createRoute(contact.id, type = "initiate")) {
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
            onCreated = { newChat ->
                navController.navigate(Screen.Chat.createRoute(newChat.id, type = "group")) {
                    popUpTo(Screen.Main.route)
                }
            },
            viewModel = createGroupViewModel,
            onRemoveContact = { contact ->
                val updated = selectedContacts.filter { it.id != contact.id }
                chatViewModel.updateSelectedContacts(updated)
            }
        )
    }

    composable(
        route = Screen.SharedMedia.route,
        arguments = kotlin.collections.listOf(navArgument("chatId") { type = NavType.StringType })
    ) { backStackEntry ->
        val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
        val chatViewModel: ChatViewModel = hiltViewModel()
        val viewModel: SharedMediaViewModel = hiltViewModel()
        
        val chatsState by chatViewModel.chats.collectAsState()
        val chats = chatsState.data ?: emptyList()
        val chat = chats.firstOrNull { it.id == chatId } ?: ChatMessage(
            id = chatId,
            type = ConversationType.DIRECT,
            senderName = "Chat",
            lastMessage = "",
            timestamp = 0L,
            avatarColor = BoyishPurple
        )

        SharedMediaScreen(
            chat = chat,
            onBack = { navController.popBackStack() },
            viewModel = viewModel
        )
    }
}
