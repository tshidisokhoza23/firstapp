@file:OptIn(ExperimentalMaterial3Api::class)

package com.tpsk.tj.chat.nearby.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tpsk.tj.LocalCurrentUser
import com.tpsk.tj.chat.nearby.model.NearbyPost
import com.tpsk.tj.chat.nearby.ui.viewmodel.NearbyViewModel

@Composable
fun MyNearbyPostsScreen(
    onBack: () -> Unit,
    onOpenPost: (NearbyPost) -> Unit = {},
    nearbyViewModel: NearbyViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by nearbyViewModel.uiState.collectAsState()
    val myPosts = uiState.myPosts

    LaunchedEffect(Unit) {
        nearbyViewModel.loadMyPosts()
    }

    var postToDelete by remember { mutableStateOf<NearbyPost?>(null) }
    var postToEdit by remember { mutableStateOf<NearbyPost?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Posts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (uiState.myPostsLoading && myPosts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF00B4D8))
            }
        } else if (uiState.myPostsError != null && myPosts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.myPostsError ?: "Could not load your posts", textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { nearbyViewModel.loadMyPosts() }) { Text("Retry") }
                }
            }
        } else if (myPosts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "You haven't posted anything nearby yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Questions, local announcements, and items you post will appear here for management.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(myPosts, key = { it.id }) { post ->
                    MyPostCard(
                        post = post,
                        onOpen = { onOpenPost(post) },
                        onDelete = { postToDelete = post },
                        onToggleResolved = {
                            nearbyViewModel.markPostResolved(post.id, !post.isResolved) { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        },
                        onEdit = { postToEdit = post }
                    )
                }
            }
        }
    }

    // Confirmation Dialog for Deleting
    postToDelete?.let { post ->
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text("Delete Post") },
            text = { Text("Are you sure you want to delete this nearby post?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        nearbyViewModel.deletePost(
                            post.id,
                            onSuccess = {
                                postToDelete = null
                                Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Post Dialog
    postToEdit?.let { post ->
        var editContent by remember { mutableStateOf(post.content) }

        AlertDialog(
            onDismissRequest = { postToEdit = null },
            title = { Text("Edit Post") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { if (it.length <= 500) editContent = it },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = post.coarseAreaName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editContent.isNotBlank()) {
                            nearbyViewModel.editPost(
                                post.id,
                                editContent,
                                post.coarseAreaName,
                                onSuccess = {
                                    postToEdit = null
                                    Toast.makeText(context, "Post updated", Toast.LENGTH_SHORT).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            )
                        }
                    }
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { postToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MyPostCard(
    post: NearbyPost,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onToggleResolved: () -> Unit,
    onEdit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Location Tag
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF00B4D8).copy(alpha = 0.12f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF00B4D8), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(post.coarseAreaName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00B4D8))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Status Badge ("Active" vs "Resolved")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (post.isResolved) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFFF9800).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = post.statusBadge.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (post.isResolved) Color(0xFF4CAF50) else Color(0xFFFF9800),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = post.content,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (post.imageUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(post.imageUrls) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Card Action Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onToggleResolved,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (post.isResolved) Color.Gray else Color(0xFF4CAF50)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (post.isResolved) Icons.Default.Replay else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (post.isResolved) Color.Gray else Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (post.isResolved) "Reopen" else "Mark Resolved",
                        fontSize = 12.sp,
                        color = if (post.isResolved) Color.Gray else Color(0xFF4CAF50)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF00B4D8), modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
