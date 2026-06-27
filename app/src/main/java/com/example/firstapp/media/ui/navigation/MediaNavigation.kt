package com.example.firstapp.media.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel

import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.firstapp.media.model.*
import com.example.firstapp.media.ui.screens.*
import com.example.firstapp.media.ui.viewmodel.*
import com.example.firstapp.ui.navigation.Screen
import com.example.firstapp.common.util.Resource

fun NavGraphBuilder.mediaNavGraph(navController: NavHostController) {
    composable(
        route = Screen.Story.route,
        arguments = listOf(
            navArgument("userId") { type = NavType.IntType; defaultValue = -1 },
            navArgument("showComposer") { type = NavType.BoolType; defaultValue = false }
        )
    ) { backStackEntry ->
        val initialUserId = backStackEntry.arguments?.getInt("userId") ?: -1
        val showComposer = backStackEntry.arguments?.getBoolean("showComposer") ?: false
        val storyViewModel: StoryViewModel = hiltViewModel()
        val userViewModel: com.example.firstapp.common.ui.viewmodel.UserViewModel = hiltViewModel()
        val storiesState = storyViewModel.stories.collectAsState(Resource.Loading()).value
        val currentUser by userViewModel.currentUser.collectAsState()
        val postResult by storyViewModel.postResult.collectAsState()
        val context = androidx.compose.ui.platform.LocalContext.current
        
        androidx.compose.runtime.LaunchedEffect(postResult) {
            postResult?.let {
                if (it is Resource.Success) {
                    Toast.makeText(context, "Status update shared", Toast.LENGTH_SHORT).show()
                    storyViewModel.clearPostResult()
                } else if (it is Resource.Error) {
                    Toast.makeText(context, it.message ?: "Failed to share status", Toast.LENGTH_LONG).show()
                    storyViewModel.clearPostResult()
                }
            }
        }

        StoryScreen(
            stories = storiesState.data ?: emptyList(),
            currentUserId = currentUser?.id,
            initialViewingUserId = if (initialUserId != -1) initialUserId else null,
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
        arguments = listOf(navArgument("channelId") { type = NavType.IntType })
    ) { backStackEntry ->
        val channelDetailViewModel: ChannelDetailViewModel = hiltViewModel()
        val channelId = backStackEntry.arguments?.getInt("channelId") ?: return@composable
        androidx.compose.runtime.LaunchedEffect(channelId) { channelDetailViewModel.loadChannel(channelId) }
        val channelState by channelDetailViewModel.channel.collectAsState()
        
        channelState.data?.let { channel ->
            ChannelDetailScreen(
                channel = channel,
                onBack = { navController.popBackStack() },
                onSubscribe = { channelDetailViewModel.subscribe(it) },
                onMute = { id, muted -> channelDetailViewModel.mute(id, muted) }
            )
        }
    }
    
    composable(
        route = Screen.AdvertDetail.route,
        arguments = listOf(navArgument("advertId") { type = NavType.IntType })
    ) { backStackEntry ->
        val advertDetailViewModel: AdvertDetailViewModel = hiltViewModel()
        val advertId = backStackEntry.arguments?.getInt("advertId") ?: return@composable
        androidx.compose.runtime.LaunchedEffect(advertId) { advertDetailViewModel.loadAdvert(advertId) }
        val advertState by advertDetailViewModel.advert.collectAsState()

        advertState.data?.let { advert ->
            AdvertDetailScreen(
                advert = advert,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
