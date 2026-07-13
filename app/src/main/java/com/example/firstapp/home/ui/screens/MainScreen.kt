package com.example.firstapp.home.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.firstapp.home.ui.viewmodel.MainViewModel
import com.example.firstapp.ui.navigation.BottomNavItem
import com.example.firstapp.ui.navigation.Screen
import com.example.firstapp.common.ui.theme.AppBackground
import com.example.firstapp.common.ui.theme.AppAccent
import com.example.firstapp.common.ui.theme.AppSecondary
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
    onLogout: () -> Unit
) {
    val selectedIndex by viewModel.selectedBottomItem.collectAsState()
    val currentTab = BottomNavItem.fromIndex(selectedIndex)
    
    // Remember the last active tab before switching to "More" overlay
    val lastActiveTab = remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != 5) {
            lastActiveTab.intValue = selectedIndex
        }
    }

    GlobalOverlayContainer(musicViewModel = musicViewModel) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AppBackground,
            bottomBar = {
                MainBottomNavigation(
                    selectedRoute = currentTab.route,
                    onTabSelected = { viewModel.setSelectedBottomItem(it.index) }
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier
                .padding(bottom = innerPadding.calculateBottomPadding())
                .fillMaxSize()
            ) {
                // Background Content (Last active tab)
                val tabToDisplay = if (selectedIndex == 5) {
                    BottomNavItem.fromIndex(lastActiveTab.intValue)
                } else {
                    currentTab
                }

                when (tabToDisplay) {
                    BottomNavItem.Chat -> ChatsListScreen(navController, onLogout = onLogout)
                    BottomNavItem.Media -> MediaTabScreen(navController)
                    BottomNavItem.Shorts -> ShortsTabScreen(
                        navController = navController,
                        onDirectMessagesClick = { navController.navigate(Screen.MediaMessages.route) }
                    )
                    BottomNavItem.Music -> MusicTabScreen(navController)
                    BottomNavItem.Drive -> DriveTabScreen(navController)
                    else -> ChatsListScreen(navController, onLogout = onLogout)
                }

                // The "More" Sheet Overlay - positioned precisely ABOVE the bottom nav
                // Using a custom scrim that only covers this Box (the content area)
                if (selectedIndex == 5) {
                    MoreShortcutsSheet(
                        isVisible = true,
                        onDismiss = { viewModel.setSelectedBottomItem(lastActiveTab.intValue) },
                        onNavigate = { route -> 
                            viewModel.setSelectedBottomItem(lastActiveTab.intValue)
                            navController.navigate(route) 
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
    NavigationBar(
        containerColor = AppSecondary,
        tonalElevation = 0.dp
    ) {
        val items = BottomNavItem.items
        var i = 0
        while (i < items.size) {
            val item = items.get(i)
            val isSelected = selectedRoute == item.route
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = isSelected,
                onClick = { onTabSelected(item) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppAccent,
                    selectedTextColor = AppAccent,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = AppAccent.copy(alpha = 0.1f)
                )
            )
            i++
        }
    }
}

@Composable
fun <T> DataStateHandler(
    resource: com.example.firstapp.common.util.Resource<T>,
    onLoading: @Composable (() -> Unit) = { },
    onEmpty: @Composable (() -> Unit) = { },
    onError: @Composable (String?) -> Unit = { _ -> },
    onSuccess: @Composable ((T) -> Unit)
) {
    com.example.firstapp.common.ui.components.DataStateHandler(
        resource = resource,
        onLoading = onLoading,
        onEmpty = onEmpty,
        onError = onError,
        onSuccess = onSuccess
    )
}
