package com.example.firstapp.home.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.firstapp.home.ui.viewmodel.MainViewModel
import com.example.firstapp.ui.navigation.BottomNavItem
import com.example.firstapp.ui.navigation.Screen
import com.example.firstapp.ui.components.GlobalOverlayContainer
import com.example.firstapp.music.ui.viewmodel.MusicViewModel
import com.example.firstapp.home.ui.components.MoreShortcutsSheet

// Isolated Feature Tab Screens
import com.example.firstapp.chat.ui.screens.ChatsListScreen
import com.example.firstapp.media.ui.screens.MediaTabScreen
import com.example.firstapp.media.ui.screens.shorts.ShortsTabScreen
import com.example.firstapp.music.ui.screens.MusicTabScreen
import com.example.firstapp.drive.ui.screens.DriveTabScreen

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    musicViewModel: MusicViewModel,
    navController: NavController,
    adManager: com.example.firstapp.common.data.ads.AdManager,
    userViewModel: com.example.firstapp.common.ui.viewmodel.UserViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onLogout: () -> Unit,
    initialShortId: String? = null
) {
    val selectedIndex by viewModel.selectedBottomItem.collectAsState()
    val currentTab = BottomNavItem.fromIndex(selectedIndex)
    val currentUser = com.example.firstapp.LocalCurrentUser.current

    LaunchedEffect(Unit) {
        userViewModel.fetchCurrentUser()
    }

    // Remember the last active tab before switching to "More" overlay (index 5)
    val lastActiveTab = remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != 5) {
            lastActiveTab.intValue = selectedIndex
        }
    }

    // Helper function to sync NavController with selected tab without circular duplicates
    val navigateToTab = remember(navController) {
        { item: BottomNavItem ->
            val tabRouteName = when(item) {
                BottomNavItem.Media -> "media"
                BottomNavItem.Shorts -> "shorts"
                BottomNavItem.Music -> "music"
                BottomNavItem.Drive -> "drive"
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
    BackHandler(enabled = (selectedIndex != 0 && selectedIndex != 5) || (selectedIndex == 5) || hasHistory) {
        if (selectedIndex == 5) {
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

    GlobalOverlayContainer(musicViewModel = musicViewModel) {
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
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                val tabToDisplay = if (selectedIndex == 5) {
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
                            onLogout = onLogout
                        )
                        BottomNavItem.Media -> MediaTabScreen(
                            navController = navController,
                            adManager = adManager
                        )
                        BottomNavItem.Shorts -> ShortsTabScreen(
                            navController = navController,
                            adManager = adManager,
                            onDirectMessagesClick = { navController.navigate(Screen.MediaMessages.route) },
                            initialShortId = initialShortId
                        )
                        BottomNavItem.Music -> MusicTabScreen(
                            navController = navController
                        )
                        BottomNavItem.Drive -> DriveTabScreen(
                            navController = navController
                        )
                        else -> ChatsListScreen(
                            navController = navController,
                            adManager = adManager,
                            onLogout = onLogout
                        )
                    }
                }

                // "More" Shortcuts Sheet Overlay
                if (selectedIndex == 5) {
                    MoreShortcutsSheet(
                        isVisible = true,
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

@Composable
fun MainBottomNavigation(
    selectedRoute: String,
    onTabSelected: (BottomNavItem) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        Column {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)
            ) {
                for (item in BottomNavItem.items) {
                    val isSelected = selectedRoute == item.route
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title, modifier = Modifier.size(22.dp)) },
                        label = { Text(item.title, fontSize = 10.sp) },
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
}
