package com.example.firstapp.ui.screens

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
import com.example.firstapp.ui.theme.*

import com.example.firstapp.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: User?,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onEditProfile: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppSecondary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Profile Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditProfile() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AppAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user?.display_name?.take(1)?.uppercase() ?: "U",
                        color = AppBackground,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user?.display_name ?: "User", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(user?.bio ?: "Hey there! I am using FirstApp.", color = AppTextSecondary, fontSize = 14.sp)
                }
                Icon(Icons.Default.QrCode, contentDescription = null, tint = AppAccent)
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))

            // Settings Items
            SettingsItem(Icons.Default.Key, "Account", "Security notifications, change number")
            SettingsItem(Icons.Default.Lock, "Privacy", "Block contacts, disappearing messages")
            SettingsItem(Icons.Default.Face, "Avatar", "Create, edit, profile photo")
            SettingsItem(Icons.Default.Chat, "Chats", "Theme, wallpapers, chat history")
            SettingsItem(Icons.Default.Notifications, "Notifications", "Message, group & call tones")
            SettingsItem(Icons.Default.DataUsage, "Storage and Data", "Network usage, auto-download")
            SettingsItem(Icons.Default.Language, "App Language", "English (phone's language)")
            SettingsItem(Icons.AutoMirrored.Filled.Help, "Help", "Help center, contact us, privacy policy")
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLogout() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.Red)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Log out", color = Color.Red, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("from", color = AppTextSecondary, fontSize = 12.sp)
                Text("FIRSTAPP", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Handle click */ }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(24.dp))
        Column {
            Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = AppTextSecondary, fontSize = 13.sp)
        }
    }
}
