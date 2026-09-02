package com.example.firstapp.settings.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.common.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.settings.ui.viewmodel.NotificationSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit, viewModel: NotificationSettingsViewModel = hiltViewModel()) {
    val privateChatsEnabled by viewModel.privateChatsEnabled.collectAsState()
    val groupsEnabled by viewModel.groupsEnabled.collectAsState()
    val channelsEnabled by viewModel.channelsEnabled.collectAsState()
    val soundsEnabled by viewModel.soundsEnabled.collectAsState()
    val vibrateEnabled by viewModel.vibrateEnabled.collectAsState()
    val previewsEnabled by viewModel.previewsEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.onBackground)
            )
        },
        bottomBar = {
            com.example.firstapp.common.ui.components.BannerAdView(
                modifier = Modifier.navigationBarsPadding()
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Message Notifications",
                modifier = Modifier.padding(16.dp),
                color = AppAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            NotificationSwitch("Private Chats", "Enabled", privateChatsEnabled) { viewModel.setPrivateChatsEnabled(it) }
            NotificationSwitch("Groups", "Enabled", groupsEnabled) { viewModel.setGroupsEnabled(it) }
            NotificationSwitch("Channels", "Enabled", channelsEnabled) { viewModel.setChannelsEnabled(it) }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = AppSecondary)

            Text(
                "General",
                modifier = Modifier.padding(16.dp),
                color = AppAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            NotificationSwitch("Notification Sounds", "Play sounds for incoming messages", soundsEnabled) { viewModel.setSoundsEnabled(it) }
            NotificationSwitch("Vibrate", "Vibrate for incoming messages", vibrateEnabled) { viewModel.setVibrateEnabled(it) }
            NotificationSwitch("Message Previews", "Show preview text in notifications", previewsEnabled) { viewModel.setPreviewsEnabled(it) }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun NotificationSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = AppAccent)
        )
    }
}
