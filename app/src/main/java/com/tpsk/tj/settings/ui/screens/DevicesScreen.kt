package com.tpsk.tj.settings.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Smartphone
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
import com.tpsk.tj.common.ui.theme.*
import com.tpsk.tj.settings.ui.viewmodel.DevicesViewModel
import com.tpsk.tj.settings.ui.viewmodel.DeviceSession

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    onBack: () -> Unit,
    viewModel: DevicesViewModel = hiltViewModel()
) {
    val sessions by viewModel.sessions.collectAsState()
    val note by viewModel.note.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Sessions", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppBackground
                )
            )
        },
        bottomBar = {
            com.tpsk.tj.common.ui.components.BannerAdView(
                modifier = Modifier.navigationBarsPadding()
            )
        },
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(AppSecondary.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Smartphone, contentDescription = null, tint = AppAccent, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("This phone", color = AppAccent, fontWeight = FontWeight.Bold)
                }
            }

            val currentSession = sessions.find { it.isCurrent }
            val otherSessions = sessions.filter { !it.isCurrent }

            if (currentSession != null) {
                Text("This Device", color = AppAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                DeviceItem(
                    icon = Icons.Default.Smartphone,
                    name = currentSession.deviceName,
                    details = "${currentSession.location}",
                    status = currentSession.lastActive,
                    isCurrent = true
                )
            }

            if (otherSessions.isNotEmpty()) {
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 8.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.revokeOthers() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Terminate All Other Sessions", color = Color.Red, fontSize = 16.sp)
                }
                
                Text("Logs out all devices except for this one.", color = AppTextSecondary, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))

                HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 8.dp)

                Text("Other Sessions", color = AppAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                
                otherSessions.forEach { session ->
                    DeviceItem(
                        icon = Icons.Default.Smartphone,
                        name = session.deviceName,
                        details = "${session.location}",
                        status = session.lastActive,
                        onClick = { viewModel.terminateSession(session.id) }
                    )
                }
            }

            if (note.isNotEmpty()) {
                Text(note, color = AppTextSecondary, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun DeviceItem(
    icon: ImageVector, 
    name: String, 
    details: String, 
    status: String, 
    isCurrent: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (!isCurrent) onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.05f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (isCurrent) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(" (this device)", color = AppAccent, fontSize = 12.sp)
                }
            }
            Text(details, color = AppTextSecondary, fontSize = 13.sp)
            Text(status, color = if (status == "Active now") AppAccent else AppTextSecondary, fontSize = 13.sp)
        }
    }
}
