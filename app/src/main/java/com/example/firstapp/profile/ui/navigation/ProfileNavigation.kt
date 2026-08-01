package com.example.firstapp.profile.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.*
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.firstapp.profile.ui.screens.*
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import com.example.firstapp.common.domain.auth.SessionManager
import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import com.example.firstapp.media.ui.viewmodel.SharedMediaViewModel
import com.example.firstapp.ui.navigation.Screen
import javax.inject.Inject

fun NavGraphBuilder.profileNavGraph(
    navController: NavHostController,
    adManager: com.example.firstapp.common.data.ads.AdManager
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
        route = Screen.UserProfile.route,
        arguments = listOf(
            navArgument("userId") { type = NavType.StringType },
            navArgument("isGroup") { type = NavType.BoolType; defaultValue = false }
        )
    ) { backStackEntry ->
        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
        val isGroup = backStackEntry.arguments?.getBoolean("isGroup") ?: false
        
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
            onShortClick = { short ->
                navController.navigate(Screen.Main.createRoute(tab = "shorts", shortId = short.id))
            },
            onNavigateToMediaViewer = { url, type, pos ->
                navController.navigate(Screen.MediaViewer.createRoute(url, type, pos))
            },
            onNavigateToPostDetail = { postId ->
                navController.navigate(Screen.PostDetail.createRoute(postId))
            }
        )
    }

}
