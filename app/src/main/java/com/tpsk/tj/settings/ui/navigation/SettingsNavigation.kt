package com.tpsk.tj.settings.ui.navigation

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.tpsk.tj.settings.ui.screens.*
import com.tpsk.tj.settings.ui.viewmodel.*
import com.tpsk.tj.ui.navigation.Screen

fun NavGraphBuilder.settingsNavGraph(navController: NavHostController, onLogout: () -> Unit) {
    composable(Screen.Settings.route) {
        SettingsScreen(
            onBack = { navController.popBackStack() },
            onEditProfile = { navController.navigate(Screen.EditProfile.route) },
            onLogout = onLogout,
            onNavigateToPrivacy = { navController.navigate(Screen.PrivacySettings.route) },
            onNavigateToAccount = { navController.navigate(Screen.AccountSettings.route) },
            onNavigateToChats = { navController.navigate(Screen.ChatSettings.route) },
            onNavigateToNotifications = { navController.navigate(Screen.NotificationSettings.route) },
            onNavigateToStorage = { navController.navigate(Screen.StorageSettings.route) },
            onNavigateToLanguage = { navController.navigate(Screen.LanguageSettings.route) },
            onNavigateToHelp = { navController.navigate(Screen.HelpSettings.route) },
            onNavigateToDevices = { navController.navigate(Screen.DevicesSettings.route) }
        )
    }

    composable(Screen.PrivacySettings.route) {
        val privacyViewModel: PrivacySettingsViewModel = hiltViewModel()
        PrivacySettingsScreen(
            onBack = { navController.popBackStack() },
            onNavigateToBlockedUsers = { navController.navigate(Screen.BlockedUsers.route) },
            onNavigateToStoryPrivacy = { navController.navigate(Screen.StoryPrivacy.route) },
            viewModel = privacyViewModel
        )
    }

    composable(Screen.StoryPrivacy.route) {
        val privacyViewModel: PrivacySettingsViewModel = hiltViewModel()
        StoryPrivacySelectionScreen(
            onBack = { navController.popBackStack() },
            viewModel = privacyViewModel
        )
    }

    composable(Screen.BlockedUsers.route) {
        val privacyViewModel: PrivacySettingsViewModel = hiltViewModel()
        BlockedUsersScreen(
            onBack = { navController.popBackStack() },
            viewModel = privacyViewModel
        )
    }

    composable(Screen.AccountSettings.route) {
        val accountViewModel: AccountSettingsViewModel = hiltViewModel()
        AccountSettingsScreen(
            onBack = { navController.popBackStack() },
            viewModel = accountViewModel
        )
    }

    composable(Screen.ChatSettings.route) {
        val chatSettingsViewModel: ChatSettingsViewModel = hiltViewModel()
        ChatSettingsScreen(
            onBack = { navController.popBackStack() },
            viewModel = chatSettingsViewModel
        )
    }

    composable(Screen.NotificationSettings.route) {
        val notificationViewModel: NotificationSettingsViewModel = hiltViewModel()
        NotificationSettingsScreen(
            onBack = { navController.popBackStack() },
            onNavigateToChat = { chatId ->
                navController.navigate(Screen.Chat.createRoute(chatId, "mingle"))
            },
            viewModel = notificationViewModel
        )
    }

    composable(Screen.MingleNotification.route) {
        com.tpsk.tj.mingle.ui.MingleNotificationScreen(
            navController = navController,
            onBack = { navController.popBackStack() }
        )
    }

    composable(Screen.MingleChats.route) {
        com.tpsk.tj.mingle.ui.MingleChatsScreen(
            navController = navController,
            onBack = { navController.popBackStack() }
        )
    }

    composable(Screen.StorageSettings.route) {
        val storageViewModel: StorageSettingsViewModel = hiltViewModel()
        StorageSettingsScreen(
            onBack = { navController.popBackStack() },
            viewModel = storageViewModel
        )
    }

    composable(Screen.HelpSettings.route) {
        val helpViewModel: HelpSettingsViewModel = hiltViewModel()
        HelpSettingsScreen(
            onBack = { navController.popBackStack() },
            viewModel = helpViewModel
        )
    }

    composable(Screen.LanguageSettings.route) {
        val languageViewModel: LanguageSettingsViewModel = hiltViewModel()
        LanguageSettingsScreen(
            onBack = { navController.popBackStack() },
            viewModel = languageViewModel
        )
    }

    composable(Screen.DevicesSettings.route) {
        val devicesViewModel: DevicesViewModel = hiltViewModel()
        DevicesScreen(
            onBack = { navController.popBackStack() },
            viewModel = devicesViewModel
        )
    }
}
