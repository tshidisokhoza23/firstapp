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
        val sharedMediaViewModel: SharedMediaViewModel = hiltViewModel()
        
        val pagedPosts = mediaViewModel.getUserPosts(userId).collectAsLazyPagingItems()

        ProfileView(
            userId = userId,
            isGroup = isGroup,
            onBack = { navController.popBackStack() },
            posts = pagedPosts,
            userViewModel = userViewModel,
            mediaViewModel = mediaViewModel,
            sharedMediaViewModel = sharedMediaViewModel,
            onNavigateToSharedMedia = { id -> navController.navigate(Screen.SharedMedia.createRoute(id)) },
            onRequireLogin = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            },
            onNavigateToEditExperience = { id ->
                navController.navigate(Screen.EditWorkExperience.createRoute(id))
            },
            onNavigateToEditProfile = {
                navController.navigate(Screen.EditProfile.route)
            },
            onShortClick = { short ->
                navController.navigate(Screen.Main.createRoute(tab = "shorts", shortId = short.id))
            }
        )
    }

}
