package com.example.firstapp.settings.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firstapp.common.ui.components.DeveloperDiagnosticsSheet
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.settings.ui.viewmodel.HelpSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSettingsScreen(
    onBack: () -> Unit,
    viewModel: HelpSettingsViewModel = hiltViewModel()
) {
    val appVersion by viewModel.appVersion.collectAsState()
    
    var tapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var showDeveloperDiagnostics by remember { mutableStateOf(false) }

    val onAppInfoClick = {
        if (com.example.firstapp.BuildConfig.ENABLE_DEV_SWITCH) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTapTime < 3000) {
                tapCount++
                if (tapCount >= 5) {
                    showDeveloperDiagnostics = true
                    tapCount = 0
                }
            } else {
                tapCount = 1
            }
            lastTapTime = currentTime
        }
    }

    if (showDeveloperDiagnostics) {
        DeveloperDiagnosticsSheet(
            envManager = viewModel.envManager,
            onDismiss = { showDeveloperDiagnostics = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
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
            com.example.firstapp.media.ui.components.BannerAdView(
                modifier = Modifier.navigationBarsPadding()
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            HelpItem(Icons.AutoMirrored.Filled.Help, "Help Center", "Get answers, contact us")
            HelpItem(Icons.Default.Group, "Contact us", "Questions? Need help?")
            HelpItem(Icons.Default.Description, "Terms and Privacy Policy", "")
            HelpItem(Icons.Default.Info, "App info", "Version $appVersion", onClick = onAppInfoClick)

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun HelpItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = AppTextSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, color = AppTextSecondary, fontSize = 14.sp)
            }
        }
    }
}
