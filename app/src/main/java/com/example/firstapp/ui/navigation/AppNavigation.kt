package com.example.firstapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.auth.ui.navigation.authNavGraph
import com.example.firstapp.chat.ui.navigation.chatsNav
import com.example.firstapp.vocab.ui.navigation.vocabNavGraph
import com.example.firstapp.media.ui.navigation.mediaNavGraph
import com.example.firstapp.chat.jobs.ui.navigation.jobsNavGraph
import com.example.firstapp.settings.ui.navigation.settingsNavGraph
import com.example.firstapp.profile.ui.navigation.profileNavGraph
import com.example.firstapp.home.ui.screens.MainScreen
import com.example.firstapp.home.ui.viewmodel.MainViewModel
import com.example.firstapp.home.ui.viewmodel.SearchViewModel
import com.example.firstapp.music.ui.viewmodel.MusicViewModel
import com.example.firstapp.chat.ui.viewmodel.ChatViewModel
import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import com.example.firstapp.media.ui.viewmodel.AdvertsViewModel
import com.example.firstapp.media.ui.viewmodel.ChannelsViewModel
import com.example.firstapp.media.ui.viewmodel.StoryViewModel
import com.example.firstapp.chat.jobs.ui.viewmodel.JobViewModel
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import com.example.firstapp.common.ui.viewmodel.FilesViewModel
import com.example.firstapp.common.ui.viewmodel.CalendarViewModel
import com.example.firstapp.ui.screens.more.FilesScreen
import com.example.firstapp.ui.screens.more.CalendarScreen
import com.example.firstapp.wallet.ui.screens.WalletScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    viewModel: MainViewModel,
    musicViewModel: MusicViewModel,
    adManager: com.example.firstapp.common.data.ads.AdManager,
    onLogout: () -> Unit
) {
    NavHost(navController = navController, startDestination = startDestination, route = "app_root") {
        authNavGraph(navController)
        
        composable(
            route = "main?tab={tab}&shortId={shortId}",
            arguments = listOf(
                navArgument("tab") { defaultValue = "" },
                navArgument("shortId") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val tabArg = backStackEntry.arguments?.getString("tab")
            val shortId = backStackEntry.arguments?.getString("shortId")
            
            LaunchedEffect(tabArg) {
                if (tabArg != null && tabArg.length > 0) {
                    val tabIndex = when(tabArg) {
                        "media" -> 1
                        "shorts" -> 2
                        "music" -> 3
                        "drive" -> 4
                        else -> 0
                    }
                    viewModel.setSelectedBottomItem(tabIndex)
                }
            }

            MainScreen(
                viewModel = viewModel,
                musicViewModel = musicViewModel,
                navController = navController,
                adManager = adManager,
                onLogout = onLogout,
                initialShortId = if (shortId != null && shortId.length > 0) shortId else null
            )
        }
        
        chatsNav(navController)
        vocabNavGraph(navController)
        mediaNavGraph(navController)
        jobsNavGraph(navController, viewModel)
        settingsNavGraph(navController, onLogout)
        profileNavGraph(navController, adManager)
        
        composable(Screen.Files.route) {
            val filesViewModel: FilesViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            FilesScreen(
                onBack = { navController.popBackStack() },
                viewModel = filesViewModel
            )
        }
        
        composable(Screen.Calendar.route) {
            val calendarViewModel: CalendarViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            CalendarScreen(
                onBack = { navController.popBackStack() },
                viewModel = calendarViewModel
            )
        }

        composable(Screen.Wallet.route) {
            val advertsViewModel: AdvertsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            WalletScreen(
                onBack = { navController.popBackStack() },
                advertsViewModel = advertsViewModel
            )
        }
    }
}
