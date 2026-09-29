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
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.BorderStroke
import com.tpsk.tj.common.ui.theme.*
import com.tpsk.tj.common.model.User

import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

import androidx.lifecycle.viewmodel.compose.viewModel
import com.tpsk.tj.settings.ui.viewmodel.SettingsViewModel

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit,
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToAccount: () -> Unit = {},
    onNavigateToChats: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onNavigateToLanguage: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToTerms: () -> Unit = {},
    onNavigateToDevices: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val user by viewModel.currentUser.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = onEditProfile) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
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
                .padding(horizontal = 16.dp)
        ) {
            // Profile Card (Modern/Glassmorphism feel)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val formattedAvatarUrl = remember(user?.avatarUrl) { com.tpsk.tj.common.util.MediaUtils.formatUrl(user?.avatarUrl) }

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!formattedAvatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = formattedAvatarUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = user?.displayName?.take(1)?.uppercase() ?: "U",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = user?.displayName ?: "User",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "@${user?.username ?: "username"}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )

                    val phoneVal = user?.phoneNumber?.takeIf { it.isNotBlank() }
                    val locVal = user?.location?.takeIf { it.isNotBlank() } ?: "Add location"
                    val phoneLocLine = listOfNotNull(phoneVal, locVal).joinToString(" • ")

                    if (phoneLocLine.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = phoneLocLine,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = user?.bio ?: "No bio set",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Settings Grid-ish Layout
            SettingsSectionTitle("Preferences")
            
            Row(modifier = Modifier.fillMaxWidth()) {
                ModernSettingsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Chat,
                    title = "Chats",
                    color = Color(0xFF64B5F6),
                    onClick = onNavigateToChats
                )
                Spacer(modifier = Modifier.width(12.dp))
                ModernSettingsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Notifications,
                    title = "Alerts",
                    color = Color(0xFFFFB74D),
                    onClick = onNavigateToNotifications
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                ModernSettingsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Lock,
                    title = "Security",
                    color = Color(0xFF81C784),
                    onClick = onNavigateToPrivacy
                )
                Spacer(modifier = Modifier.width(12.dp))
                ModernSettingsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DataUsage,
                    title = "Storage",
                    color = Color(0xFF4DB6AC),
                    onClick = onNavigateToStorage
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ModernSettingsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Devices,
                    title = "Sessions",
                    color = Color(0xFF9575CD),
                    onClick = onNavigateToDevices
                )
                Spacer(modifier = Modifier.width(12.dp))
                ModernSettingsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Language,
                    title = "Language",
                    color = Color(0xFFF06292),
                    onClick = onNavigateToLanguage
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Help Section
            SettingsSectionTitle("Support")
            Row(modifier = Modifier.fillMaxWidth()) {
                ModernSettingsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Filled.Help,
                    title = "Help Center",
                    color = Color(0xFFFFD54F),
                    onClick = onNavigateToHelp
                )
                Spacer(modifier = Modifier.width(12.dp))
                ModernSettingsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Description,
                    title = "Terms & EULA",
                    color = Color(0xFF26A69A),
                    onClick = onNavigateToTerms
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Management Section
            SettingsSectionTitle("Management")
            Row(modifier = Modifier.fillMaxWidth()) {
                ModernSettingsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AccountBox,
                    title = "Account",
                    color = Color(0xFF42A5F5),
                    onClick = onNavigateToAccount
                )
                Spacer(modifier = Modifier.width(12.dp))
                ModernSettingsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CleaningServices,
                    title = "Data & Cache",
                    color = Color(0xFF66BB6A),
                    onClick = onNavigateToStorage
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ActionRow(Icons.Default.Sync, "Refresh & Sync", MaterialTheme.colorScheme.primary) { showResetConfirm = true }
            ActionRow(Icons.AutoMirrored.Filled.Logout, "Log Out", MaterialTheme.colorScheme.onSurface) { onLogout() }
            ActionRow(Icons.Default.DeleteForever, "Terminate Account", Color(0xFFE57373).copy(alpha = 0.8f)) { showDeleteConfirm = true }

            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("FIRSTAPP ECOSYSTEM", color = MaterialTheme.colorScheme.onBackground.copy(0.2f), fontSize = 10.sp, letterSpacing = 4.sp)
                Text("v1.0.6-unique", color = MaterialTheme.colorScheme.onBackground.copy(0.1f), fontSize = 10.sp)
            }

            Spacer(modifier = Modifier.height(140.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Delete Account?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("This will permanently delete your account and all your data. This action cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = { 
                        showDeleteConfirm = false
                        viewModel.deleteAccount(onLogout)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Forever", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Reset and Sync?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("This will clear all currently cached data (chats, messages, etc.) and register your phone contacts as chats. Country codes will be handled automatically.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = { 
                        showResetConfirm = false
                        viewModel.resetAndSyncContacts(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Reset and Sync", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun QuickInfoBadge(title: String, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}

@Composable
fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun ModernSettingsCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ActionRow(icon: ImageVector, title: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f), fontSize = 15.sp)
    }
}
