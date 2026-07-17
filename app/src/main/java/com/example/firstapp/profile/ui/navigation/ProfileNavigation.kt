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
import com.example.firstapp.media.ui.viewmodel.shorts.ShortsViewModel
import com.example.firstapp.chat.ui.viewmodel.ChatViewModel
import com.example.firstapp.ui.navigation.Screen
import com.example.firstapp.media.ui.screens.shorts.ShortsProfileScreen
import kotlin.collections.listOf

fun NavGraphBuilder.profileNavGraph(navController: NavHostController) {
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
        val chatViewModel: ChatViewModel = hiltViewModel()
        val sharedMediaViewModel: SharedMediaViewModel = hiltViewModel()
        
        val pagedPosts = mediaViewModel.getUserPosts(userId).collectAsLazyPagingItems()

        ProfileView(
            userId = userId,
            isGroup = isGroup,
            onBack = { navController.popBackStack() },
            posts = pagedPosts,
            userViewModel = userViewModel,
            mediaViewModel = mediaViewModel,
            chatViewModel = chatViewModel,
            sharedMediaViewModel = sharedMediaViewModel,
            onNavigateToSharedMedia = { id -> navController.navigate(Screen.SharedMedia.createRoute(id)) },
            onNavigateToChat = { id -> 
                navController.navigate(Screen.DirectChat.createRoute(id)) {
                    popUpTo(Screen.UserProfile.route) { inclusive = true }
                }
            },
            onNavigateToEditExperience = { id ->
                navController.navigate(Screen.EditWorkExperience.createRoute(id))
            },
            onNavigateToEditProfile = {
                navController.navigate(Screen.EditProfile.route)
            }
        )
    }

    composable(
        route = Screen.ShortsProfile.route,
        arguments = listOf(
            navArgument("userId") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
        val shortsViewModel: ShortsViewModel = hiltViewModel()
        val userViewModel: UserViewModel = hiltViewModel()
        val mediaViewModel: MediaViewModel = hiltViewModel()
        val mainViewModel: com.example.firstapp.home.ui.viewmodel.MainViewModel = hiltViewModel(
            navController.getBackStackEntry(Screen.Main.route)
        )
        
        ShortsProfileScreen(
            userId = userId,
            shortsViewModel = shortsViewModel,
            userViewModel = userViewModel,
            mediaViewModel = mediaViewModel,
            onBack = { navController.popBackStack() },
            onShortClick = { short -> 
                shortsViewModel.setSelectedShortsUserId(userId)
                mainViewModel.setSelectedBottomItem(2)
                shortsViewModel.setSelectedShortId(short.id)
                navController.navigate("main?tab=shorts")
            },
            onMessageClick = { id ->
                navController.navigate(Screen.DirectChat.createRoute(id))
            }
        )
    }
}
