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
import com.example.firstapp.settings.ui.viewmodel.ChatSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsScreen(onBack: () -> Unit, viewModel: ChatSettingsViewModel = hiltViewModel()) {
    val fontSize by viewModel.fontSize.collectAsState()
    val enterIsSend by viewModel.enterIsSend.collectAsState()
    val mediaVisibility by viewModel.mediaVisibility.collectAsState()
    val animationsEnabled by viewModel.animationsEnabled.collectAsState()
    val autoNightMode by viewModel.autoNightMode.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chats", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, titleContentColor = MaterialTheme.colorScheme.onBackground)
            )
        },
        bottomBar = {
            com.example.firstapp.media.ui.components.BannerAdView()
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Display",
                modifier = Modifier.padding(16.dp),
                color = AppAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Font size", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                Slider(
                    value = fontSize,
                    onValueChange = { viewModel.setFontSize(it) },
                    valueRange = 12f..24f,
                    colors = SliderDefaults.colors(thumbColor = AppAccent, activeTrackColor = AppAccent)
                )
            }

            ChatSettingsSwitch(
                title = "Animations",
                subtitle = "Enable UI animations",
                checked = animationsEnabled,
                onCheckedChange = { viewModel.setAnimationsEnabled(it) }
            )

            ChatSettingsSwitch(
                title = "Auto night mode",
                subtitle = "Switch to dark theme based on system",
                checked = autoNightMode,
                onCheckedChange = { viewModel.setAutoNightMode(it) }
            )

            if (!autoNightMode) {
                ChatSettingsSwitch(
                    title = "Dark mode",
                    subtitle = "Manually enable dark theme",
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.setIsDarkMode(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = AppSecondary)

            Text(
                "Chat Settings",
                modifier = Modifier.padding(16.dp),
                color = AppAccent,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )

            ChatSettingsSwitch(
                title = "Enter is send",
                subtitle = "Enter key will send your message",
                checked = enterIsSend,
                onCheckedChange = { viewModel.setEnterIsSend(it) }
            )

            ChatSettingsSwitch(
                title = "Media visibility",
                subtitle = "Show newly downloaded media in your phone's gallery",
                checked = mediaVisibility,
                onCheckedChange = { viewModel.setMediaVisibility(it) }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = AppSecondary)

            TextButton(
                onClick = { viewModel.clearAllChats() },
                modifier = Modifier.padding(16.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red)
                Spacer(Modifier.width(8.dp))
                Text("Clear all chats", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun ChatSettingsSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
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
