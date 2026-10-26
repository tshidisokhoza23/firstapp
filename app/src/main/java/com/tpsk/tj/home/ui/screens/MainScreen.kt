package com.tpsk.tj.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.tpsk.tj.common.ui.theme.*
import com.tpsk.tj.home.ui.viewmodel.MainViewModel

import com.tpsk.tj.ui.navigation.BottomNavItem
import com.tpsk.tj.ui.navigation.Screen
import com.tpsk.tj.ui.components.GlobalOverlayContainer
import com.tpsk.tj.music.ui.viewmodel.MusicViewModel
import com.tpsk.tj.home.ui.components.MoreShortcutsSheet

// Isolated Feature Tab Screens
import com.tpsk.tj.chat.ui.screens.ChatsListScreen
import com.tpsk.tj.media.ui.screens.MediaTabScreen
import com.tpsk.tj.media.ui.screens.shorts.ShortsTabScreen
import com.tpsk.tj.music.ui.screens.MusicTabScreen
import com.tpsk.tj.ride.ui.screens.RideTabScreen

import com.tpsk.tj.chat.ui.viewmodel.ChatViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    musicViewModel: MusicViewModel,
    navController: NavController,
    adManager: com.tpsk.tj.common.data.ads.AdManager,
    userViewModel: com.tpsk.tj.common.ui.viewmodel.UserViewModel = hiltViewModel(),
    onLogout: () -> Unit,
    initialSubTab: String? = null,
    initialShortId: String? = null
) {
    val selectedIndex by viewModel.selectedBottomItem.collectAsState()
    val currentTab = BottomNavItem.fromIndex(selectedIndex)
    val currentUser = com.tpsk.tj.LocalCurrentUser.current

    // Scoped ChatViewModel to sync state across the app flow
    val navHostController = navController as? androidx.navigation.NavHostController
    val chatViewModel: ChatViewModel = run {
        val entry = remember(navHostController) { 
            runCatching { navHostController?.getBackStackEntry("app_root") }.getOrNull() 
        }
        if (entry != null) hiltViewModel(entry) else hiltViewModel()
    }

    val areaPresenceViewModel: com.tpsk.tj.drive.ui.viewmodel.AreaPresenceViewModel = hiltViewModel()
    val appContext = LocalContext.current
    LaunchedEffect(Unit) {
        userViewModel.fetchCurrentUser()
        areaPresenceViewModel.start(appContext)
        com.tpsk.tj.common.util.GlobalNavigation.isBottomBarVisible.value = true
    }
    
    DisposableEffect(Unit) {
        onDispose {
            com.tpsk.tj.common.util.GlobalNavigation.isBottomBarVisible.value = false
        }
    }

    // Remember the last active tab before switching to "More" overlay (More index)
    val moreIndex = BottomNavItem.More.index
    val lastActiveTab = remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != moreIndex) {
            lastActiveTab.intValue = selectedIndex
        }
    }

    // Helper function to sync NavController with selected tab without circular duplicates
    val navigateToTab = remember(navController) {
        { item: BottomNavItem ->
            val tabRouteName = when(item) {
                BottomNavItem.Social -> "social"
                BottomNavItem.Scroll -> "scroll"
                BottomNavItem.Jobs -> "jobs"
                BottomNavItem.Mingle -> "mingle"
                BottomNavItem.Stream -> "stream"
                BottomNavItem.Ride -> "ride"
                else -> "home"
            }
            navController.navigate(Screen.Main.createRoute(tab = tabRouteName)) {
                popUpTo(navController.graph.startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val hasHistory = viewModel.hasTabHistory()
    BackHandler(enabled = initialShortId != null && initialShortId.isNotBlank() || (selectedIndex != 0 && selectedIndex != moreIndex) || (selectedIndex == moreIndex) || hasHistory) {
        if (initialShortId != null && initialShortId.isNotBlank()) {
            navController.popBackStack()
        } else if (selectedIndex == moreIndex) {
            // Dismiss "More" sheet overlay and return to prior tab
            viewModel.setSelectedBottomItem(lastActiveTab.intValue)
        } else {
            if (viewModel.popTabHistory()) {
                val newIndex = viewModel.selectedBottomItem.value
                val newItem = BottomNavItem.fromIndex(newIndex)
                navigateToTab(newItem)
            } else if (selectedIndex != 0) {
                viewModel.setSelectedBottomItem(0)
                navigateToTab(BottomNavItem.Chat)
            }
        }
    }

    val backgroundBrush = if (LocalIsDarkTheme.current) {
        Brush.verticalGradient(
            colors = listOf(AppBackgroundDarkTop, AppBackgroundDarkBottom)
        )
    } else {
         Brush.verticalGradient(
            colors = listOf(Color.White, AppBackgroundLight)
        )
    }

    GlobalOverlayContainer(musicViewModel = musicViewModel) {
        Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
            Scaffold(
                bottomBar = {
                    MainBottomNavigation(
                        selectedRoute = currentTab.route,
                        onTabSelected = { item: BottomNavItem ->
                            viewModel.setSelectedBottomItem(item.index)
                            if (item != BottomNavItem.More) {
                                navigateToTab(item)
                            }
                        }
                    )
                },
                containerColor = Color.Transparent
            ) { innerPadding ->
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val tabToDisplay = if (selectedIndex == moreIndex) {
                        BottomNavItem.fromIndex(lastActiveTab.intValue)
                    } else {
                        currentTab
                    }

                    // Crossfade provides clean UI transitions between active screens
                    Crossfade(
                        targetState = tabToDisplay, 
                        label = "TabSwitch",
                        modifier = Modifier.fillMaxSize()
                    ) { targetTab ->
                        when (targetTab) {
                            BottomNavItem.Chat -> ChatsListScreen(
                                navController = navController,
                                adManager = adManager,
                                bottomPadding = innerPadding.calculateBottomPadding(),
                                onLogout = onLogout,
                                chatViewModel = chatViewModel,
                                initialTab = initialSubTab
                            )
                            BottomNavItem.Social -> MediaTabScreen(
                                navController = navController,
                                adManager = adManager,
                                bottomPadding = innerPadding.calculateBottomPadding(),
                                chatViewModel = chatViewModel,
                                mediaViewModel = hiltViewModel(key = "social_feed")
                            )
                            BottomNavItem.Scroll -> ShortsTabScreen(
                                navController = navController,
                                adManager = adManager,
                                bottomPadding = innerPadding.calculateBottomPadding(),
                                onDirectMessagesClick = { navController.navigate(Screen.MediaMessages.route) },
                                initialShortId = initialShortId,
                                mediaViewModel = hiltViewModel(key = "shorts_feed")
                            )
                            BottomNavItem.Jobs -> com.tpsk.tj.chat.jobs.ui.screens.JobsHomeScreen(
                                navController = navController,
                                bottomPadding = innerPadding.calculateBottomPadding()
                            )
                            BottomNavItem.Mingle -> com.tpsk.tj.mingle.ui.MingleScreen(
                                navController = navController,
                                userViewModel = userViewModel,
                                bottomPadding = innerPadding.calculateBottomPadding()
                            )
                            BottomNavItem.Stream -> MusicTabScreen(
                                navController = navController,
                                bottomPadding = innerPadding.calculateBottomPadding()
                            )
                            BottomNavItem.Ride -> RideTabScreen(
                                navController = navController,
                                bottomPadding = innerPadding.calculateBottomPadding(),
                                chatViewModel = chatViewModel
                            )
                            else -> ChatsListScreen(
                                navController = navController,
                                adManager = adManager,
                                bottomPadding = innerPadding.calculateBottomPadding(),
                                onLogout = onLogout,
                                chatViewModel = chatViewModel,
                                initialTab = initialSubTab
                            )
                        }
                    }

                    // "More" Shortcuts Sheet Overlay
                    if (selectedIndex == moreIndex) {
                        MoreShortcutsSheet(
                            isVisible = true,
                            bottomPadding = innerPadding.calculateBottomPadding(),
                            onDismiss = { viewModel.setSelectedBottomItem(lastActiveTab.intValue) },
                            onNavigate = { route: String ->
                                viewModel.setSelectedBottomItem(lastActiveTab.intValue)
                                navController.navigate(route) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }

}

@Composable
fun MainBottomNavigation(
    selectedRoute: String,
    onTabSelected: (BottomNavItem) -> Unit
) {
    val isScrollTab = selectedRoute == BottomNavItem.Scroll.route
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            windowInsets = NavigationBarDefaults.windowInsets
        ) {
            for (item in BottomNavItem.items) {
                val isSelected = selectedRoute == item.route
                NavigationBarItem(
                    icon = { 
                        Icon(
                            item.icon, 
                            contentDescription = item.title, 
                            modifier = Modifier.size(24.dp)
                        ) 
                    },
                    label = { 
                        Text(
                            item.title, 
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        ) 
                    },
                    selected = isSelected,
                    onClick = { onTabSelected(item) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
            }
        }
    }
}
