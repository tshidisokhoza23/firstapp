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
import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import com.example.firstapp.media.ui.viewmodel.SharedMediaViewModel
import com.example.firstapp.ui.navigation.Screen
import kotlin.collections.listOf

fun NavGraphBuilder.profileNavGraph(
    navController: NavHostController,
    adManager: com.example.firstapp.common.data.ads.AdManager
) {
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
        val userId = backStackEntry.arguments?.getString("userId")?.let { 
            if (it == "null" || it.isBlank()) null else it 
        } ?: return@composable
        
        val isGroup = backStackEntry.arguments?.getBoolean("isGroup") ?: false
        val userViewModel: UserViewModel = hiltViewModel()
        val mediaViewModel: MediaViewModel = hiltViewModel()
        val sharedMediaViewModel: SharedMediaViewModel = hiltViewModel()
        
        // Ensure we use a stable ID for the paging items, default to current user if requested ID is effectively 'me'
        val currentUser by userViewModel.currentUser.collectAsState()
        val effectiveId = remember(userId, currentUser?.id) {
            userId ?: currentUser?.id ?: ""
        }

        val pagedPosts = remember(effectiveId, mediaViewModel) {
            mediaViewModel.getUserPosts(effectiveId, "standard")
        }.collectAsLazyPagingItems()
        
        val pagedShorts = remember(effectiveId, mediaViewModel) {
            mediaViewModel.getUserPosts(effectiveId, "short")
        }.collectAsLazyPagingItems()
        
        val pagedMedia = remember(effectiveId, mediaViewModel) {
            mediaViewModel.getUserPosts(effectiveId, "media")
        }.collectAsLazyPagingItems()
        
        val pagedReplies = remember(effectiveId, mediaViewModel) {
            mediaViewModel.getUserPosts(effectiveId, "reply")
        }.collectAsLazyPagingItems()

        ProfileView(
            userId = effectiveId,
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
            onRequireLogin = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onNavigateToEditProfile = {
                navController.navigate(Screen.EditProfile.route)
            },
            onShortClick = { short ->
                navController.navigate(Screen.Main.createRoute(tab = "shorts", shortId = short.id))
            },
            onNavigateToMediaViewer = { url, type ->
                navController.navigate(Screen.MediaViewer.createRoute(url, type))
            },
            onNavigateToPostDetail = { postId ->
                navController.navigate(Screen.PostDetail.createRoute(postId))
            }
        )
    }

}
