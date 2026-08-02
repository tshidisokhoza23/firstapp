package com.example.firstapp.settings.ui.screens

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
import com.example.firstapp.settings.ui.viewmodel.PrivacySettingsViewModel
import com.example.firstapp.common.model.PrivacySettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onBack: () -> Unit, 
    onNavigateToBlockedUsers: () -> Unit = {},
    viewModel: PrivacySettingsViewModel = hiltViewModel()
) {
    val settingsResource by viewModel.settings.collectAsState()
    val settings = settingsResource.data ?: PrivacySettings()
    var showLastSeenDialog by remember { mutableStateOf(false) }
    var showPhoneNumberDialog by remember { mutableStateOf(false) }

    if (showLastSeenDialog) {
        PrivacyOptionDialog(
            title = "Last seen & online",
            currentOption = settings.show_last_seen,
            onOptionSelected = {
                viewModel.updateSettings(settings.copy(show_last_seen = it))
                showLastSeenDialog = false
            },
            onDismiss = { showLastSeenDialog = false }
        )
    }

    if (showPhoneNumberDialog) {
        PrivacyOptionDialog(
            title = "Phone number",
            currentOption = "everyone", // Default mock
            onOptionSelected = {
                showPhoneNumberDialog = false
            },
            onDismiss = { showPhoneNumberDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Security", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    PrivacyItemUnique(title = "Blocked Users", subtitle = "Manage blocked accounts", onClick = onNavigateToBlockedUsers)
                    PrivacyItemUnique(title = "Phone Number", subtitle = "Everyone", onClick = { showPhoneNumberDialog = true })
                    PrivacyItemUnique(title = "Last Seen & Online", subtitle = settings.show_last_seen.replace("_", " ").replaceFirstChar { it.uppercase() }, onClick = { showLastSeenDialog = true })
                    PrivacyItemUnique(title = "Profile Photos", subtitle = "Everyone")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Security", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp, bottom = 12.dp))
            
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    PrivacyItemUnique(title = "Passcode Lock", subtitle = "Off")
                    PrivacyItemUnique(title = "Two-Step Verification", subtitle = "Off")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            PrivacyToggleItemUnique(
                title = "Read receipts", 
                subtitle = "If turned off, you won't send or receive read receipts.", 
                checked = settings.show_read_receipts,
                onCheckedChange = { viewModel.updateSettings(settings.copy(show_read_receipts = it)) }
            )
        }
    }
}

@Composable
fun PrivacyItemUnique(title: String, subtitle: String, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    }
}

@Composable
fun PrivacyToggleItemUnique(
    title: String, 
    subtitle: String, 
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
fun PrivacyOptionDialog(
    title: String,
    currentOption: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Column {
                listOf("everyone", "my_contacts", "nobody").forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(option) }
                            .padding(vertical = 12.dp)
                    ) {
                        RadioButton(
                            selected = currentOption == option,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = option.replace("_", " ").replaceFirstChar { it.uppercase() },
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface
    )
}

