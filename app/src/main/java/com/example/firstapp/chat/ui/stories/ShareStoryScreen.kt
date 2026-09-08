package com.example.firstapp.chat.ui.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.firstapp.common.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareStoryScreen(
    content: String,
    url: String,
    type: String,
    isPosting: Boolean = false,
    onBack: () -> Unit,
    onPost: (content: String, url: String, type: String, privacy: String) -> Unit
) {
    var selectedPrivacy by remember { mutableStateOf("Everyone") }
    var allowScreenshots by remember { mutableStateOf(true) }
    var postToProfile by remember { mutableStateOf(true) }
    var selectedAlbum by remember { mutableStateOf("All Stories") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Story", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isPosting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Download logic */ }, enabled = !isPosting) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = {
                        val privacy = when (selectedPrivacy) {
                            "Everyone" -> "everyone"
                            "Friends" -> "close_friends"
                            "Selected" -> "selected"
                            else -> "contacts"
                        }
                        onPost(content, url, type, privacy)
                    },
                    enabled = !isPosting,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppAccent),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(
                            color = AppBackground,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Post Story", color = AppBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    color = Color(0xFF1C1C1E),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Share story",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Choose who can view your story",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                PrivacyOption(
                    title = "Everyone",
                    subtitle = "exclude people >",
                    icon = Icons.Default.Campaign,
                    iconColor = Color(0xFF2196F3),
                    selected = selectedPrivacy == "Everyone",
                    onClick = { selectedPrivacy = "Everyone" }
                )
                PrivacyOption(
                    title = "My Contacts",
                    subtitle = "exclude people >",
                    icon = Icons.Default.Person,
                    iconColor = Color(0xFF9C27B0),
                    selected = selectedPrivacy == "Contacts",
                    onClick = { selectedPrivacy = "Contacts" }
                )
                PrivacyOption(
                    title = "Close Friends",
                    subtitle = "edit list >",
                    icon = Icons.Default.Star,
                    iconColor = Color(0xFF4CAF50),
                    selected = selectedPrivacy == "Friends",
                    onClick = { selectedPrivacy = "Friends" }
                )
                PrivacyOption(
                    title = "Selected Users",
                    subtitle = "edit list >",
                    icon = Icons.Default.Group,
                    iconColor = Color(0xFFFF9800),
                    selected = selectedPrivacy == "Selected",
                    onClick = { selectedPrivacy = "Selected" }
                )
                
                Text(
                    "Select people who will never see your stories.",
                    color = Color(0xFF2196F3),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                
                ToggleOption(
                    title = "Allow Screenshots",
                    checked = allowScreenshots,
                    onCheckedChange = { allowScreenshots = it }
                )
                ToggleOption(
                    title = "Post to My Profile",
                    checked = postToProfile,
                    onCheckedChange = { postToProfile = it }
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Album", color = Color.White)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { /* Select album */ }
                    ) {
                        Text(selectedAlbum, color = Color(0xFF2196F3))
                        Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF2196F3))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Keep this story on your profile even after it expires in 24 hours. Privacy settings will apply.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun PrivacyOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2196F3), unselectedColor = Color.Gray)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = iconColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color(0xFF2196F3), fontSize = 12.sp)
        }
    }
}

@Composable
fun ToggleOption(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.White)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2196F3),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Black
            )
        )
    }
}
