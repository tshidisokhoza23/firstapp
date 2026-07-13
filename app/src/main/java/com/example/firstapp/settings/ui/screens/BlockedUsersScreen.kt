package com.example.firstapp.settings.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.firstapp.common.ui.theme.*
import com.example.firstapp.common.util.Resource
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.settings.ui.viewmodel.PrivacySettingsViewModel
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(onBack: () -> Unit, viewModel: PrivacySettingsViewModel = hiltViewModel()) {
    val blockedUsersResource by viewModel.blockedUsers.collectAsState()
    val blockedUsers = (blockedUsersResource as? Resource.Success<List<Contact>>)?.data ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Users", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Show contact picker to block */ },
                containerColor = AppAccent,
                contentColor = AppBackground,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Block new user")
            }
        },
        containerColor = AppBackground
    ) { padding ->
        if (blockedUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No blocked users", color = AppTextSecondary)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(blockedUsers) { user ->
                    BlockedUserItem(user) { viewModel.unblockUser(user.id) }
                }
            }
        }
    }
}

@Composable
fun BlockedUserItem(user: Contact, onUnblock: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AppSecondary),
            contentAlignment = Alignment.Center
        ) {
            if (user.avatarUrl != null) {
                AsyncImage(
                    model = user.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(user.displayName.take(1).uppercase(), color = Color.White)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.displayName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Text(user.phoneNumber ?: "", color = AppTextSecondary, fontSize = 13.sp)
        }
        TextButton(onClick = onUnblock) {
            Text("Unblock", color = AppAccent)
        }
    }
}
