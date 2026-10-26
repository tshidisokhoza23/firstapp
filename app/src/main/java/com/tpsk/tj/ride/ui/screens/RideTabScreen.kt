package com.tpsk.tj.ride.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tpsk.tj.chat.ui.viewmodel.ChatViewModel
import com.tpsk.tj.drive.ui.viewmodel.DriveViewModel
import com.tpsk.tj.ui.navigation.Screen

@Composable
fun RideTabScreen(
    navController: NavController,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    rideViewModel: DriveViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    Box(modifier = Modifier.fillMaxSize()) {
        RideScreen(
            bottomPadding = bottomPadding,
            viewModel = rideViewModel,
            chatViewModel = chatViewModel,
            onShowSettings = { navController.navigate(Screen.Settings.route) }
        )
    }
}
