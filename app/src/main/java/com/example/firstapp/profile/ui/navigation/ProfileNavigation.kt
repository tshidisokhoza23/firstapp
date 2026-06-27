package com.example.firstapp.profile.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.firstapp.chat.ui.viewmodel.ChatViewModel
import com.example.firstapp.ui.navigation.Screen

fun NavGraphBuilder.profileNavGraph(navController: NavHostController) {
    composable(Screen.EditProfile.route) {
        val userViewModel: UserViewModel = hiltViewModel()
        EditProfileScreen(
            onBack = { navController.popBackStack() },
            userViewModel = userViewModel
        )
    }

    composable(
        route = Screen.UserProfile.route,
        arguments = listOf(
            navArgument("userId") { type = NavType.IntType },
            navArgument("isGroup") { type = NavType.BoolType; defaultValue = false }
        )
    ) { backStackEntry ->
        val userId = backStackEntry.arguments?.getInt("userId") ?: return@composable
        val isGroup = backStackEntry.arguments?.getBoolean("isGroup") ?: false
        val userViewModel: UserViewModel = hiltViewModel()
        val mediaViewModel: MediaViewModel = hiltViewModel()
        val chatViewModel: ChatViewModel = hiltViewModel()
        val sharedMediaViewModel: SharedMediaViewModel = hiltViewModel()
        
        val uiState by mediaViewModel.uiState.collectAsState()
        val pagedPosts = uiState.posts.collectAsLazyPagingItems()

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
                navController.navigate(Screen.Chat.createRoute(id)) {
                    popUpTo(Screen.UserProfile.route) { inclusive = true }
                }
            }
        )
    }
}
