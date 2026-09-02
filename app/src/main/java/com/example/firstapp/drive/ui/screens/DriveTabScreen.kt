package com.example.firstapp.drive.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.firstapp.drive.ui.viewmodel.DriveViewModel

import androidx.compose.ui.unit.dp
import com.example.firstapp.common.ui.viewmodel.UserViewModel
import com.example.firstapp.chat.ui.viewmodel.ChatViewModel

@Composable
fun DriveTabScreen(
    navController: NavController,
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
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
            onShowSettings = { /* Handle drawer/settings */ }
        )
    }
}
