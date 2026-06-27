package com.example.firstapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.auth.ui.navigation.authNavGraph
import com.example.firstapp.chat.ui.navigation.chatNavGraph
import com.example.firstapp.vocab.ui.navigation.vocabNavGraph
import com.example.firstapp.media.ui.navigation.mediaNavGraph
import com.example.firstapp.jobs.ui.navigation.jobsNavGraph
import com.example.firstapp.settings.ui.navigation.settingsNavGraph
import com.example.firstapp.profile.ui.navigation.profileNavGraph
import com.example.firstapp.home.ui.screens.MainScreen
import com.example.firstapp.home.ui.viewmodel.MainViewModel
import com.example.firstapp.home.ui.viewmodel.SearchViewModel
import com.example.firstapp.music.ui.viewmodel.MusicViewModel
import com.example.firstapp.chat.ui.viewmodel.ChatViewModel
import com.example.firstapp.media.ui.viewmodel.MediaViewModel
import com.example.firstapp.media.ui.viewmodel.ShortsViewModel
import com.example.firstapp.media.ui.viewmodel.AdvertsViewModel
import com.example.firstapp.media.ui.viewmodel.ChannelsViewModel
import com.example.firstapp.media.ui.viewmodel.StoryViewModel
import com.example.firstapp.jobs.ui.viewmodel.JobViewModel
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
    onLogout: () -> Unit
) {
    NavHost(navController = navController, startDestination = startDestination) {
        authNavGraph(navController)
        
        composable(
            route = "main?tab={tab}",
            arguments = listOf(navArgument("tab") { defaultValue = "home" })
        ) { backStackEntry ->
            val initialTab = backStackEntry.arguments?.getString("tab") ?: "home"
            val chatViewModel: ChatViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val mediaViewModel: MediaViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val shortsViewModel: ShortsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val jobViewModel: JobViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val advertsViewModel: AdvertsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val channelsViewModel: ChannelsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val searchViewModel: SearchViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val userViewModel: UserViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val storyViewModel: StoryViewModel = androidx.hilt.navigation.compose.hiltViewModel()

            MainScreen(
                viewModel = viewModel,
                chatViewModel = chatViewModel,
                mediaViewModel = mediaViewModel,
                shortsViewModel = shortsViewModel,
                advertsViewModel = advertsViewModel,
                channelsViewModel = channelsViewModel,
                searchViewModel = searchViewModel,
                jobViewModel = jobViewModel,
                userViewModel = userViewModel,
                musicViewModel = musicViewModel,
                storyViewModel = storyViewModel,
                navController = navController,
                onLogout = onLogout,
                initialTab = initialTab
            )
        }
        
        chatNavGraph(navController)
        vocabNavGraph(navController)
        mediaNavGraph(navController)
        jobsNavGraph(navController, viewModel)
        settingsNavGraph(navController, onLogout)
        profileNavGraph(navController)
        
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
