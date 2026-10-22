package com.tpsk.tj.media.ui.navigation

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
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.chat.model.Contact
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tpsk.tj.media.ui.screens.*
import com.tpsk.tj.media.ui.viewmodel.*
import com.tpsk.tj.ui.navigation.Screen

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
        val userViewModel: com.tpsk.tj.common.ui.viewmodel.UserViewModel = hiltViewModel()
        val adManager = (navController.context as? com.tpsk.tj.MainActivity)?.adManager 
            ?: com.tpsk.tj.common.data.ads.AdMobManagerImpl() // Fallback

        val currentUser = com.tpsk.tj.LocalCurrentUser.current

        PostDetailScreen(
            postId = postId,
            mediaViewModel = mediaViewModel,
            adManager = adManager,
            ownProfileId = currentUser?.id.orEmpty(),
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
            },
            onRequestCreatePost = { replyId, quoteId ->
                navController.navigate(Screen.CreatePost.createRoute(replyId, quoteId))
            },
            onHashtagClick = { tag ->
                val clean = tag.removePrefix("#").trim()
                if (clean.isNotBlank()) navController.navigate(Screen.ContestFeed.createRoute(clean))
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
        val adManager = (navController.context as? com.tpsk.tj.MainActivity)?.adManager 
            ?: com.tpsk.tj.common.data.ads.AdMobManagerImpl()

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
                com.tpsk.tj.common.util.ShareUtils.sharePost(navController.context, post)
            },
            onHashtagClick = { tag ->
                val clean = tag.removePrefix("#").trim()
                if (clean.isNotBlank()) navController.navigate(Screen.ContestFeed.createRoute(clean))
            }
        )
    }

    composable(
        route = Screen.CreateShort.route,
        arguments = listOf(navArgument("videoUri") { type = NavType.StringType; defaultValue = "" })
    ) { backStackEntry ->
        val videoUriStr = backStackEntry.arguments?.getString("videoUri") ?: ""
        val videoUri = if (videoUriStr.isNotBlank()) android.net.Uri.parse(videoUriStr) else null

        com.tpsk.tj.media.ui.screens.shorts.CreateShortScreen(
            initialVideoUri = videoUri,
            onBackClick = { navController.popBackStack() },
            onPostSuccess = { navController.popBackStack() }
        )
    }

    composable(
        route = Screen.CreatePost.route,
        arguments = listOf(
            navArgument("replyId") { type = NavType.StringType; defaultValue = "" },
            navArgument("quoteId") { type = NavType.StringType; defaultValue = "" }
        )
    ) { backStackEntry ->
        val replyId = backStackEntry.arguments?.getString("replyId")?.takeIf { it.isNotBlank() }
        val quoteId = backStackEntry.arguments?.getString("quoteId")?.takeIf { it.isNotBlank() }
        val mediaViewModel: MediaViewModel = hiltViewModel()

        var replyPost by remember { mutableStateOf<com.tpsk.tj.media.model.Post?>(null) }
        var quotePost by remember { mutableStateOf<com.tpsk.tj.media.model.Post?>(null) }

        androidx.compose.runtime.LaunchedEffect(replyId, quoteId) {
            if (replyId != null) {
                mediaViewModel.getPostByIdFlow(replyId).collect { res ->
                    if (res is com.tpsk.tj.common.util.Resource.Success) replyPost = res.data
                }
            }
            if (quoteId != null) {
                mediaViewModel.getPostByIdFlow(quoteId).collect { res ->
                    if (res is com.tpsk.tj.common.util.Resource.Success) quotePost = res.data
                }
            }
        }

        com.tpsk.tj.media.ui.screens.CreatePostScreen(
            replyToPost = replyPost,
            quotingPost = quotePost,
            onDismiss = { navController.popBackStack() },
            onPostCreated = { navController.popBackStack() },
            mediaViewModel = mediaViewModel
        )
    }

    composable(route = Screen.SocialNotification.route) {
        SocialNotificationsScreen(
            onBackClick = { navController.popBackStack() },
            onPostClick = { postId -> navController.navigate(Screen.PostDetail.createRoute(postId)) },
            onUserProfileClick = { userId -> navController.navigate(Screen.UserProfile.createRoute(userId)) }
        )
    }

    composable(route = Screen.Circles.route) {
        com.tpsk.tj.chat.ui.screens.CirclesScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable(route = Screen.CreatorDashboard.route) {
        com.tpsk.tj.media.ui.screens.CreatorDashboardScreen(
            onBack = { navController.popBackStack() },
            onCreatePostClick = {
                navController.navigate(Screen.CreatePost.createRoute())
            }
        )
    }

    composable(route = Screen.ContestDiscovery.route) {
        com.tpsk.tj.ui.screens.contest.ContestDiscoveryScreen(
            onNavigateBack = { navController.popBackStack() },
            onSelectContest = { hashtag ->
                navController.navigate(Screen.ContestFeed.createRoute(hashtag))
            },
            onCreateContestClick = {
                navController.navigate(Screen.CreateContest.route)
            }
        )
    }

    composable(
        route = Screen.ContestFeed.route,
        arguments = listOf(navArgument("hashtag") { type = NavType.StringType })
    ) { backStackEntry ->
        val hashtag = backStackEntry.arguments?.getString("hashtag") ?: ""
        com.tpsk.tj.ui.screens.contest.ContestFeedScreen(
            hashtag = hashtag,
            onNavigateBack = { navController.popBackStack() },
            onPostClick = { postId ->
                navController.navigate(Screen.PostDetail.createRoute(postId))
            },
            onProfileClick = { userId ->
                if (userId.isNotBlank() && userId != "null") {
                    navController.navigate(Screen.UserProfile.createRoute(userId))
                }
            }
        )
    }

    composable(route = Screen.CreateContest.route) {
        com.tpsk.tj.ui.screens.contest.CreateContestScreen(
            onNavigateBack = { navController.popBackStack() },
            onContestCreated = { hashtag ->
                navController.navigate(Screen.ContestFeed.createRoute(hashtag)) {
                    popUpTo(Screen.ContestDiscovery.route)
                }
            }
        )
    }

    composable(route = Screen.TrendingFeed.route) {
        com.tpsk.tj.ui.screens.trending.TrendingFeedScreen(
            onNavigateBack = { navController.popBackStack() },
            onTopicClick = { topic ->
                val tag = if (topic.startsWith("#")) topic else "#$topic"
                navController.navigate(Screen.Main.createRoute(tab = "social", subTab = tag)) {
                    popUpTo(Screen.Main.route) { inclusive = false }
                }
            }
        )
    }
}
