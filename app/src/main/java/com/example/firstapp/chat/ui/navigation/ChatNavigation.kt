package com.example.firstapp.chat.ui.navigation

import kotlin.collections.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.firstapp.chat.model.*
import com.example.firstapp.chat.ui.screens.*
import com.example.firstapp.chat.ui.stories.*
import com.example.firstapp.chat.ui.channels.*
import com.example.firstapp.chat.ui.screens.*
import com.example.firstapp.chat.ui.viewmodel.*
import com.example.firstapp.chat.data.GlobalCallState
import com.example.firstapp.media.ui.screens.SharedMediaScreen
import com.example.firstapp.media.ui.viewmodel.SharedMediaViewModel
import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import com.example.firstapp.common.ui.theme.BoyishPurple
import com.example.firstapp.common.util.Resource
import com.example.firstapp.ui.navigation.Screen
import com.example.firstapp.common.model.ConversationType
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable

fun NavGraphBuilder.chatsNav(navController: NavHostController) {
    composable(
        route = Screen.Story.route,
        arguments = listOf(
            navArgument("userId") { type = NavType.StringType; defaultValue = "" },
            navArgument("showComposer") { type = NavType.BoolType; defaultValue = false },
            navArgument("play") { type = NavType.BoolType; defaultValue = true }
        )
    ) { backStackEntry ->
        val initialUserId = backStackEntry.arguments?.getString("userId") ?: ""
        val showComposer = backStackEntry.arguments?.getBoolean("showComposer") ?: false
        val play = backStackEntry.arguments?.getBoolean("play") ?: true
        val storyViewModel: StoryViewModel = hiltViewModel()
        val mediaViewModel: MediaViewModel = hiltViewModel()
        val adManager = (navController.context as? com.example.firstapp.MainActivity)?.adManager 
            ?: com.example.firstapp.common.data.ads.AdMobManagerImpl()

        val storiesState = storyViewModel.stories.collectAsState(Resource.Loading()).value
        val contactsState = storyViewModel.contacts.collectAsState(Resource.Loading()).value
        val currentUser = com.example.firstapp.LocalCurrentUser.current
        val postResult by storyViewModel.postResult.collectAsState()
        val context = LocalContext.current
        
        LaunchedEffect(postResult) {
            postResult?.let {
                if (it is Resource.Error) {
                    Toast.makeText(context, it.message ?: "Failed to share story", Toast.LENGTH_LONG).show()
                    storyViewModel.clearPostResult()
                }
            }
        }

        StoryScreen(
            stories = storiesState.data ?: emptyList(),
            contacts = contactsState.data ?: emptyList(),
            currentUserId = currentUser?.id,
            mediaViewModel = mediaViewModel,
            adManager = adManager,
            initialViewingUserId = if (initialUserId.isNotBlank()) initialUserId else null,
            initialShowComposer = showComposer,
            initialPlay = play,
            onBack = { navController.popBackStack() },
            onPostStory = { content, url, type ->
                navController.navigate(Screen.ShareStory.createRoute(content, url ?: "", type))
            },
            onDeleteStory = { storyId -> storyViewModel.deleteStory(storyId) },
            onStoryViewed = { userId, storyId -> storyViewModel.markStoryAsViewed(userId, storyId) },
            onMuteStory = { userId, isMuted -> storyViewModel.muteStory(userId, isMuted) },
            onNavigateToNewStory = { navController.navigate(Screen.NewStory.route) }
        )
    }

    composable(route = Screen.NewStory.route) {
        NewStoryScreen(
            onBack = { navController.popBackStack() },
            onNext = { content, url, type ->
                navController.navigate(Screen.ShareStory.createRoute(content, url, type))
            }
        )
    }

    composable(
        route = Screen.ShareStory.route,
        arguments = listOf(
            navArgument("content") { type = NavType.StringType; defaultValue = "" },
            navArgument("url") { type = NavType.StringType; defaultValue = "" },
            navArgument("type") { type = NavType.StringType; defaultValue = "text" }
        )
    ) { backStackEntry ->
        val content = backStackEntry.arguments?.getString("content") ?: ""
        val url = backStackEntry.arguments?.getString("url") ?: ""
        val type = backStackEntry.arguments?.getString("type") ?: "text"
        val storyViewModel: StoryViewModel = hiltViewModel()
        val postResult by storyViewModel.postResult.collectAsState()
        val context = androidx.compose.ui.platform.LocalContext.current

        LaunchedEffect(postResult) {
            when (val res = postResult) {
                is com.example.firstapp.common.util.Resource.Success -> {
                    android.widget.Toast.makeText(context, "Story posted!", android.widget.Toast.LENGTH_SHORT).show()
                    storyViewModel.clearPostResult()
                    if (!navController.popBackStack(Screen.Main.createRoute("chat"), false)) {
                        navController.popBackStack()
                    }
                }
                is com.example.firstapp.common.util.Resource.Error -> {
                    android.widget.Toast.makeText(context, res.message ?: "Failed to post story", android.widget.Toast.LENGTH_LONG).show()
                    storyViewModel.clearPostResult()
                }
                else -> {}
            }
        }
        
        ShareStoryScreen(
            content = content,
            url = url,
            type = type,
            isPosting = postResult is com.example.firstapp.common.util.Resource.Loading,
            onBack = { navController.popBackStack() },
            onPost = { c, u, t, privacy ->
                storyViewModel.postStory(c, u.takeIf { it.isNotBlank() }, t, privacy)
            }
        )
    }

    composable(route = Screen.CreateChannel.route) {
        val channelsViewModel: ChannelsViewModel = hiltViewModel()
        CreateChannelScreen(
            onBack = { navController.popBackStack() },
            onCreated = { navController.popBackStack() },
            viewModel = channelsViewModel
        )
    }

    composable(
        route = Screen.ChannelDetail.route,
        arguments = listOf(navArgument("channelId") { type = NavType.StringType })
    ) { backStackEntry ->
        val channelId = backStackEntry.arguments?.getString("channelId") ?: return@composable
        val viewModel: ChannelDetailViewModel = hiltViewModel()
        val channelState by viewModel.channel.collectAsState()
        val postsState by viewModel.posts.collectAsState()
        val contacts by viewModel.contacts.collectAsState()
        val members by viewModel.members.collectAsState()
        val toast by viewModel.toast.collectAsState()
        val currentUser = com.example.firstapp.LocalCurrentUser.current
        var showComposer by remember { mutableStateOf(false) }
        var postText by remember { mutableStateOf("") }
        val context = LocalContext.current

        LaunchedEffect(channelId) {
            viewModel.loadChannel(channelId)
        }
        LaunchedEffect(toast) {
            val message = toast
            if (!message.isNullOrBlank()) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }

        if (showComposer) {
            AlertDialog(
                onDismissRequest = { showComposer = false },
                title = { Text("Post to channel") },
                text = {
                    OutlinedTextField(
                        value = postText,
                        onValueChange = { postText = it },
                        placeholder = { Text("Write an update") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val body = postText.trim()
                            if (body.isNotEmpty()) {
                                viewModel.createPost(channelId, body, emptyList())
                                postText = ""
                                showComposer = false
                            }
                        }
                    ) { Text("Post") }
                },
                dismissButton = {
                    TextButton(onClick = { showComposer = false }) { Text("Cancel") }
                }
            )
        }

        when (val ch = channelState) {
            is Resource.Success -> {
                val channel = ch.data
                if (channel != null) {
                    ChannelDetailScreen(
                        channel = channel,
                        posts = postsState.data ?: emptyList(),
                        currentUserId = viewModel.currentUserId ?: currentUser?.id,
                        contacts = contacts,
                        members = members,
                        onBack = { navController.popBackStack() },
                        onSubscribe = { viewModel.subscribe(it) },
                        onMute = { id, muted -> viewModel.mute(id, muted) },
                        onCreatePost = { showComposer = true },
                        onAddAdmin = { userId -> viewModel.addAdmin(channel.id, userId) },
                        onRemoveAdmin = { userId -> viewModel.removeAdmin(channel.id, userId) },
                        onToggleAdminPosting = { viewModel.toggleAdminPosting(channelId, it) },
                        onOpenDiscussion = {
                            Toast.makeText(context, "Channels use posts, not a 1:1 chat. Use + to publish.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
            is Resource.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(ch.message ?: "Channel not found")
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }


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
        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("app_root") }
        val chatViewModel: ChatViewModel = hiltViewModel(parentEntry)
        val channelsViewModel: ChannelsViewModel = hiltViewModel(parentEntry)
        val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
        val typeStr = backStackEntry.arguments?.getString("type") ?: "direct"
        val jobId = backStackEntry.arguments?.getString("jobId")?.takeIf { it.isNotBlank() }
        val jobTitle = backStackEntry.arguments?.getString("jobTitle")?.takeIf { it.isNotBlank() }
        val companyName = backStackEntry.arguments?.getString("companyName")?.takeIf { it.isNotBlank() }
        
        val type = when (typeStr.trim().lowercase()) {
            "group" -> ConversationType.GROUP
            "channel", "hiring_channel" -> ConversationType.CHANNEL
            "job_chat", "job_application" -> ConversationType.JOB_CHAT
            else -> ConversationType.DIRECT
        }

        if (type == ConversationType.GROUP) {
            LaunchedEffect(chatId) {
                navController.navigate(Screen.GroupChat.createRoute(chatId)) {
                    popUpTo(backStackEntry.destination.id) { inclusive = true }
                }
            }
            return@composable
        }
        if (type == ConversationType.CHANNEL) {
            LaunchedEffect(chatId) {
                navController.navigate(Screen.ChannelDetail.createRoute(chatId)) {
                    popUpTo(backStackEntry.destination.id) { inclusive = true }
                }
            }
            return@composable
        }
        
        val isInitiate = typeStr == "initiate" || typeStr == "initiate_dm"
        var resolvedChatId by remember { mutableStateOf<String?>(if (isInitiate) null else chatId) }
        var targetUserId by remember { mutableStateOf<String?>(if (isInitiate) chatId else null) }
        
        LaunchedEffect(chatId, typeStr) {
            if (isInitiate) {
                chatViewModel.initiateDM(chatId).collect { resource ->
                    if (resource is Resource.Success<String> && resource.data != null) {
                        resolvedChatId = resource.data
                        targetUserId = chatId
                        chatViewModel.fetchMessages(resource.data)
                    }
                }
            } else {
                resolvedChatId = chatId
                chatViewModel.fetchMessages(chatId)
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
            ?: chats.firstOrNull { it.id == resolvedChatId }
        
        val channelsState by channelsViewModel.channels.collectAsState()
        val channelFromRepo = channelsState.data?.find { it.id == resolvedChatId || it.conversationId == resolvedChatId }

        // Use conversationId as the primary key for the chat screen if it's a channel
        val effectiveChatId = if (type == ConversationType.CHANNEL) {
            channelFromRepo?.conversationId ?: resolvedChatId!!
        } else {
            resolvedChatId!!
        }

        // Channels can resolve their real conversationId after the initial fetch above
        // (e.g. channel list still syncing), so re-fetch once effectiveChatId settles to
        // avoid showing message history/real-time updates for the wrong conversation.
        LaunchedEffect(effectiveChatId) {
            if (!isInitiate && effectiveChatId != chatId) {
                chatViewModel.fetchMessages(effectiveChatId)
            }
        }

        val baseName = if (type == ConversationType.GROUP) {
            existingChat?.senderName ?: chatViewModel.getCachedGroupName(effectiveChatId) ?: ""
        } else if (type == ConversationType.CHANNEL) {
            existingChat?.senderName ?: channelFromRepo?.name ?: ""
        } else {
            // Priority: Local DB Name > Contact Cache Name > Phone Number
            existingChat?.senderName?.takeIf { it.isNotBlank() && it != effectiveChatId }
                ?: GlobalCallState.getContactName(effectiveChatId)
                ?: GlobalCallState.getContactPhone(effectiveChatId) 
                ?: existingChat?.phoneNumber
                ?: ""
        }
        
        val sanitizedName = if (baseName == "" || baseName.lowercase() == "unknown" || baseName.lowercase() == "null") {
            effectiveChatId
        } else {
            baseName
        }

        val chat = (existingChat ?: ChatMessage(
            id = effectiveChatId,
            type = type,
            senderName = sanitizedName,
            lastMessage = "",
            timestamp = 0L,
            avatarUrl = channelFromRepo?.avatarUrl,
            avatarColor = BoyishPurple,
            isGroup = type == ConversationType.GROUP,
            isAdmin = channelFromRepo?.isAdmin ?: false,
            isSubscribed = channelFromRepo?.isSubscribed ?: false,
            adminsCanPost = channelFromRepo?.adminsCanPost ?: false,
            followers = channelFromRepo?.followers ?: "0",
            createdBy = channelFromRepo?.createdBy,
            otherUserId = targetUserId
        )).copy(
            type = type,
            isAdmin = existingChat?.isAdmin ?: channelFromRepo?.isAdmin ?: false,
            isSubscribed = existingChat?.isSubscribed ?: channelFromRepo?.isSubscribed ?: false,
            adminsCanPost = existingChat?.adminsCanPost ?: channelFromRepo?.adminsCanPost ?: false,
            createdBy = existingChat?.createdBy ?: channelFromRepo?.createdBy
        )

        ChatMessagesScreen(
            chat = chat,
            chatViewModel = chatViewModel,
            onBack = { navController.popBackStack() },
            onShowProfile = { userId, isGroup ->
                when {
                    type == ConversationType.GROUP || isGroup -> navController.navigate(Screen.GroupDetail.createRoute(userId))
                    type == ConversationType.CHANNEL -> navController.navigate(Screen.ChannelDetail.createRoute(channelFromRepo?.id ?: userId))
                    else -> navController.navigate(Screen.ChatProfile.createRoute(userId, false))
                }
            },
            onAvatarClick = { userId ->
                when {
                    chat.avatarUrl != null -> navController.navigate(Screen.MediaViewer.createRoute(chat.avatarUrl, "image", 0L))
                    type == ConversationType.GROUP || chat.isGroup -> navController.navigate(Screen.GroupDetail.createRoute(userId))
                    type == ConversationType.CHANNEL -> navController.navigate(Screen.ChannelDetail.createRoute(channelFromRepo?.id ?: userId))
                    else -> navController.navigate(Screen.ChatProfile.createRoute(userId, false))
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
        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("app_root") }
        val chatViewModel: ChatViewModel = hiltViewModel(parentEntry)
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
                navController.navigate(Screen.ChatProfile.createRoute(userId, isGroup)) 
            },
            onAvatarClick = { userId ->
                if (chat.avatarUrl != null) {
                    navController.navigate(Screen.MediaViewer.createRoute(chat.avatarUrl, "image", 0L))
                } else {
                    navController.navigate(Screen.ChatProfile.createRoute(userId, chat.isGroup))
                }
            }
        )
    }

    composable(Screen.ArchivedChats.route) { backStackEntry ->
        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("app_root") }
        val chatViewModel: ChatViewModel = hiltViewModel(parentEntry)
        val channelsViewModel: ChannelsViewModel = hiltViewModel(parentEntry)
        ArchivedChatsScreen(
            onBack = { navController.popBackStack() },
            onChatClick = { chat ->
                when (chat.type) {
                    ConversationType.CHANNEL ->
                        navController.navigate(Screen.ChannelDetail.createRoute(chat.id))
                    ConversationType.GROUP ->
                        navController.navigate(Screen.GroupChat.createRoute(chat.id))
                    ConversationType.JOB_CHAT ->
                        navController.navigate(Screen.Chat.createRoute(chat.id, type = "job_chat"))
                    else ->
                        navController.navigate(Screen.Chat.createRoute(chat.id, type = "direct"))
                }
            },
            viewModel = chatViewModel
        )
    }

    composable(Screen.StarredMessages.route) { backStackEntry ->
        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("app_root") }
        val chatViewModel: ChatViewModel = hiltViewModel(parentEntry)
        val channelsViewModel: ChannelsViewModel = hiltViewModel(parentEntry)
        val starredMessages by chatViewModel.starredMessages.collectAsState()
        StarredMessagesScreen(
            messagesState = starredMessages,
            onBack = { navController.popBackStack() },
            onMessageClick = { msg ->
                navController.navigate(Screen.Chat.createRoute(msg.conversationId))
            },
            viewModel = hiltViewModel()
        )
    }

    composable(
        route = Screen.ContactPicker.route,
        arguments = kotlin.collections.listOf(navArgument("isGroupMode") { type = NavType.BoolType; defaultValue = false })
    ) { backStackEntry ->
        val isGroupMode = backStackEntry.arguments?.getBoolean("isGroupMode") ?: false
        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("app_root") }
        val chatViewModel: ChatViewModel = hiltViewModel(parentEntry)
        val channelsViewModel: ChannelsViewModel = hiltViewModel(parentEntry)
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
    
    composable(Screen.CreateGroup.route) { backStackEntry ->
        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("app_root") }
        val chatViewModel: ChatViewModel = hiltViewModel(parentEntry)
        val channelsViewModel: ChannelsViewModel = hiltViewModel(parentEntry)
        val createGroupViewModel: CreateGroupViewModel = hiltViewModel()
        
        val uiState by chatViewModel.uiState.collectAsState()
        val selectedContacts = uiState.selectedContactsForGroup
        
        CreateGroupScreen(
            selectedContacts = selectedContacts,
            onBack = { navController.popBackStack() },
            onCreated = { newChat ->
                navController.navigate(Screen.GroupChat.createRoute(newChat.id)) {
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
        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("app_root") }
        val chatViewModel: ChatViewModel = hiltViewModel(parentEntry)
        val channelsViewModel: ChannelsViewModel = hiltViewModel(parentEntry)
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

    composable(
        route = Screen.JoinGroup.route,
        arguments = listOf(navArgument("inviteCode") { type = NavType.StringType })
    ) { backStackEntry ->
        val inviteCode = backStackEntry.arguments?.getString("inviteCode") ?: return@composable
        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("app_root") }
        val chatViewModel: ChatViewModel = hiltViewModel(parentEntry)
        val context = LocalContext.current
        LaunchedEffect(inviteCode) {
            chatViewModel.joinGroupByInvite(inviteCode) { groupId ->
                navController.navigate(Screen.GroupChat.createRoute(groupId)) {
                    popUpTo(Screen.Main.route)
                }
            }
        }
        val joinError = chatViewModel.uiState.collectAsState().value.error
        LaunchedEffect(joinError) {
            if (!joinError.isNullOrBlank()) {
                Toast.makeText(context, joinError, Toast.LENGTH_LONG).show()
            }
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    composable(
        route = Screen.GroupDetail.route,
        arguments = listOf(navArgument("groupId") { type = NavType.StringType })
    ) { backStackEntry ->
        val groupId = android.net.Uri.decode(backStackEntry.arguments?.getString("groupId") ?: return@composable)
        val viewModel: GroupDetailViewModel = hiltViewModel()
        val chatState by viewModel.chat.collectAsState()
        val groupInfo by viewModel.groupInfo.collectAsState()
        val contacts by viewModel.contacts.collectAsState()
        val sharedMedia by viewModel.sharedMedia.collectAsState()
        val inviteCode by viewModel.inviteCode.collectAsState()
        val toast by viewModel.toast.collectAsState()
        val context = LocalContext.current

        LaunchedEffect(groupId) { viewModel.load(groupId) }
        LaunchedEffect(toast) {
            toast?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                viewModel.clearToast()
            }
        }

        val chat = chatState.data ?: ChatMessage(
            id = groupId,
            type = ConversationType.GROUP,
            senderName = groupInfo.data?.title ?: groupInfo.data?.name ?: "Group",
            isGroup = true,
            createdBy = groupInfo.data?.created_by,
            isAdmin = groupInfo.data?.admin_ids?.contains(viewModel.currentUserId) == true
        )

        GroupDetailScreen(
            chat = chat,
            groupInfo = groupInfo,
            contacts = contacts,
            sharedMedia = sharedMedia,
            currentUserId = viewModel.currentUserId,
            inviteCode = inviteCode ?: groupInfo.data?.invite_code,
            onBack = { navController.popBackStack() },
            onOpenChat = {
                val conversationId = groupInfo.data?.conversation_id?.takeIf { it.isNotBlank() } ?: chat.id
                navController.navigate(Screen.GroupChat.createRoute(conversationId))
            },
            onMute = { muted -> viewModel.mute(groupId, muted) },
            onAddMember = { userId -> viewModel.addMember(groupId, userId) },
            onRemoveMember = { userId -> viewModel.removeMember(groupId, userId) },
            onPromoteAdmin = { userId -> viewModel.promoteAdmin(groupId, userId) },
            onMessageMember = { userId ->
                navController.navigate(Screen.Chat.createRoute(userId, type = "initiate"))
            },
            onLeaveGroup = {
                viewModel.leave(groupId) {
                    if (!navController.popBackStack(Screen.Main.createRoute("chat"), false)) {
                        navController.popBackStack()
                    }
                }
            },
            onGenerateInvite = { viewModel.generateInvite(groupId) },
            onSharedMedia = { navController.navigate(Screen.SharedMedia.createRoute(groupId)) },
            onMediaClick = { url, type ->
                navController.navigate(Screen.MediaViewer.createRoute(url, type))
            }
        )
    }

    composable(
        route = Screen.GroupChat.route,
        arguments = listOf(navArgument("groupId") { type = NavType.StringType })
    ) { backStackEntry ->
        val groupId = android.net.Uri.decode(backStackEntry.arguments?.getString("groupId") ?: return@composable)
        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("app_root") }
        val chatViewModel: ChatViewModel = hiltViewModel(parentEntry)
        val chatsState by chatViewModel.chats.collectAsState()
        val chats = chatsState.data ?: emptyList()
        val existingChat = chats.firstOrNull { it.id == groupId }

        LaunchedEffect(groupId) {
            chatViewModel.fetchMessages(groupId)
        }

        val chat = (existingChat ?: ChatMessage(
            id = groupId,
            type = ConversationType.GROUP,
            senderName = chatViewModel.getCachedGroupName(groupId) ?: "Group",
            isGroup = true
        )).copy(type = ConversationType.GROUP, isGroup = true)

        GroupChatScreen(
            chat = chat,
            chatViewModel = chatViewModel,
            onBack = { navController.popBackStack() },
            onOpenGroupInfo = { navController.navigate(Screen.GroupDetail.createRoute(groupId)) }
        )
    }

    composable(
        route = Screen.ChatProfile.route,
        arguments = listOf(
            navArgument("userId") { type = NavType.StringType },
            navArgument("isGroup") { type = NavType.BoolType; defaultValue = false }
        )
    ) { backStackEntry ->
        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
        val isGroup = backStackEntry.arguments?.getBoolean("isGroup") ?: false
        if (isGroup) {
            LaunchedEffect(userId) {
                navController.navigate(Screen.GroupDetail.createRoute(userId)) {
                    popUpTo(backStackEntry.destination.id) { inclusive = true }
                }
            }
            return@composable
        }
        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("app_root") }
        val chatViewModel: ChatViewModel = hiltViewModel(parentEntry)
        val channelsViewModel: ChannelsViewModel = hiltViewModel(parentEntry)
        
        val channelsState by channelsViewModel.channels.collectAsState()
        val channelFromRepo = channelsState.data?.find { it.id == userId || it.conversationId == userId }
        
        val chatsState by chatViewModel.chats.collectAsState()
        val chats = chatsState.data ?: emptyList()
        
        val chat = chats.find { it.id == userId || it.otherUserId == userId } ?: ChatMessage(
            id = userId,
            type = when {
                channelFromRepo != null -> ConversationType.CHANNEL
                isGroup -> ConversationType.GROUP
                else -> ConversationType.DIRECT
            },
            senderName = channelFromRepo?.name ?: GlobalCallState.getContactName(userId) ?: GlobalCallState.getContactPhone(userId) ?: "",
            isGroup = isGroup || channelFromRepo != null,
            createdBy = channelFromRepo?.createdBy,
            followers = channelFromRepo?.followers ?: "0",
            isAdmin = channelFromRepo?.isAdmin ?: false,
            adminsCanPost = channelFromRepo?.adminsCanPost ?: false,
            otherUserId = if (isGroup || channelFromRepo != null) null else userId
        )
        
        val sharedMediaResource by chatViewModel.getSharedMedia(chat.id).collectAsState(Resource.Loading<List<Message>>())
        val sharedMedia: List<Message> = sharedMediaResource.data ?: emptyList()
        val groupInfo by chatViewModel.uiState.collectAsState()
        val contactsState by chatViewModel.contacts.collectAsState()
        val contacts = contactsState.data ?: emptyList()
        val contactUser by chatViewModel.contactUser.collectAsState()
        val blockedUserIds by chatViewModel.blockedUserIds.collectAsState()
        val otherUserId = chat.otherUserId ?: if (chat.type == ConversationType.DIRECT) userId else null
        val isDirectProfile = chat.type == ConversationType.DIRECT && !isGroup && channelFromRepo == null

        LaunchedEffect(userId, isGroup, otherUserId) {
            if (isGroup) {
                chatViewModel.fetchGroupInfo(userId)
            }
            if (isDirectProfile && !otherUserId.isNullOrBlank()) {
                chatViewModel.loadContactUser(otherUserId)
                chatViewModel.refreshBlockedUsers()
            }
        }

        val profileChat = chat.copy(
            phoneNumber = chat.phoneNumber ?: contactUser?.phoneNumber,
            avatarUrl = chat.avatarUrl ?: contactUser?.avatarUrl,
            senderName = chat.senderName.takeIf { it.isNotBlank() } ?: contactUser?.displayName.orEmpty()
        )

        com.example.firstapp.chat.ui.screens.ChatProfileScreen(
            chat = profileChat,
            onBack = { navController.popBackStack() },
            onMediaClick = { url, type ->
                navController.navigate(Screen.MediaViewer.createRoute(url, type))
            },
            onNavigateToSharedMedia = { chatId ->
                navController.navigate(Screen.SharedMedia.createRoute(chatId))
            },
            sharedMedia = sharedMedia,
            groupInfo = if (isGroup) groupInfo.groupInfo else Resource.Loading(),
            contacts = contacts,
            contactBio = contactUser?.bio,
            contactUsername = contactUser?.username,
            isBlocked = otherUserId != null && blockedUserIds.contains(otherUserId),
            onClearChat = { chatViewModel.clearChat(chat.id) },
            onDeleteChat = { chatViewModel.deleteChat(chat.id); navController.popBackStack() },
            onMuteToggle = { chatViewModel.muteChat(chat.id, it) },
            onPinToggle = { chatViewModel.pinChat(chat.id, it) },
            onAddMember = { memberId -> chatViewModel.addGroupMember(chat.id, memberId) },
            onRemoveMember = { memberId -> chatViewModel.removeGroupMember(chat.id, memberId) },
            onPromoteAdmin = { memberId -> chatViewModel.promoteToAdmin(chat.id, memberId) },
            onLeaveGroup = { 
                chatViewModel.leaveGroup(chat.id) {
                    if (!navController.popBackStack(Screen.Main.createRoute("chat"), false)) {
                        navController.popBackStack()
                    }
                }
            },
            onToggleAdminPosting = { allowed ->
                val realChannelId = channelFromRepo?.id ?: chat.id
                channelsViewModel.toggleAdminPosting(realChannelId, allowed)
            },
            onSubscribe = { id ->
                val realChannelId = channelFromRepo?.id ?: id
                channelsViewModel.subscribeToChannel(realChannelId)
            },
            onAvatarSelected = { uri ->
                chatViewModel.updateChatAvatar(chat.id, uri)
            },
            onBlock = {
                otherUserId?.let { target ->
                    chatViewModel.blockUser(target) { ok ->
                        if (ok) {
                            if (!navController.popBackStack(Screen.Main.createRoute("chat"), false)) {
                                navController.popBackStack()
                            }
                        }
                    }
                }
            },
            onUnblock = {
                otherUserId?.let { chatViewModel.unblockUser(it) }
            },
            onReport = { reason ->
                otherUserId?.let { chatViewModel.reportUser(it, reason) }
            },
            onDisappearingChange = { duration ->
                chatViewModel.updateDisappearingMessages(chat.id, duration)
            },
            onMediaVisibilityChange = { visible ->
                chatViewModel.updateMediaVisibility(chat.id, visible)
            }
        )
    }

    composable(
        route = Screen.MessageInfo.route,
        arguments = listOf(navArgument("messageId") { type = NavType.StringType })
    ) { backStackEntry ->
        val messageId = backStackEntry.arguments?.getString("messageId") ?: return@composable
        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("app_root") }
        val chatViewModel: ChatViewModel = hiltViewModel(parentEntry)
        
        MessageInfoScreen(
            messageId = messageId,
            viewModel = chatViewModel,
            onBack = { navController.popBackStack() }
        )
    }
}
