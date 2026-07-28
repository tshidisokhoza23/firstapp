package com.example.firstapp.media.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel

import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.common.util.Resource
import com.example.firstapp.chat.model.Contact
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.firstapp.media.model.*
import com.example.firstapp.media.ui.screens.*
import com.example.firstapp.media.ui.viewmodel.*
import com.example.firstapp.ui.navigation.Screen

fun NavGraphBuilder.mediaNavGraph(navController: NavHostController) {
    composable(
        route = Screen.Story.route,
        arguments = listOf(
            navArgument("userId") { type = NavType.StringType; defaultValue = "" },
            navArgument("showComposer") { type = NavType.BoolType; defaultValue = false }
        )
    ) { backStackEntry ->
        val initialUserId = backStackEntry.arguments?.getString("userId") ?: ""
        val showComposer = backStackEntry.arguments?.getBoolean("showComposer") ?: false
        val storyViewModel: StoryViewModel = hiltViewModel()
        val userViewModel: com.example.firstapp.common.ui.viewmodel.UserViewModel = hiltViewModel()
        val mediaViewModel: MediaViewModel = hiltViewModel()
        val adManager = (navController.context as? com.example.firstapp.MainActivity)?.adManager 
            ?: com.example.firstapp.common.data.ads.AdMobManagerImpl()

        val storiesState = storyViewModel.stories.collectAsState(Resource.Loading()).value
        val currentUser by userViewModel.currentUser.collectAsState()
        val postResult by storyViewModel.postResult.collectAsState()
        val context = androidx.compose.ui.platform.LocalContext.current
        
        androidx.compose.runtime.LaunchedEffect(postResult) {
            postResult?.let {
                if (it is Resource.Success) {
                    Toast.makeText(context, "Story update shared", Toast.LENGTH_SHORT).show()
                    storyViewModel.clearPostResult()
                } else if (it is Resource.Error) {
                    Toast.makeText(context, it.message ?: "Failed to share story", Toast.LENGTH_LONG).show()
                    storyViewModel.clearPostResult()
                }
            }
        }

        StoryScreen(
            stories = storiesState.data ?: emptyList(),
            currentUserId = currentUser?.id,
            mediaViewModel = mediaViewModel,
            adManager = adManager,
            initialViewingUserId = if (initialUserId.isNotBlank()) initialUserId else null,
            initialShowComposer = showComposer,
            onBack = { navController.popBackStack() },
            onPostStory = { content, url, type -> storyViewModel.postStory(content, url, type) },
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
        
        ShareStoryScreen(
            content = content,
            url = url,
            type = type,
            onBack = { navController.popBackStack() },
            onPost = { c, u, t ->
                storyViewModel.postStory(c, u, t)
                navController.popBackStack(Screen.Main.route, false)
            }
        )
    }

    composable(
        route = Screen.ChannelDetail.route,
        arguments = listOf(navArgument("channelId") { type = NavType.StringType })
    ) { backStackEntry ->
        val channelDetailViewModel: ChannelDetailViewModel = hiltViewModel()
        val channelId = backStackEntry.arguments?.getString("channelId") ?: return@composable
        val context = androidx.compose.ui.platform.LocalContext.current
        
        androidx.compose.runtime.LaunchedEffect(channelId) { channelDetailViewModel.loadChannel(channelId) }
        
        val channelState by channelDetailViewModel.channel.collectAsState()
        val postsState by channelDetailViewModel.posts.collectAsState()
        
        var showAddAdminDialog by remember { mutableStateOf(false) }
        var showCreatePostDialog by remember { mutableStateOf(false) }

        channelState.data?.let { channel ->
            ChannelDetailScreen(
                channel = channel,
                posts = postsState.data ?: emptyList(),
                onBack = { navController.popBackStack() },
                onSubscribe = { id: String -> channelDetailViewModel.subscribe(id) },
                onMute = { id: String, muted: Boolean -> channelDetailViewModel.mute(id, muted) },
                onCreatePost = { showCreatePostDialog = true },
                onAddAdmin = { showAddAdminDialog = true }
            )
        }

        if (showAddAdminDialog) {
            // Basic Contact Picker Dialog
            val contactsViewModel: com.example.firstapp.chat.ui.viewmodel.ChatViewModel = hiltViewModel()
            val contactsState by contactsViewModel.contacts.collectAsState()
            
            AlertDialog(
                onDismissRequest = { showAddAdminDialog = false },
                title = { Text("Select Admin", color = Color.White) },
                text = {
                    val contacts = (contactsState as? Resource.Success<List<Contact>>)?.data ?: emptyList()
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(contacts) { contact: Contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        channelDetailViewModel.addAdmin(channelId, contact.id)
                                        showAddAdminDialog = false
                                        Toast.makeText(context, "${contact.displayName} added as admin", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val initial = if (contact.displayName.isNotEmpty()) contact.displayName.substring(0, 1) else ""
                                    Text(initial, color = Color.White)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(contact.displayName, color = Color.White)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showAddAdminDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = AppSecondary
            )
        }

        if (showCreatePostDialog) {
            var postContent by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreatePostDialog = false },
                title = { Text("Post to Channel", color = Color.White) },
                text = {
                    TextField(
                        value = postContent,
                        onValueChange = { postContent = it },
                        placeholder = { Text("What's happening?") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (postContent.isNotBlank()) {
                                // socialRepository call via viewmodel
                                showCreatePostDialog = false
                                postContent = ""
                                Toast.makeText(context, "Post shared to channel", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
                    ) {
                        Text("Post", color = AppBackground)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePostDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                },
                containerColor = AppSecondary
            )
        }
    }
    
    composable(
        route = Screen.AdvertDetail.route,
        arguments = listOf(navArgument("advertId") { type = NavType.StringType })
    ) { backStackEntry ->
        val advertDetailViewModel: AdvertDetailViewModel = hiltViewModel()
        val advertId = backStackEntry.arguments?.getString("advertId") ?: return@composable
        androidx.compose.runtime.LaunchedEffect(advertId) { advertDetailViewModel.loadAdvert(advertId) }
        val advertState by advertDetailViewModel.advert.collectAsState()

        advertState.data?.let { advert ->
            AdvertDetailScreen(
                advert = advert,
                onBack = { navController.popBackStack() },
                onImpression = { advertDetailViewModel.loadAdvert(advertId) },
                onActionClick = { advertDetailViewModel.trackClick(advertId) },
                onShare = { advertDetailViewModel.trackShare(advertId) },
                onLike = { /* handle */ }
            )
        }
    }

    composable(route = Screen.MediaMessages.route) {
        MediaMessagesScreen(
            onBack = { navController.popBackStack() },
            onConversationClick = { userId -> 
                navController.navigate(Screen.MediaChat.createRoute(userId))
            }
        )
    }

    composable(
        route = Screen.MediaChat.route,
        arguments = listOf(navArgument("userId") { type = NavType.StringType })
    ) { backStackEntry ->
        val userId = backStackEntry.arguments?.getString("userId") ?: ""
        MediaChatScreen(
            userId = userId,
            onBack = { navController.popBackStack() }
        )
    }

    composable(
        route = Screen.PostDetail.route,
        arguments = listOf(navArgument("postId") { type = NavType.StringType })
    ) { backStackEntry ->
        val postId = backStackEntry.arguments?.getString("postId") ?: ""
        val mediaViewModel: MediaViewModel = hiltViewModel()
        val userViewModel: com.example.firstapp.common.ui.viewmodel.UserViewModel = hiltViewModel()
        val adManager = (navController.context as? com.example.firstapp.MainActivity)?.adManager 
            ?: com.example.firstapp.common.data.ads.AdMobManagerImpl() // Fallback

        val currentUser by userViewModel.currentUser.collectAsState()

        PostDetailScreen(
            postId = postId,
            mediaViewModel = mediaViewModel,
            adManager = adManager,
            ownProfileId = currentUser?.id ?: "",
            onBack = { navController.popBackStack() },
            onProfileClick = { user ->
                navController.navigate(Screen.UserProfile.createRoute(user.id))
            },
            onMediaClick = { url, type ->
                navController.navigate(Screen.MediaViewer.createRoute(url, type))
            }
        )
    }

    composable(
        route = Screen.MediaViewer.route,
        arguments = listOf(
            navArgument("url") { type = NavType.StringType },
            navArgument("type") { type = NavType.StringType; defaultValue = "image" }
        )
    ) { backStackEntry ->
        val url = backStackEntry.arguments?.getString("url") ?: ""
        val type = backStackEntry.arguments?.getString("type") ?: "image"
        MediaViewerScreen(
            url = url,
            type = type,
            onBack = { navController.popBackStack() }
        )
    }
}
