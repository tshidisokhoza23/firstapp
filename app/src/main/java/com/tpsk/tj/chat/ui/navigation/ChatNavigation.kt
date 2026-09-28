package com.tpsk.tj.chat.ui.navigation

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
import com.tpsk.tj.chat.model.*
import com.tpsk.tj.chat.ui.screens.*
import com.tpsk.tj.stories.ui.*
import com.tpsk.tj.channels.ui.*
import com.tpsk.tj.groups.ui.*
import com.tpsk.tj.direct.ui.*
import com.tpsk.tj.chat.ui.viewmodel.*
import com.tpsk.tj.chat.data.GlobalCallState
import com.tpsk.tj.media.ui.screens.SharedMediaScreen
import com.tpsk.tj.media.ui.viewmodel.SharedMediaViewModel
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel
import com.tpsk.tj.common.ui.theme.BoyishPurple
import com.tpsk.tj.common.util.isIdString
import com.tpsk.tj.common.util.sanitizeDisplayName
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.ui.navigation.Screen
import com.tpsk.tj.common.model.ConversationType
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch

fun NavGraphBuilder.chatsNav(navController: NavHostController) {
    composable(
        route = Screen.Story.route,
        arguments = listOf(
            navArgument("userId") { type = NavType.StringType; defaultValue = "" },
            navArgument("showComposer") { type = NavType.BoolType; defaultValue = false },
            navArgument("play") { type = NavType.BoolType; defaultValue = true },
            navArgument("storyId") { type = NavType.StringType; defaultValue = "" }
        )
    ) { backStackEntry ->
        val initialUserId = backStackEntry.arguments?.getString("userId") ?: ""
        val showComposer = backStackEntry.arguments?.getBoolean("showComposer") ?: false
        val play = backStackEntry.arguments?.getBoolean("play") ?: true
        val initialStoryId = backStackEntry.arguments?.getString("storyId") ?: ""
        val storyViewModel: StoryViewModel = hiltViewModel()
        val mediaViewModel: MediaViewModel = hiltViewModel()
        val adManager = (navController.context as? com.tpsk.tj.MainActivity)?.adManager 
            ?: com.tpsk.tj.common.data.ads.AdMobManagerImpl()

        val storiesState = storyViewModel.stories.collectAsState(Resource.Loading()).value
        val contactsState = storyViewModel.contacts.collectAsState(Resource.Loading()).value
        val currentUser = com.tpsk.tj.LocalCurrentUser.current
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

        val chatViewModel: ChatViewModel = hiltViewModel()
        val scope = rememberCoroutineScope()

        StoryScreen(
            stories = storiesState.data ?: emptyList(),
            contacts = contactsState.data ?: emptyList(),
            currentUserId = currentUser?.id,
            mediaViewModel = mediaViewModel,
            adManager = adManager,
            initialViewingUserId = if (initialUserId.isNotBlank()) initialUserId else null,
            initialStoryId = initialStoryId.takeIf { it.isNotBlank() },
            storiesReady = storiesState is Resource.Success,
            initialShowComposer = showComposer,
            initialPlay = play,
            onBack = { navController.popBackStack() },
            onPostStory = { content, url, type ->
                navController.navigate(Screen.ShareStory.createRoute(content, url ?: "", type))
            },
            onDeleteStory = { storyId -> storyViewModel.deleteStory(storyId) },
            onStoryViewed = { userId, storyId -> storyViewModel.markStoryAsViewed(userId, storyId) },
            onMuteStory = { userId, isMuted -> storyViewModel.muteStory(userId, isMuted) },
            onNavigateToNewStory = { navController.navigate(Screen.NewStory.route) },
            onGetStoryViewers = { storyId -> storyViewModel.getStoryViewers(storyId) },
            onReplyStory = { userId, storyId, text, storySummary, authorName ->
                if (userId.isNotBlank()) {
                    val targetUser = userId.removePrefix("user_")
                    val chats = chatViewModel.chats.value.data.orEmpty()
                    val existingChat = chats.find {
                        it.otherUserId == targetUser || it.id == targetUser
                    }
                    val replyId = com.tpsk.tj.chat.model.MessageMarkers.storyReplyId(storyId, targetUser)
                    val replyText = "[Story] $storySummary"

                    if (existingChat != null) {
                        chatViewModel.sendMessage(
                            chatId = existingChat.id,
                            body = text,
                            messageType = "text",
                            replyToId = replyId,
                            replyToText = replyText,
                            replyToSenderName = authorName
                        )
                    } else {
                        scope.launch {
                            chatViewModel.initiateDM(targetUser).collect { res ->
                                if (res is com.tpsk.tj.common.util.Resource.Success && !res.data.isNullOrBlank()) {
                                    chatViewModel.sendMessage(
                                        chatId = res.data,
                                        body = text,
                                        messageType = "text",
                                        replyToId = replyId,
                                        replyToText = replyText,
                                        replyToSenderName = authorName
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
        val currentUser = com.tpsk.tj.LocalCurrentUser.current

        LaunchedEffect(postResult) {
            when (val res = postResult) {
                is com.tpsk.tj.common.util.Resource.Success -> {
                    android.widget.Toast.makeText(context, "Story posted!", android.widget.Toast.LENGTH_SHORT).show()
                    storyViewModel.clearPostResult()
                    val popped = navController.popBackStack(Screen.NewStory.route, true)
                    if (!popped) {
                        navController.popBackStack(Screen.ShareStory.route, true)
                    }
                    navController.navigate(Screen.Story.createRoute(userId = currentUser?.id, play = true))
                }
                is com.tpsk.tj.common.util.Resource.Error -> {
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
            isPosting = postResult is com.tpsk.tj.common.util.Resource.Loading,
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
        val chatViewModel: ChatViewModel = hiltViewModel()
        val channelState by viewModel.channel.collectAsState()
        val messagesState by viewModel.messages.collectAsState()
        val contacts by viewModel.contacts.collectAsState()
        val members by viewModel.members.collectAsState()
        val toast by viewModel.toast.collectAsState()
        val currentUser = com.tpsk.tj.LocalCurrentUser.current
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
                                viewModel.sendMessage(channelId, body, emptyList())
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
                        posts = emptyList(),
                        messages = messagesState,
                        currentUserId = viewModel.currentUserId ?: currentUser?.id,
                        contacts = contacts,
                        members = members,
                        onBack = { navController.popBackStack() },
                        onSubscribe = { viewModel.subscribe(it) },
                        onMute = { id, muted -> viewModel.mute(id, muted) },
                        onCreatePost = { showComposer = true },
                        onSendPost = { text, media -> viewModel.sendMediaPost(context, channel.id, text, media) },
                        onAddAdmin = { userId -> viewModel.addAdmin(channel.id, userId) },
                        onRemoveAdmin = { userId -> viewModel.removeAdmin(channel.id, userId) },
                        onToggleAdminPosting = { viewModel.toggleAdminPosting(channelId, it) },
                        onReportChannel = { id, reason -> chatViewModel.reportUser(id, reason) },
                        onOpenDiscussion = {
                            Toast.makeText(context, "Channels use posts, not a 1:1 chat.", Toast.LENGTH_SHORT).show()
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
            navArgument("companyName") { type = NavType.StringType; nullable = true; defaultValue = "" },
            navArgument("displayName") { type = NavType.StringType; nullable = true; defaultValue = "" }
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
        val displayNameNavParam = backStackEntry.arguments?.getString("displayName")?.takeIf { it.isNotBlank() }?.let { android.net.Uri.decode(it) }
        
        val context = androidx.compose.ui.platform.LocalContext.current
        val type = when (typeStr.trim().lowercase()) {
            "group" -> ConversationType.GROUP
            "channel", "hiring_channel" -> ConversationType.CHANNEL
            "job_chat", "job_application" -> ConversationType.JOB_CHAT
            "mingle" -> ConversationType.MINGLE
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
                CircularProgressIndicator(color = com.tpsk.tj.common.ui.theme.AppAccent)
            }
            return@composable
        }

        val chatsState by chatViewModel.chats.collectAsState()
        val chats = chatsState.data ?: emptyList<ChatMessage>()
        val existingChat = chats.firstOrNull { it.id == resolvedChatId && it.type == type }
            ?: chats.firstOrNull { it.id == resolvedChatId }
        
        val channelsState by channelsViewModel.channels.collectAsState()
        val channelFromRepo = channelsState.data?.find { it.id == resolvedChatId || it.conversationId == resolvedChatId }

        // Use distinct conversationId for channels and Mingle secret chats
        val effectiveChatId = when (type) {
            ConversationType.CHANNEL -> channelFromRepo?.conversationId ?: resolvedChatId!!
            ConversationType.MINGLE -> resolvedChatId!!
            else -> resolvedChatId!!
        }

        val realUserId = existingChat?.otherUserId ?: targetUserId ?: effectiveChatId.removePrefix("mingle_")

        // Ensure Mingle secret chats fetch their distinct conversation messages
        LaunchedEffect(effectiveChatId) {
            chatViewModel.fetchMessages(effectiveChatId)
        }

        val baseName = if (type == ConversationType.GROUP) {
            existingChat?.senderName ?: chatViewModel.getCachedGroupName(effectiveChatId) ?: ""
        } else if (type == ConversationType.CHANNEL) {
            existingChat?.senderName ?: channelFromRepo?.name ?: ""
        } else if (type == ConversationType.MINGLE) {
            existingChat?.senderName?.takeIf { it.isNotBlank() && !it.startsWith("mingle_") }
                ?: displayNameNavParam
                ?: "Mingle User"
        } else {
            // Priority: Local DB Name > Contact Cache Name > Phone Number
            existingChat?.senderName?.takeIf { it.isNotBlank() && !it.startsWith("mingle_") }
                ?: displayNameNavParam
                ?: GlobalCallState.getContactName(realUserId)
                ?: GlobalCallState.getContactPhone(realUserId) 
                ?: existingChat?.phoneNumber
                ?: ""
        }
        
        val sanitizedName = sanitizeDisplayName(
            baseName,
            realUserId,
            GlobalCallState.getContactPhone(realUserId) ?: existingChat?.phoneNumber
        ).ifEmpty {
            if (type == ConversationType.GROUP) "Group" else if (type == ConversationType.CHANNEL) "Channel" else existingChat?.phoneNumber.orEmpty()
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
            otherUserId = realUserId,
            isMingle = type == ConversationType.MINGLE
        )).copy(
            type = type,
            isMingle = type == ConversationType.MINGLE,
            isAdmin = existingChat?.isAdmin ?: channelFromRepo?.isAdmin ?: false,
            isSubscribed = existingChat?.isSubscribed ?: channelFromRepo?.isSubscribed ?: false,
            adminsCanPost = existingChat?.adminsCanPost ?: channelFromRepo?.adminsCanPost ?: false,
            createdBy = existingChat?.createdBy ?: channelFromRepo?.createdBy
        )

        val isSocialDm = typeStr.trim().lowercase() == "dm" || typeStr.trim().lowercase() == "media" || typeStr.trim().lowercase() == "social"

        ChatMessagesScreen(
            chat = chat,
            chatViewModel = chatViewModel,
            onBack = { navController.popBackStack() },
            onShowProfile = { userId, isGroup ->
                when {
                    type == ConversationType.MINGLE -> android.widget.Toast.makeText(context, "🔒 Secret Mingle Chat - Profiles and contacts are kept private.", android.widget.Toast.LENGTH_SHORT).show()
                    type == ConversationType.GROUP || isGroup -> navController.navigate(Screen.GroupDetail.createRoute(userId))
                    type == ConversationType.CHANNEL -> navController.navigate(Screen.ChannelDetail.createRoute(channelFromRepo?.id ?: userId))
                    isSocialDm -> navController.navigate(Screen.UserProfile.createRoute(userId))
                    else -> navController.navigate(Screen.ChatProfile.createRoute(userId, false))
                }
            },
            onAvatarClick = { userId ->
                when {
                    type == ConversationType.MINGLE -> android.widget.Toast.makeText(context, "🔒 Secret Mingle Chat - Profiles and contacts are kept private.", android.widget.Toast.LENGTH_SHORT).show()
                    chat.avatarUrl != null -> navController.navigate(Screen.MediaViewer.createRoute(chat.avatarUrl, "image", 0L))
                    type == ConversationType.GROUP || chat.isGroup -> navController.navigate(Screen.GroupDetail.createRoute(userId))
                    type == ConversationType.CHANNEL -> navController.navigate(Screen.ChannelDetail.createRoute(channelFromRepo?.id ?: userId))
                    isSocialDm -> navController.navigate(Screen.UserProfile.createRoute(userId))
                    else -> navController.navigate(Screen.ChatProfile.createRoute(userId, false))
                }
            },
            onOpenStory = { storyUserId, storyId ->
                navController.navigate(Screen.Story.createRoute(userId = storyUserId, play = true, storyId = storyId))
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
            senderName = sanitizeDisplayName(GlobalCallState.getContactName(chatId), chatId, GlobalCallState.getContactPhone(chatId) ?: existingChat?.phoneNumber),
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
                if (isGroup) {
                    navController.navigate(Screen.GroupDetail.createRoute(userId))
                } else {
                    navController.navigate(Screen.ChatProfile.createRoute(userId, false))
                }
            },
            onAvatarClick = { userId ->
                if (chat.avatarUrl != null) {
                    navController.navigate(Screen.MediaViewer.createRoute(chat.avatarUrl, "image", 0L))
                } else if (chat.isGroup) {
                    navController.navigate(Screen.GroupDetail.createRoute(userId))
                } else {
                    navController.navigate(Screen.ChatProfile.createRoute(userId, false))
                }
            },
            onOpenStory = { storyUserId, storyId ->
                navController.navigate(Screen.Story.createRoute(userId = storyUserId, play = true, storyId = storyId))
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
                val chats = chatViewModel.chats.value.data.orEmpty()
                val chat = chats.firstOrNull { it.id == msg.conversationId }
                when (chat?.type) {
                    ConversationType.CHANNEL ->
                        navController.navigate(Screen.ChannelDetail.createRoute(chat.id))
                    ConversationType.GROUP ->
                        navController.navigate(Screen.GroupChat.createRoute(chat.id))
                    ConversationType.JOB_CHAT ->
                        navController.navigate(Screen.Chat.createRoute(msg.conversationId, type = "job_chat"))
                    else ->
                        navController.navigate(Screen.Chat.createRoute(msg.conversationId, type = "direct"))
                }
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
            onInviteClicked = {
                val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        android.content.Intent.EXTRA_TEXT,
                        "Join me on FirstApp: https://firstapp.app/invite"
                    )
                }
                navController.context.startActivity(android.content.Intent.createChooser(share, "Invite friends"))
            },
            onGroupCreate = { contacts ->
                chatViewModel.updateSelectedContacts(contacts)
                navController.navigate(Screen.CreateGroup.route)
            }
        )
    }

    composable(Screen.DirectMessages.route) { backStackEntry ->
        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("app_root") }
        val directViewModel: DirectMessagesViewModel = hiltViewModel(parentEntry)
        val chatViewModel: ChatViewModel = hiltViewModel(parentEntry)
        DirectMessagesScreen(
            onBack = { navController.popBackStack() },
            onChatSelect = { chat ->
                navController.navigate(Screen.Chat.createRoute(chat.id, type = "direct"))
            },
            onCallSelect = { call ->
            chatViewModel.startVoiceOrVideoCall(
                receiverId = call.contactId ?: call.calleeId ?: call.callerId ?: "",
                callType = com.tpsk.tj.chat.data.CallType.VOICE,
                name = call.contactName ?: "Call"
            )
        },
            onNewDM = {
                navController.navigate(Screen.ContactPicker.route)
            },
            viewModel = directViewModel
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
        val parentEntry = remember(backStackEntry) { navController.getBackStackEntry("app_root") }
        val chatViewModel: ChatViewModel = hiltViewModel(parentEntry)
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
            onDemoteAdmin = { userId -> viewModel.demoteAdmin(groupId, userId) },
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
            onRename = { name -> viewModel.rename(groupId, name) },
            onChangeAvatar = { uri ->
                val conversationId = groupInfo.data?.conversation_id?.takeIf { it.isNotBlank() } ?: groupId
                chatViewModel.updateChatAvatar(conversationId, uri)
            },
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
            chatViewModel.fetchGroupInfo(groupId)
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
        
        val chat = chats.find {
            it.id == userId || it.otherUserId == userId ||
                (!it.phoneNumber.isNullOrBlank() && it.phoneNumber == userId)
        } ?: ChatMessage(
            id = userId,
            type = when {
                channelFromRepo != null -> ConversationType.CHANNEL
                isGroup -> ConversationType.GROUP
                else -> ConversationType.DIRECT
            },
            senderName = channelFromRepo?.name ?: sanitizeDisplayName(GlobalCallState.getContactName(userId), userId, GlobalCallState.getContactPhone(userId)),
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

        val matchedContact = contacts.firstOrNull { contact ->
            contact.id == userId || contact.id == chat.otherUserId ||
                (!contact.phoneNumber.isNullOrBlank() && contact.phoneNumber == chat.phoneNumber)
        }
        val listName = chat.consistentName().takeIf { it.isNotBlank() && !com.tpsk.tj.common.util.looksLikePhone(it) }
            ?: matchedContact?.displayName?.takeIf { it.isNotBlank() && !com.tpsk.tj.common.util.looksLikePhone(it) && !com.tpsk.tj.common.util.isIdString(it, userId) }
            ?: chat.consistentName()
        val profileChat = chat.copy(
            phoneNumber = chat.phoneNumber ?: contactUser?.phoneNumber,
            avatarUrl = chat.avatarUrl ?: contactUser?.avatarUrl,
            localContactName = listName.takeIf { it.isNotBlank() },
            senderName = listName.ifBlank { chat.senderName }
        )

        com.tpsk.tj.chat.ui.screens.ChatProfileScreen(
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
            onDemoteAdmin = { memberId -> chatViewModel.demoteAdmin(chat.id, memberId) },
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
                val targetUser = otherUserId ?: chat.otherUserId ?: userId
                chatViewModel.reportUser(targetUser, reason)
            },
            onInitiateCall = { targetId, name, type ->
                chatViewModel.startVoiceOrVideoCall(targetId, type, name)
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
