package com.tpsk.tj.profile.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.*
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.paging.compose.collectAsLazyPagingItems
import com.tpsk.tj.profile.ui.screens.*
import com.tpsk.tj.common.ui.viewmodel.UserViewModel
import com.tpsk.tj.common.domain.auth.SessionManager
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel
import com.tpsk.tj.media.ui.viewmodel.SharedMediaViewModel
import com.tpsk.tj.ui.navigation.Screen
import javax.inject.Inject

fun NavGraphBuilder.profileNavGraph(
    navController: NavHostController,
    adManager: com.tpsk.tj.common.data.ads.AdManager
) {
    // ... previous composables ...
    composable(Screen.EditProfile.route) {
        val userViewModel: UserViewModel = hiltViewModel()
        EditProfileScreen(
            onBack = { navController.popBackStack() },
            userViewModel = userViewModel
        )
    }

    composable(
        route = Screen.EditWorkExperience.route,
        arguments = listOf(navArgument("experienceId") { type = NavType.StringType; nullable = true; defaultValue = "" })
    ) { backStackEntry ->
        val experienceId = backStackEntry.arguments?.getString("experienceId")?.let { if (it.isBlank()) null else it }
        val userViewModel: UserViewModel = hiltViewModel()
        EditWorkExperienceScreen(
            experienceId = experienceId,
            onBack = { navController.popBackStack() },
            viewModel = userViewModel
        )
    }

    composable(
        route = Screen.FollowList.route,
        arguments = listOf(
            navArgument("userId") { type = NavType.StringType },
            navArgument("type") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
        val type = backStackEntry.arguments?.getString("type") ?: "followers"
        val userViewModel: UserViewModel = hiltViewModel()
        FollowListScreen(
            userId = userId,
            type = type,
            userViewModel = userViewModel,
            onBack = { navController.popBackStack() },
            onUserClick = { targetUserId ->
                navController.navigate(Screen.UserProfile.createRoute(targetUserId))
            }
        )
    }

    composable(
        route = Screen.UserProfile.route,
        arguments = listOf(
            navArgument("userId") { type = NavType.StringType },
            navArgument("isGroup") { type = NavType.BoolType; defaultValue = false },
            navArgument("tab") { type = NavType.StringType; nullable = true; defaultValue = "Posts" }
        )
    ) { backStackEntry ->
        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
        val isGroup = backStackEntry.arguments?.getBoolean("isGroup") ?: false
        val initialTab = backStackEntry.arguments?.getString("tab") ?: "Posts"
        
        val userViewModel: UserViewModel = hiltViewModel()
        val mediaViewModel: MediaViewModel = hiltViewModel()
        val sharedMediaViewModel: SharedMediaViewModel = hiltViewModel()
        
        val pagedPosts = remember(userId, mediaViewModel) {
            mediaViewModel.getUserPosts(userId, "standard")
        }.collectAsLazyPagingItems()
        
        val pagedShorts = remember(userId, mediaViewModel) {
            mediaViewModel.getUserPosts(userId, "short")
        }.collectAsLazyPagingItems()
        
        val pagedMedia = remember(userId, mediaViewModel) {
            mediaViewModel.getUserPosts(userId, "media")
        }.collectAsLazyPagingItems()
        
        val pagedReplies = remember(userId, mediaViewModel) {
            mediaViewModel.getUserPosts(userId, "reply")
        }.collectAsLazyPagingItems()

        ProfileView(
            userId = userId,
            isGroup = isGroup,
            initialTab = initialTab,
            adManager = adManager,
            onBack = { navController.popBackStack() },
            posts = pagedPosts,
            shorts = pagedShorts,
            mediaItems = pagedMedia,
            replies = pagedReplies,
            userViewModel = userViewModel,
            mediaViewModel = mediaViewModel,
            sharedMediaViewModel = sharedMediaViewModel,
            onNavigateToSharedMedia = { id -> navController.navigate(Screen.SharedMedia.createRoute(id)) },
            onNavigateToEditProfile = {
                navController.navigate(Screen.EditProfile.route)
            },
            onNavigateToSearch = {
                navController.navigate(Screen.Main.createRoute(tab = "social"))
            },
            onNavigateToNotifications = {
                navController.navigate(Screen.NotificationSettings.route)
            },
            onNavigateToMore = {
                navController.navigate(Screen.Settings.route)
            },
            onShortClick = { short ->
                navController.navigate(Screen.Main.createRoute(tab = "scroll", shortId = short.id))
            },
            onNavigateToMediaViewer = { url, type, pos ->
                navController.navigate(Screen.MediaViewer.createRoute(url, type, pos))
            },
            onNavigateToPostDetail = { postId ->
                navController.navigate(Screen.PostDetail.createRoute(postId))
            },
            onNavigateToFollowList = { userId, type ->
                navController.navigate(Screen.FollowList.createRoute(userId, type))
            }
        )
    }

}
