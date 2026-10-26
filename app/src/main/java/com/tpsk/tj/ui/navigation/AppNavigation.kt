package com.tpsk.tj.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tpsk.tj.auth.ui.navigation.authNavGraph
import com.tpsk.tj.chat.ui.navigation.chatsNav
import com.tpsk.tj.vocab.ui.navigation.vocabNavGraph
import com.tpsk.tj.media.ui.navigation.mediaNavGraph
import com.tpsk.tj.chat.jobs.ui.navigation.jobsNavGraph
import com.tpsk.tj.settings.ui.navigation.settingsNavGraph
import com.tpsk.tj.profile.ui.navigation.profileNavGraph
import com.tpsk.tj.home.ui.screens.MainScreen
import com.tpsk.tj.home.ui.viewmodel.MainViewModel
import com.tpsk.tj.home.ui.viewmodel.SearchViewModel
import com.tpsk.tj.music.ui.viewmodel.MusicViewModel
import com.tpsk.tj.chat.ui.viewmodel.ChatViewModel
import com.tpsk.tj.media.ui.viewmodel.MediaViewModel
import com.tpsk.tj.chat.ui.viewmodel.AdvertsViewModel
import com.tpsk.tj.chat.jobs.ui.viewmodel.JobViewModel
import com.tpsk.tj.common.ui.viewmodel.UserViewModel
import com.tpsk.tj.common.ui.viewmodel.FilesViewModel
import com.tpsk.tj.common.ui.viewmodel.CalendarViewModel
import com.tpsk.tj.ui.screens.more.FilesScreen
import com.tpsk.tj.ui.screens.more.CalendarScreen
import com.tpsk.tj.wallet.ui.screens.WalletScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String,
    viewModel: MainViewModel,
    musicViewModel: MusicViewModel,
    adManager: com.tpsk.tj.common.data.ads.AdManager,
    onLogout: () -> Unit
) {
    NavHost(navController = navController, startDestination = startDestination, route = "app_root") {
        authNavGraph(navController)
        
        composable(
            route = "main?tab={tab}&subTab={subTab}&shortId={shortId}",
            arguments = listOf(
                navArgument("tab") { defaultValue = "" },
                navArgument("subTab") { defaultValue = "" },
                navArgument("shortId") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val tabArg = backStackEntry.arguments?.getString("tab")
            val subTabArg = backStackEntry.arguments?.getString("subTab")
            val shortId = backStackEntry.arguments?.getString("shortId")
            
            LaunchedEffect(tabArg) {
                if (!tabArg.isNullOrEmpty()) {
                    val tabIndex = when(tabArg) {
                        "social" -> 1
                        "scroll" -> 2
                        "jobs" -> 3
                        "mingle" -> 4
                        "stream" -> 5
                        "ride" -> 6
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
                initialSubTab = if (subTabArg != null && subTabArg.length > 0) subTabArg else null,
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

        composable(Screen.Stream.route) {
            com.tpsk.tj.music.ui.screens.MusicTabScreen(
                navController = navController,
                bottomPadding = 0.dp
            )
        }

        composable(Screen.Ride.route) {
            val parentEntry = remember(navController) { 
                runCatching { navController.getBackStackEntry("app_root") }.getOrNull() 
            }
            val chatVm: ChatViewModel = if (parentEntry != null) androidx.hilt.navigation.compose.hiltViewModel(parentEntry) else androidx.hilt.navigation.compose.hiltViewModel()
            com.tpsk.tj.ride.ui.screens.RideTabScreen(
                navController = navController,
                bottomPadding = 0.dp,
                chatViewModel = chatVm
            )
        }

        composable(Screen.Drive.route) {
            val parentEntry = remember(navController) {
                runCatching { navController.getBackStackEntry("app_root") }.getOrNull()
            }
            val chatVm: ChatViewModel = if (parentEntry != null) androidx.hilt.navigation.compose.hiltViewModel(parentEntry) else androidx.hilt.navigation.compose.hiltViewModel()
            com.tpsk.tj.drive.ui.screens.DriveTabScreen(
                navController = navController,
                bottomPadding = 0.dp,
                chatViewModel = chatVm,
                role = "driver"
            )
        }

        composable(Screen.News.route) {
            com.tpsk.tj.media.ui.screens.NewsScreen(
                onBack = { navController.popBackStack() },
                onArticleClick = { article ->
                    navController.navigate(
                        Screen.NewsWebView.createRoute(
                            url = article.originalUrl,
                            title = article.title,
                            source = article.sourceName
                        )
                    )
                },
                onCommentsClick = { article ->
                    navController.navigate(
                        Screen.DiscussionThread.createRoute(
                            postId = article.id,
                            postTitle = article.title,
                            postSubtitle = "${article.sourceName} • ${article.commentCount} Comments",
                            authorName = article.sourceName,
                            authorDistance = article.category,
                            authorTime = article.timeAgo,
                            postContent = article.title
                        )
                    )
                }
            )
        }

        composable(Screen.NewsWebView.route) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            val title = backStackEntry.arguments?.getString("title") ?: ""
            val source = backStackEntry.arguments?.getString("source") ?: ""

            com.tpsk.tj.media.ui.screens.NewsWebViewScreen(
                url = url,
                title = title,
                source = source,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Events.route) {
            com.tpsk.tj.media.ui.screens.EventsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DiscussionThread.route) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            val postTitle = backStackEntry.arguments?.getString("postTitle") ?: ""
            val postSubtitle = backStackEntry.arguments?.getString("postSubtitle") ?: ""
            val authorName = backStackEntry.arguments?.getString("authorName") ?: ""
            val authorDistance = backStackEntry.arguments?.getString("authorDistance") ?: ""
            val authorTime = backStackEntry.arguments?.getString("authorTime") ?: ""
            val postContent = backStackEntry.arguments?.getString("postContent") ?: ""
            val authorId = backStackEntry.arguments?.getString("authorId") ?: ""

            com.tpsk.tj.chat.nearby.ui.DiscussionThreadScreen(
                postId = postId,
                postTitle = postTitle,
                postSubtitle = postSubtitle,
                authorName = authorName,
                authorDistance = authorDistance,
                authorTime = authorTime,
                postContent = postContent,
                authorId = authorId,
                onBack = { navController.popBackStack() },
                onOpenProfile = { userId ->
                    if (userId.isNotBlank()) navController.navigate(Screen.UserProfile.createRoute(userId))
                }
            )
        }

        composable(Screen.CreateNearbyPost.route) { backStackEntry ->
            val area = backStackEntry.arguments?.getString("area") ?: ""
            com.tpsk.tj.chat.nearby.ui.CreateNearbyPostScreen(
                initialArea = area,
                onBack = { navController.popBackStack() },
                onPostCreated = { navController.popBackStack() }
            )
        }

        composable(Screen.MyNearbyPosts.route) {
            com.tpsk.tj.chat.nearby.ui.MyNearbyPostsScreen(
                onBack = { navController.popBackStack() },
                onOpenPost = { post ->
                    navController.navigate(
                        Screen.DiscussionThread.createRoute(
                            postId = post.id,
                            postTitle = "Your Nearby Post",
                            postSubtitle = post.coarseAreaName,
                            authorName = post.authorName,
                            authorDistance = "You",
                            authorTime = "",
                            postContent = post.content,
                            authorId = post.userId
                        )
                    )
                }
            )
        }

        composable(Screen.ChatRequests.route) {
            com.tpsk.tj.chat.ui.screens.ChatRequestsScreen(
                navController = navController,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
