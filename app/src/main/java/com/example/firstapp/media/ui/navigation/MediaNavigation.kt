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
import com.example.firstapp.common.util.Resource
import com.example.firstapp.chat.model.Contact
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.firstapp.media.ui.screens.*
import com.example.firstapp.media.ui.viewmodel.*
import com.example.firstapp.ui.navigation.Screen

fun NavGraphBuilder.mediaNavGraph(navController: NavHostController) {
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
            onBack = { navController.popBackStack() },
            onAvatarClick = { avatarUrl ->
                if (avatarUrl != null) {
                    navController.navigate(Screen.MediaViewer.createRoute(avatarUrl, "image", 0L))
                } else {
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                }
            }
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

        val currentUser = com.example.firstapp.LocalCurrentUser.current

        PostDetailScreen(
            postId = postId,
            mediaViewModel = mediaViewModel,
            adManager = adManager,
            ownProfileId = currentUser.id,
            onBack = { 
                mediaViewModel.selectPost(null)
                navController.popBackStack() 
            },
            onProfileClick = { userId ->
                navController.navigate(Screen.UserProfile.createRoute(userId))
            },
            onMediaClick = { url, type, pos ->
                navController.navigate(Screen.MediaViewer.createRoute(url, type, pos))
            },
            onReplyDetailClick = { replyId ->
                navController.navigate(Screen.PostDetail.createRoute(replyId))
            }
        )
    }

    composable(
        route = Screen.MediaViewer.route,
        arguments = listOf(
            navArgument("url") { type = NavType.StringType },
            navArgument("type") { type = NavType.StringType; defaultValue = "image" },
            navArgument("position") { type = NavType.LongType; defaultValue = 0L }
        )
    ) { backStackEntry ->
        val url = backStackEntry.arguments?.getString("url") ?: ""
        val type = backStackEntry.arguments?.getString("type") ?: "image"
        val position = backStackEntry.arguments?.getLong("position") ?: 0L
        MediaViewerScreen(
            url = url,
            type = type,
            initialPosition = position,
            onBack = { navController.popBackStack() }
        )
    }

    composable(route = Screen.SavedPosts.route) {
        val mediaViewModel: MediaViewModel = hiltViewModel()
        val adManager = (navController.context as? com.example.firstapp.MainActivity)?.adManager 
            ?: com.example.firstapp.common.data.ads.AdMobManagerImpl()

        SavedPostsScreen(
            onBack = { navController.popBackStack() },
            adManager = adManager,
            mediaViewModel = mediaViewModel,
            onPostClick = { postId ->
                navController.navigate(Screen.PostDetail.createRoute(postId))
            },
            onProfileClick = { userId ->
                navController.navigate(Screen.UserProfile.createRoute(userId))
            },
            onMediaClick = { url, type, pos ->
                navController.navigate(Screen.MediaViewer.createRoute(url, type, pos))
            },
            onShareClick = { post ->
                mediaViewModel.sharePost(post.id, post.shares)
                com.example.firstapp.common.util.ShareUtils.sharePost(navController.context, post)
            }
        )
    }
}
