package com.tpsk.tj.drive.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tpsk.tj.drive.ui.viewmodel.DriveViewModel
import com.tpsk.tj.ui.navigation.Screen

import androidx.compose.ui.unit.dp
import com.tpsk.tj.common.ui.viewmodel.UserViewModel
import com.tpsk.tj.chat.ui.viewmodel.ChatViewModel

@Composable
fun DriveTabScreen(
    navController: NavController,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    role: String = "driver",
    driveViewModel: DriveViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    Box(modifier = Modifier.fillMaxSize()) {
        DriveScreen(
            bottomPadding = bottomPadding,
            viewModel = driveViewModel,
            userViewModel = userViewModel,
            chatViewModel = chatViewModel,
            role = role,
            onShowSettings = { navController.navigate(Screen.Settings.route) }
        )
    }
}
