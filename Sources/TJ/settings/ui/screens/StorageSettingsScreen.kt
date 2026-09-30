package com.tpsk.tj.settings.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tpsk.tj.common.ui.theme.*
import com.tpsk.tj.settings.ui.viewmodel.StorageSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettingsScreen(
    onBack: () -> Unit,
    viewModel: StorageSettingsViewModel = hiltViewModel()
) {
    val autoplayGifs by viewModel.autoplayGifs.collectAsState()
    val autoplayVideos by viewModel.autoplayVideos.collectAsState()
    val streamingVideos by viewModel.streamingVideos.collectAsState()
    val storageUsage by viewModel.storageUsage.collectAsState()
    val networkUsage by viewModel.networkUsage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data & Resource", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            com.tpsk.tj.common.ui.components.BannerAdView(
                modifier = Modifier.navigationBarsPadding()
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Usage Metrics", color = AppAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp, bottom = 12.dp))
            
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.03f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    StorageSettingItemUnique("Storage Analytics", "$storageUsage local cache") {
                        viewModel.clearCache()
                    }
                    StorageSettingItemUnique("Network Traffic", "$networkUsage total usage")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Auto-Download Rules", color = AppAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp, bottom = 12.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.03f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    StorageSettingItemUnique("On Mobile Data", "Photos only")
                    StorageSettingItemUnique("On Wi-Fi", "All resources")
                    StorageSettingItemUnique("While Roaming", "Disabled")
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text("Media Playback", color = AppAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp, bottom = 12.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.03f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    StorageSwitchItemUnique("Auto-play GIFs", "Visual enhancement", autoplayGifs) { viewModel.setAutoplayGifs(it) }
                    StorageSwitchItemUnique("Auto-play Videos", "Immediate playback", autoplayVideos) { viewModel.setAutoplayVideos(it) }
                    StorageSwitchItemUnique("Smart Streaming", "Bandwidth optimization", streamingVideos) { viewModel.setStreamingVideos(it) }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun StorageSettingItemUnique(title: String, subtitle: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    }
}

@Composable
fun StorageSwitchItemUnique(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppAccent,
                checkedTrackColor = AppAccent.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Gray.copy(alpha = 0.2f)
            )
        )
    }
}
