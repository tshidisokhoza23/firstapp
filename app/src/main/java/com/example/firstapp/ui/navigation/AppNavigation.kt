package com.example.firstapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.example.firstapp.ui.screens.*
import com.example.firstapp.ui.screens.more.*
import com.example.firstapp.ui.viewmodel.MainViewModel
import com.example.firstapp.ui.viewmodel.MusicViewModel
import com.example.firstapp.model.ChatMessage
import com.example.firstapp.util.Resource
import androidx.compose.ui.graphics.Color

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main")
    object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: Int) = "chat/$chatId"
    }
    object Vocab : Screen("vocab")
    object Files : Screen("files")
    object Calendar : Screen("calendar")
    object Story : Screen("story")
    object Settings : Screen("settings")
    object EditProfile : Screen("edit_profile")
    object ContactPicker : Screen("contact_picker")
    object CreateGroup : Screen("create_group")
    object JobProfile : Screen("job_profile")
    object Applications : Screen("applications")
    object DirectMessages : Screen("direct_messages")
    object SharedMedia : Screen("shared_media/{chatId}") {
        fun createRoute(chatId: Int) = "shared_media/$chatId"
    }
    object JobDetail : Screen("job_detail/{jobId}") {
        fun createRoute(jobId: Int) = "job_detail/$jobId"
    }
    object ChannelDetail : Screen("channel_detail/{channelId}") {
        fun createRoute(channelId: Int) = "channel_detail/$channelId"
    }
    object AdvertDetail : Screen("advert_detail/{advertId}") {
        fun createRoute(advertId: Int) = "advert_detail/$advertId"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    viewModel: MainViewModel,
    musicViewModel: MusicViewModel,
    onLogout: () -> Unit
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Screen.Main.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            })
        }
        
        composable(Screen.Main.route) {
            MainScreen(
                onLogout = onLogout,
                viewModel = viewModel,
                musicViewModel = musicViewModel,
                navController = navController
            )
        }
        
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.IntType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getInt("chatId") ?: return@composable
            val chats = (viewModel.chats.value as? Resource.Success)?.data ?: emptyList()
            val chat = chats.find { it.id == chatId } ?: ChatMessage(id = chatId, senderName = "User $chatId", lastMessage = "", time = "", avatarColor = com.example.firstapp.ui.theme.BoyishPurple)
            
            LaunchedEffect(chatId) {
                viewModel.fetchMessages(chatId)
            }

            val messagesState = viewModel.messages.collectAsState().value
            
            ChatScreen(
                chat = chat,
                viewModel = viewModel,
                messagesState = messagesState,
                onBack = { navController.popBackStack() },
                onShowSharedMedia = { navController.navigate(Screen.SharedMedia.createRoute(chatId)) },
                onLoadMore = { viewModel.loadMoreMessages(chatId, it) },
                onReact = { msgId, emoji -> viewModel.reactToMessage(msgId, emoji) },
                onTyping = { contactId, isTyping -> viewModel.updateChatTyping(contactId, isTyping) },
                onPresence = { contactId, isOnline, lastSeen -> viewModel.updateChatPresence(contactId, isOnline, lastSeen) },
                onNewMessage = { viewModel.onNewMessage(it) }
            )
        }
        
        composable(Screen.Vocab.route) {
            VocabScreen(onBack = { navController.popBackStack() })
        }
        
        composable(Screen.Files.route) {
            FilesScreen(onBack = { navController.popBackStack() })
        }
        
        composable(Screen.Calendar.route) {
            CalendarScreen(onBack = { navController.popBackStack() })
        }
        
        composable(Screen.Story.route) {
            val storiesState = viewModel.stories.collectAsState(Resource.Loading()).value
            StoryScreen(
                stories = (storiesState as? Resource.Success)?.data ?: emptyList(),
                onBack = { navController.popBackStack() },
                onPostStory = { content, url -> viewModel.postStory(content, url) },
                onStoryViewed = { userId, storyId -> viewModel.markStoryAsViewed(userId, storyId) },
                onMuteStory = { userId, isMuted -> viewModel.muteStory(userId, isMuted) }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                user = viewModel.currentUser.collectAsState().value,
                onBack = { navController.popBackStack() },
                onEditProfile = { navController.navigate(Screen.EditProfile.route) },
                onLogout = onLogout,
                onDeleteAccount = { viewModel.deleteAccount(onLogout) }
            )
        }
        
        composable(Screen.EditProfile.route) {
            ProfileView(
                onBack = { navController.popBackStack() },
                posts = (viewModel.posts.collectAsState().value as? Resource.Success)?.data ?: emptyList(),
                viewModel = viewModel
            )
        }
        
        composable(Screen.ContactPicker.route) {
            ContactPickerScreen(
                viewModel = viewModel,
                initialIsGroupMode = false,
                onBack = { navController.popBackStack() },
                onContactSelected = { contact ->
                    navController.navigate(Screen.Chat.createRoute(contact.id)) {
                        popUpTo(Screen.ContactPicker.route) { inclusive = true }
                    }
                    viewModel.createChat(contact.id)
                },
                onInviteClicked = { /* invite logic */ },
                onGroupCreate = { contacts ->
                    viewModel.setSelectedContactsForGroup(contacts)
                    navController.navigate(Screen.CreateGroup.route)
                }
            )
        }
        
        composable(Screen.CreateGroup.route) {
            val selectedContacts = viewModel.selectedContactsForGroup.collectAsState().value
            CreateGroupScreen(
                selectedContacts = selectedContacts,
                onBack = { navController.popBackStack() },
                onCreate = { name, ids ->
                    viewModel.createGroup(name, ids) { chat ->
                        navController.navigate(Screen.Chat.createRoute(chat.id)) {
                            popUpTo(Screen.CreateGroup.route) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        composable(Screen.JobProfile.route) {
            JobProfileScreen(
                initialData = null,
                onClose = { navController.popBackStack() },
                onSubmit = { 
                    viewModel.updateJobProfile(it)
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Applications.route) {
            JobApplicationsScreen(
                applications = viewModel.applications.collectAsState().value,
                onBack = { navController.popBackStack() },
                onWithdrawApplication = { viewModel.withdrawApplication(it) },
                onViewJobDetails = { /* view job */ },
                onContactRecruiter = { /* chat with recruiter */ },
                onSetReminder = { /* set reminder */ }
            )
        }
        
        composable(Screen.DirectMessages.route) {
            DirectMessagesScreen(
                chats = (viewModel.chats.collectAsState().value as? Resource.Success)?.data ?: emptyList(),
                calls = (viewModel.callLogs.collectAsState().value as? Resource.Success)?.data ?: emptyList(),
                onBack = { navController.popBackStack() },
                onChatSelect = { chat -> navController.navigate(Screen.Chat.createRoute(chat.id)) },
                onCallSelect = { call -> 
                    navController.navigate(Screen.Chat.createRoute(call.contactId))
                    // Start call logic could be triggered here if we had a way to signal ChatScreen
                },
                onNewDM = { navController.navigate(Screen.ContactPicker.route) },
                onPinChat = { id, pin -> viewModel.pinChat(id, pin) },
                onMuteChat = { id, mute -> viewModel.muteChat(id, mute) },
                onDeleteChat = { viewModel.deleteChat(it) }
            )
        }
        
        composable(
            route = Screen.JobDetail.route,
            arguments = listOf(navArgument("jobId") { type = NavType.IntType })
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getInt("jobId") ?: return@composable
            val jobs = (viewModel.jobs.collectAsState().value as? Resource.Success)?.data ?: emptyList()
            val job = jobs.find { it.id == jobId } ?: return@composable
            JobDetailScreen(
                job = job,
                onBack = { navController.popBackStack() },
                onApply = { 
                    viewModel.applyForJob(job.id)
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.ChannelDetail.route,
            arguments = listOf(navArgument("channelId") { type = NavType.IntType })
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getInt("channelId") ?: return@composable
            val channels = (viewModel.channels.collectAsState().value as? Resource.Success)?.data ?: emptyList()
            val channel = channels.find { it.id == channelId } ?: return@composable
            ChannelDetailScreen(
                channel = channel,
                onBack = { navController.popBackStack() },
                onSubscribe = { viewModel.subscribeToChannel(it) }
            )
        }
        
        composable(
            route = Screen.AdvertDetail.route,
            arguments = listOf(navArgument("advertId") { type = NavType.IntType })
        ) { backStackEntry ->
            val advertId = backStackEntry.arguments?.getInt("advertId") ?: return@composable
            val adverts = (viewModel.adverts.collectAsState().value as? Resource.Success)?.data ?: emptyList()
            val advert = adverts.find { it.id == advertId } ?: return@composable
            AdvertDetailScreen(
                advert = advert,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.SharedMedia.route,
            arguments = listOf(navArgument("chatId") { type = NavType.IntType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getInt("chatId") ?: return@composable
            val chat = ((viewModel.chats.value as? Resource.Success)?.data ?: emptyList()).find { it.id == chatId }
                ?: ChatMessage(id = chatId, senderName = "User $chatId", lastMessage = "", time = "", avatarColor = Color.Gray)
            SharedMediaScreen(
                chat = chat,
                messages = (viewModel.messages.collectAsState().value as? Resource.Success)?.data ?: emptyList(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
