package com.example.firstapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.firstapp.model.Post
import com.example.firstapp.model.User
import com.example.firstapp.ui.theme.*
import com.example.firstapp.ui.viewmodel.MainViewModel
import com.example.firstapp.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileView(
    onBack: () -> Unit,
    posts: List<Post>,
    viewModel: MainViewModel
) {
    val user by viewModel.currentUser.collectAsState()
    var isEditing by remember { mutableStateOf(false) }
    
    var displayName by remember(user) { mutableStateOf(user?.display_name ?: "") }
    var bio by remember(user) { mutableStateOf(user?.bio ?: "") }
    var location by remember(user) { mutableStateOf(user?.location ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (isEditing) {
                            viewModel.updateProfile(displayName, bio, location, null)
                            isEditing = false
                        } else {
                            isEditing = true
                        }
                    }) {
                        Icon(if (isEditing) Icons.Default.Edit else Icons.Default.Edit, contentDescription = "Edit", tint = if (isEditing) AppAccent else Color.White)
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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                ProfileHeader(
                    user = user,
                    isEditing = isEditing,
                    displayName = displayName,
                    bio = bio,
                    location = location,
                    onDisplayNameChange = { displayName = it },
                    onBioChange = { bio = it },
                    onLocationChange = { location = it }
                )
            }
            
            item {
                Text(
                    "Your Posts",
                    modifier = Modifier.padding(16.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            
            val userPosts = posts.filter { it.authorHandle == user?.username }
            items(userPosts) { post ->
                PostItem(
                    post = post,
                    onClick = {},
                    onLike = { viewModel.likePost(post.id) },
                    onRepost = { viewModel.repostPost(post.id) },
                    onBookmark = { viewModel.bookmarkPost(post.id) },
                    onProfileClick = {}
                )
                HorizontalDivider(color = AppSecondary.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun ProfileHeader(
    user: User?,
    isEditing: Boolean,
    displayName: String,
    bio: String,
    location: String,
    onDisplayNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onLocationChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = AppSecondary,
                border = BorderStroke(2.dp, AppAccent)
            ) {
                if (user?.avatar_url != null) {
                    AsyncImage(
                        model = user.avatar_url,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            if (isEditing) {
                Surface(
                    modifier = Modifier.size(32.dp).clickable { /* Pick Image */ },
                    shape = CircleShape,
                    color = AppAccent,
                    tonalElevation = 4.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isEditing) {
            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppAccent,
                    unfocusedBorderColor = AppTextSecondary,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = onBioChange,
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppAccent,
                    unfocusedBorderColor = AppTextSecondary,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        } else {
            Text(
                text = user?.display_name ?: "Name",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "@${user?.username ?: "username"}",
                color = AppTextSecondary,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = user?.bio ?: "No bio set",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
    }
}
